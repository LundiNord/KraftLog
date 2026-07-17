<?php

declare(strict_types=1);

namespace OCA\KraftLog\Service;

use InvalidArgumentException;
use OCP\DB\QueryBuilder\IQueryBuilder;
use OCP\IDBConnection;
use OutOfBoundsException;
use Throwable;

final class KraftLogService {
    private const EXERCISES = 'kl_exercises';
    private const ROUTINES = 'kl_routines';
    private const ROUTINE_ITEMS = 'kl_routine_items';
    private const SESSIONS = 'kl_sessions';
    private const SETS = 'kl_sets';
    private const RUNS = 'kl_runs';
    private const BOULDERS = 'kl_boulders';
    private const WEIGHTS = 'kl_weights';

    private const CATEGORIES = [
        'STRENGTH',
        'CARDIO',
        'FLEXIBILITY',
        'PLYOMETRICS',
        'CALISTHENICS',
    ];

    private const MUSCLES = [
        'CHEST',
        'BACK',
        'SHOULDERS',
        'BICEPS',
        'TRICEPS',
        'FOREARMS',
        'CORE',
        'GLUTES',
        'QUADRICEPS',
        'HAMSTRINGS',
        'CALVES',
        'FULL_BODY',
    ];

    private const SESSION_TYPES = ['STRENGTH', 'RUNNING', 'BOULDERING'];

    private int $transactionDepth = 0;
    private bool $deferRoutineRefresh = false;

    /** @var array<string, true> */
    private array $pendingRoutineRefresh = [];

    public function __construct(
        private IDBConnection $db,
    ) {
    }

    public function deleteUserData(string $userId): void {
        $this->atomic(function () use ($userId): void {
            foreach ([
                self::SETS,
                self::RUNS,
                self::BOULDERS,
                self::ROUTINE_ITEMS,
                self::SESSIONS,
                self::ROUTINES,
                self::EXERCISES,
                self::WEIGHTS,
            ] as $table) {
                $this->deleteRows($table, ['user_id' => $userId]);
            }
        });
    }

    /**
     * Return the complete user-owned application state. KraftLog is intentionally
     * local to a Nextcloud account; every query is scoped by user_id.
     *
     * @return array<string, mixed>
     */
    public function getState(string $userId): array {
        $exerciseRows = $this->rows(self::EXERCISES, ['user_id' => $userId], [
            ['name', 'ASC'],
        ]);
        $exercises = array_map($this->formatExercise(...), $exerciseRows);

        $routineRows = $this->rows(self::ROUTINES, ['user_id' => $userId], [
            ['created_at', 'DESC'],
        ]);
        $routineItemRows = $this->rows(self::ROUTINE_ITEMS, ['user_id' => $userId], [
            ['order_index', 'ASC'],
        ]);
        $routineItems = [];
        foreach ($routineItemRows as $row) {
            $routineItems[(string)$row['routine_id']][] = $this->formatRoutineItem($row);
        }
        $routines = [];
        foreach ($routineRows as $row) {
            $routine = $this->formatRoutine($row);
            $routine['items'] = $routineItems[$routine['id']] ?? [];
            $routines[] = $routine;
        }
        usort($routines, static function (array $left, array $right): int {
            $leftOrder = $left['lastUsedAt'] ?? $left['createdAt'];
            $rightOrder = $right['lastUsedAt'] ?? $right['createdAt'];
            return $rightOrder <=> $leftOrder;
        });

        $setRows = $this->rows(self::SETS, ['user_id' => $userId], [
            ['logged_at', 'ASC'],
        ]);
        $sets = [];
        foreach ($setRows as $row) {
            $sets[(string)$row['session_id']][] = $this->formatSet($row);
        }

        $runRows = $this->rows(self::RUNS, ['user_id' => $userId]);
        $runs = [];
        foreach ($runRows as $row) {
            $runs[(string)$row['session_id']] = $this->formatRun($row);
        }

        $boulderRows = $this->rows(self::BOULDERS, ['user_id' => $userId], [
            ['created_at', 'ASC'],
        ]);
        $boulders = [];
        foreach ($boulderRows as $row) {
            $boulders[(string)$row['session_id']][] = $this->formatBoulder($row);
        }

        $sessionRows = $this->rows(self::SESSIONS, ['user_id' => $userId], [
            ['started_at', 'DESC'],
        ]);
        $sessions = [];
        $activeSession = null;
        foreach ($sessionRows as $row) {
            $session = $this->formatSession($row);
            $session['sets'] = $sets[$session['id']] ?? [];
            $session['running'] = $runs[$session['id']] ?? null;
            $session['boulders'] = $boulders[$session['id']] ?? [];
            $sessions[] = $session;
            if ($activeSession === null && $session['finishedAt'] === null) {
                $activeSession = $session;
            }
        }

        $weightRows = $this->rows(self::WEIGHTS, ['user_id' => $userId], [
            ['entry_date', 'DESC'],
        ]);

        return [
            'version' => 1,
            'initialized' => count($exercises) > 0,
            'exercises' => $exercises,
            'routines' => $routines,
            'sessions' => $sessions,
            'activeSession' => $activeSession,
            'weights' => array_map($this->formatWeight(...), $weightRows),
        ];
    }

    /**
     * Seed the 25 exercises and three routines from the Android application.
     *
     * @return array<string, mixed>
     */
    public function initialize(string $userId): array {
        if ($this->rows(self::EXERCISES, ['user_id' => $userId]) !== []) {
            return $this->getState($userId);
        }

        try {
            $this->atomic(function () use ($userId): void {
                $exerciseIds = [];
                foreach ($this->defaultExercises() as $exercise) {
                    $id = $this->newId();
                    $exerciseIds[$exercise['name']] = $id;
                    $this->insertRow(self::EXERCISES, [
                        'id' => $id,
                        'user_id' => $userId,
                        'name' => $exercise['name'],
                        'category' => 'STRENGTH',
                        'primary_muscles' => $this->json($exercise['primary']),
                        'secondary_muscles' => $this->json($exercise['secondary'] ?? []),
                        'instructions' => '',
                        'is_custom' => false,
                        'created_at' => $this->now(),
                    ]);
                }

                foreach ($this->defaultRoutines() as $routineName => $exerciseNames) {
                    $routineId = $this->newId();
                    $this->insertRow(self::ROUTINES, [
                        'id' => $routineId,
                        'user_id' => $userId,
                        'name' => $routineName,
                        'description' => '',
                        'created_at' => $this->now(),
                        'last_used_at' => null,
                    ]);
                    foreach ($exerciseNames as $index => $exerciseName) {
                        $this->insertRow(self::ROUTINE_ITEMS, [
                            'id' => $this->newId(),
                            'user_id' => $userId,
                            'routine_id' => $routineId,
                            'exercise_id' => $exerciseIds[$exerciseName],
                            'order_index' => $index,
                            'target_sets' => 3,
                            'target_reps' => 10,
                            'target_weight' => null,
                            'target_weights' => '[]',
                            'target_reps_set' => '[]',
                            'rest_seconds' => 90,
                            'notes' => '',
                        ]);
                    }
                }
            });
        } catch (Throwable $exception) {
            // A second tab may have completed the same unique seed concurrently.
            if ($this->rows(self::EXERCISES, ['user_id' => $userId]) === []) {
                throw $exception;
            }
        }

        return $this->getState($userId);
    }

    /**
     * @param array<string, mixed> $input
     * @return array<string, mixed>
     */
    public function saveExercise(string $userId, array $input): array {
        $requestedId = $this->optionalString($input['id'] ?? null);
        $existing = $requestedId === null
            ? null
            : $this->firstRow(self::EXERCISES, ['user_id' => $userId, 'id' => $requestedId]);
        $id = $existing === null ? $this->newId() : (string)$existing['id'];
        $name = $this->requiredString($input['name'] ?? null, 'name', 255);

        foreach ($this->rows(self::EXERCISES, ['user_id' => $userId]) as $candidate) {
            if ((string)$candidate['id'] !== $id
                && mb_strtolower(trim((string)$candidate['name'])) === mb_strtolower($name)) {
                throw new InvalidArgumentException('An exercise with this name already exists.');
            }
        }

        $category = strtoupper($this->requiredString(
            $input['category'] ?? 'STRENGTH',
            'category',
            32,
        ));
        $this->assertEnum($category, self::CATEGORIES, 'category');
        $primaryMuscles = $this->muscles($input['primaryMuscles'] ?? []);
        $secondaryMuscles = array_values(array_diff(
            $this->muscles($input['secondaryMuscles'] ?? []),
            $primaryMuscles,
        ));
        $instructions = $this->limitedString($input['instructions'] ?? '', 'instructions', 10000);

        $row = [
            'id' => $id,
            'user_id' => $userId,
            'name' => $name,
            'category' => $category,
            'primary_muscles' => $this->json($primaryMuscles),
            'secondary_muscles' => $this->json($secondaryMuscles),
            'instructions' => $instructions,
            'is_custom' => $existing === null ? true : (bool)$existing['is_custom'],
            'created_at' => $existing === null ? $this->now() : (int)$existing['created_at'],
        ];

        if ($existing === null) {
            $this->insertRow(self::EXERCISES, $row);
        } else {
            unset($row['id'], $row['user_id']);
            $this->updateRows(self::EXERCISES, $row, ['user_id' => $userId, 'id' => $id]);
        }

        return $this->formatExercise(
            $this->ownedRow(self::EXERCISES, $userId, $id, 'Exercise'),
        );
    }

    public function deleteExercise(string $userId, string $id): void {
        $this->ownedRow(self::EXERCISES, $userId, $id, 'Exercise');
        if ($this->rows(self::SETS, ['user_id' => $userId, 'exercise_id' => $id]) !== []) {
            throw new InvalidArgumentException(
                'This exercise is used by workout history and cannot be deleted.',
            );
        }

        $this->atomic(function () use ($userId, $id): void {
            $this->deleteRows(self::ROUTINE_ITEMS, [
                'user_id' => $userId,
                'exercise_id' => $id,
            ]);
            $this->deleteRows(self::EXERCISES, ['user_id' => $userId, 'id' => $id]);
        });
    }

    /**
     * @param array<string, mixed> $input
     * @return array<string, mixed>
     */
    public function saveRoutine(string $userId, array $input): array {
        $requestedId = $this->optionalString($input['id'] ?? null);
        $existing = $requestedId === null
            ? null
            : $this->firstRow(self::ROUTINES, ['user_id' => $userId, 'id' => $requestedId]);
        $id = $existing === null ? $this->newId() : (string)$existing['id'];
        $name = $this->requiredString($input['name'] ?? null, 'name', 255);
        $description = $this->limitedString($input['description'] ?? '', 'description', 10000);
        $items = is_array($input['items'] ?? null) ? $input['items'] : [];

        $this->atomic(function () use (
            $userId,
            $existing,
            $id,
            $name,
            $description,
            $items,
        ): void {
            $row = [
                'id' => $id,
                'user_id' => $userId,
                'name' => $name,
                'description' => $description,
                'created_at' => $existing === null ? $this->now() : (int)$existing['created_at'],
                'last_used_at' => $existing['last_used_at'] ?? null,
            ];
            if ($existing === null) {
                $this->insertRow(self::ROUTINES, $row);
            } else {
                unset($row['id'], $row['user_id']);
                $this->updateRows(self::ROUTINES, $row, [
                    'user_id' => $userId,
                    'id' => $id,
                ]);
            }

            $this->deleteRows(self::ROUTINE_ITEMS, [
                'user_id' => $userId,
                'routine_id' => $id,
            ]);

            $seenExerciseIds = [];
            foreach (array_values($items) as $index => $item) {
                if (!is_array($item)) {
                    throw new InvalidArgumentException('Every routine item must be an object.');
                }
                $exerciseId = $this->requiredString(
                    $item['exerciseId'] ?? null,
                    'exerciseId',
                    32,
                );
                $this->ownedRow(self::EXERCISES, $userId, $exerciseId, 'Exercise');
                if (isset($seenExerciseIds[$exerciseId])) {
                    throw new InvalidArgumentException(
                        'An exercise can occur only once in a routine.',
                    );
                }
                $seenExerciseIds[$exerciseId] = true;

                $targetWeight = $this->optionalNumber(
                    $item['targetWeightKg'] ?? $item['targetWeight'] ?? null,
                    'targetWeightKg',
                    0,
                    10000,
                );
                $this->insertRow(self::ROUTINE_ITEMS, [
                    'id' => $this->newId(),
                    'user_id' => $userId,
                    'routine_id' => $id,
                    'exercise_id' => $exerciseId,
                    'order_index' => $index,
                    'target_sets' => $this->integer(
                        $item['targetSets'] ?? 3,
                        'targetSets',
                        1,
                        20,
                    ),
                    'target_reps' => $this->integer(
                        $item['targetReps'] ?? 10,
                        'targetReps',
                        0,
                        1000,
                    ),
                    'target_weight' => $targetWeight,
                    'target_weights' => $this->json($this->numberList(
                        $item['targetWeightsPerSet'] ?? [],
                        0,
                        10000,
                    )),
                    'target_reps_set' => $this->json($this->integerList(
                        $item['targetRepsPerSet'] ?? [],
                        0,
                        1000,
                    )),
                    'rest_seconds' => $this->integer(
                        $item['restSeconds'] ?? 90,
                        'restSeconds',
                        0,
                        3600,
                    ),
                    'notes' => $this->limitedString($item['notes'] ?? '', 'notes', 5000),
                ]);
            }
        });

        return $this->routineById($userId, $id);
    }

    public function deleteRoutine(string $userId, string $id): void {
        $this->ownedRow(self::ROUTINES, $userId, $id, 'Routine');
        $this->atomic(function () use ($userId, $id): void {
            $this->updateRows(self::SESSIONS, ['routine_id' => null], [
                'user_id' => $userId,
                'routine_id' => $id,
            ]);
            $this->deleteRows(self::ROUTINE_ITEMS, [
                'user_id' => $userId,
                'routine_id' => $id,
            ]);
            $this->deleteRows(self::ROUTINES, ['user_id' => $userId, 'id' => $id]);
        });
    }

    /**
     * Save a complete session aggregate. Child records are replaced atomically,
     * which also makes active strength and bouldering sessions resumable.
     *
     * @param array<string, mixed> $input
     * @return array<string, mixed>
     */
    public function saveSession(string $userId, array $input): array {
        $requestedId = $this->optionalString($input['id'] ?? null);
        $existing = $requestedId === null
            ? null
            : $this->firstRow(self::SESSIONS, ['user_id' => $userId, 'id' => $requestedId]);
        $id = $existing === null ? $this->newId() : (string)$existing['id'];

        $sessionType = strtoupper($this->requiredString(
            $input['sessionType'] ?? 'STRENGTH',
            'sessionType',
            16,
        ));
        $this->assertEnum($sessionType, self::SESSION_TYPES, 'sessionType');
        $defaultName = match ($sessionType) {
            'RUNNING' => 'Running',
            'BOULDERING' => 'Bouldering',
            default => 'Ad-hoc Workout',
        };
        $name = $this->requiredString($input['name'] ?? $defaultName, 'name', 255);
        $startedAt = $this->timestamp($input['startedAt'] ?? $this->now(), 'startedAt');
        $finishedAt = $this->optionalTimestamp($input['finishedAt'] ?? null, 'finishedAt');
        if ($finishedAt !== null && $finishedAt < $startedAt) {
            throw new InvalidArgumentException('finishedAt cannot be earlier than startedAt.');
        }
        $sameStart = $this->firstRow(self::SESSIONS, [
            'user_id' => $userId,
            'started_at' => $startedAt,
        ]);
        if ($sameStart !== null && (string)$sameStart['id'] !== $id) {
            throw new InvalidArgumentException(
                'A session with this start time already exists.',
            );
        }
        if ($finishedAt === null) {
            $activeSession = $this->firstRow(self::SESSIONS, [
                'user_id' => $userId,
                'active_slot' => 'active',
            ]);
            if ($activeSession !== null && (string)$activeSession['id'] !== $id) {
                throw new InvalidArgumentException(
                    'Finish or discard the active session before starting another one.',
                );
            }
        }
        $routineId = $this->optionalString($input['routineId'] ?? null);
        if ($routineId !== null) {
            $this->ownedRow(self::ROUTINES, $userId, $routineId, 'Routine');
        }
        $previousRoutineId = $existing === null || $existing['routine_id'] === null
            ? null
            : (string)$existing['routine_id'];

        $sets = is_array($input['sets'] ?? null) ? $input['sets'] : [];
        $running = is_array($input['running'] ?? null)
            ? $input['running']
            : (is_array($input['runningEntry'] ?? null) ? $input['runningEntry'] : null);
        $boulders = is_array($input['boulders'] ?? null)
            ? $input['boulders']
            : (is_array($input['boulderingRoutes'] ?? null) ? $input['boulderingRoutes'] : []);

        $this->atomic(function () use (
            $userId,
            $existing,
            $id,
            $routineId,
            $previousRoutineId,
            $name,
            $startedAt,
            $finishedAt,
            $input,
            $sessionType,
            $sets,
            $running,
            $boulders,
        ): void {
            $row = [
                'id' => $id,
                'user_id' => $userId,
                'routine_id' => $routineId,
                'name' => $name,
                'started_at' => $startedAt,
                'finished_at' => $finishedAt,
                'notes' => $this->limitedString($input['notes'] ?? '', 'notes', 10000),
                'session_type' => $sessionType,
                'active_slot' => $finishedAt === null ? 'active' : null,
            ];
            if ($existing === null) {
                $this->insertRow(self::SESSIONS, $row);
            } else {
                unset($row['id'], $row['user_id']);
                $this->updateRows(self::SESSIONS, $row, [
                    'user_id' => $userId,
                    'id' => $id,
                ]);
            }

            $this->deleteRows(self::SETS, ['user_id' => $userId, 'session_id' => $id]);
            $this->deleteRows(self::RUNS, ['user_id' => $userId, 'session_id' => $id]);
            $this->deleteRows(self::BOULDERS, ['user_id' => $userId, 'session_id' => $id]);

            if ($sessionType === 'STRENGTH') {
                foreach (array_values($sets) as $index => $set) {
                    if (!is_array($set)) {
                        throw new InvalidArgumentException('Every set must be an object.');
                    }
                    $exerciseId = $this->requiredString(
                        $set['exerciseId'] ?? null,
                        'exerciseId',
                        32,
                    );
                    $exercise = $this->ownedRow(
                        self::EXERCISES,
                        $userId,
                        $exerciseId,
                        'Exercise',
                    );
                    $this->insertRow(self::SETS, [
                        'id' => $this->newId(),
                        'user_id' => $userId,
                        'session_id' => $id,
                        'exercise_id' => $exerciseId,
                        'exercise_name' => $this->limitedString(
                            $set['exerciseName'] ?? $exercise['name'],
                            'exerciseName',
                            255,
                        ),
                        'set_number' => $this->integer(
                            $set['setNumber'] ?? $index + 1,
                            'setNumber',
                            1,
                            1000,
                        ),
                        'reps' => $this->integer($set['reps'] ?? 0, 'reps', 0, 100000),
                        'weight' => $this->number(
                            $set['weightKg'] ?? $set['weight'] ?? 0,
                            'weightKg',
                            0,
                            100000,
                        ),
                        'is_bodyweight' => (bool)($set['isBodyweight'] ?? false),
                        'rpe' => $this->optionalNumber($set['rpe'] ?? null, 'rpe', 0, 10),
                        'logged_at' => $this->timestamp(
                            $set['loggedAt'] ?? $this->now(),
                            'loggedAt',
                        ),
                    ]);
                }
            } elseif ($sessionType === 'RUNNING' && $running !== null) {
                $this->insertRow(self::RUNS, [
                    'id' => $this->newId(),
                    'user_id' => $userId,
                    'session_id' => $id,
                    'distance' => $this->number(
                        $running['distanceKm'] ?? $running['distance'] ?? 0,
                        'distanceKm',
                        0,
                        100000,
                    ),
                    'duration' => $this->integer(
                        $running['durationSeconds'] ?? $running['duration'] ?? 0,
                        'durationSeconds',
                        0,
                        100000000,
                    ),
                ]);
            } elseif ($sessionType === 'BOULDERING') {
                foreach (array_values($boulders) as $route) {
                    if (!is_array($route)) {
                        throw new InvalidArgumentException('Every boulder must be an object.');
                    }
                    $this->insertRow(self::BOULDERS, [
                        'id' => $this->newId(),
                        'user_id' => $userId,
                        'session_id' => $id,
                        'description' => $this->requiredString(
                            $route['description'] ?? $route['grade'] ?? null,
                            'description',
                            255,
                        ),
                        'is_completed' => (bool)($route['isCompleted'] ?? true),
                        'created_at' => $this->timestamp(
                            $route['createdAt'] ?? $this->now(),
                            'createdAt',
                        ),
                    ]);
                }
            }

            foreach (array_unique([$previousRoutineId, $routineId]) as $affectedRoutineId) {
                if ($affectedRoutineId !== null) {
                    $this->scheduleRoutineLastUsedRefresh($userId, $affectedRoutineId);
                }
            }
        });

        return $this->sessionById($userId, $id);
    }

    public function deleteSession(string $userId, string $id): void {
        $session = $this->ownedRow(self::SESSIONS, $userId, $id, 'Session');
        $routineId = $session['routine_id'] === null ? null : (string)$session['routine_id'];
        $this->atomic(function () use ($userId, $id, $routineId): void {
            foreach ([self::SETS, self::RUNS, self::BOULDERS] as $table) {
                $this->deleteRows($table, ['user_id' => $userId, 'session_id' => $id]);
            }
            $this->deleteRows(self::SESSIONS, ['user_id' => $userId, 'id' => $id]);
            if ($routineId !== null) {
                $this->scheduleRoutineLastUsedRefresh($userId, $routineId);
            }
        });
    }

    /**
     * @param array<string, mixed> $input
     * @return array<string, mixed>
     */
    public function saveWeight(string $userId, array $input): array {
        $requestedId = $this->optionalString($input['id'] ?? null);
        $existing = $requestedId === null
            ? null
            : $this->firstRow(self::WEIGHTS, ['user_id' => $userId, 'id' => $requestedId]);
        $date = $this->timestamp($input['date'] ?? $input['entryDate'] ?? $this->now(), 'date');
        $weight = $this->number(
            $input['weightKg'] ?? $input['weight'] ?? null,
            'weightKg',
            1,
            1000,
        );

        $sameDate = $this->firstRow(self::WEIGHTS, [
            'user_id' => $userId,
            'entry_date' => $date,
        ]);
        if ($sameDate !== null && $existing !== null
            && (string)$sameDate['id'] !== (string)$existing['id']) {
            throw new InvalidArgumentException(
                'A body-weight entry already exists for this date.',
            );
        }
        if ($existing === null) {
            $existing = $sameDate;
        }
        $id = $existing === null ? $this->newId() : (string)$existing['id'];
        $row = [
            'id' => $id,
            'user_id' => $userId,
            'entry_date' => $date,
            'weight' => $weight,
        ];
        if ($existing === null) {
            $this->insertRow(self::WEIGHTS, $row);
        } else {
            unset($row['id'], $row['user_id']);
            $this->updateRows(self::WEIGHTS, $row, ['user_id' => $userId, 'id' => $id]);
        }

        return $this->formatWeight($this->ownedRow(self::WEIGHTS, $userId, $id, 'Weight'));
    }

    public function deleteWeight(string $userId, string $id): void {
        $this->ownedRow(self::WEIGHTS, $userId, $id, 'Weight');
        $this->deleteRows(self::WEIGHTS, ['user_id' => $userId, 'id' => $id]);
    }

    /**
     * Import a KraftLog full export, Android history export, or Android routine
     * export. Imported IDs are never trusted; relations are remapped by ID/name.
     *
     * @param array<string, mixed> $payload
     * @return array<string, int>
     */
    public function importData(string $userId, array $payload): array {
        return $this->atomic(function () use ($userId, $payload): array {
            $this->deferRoutineRefresh = true;
            $this->pendingRoutineRefresh = [];
            try {
                $counts = $this->performImport($userId, $payload);
                $this->deferRoutineRefresh = false;
                foreach (array_keys($this->pendingRoutineRefresh) as $routineId) {
                    $this->refreshRoutineLastUsed($userId, $routineId);
                }
                return $counts;
            } finally {
                $this->deferRoutineRefresh = false;
                $this->pendingRoutineRefresh = [];
            }
        });
    }

    /**
     * @param array<string, mixed> $payload
     * @return array<string, int>
     */
    private function performImport(string $userId, array $payload): array {
        $data = is_array($payload['data'] ?? null) ? $payload['data'] : $payload;
        $counts = ['exercises' => 0, 'routines' => 0, 'sessions' => 0, 'weights' => 0];
        $exerciseIdMap = [];
        $routineIdMap = [];
        $isAndroidRoutineExport = isset($data['name'])
            && !isset($data['sessions'])
            && is_array($data['exercises'] ?? null)
            && isset($data['exercises'][0]['exerciseName']);

        if (!$isAndroidRoutineExport && is_array($data['exercises'] ?? null)) {
            foreach ($data['exercises'] as $sourceExercise) {
                if (!is_array($sourceExercise)) {
                    continue;
                }
                $sourceId = isset($sourceExercise['id']) ? (string)$sourceExercise['id'] : null;
                $existing = $this->exerciseByName(
                    $userId,
                    (string)($sourceExercise['name'] ?? ''),
                );
                $saved = $existing === null
                    ? $this->saveExercise($userId, $sourceExercise)
                    : $this->formatExercise($existing);
                if ($sourceId !== null) {
                    $exerciseIdMap[$sourceId] = $saved['id'];
                }
                if ($existing === null) {
                    $counts['exercises']++;
                }
            }
        }

        $routineSources = [];
        if (is_array($data['routines'] ?? null)) {
            $routineSources = $data['routines'];
        } elseif ($isAndroidRoutineExport) {
            $routineSources = [[
                'name' => $data['name'],
                'description' => $data['description'] ?? '',
                'items' => $data['exercises'],
            ]];
        }

        foreach ($routineSources as $sourceRoutine) {
            if (!is_array($sourceRoutine)) {
                continue;
            }
            $sourceItems = is_array($sourceRoutine['items'] ?? null)
                ? $sourceRoutine['items']
                : (is_array($sourceRoutine['exercises'] ?? null)
                    ? $sourceRoutine['exercises']
                    : []);
            $items = [];
            foreach ($sourceItems as $sourceItem) {
                if (!is_array($sourceItem)) {
                    continue;
                }
                $sourceExerciseId = isset($sourceItem['exerciseId'])
                    ? (string)$sourceItem['exerciseId']
                    : null;
                $exerciseId = $sourceExerciseId === null
                    ? null
                    : ($exerciseIdMap[$sourceExerciseId] ?? null);
                if ($exerciseId === null) {
                    $exerciseName = trim((string)(
                        $sourceItem['exerciseName']
                        ?? $sourceItem['name']
                        ?? ''
                    ));
                    $exercise = $this->exerciseByName($userId, $exerciseName);
                    if ($exercise === null && $exerciseName !== '') {
                        $created = $this->saveExercise($userId, [
                            'name' => $exerciseName,
                            'category' => 'STRENGTH',
                            'primaryMuscles' => [],
                        ]);
                        $exerciseId = $created['id'];
                        $counts['exercises']++;
                    } elseif ($exercise !== null) {
                        $exerciseId = (string)$exercise['id'];
                    }
                }
                if ($exerciseId === null) {
                    continue;
                }
                $sourceItem['exerciseId'] = $exerciseId;
                $items[] = $sourceItem;
            }
            $saved = $this->saveRoutine($userId, [
                'name' => $sourceRoutine['name'] ?? 'Imported routine',
                'description' => $sourceRoutine['description'] ?? '',
                'items' => $items,
            ]);
            if (isset($sourceRoutine['id'])) {
                $routineIdMap[(string)$sourceRoutine['id']] = $saved['id'];
            }
            $counts['routines']++;
        }

        $existingStartedAt = [];
        foreach ($this->rows(self::SESSIONS, ['user_id' => $userId]) as $row) {
            $existingStartedAt[(string)$row['started_at']] = true;
        }
        if (is_array($data['sessions'] ?? null)) {
            foreach ($data['sessions'] as $sourceSession) {
                if (!is_array($sourceSession)) {
                    continue;
                }
                $startedAt = $this->timestamp(
                    $sourceSession['startedAt'] ?? $this->now(),
                    'startedAt',
                );
                if (isset($existingStartedAt[(string)$startedAt])) {
                    continue;
                }
                $sessionType = strtoupper((string)(
                    $sourceSession['sessionType'] ?? 'STRENGTH'
                ));
                $sets = [];
                foreach (
                    is_array($sourceSession['sets'] ?? null) ? $sourceSession['sets'] : []
                    as $sourceSet
                ) {
                    if (!is_array($sourceSet)) {
                        continue;
                    }
                    $exerciseName = trim((string)($sourceSet['exerciseName'] ?? ''));
                    $exercise = $this->exerciseByName($userId, $exerciseName);
                    if ($exercise === null && $exerciseName !== '') {
                        $created = $this->saveExercise($userId, [
                            'name' => $exerciseName,
                            'category' => 'STRENGTH',
                            'primaryMuscles' => [],
                        ]);
                        $sourceSet['exerciseId'] = $created['id'];
                        $counts['exercises']++;
                    } elseif ($exercise !== null) {
                        $sourceSet['exerciseId'] = (string)$exercise['id'];
                    } else {
                        continue;
                    }
                    $sets[] = $sourceSet;
                }
                $sourceRoutineId = isset($sourceSession['routineId'])
                    ? (string)$sourceSession['routineId']
                    : null;
                $this->saveSession($userId, [
                    'routineId' => $sourceRoutineId === null
                        ? null
                        : ($routineIdMap[$sourceRoutineId] ?? null),
                    'name' => $sourceSession['name'] ?? 'Imported workout',
                    'startedAt' => $startedAt,
                    'finishedAt' => array_key_exists('finishedAt', $sourceSession)
                        ? $sourceSession['finishedAt']
                        : $startedAt,
                    'notes' => $sourceSession['notes'] ?? '',
                    'sessionType' => $sessionType,
                    'sets' => $sets,
                    'running' => $sourceSession['running']
                        ?? $sourceSession['runningEntry']
                        ?? null,
                    'boulders' => $sourceSession['boulders']
                        ?? $sourceSession['boulderingRoutes']
                        ?? [],
                ]);
                $existingStartedAt[(string)$startedAt] = true;
                $counts['sessions']++;
            }
        }

        $weightSources = is_array($data['weights'] ?? null)
            ? $data['weights']
            : (is_array($data['bodyWeightEntries'] ?? null)
                ? $data['bodyWeightEntries']
                : []);
        foreach ($weightSources as $sourceWeight) {
            if (!is_array($sourceWeight)) {
                continue;
            }
            $this->saveWeight($userId, $sourceWeight);
            $counts['weights']++;
        }

        return $counts;
    }

    /**
     * @return array<string, mixed>
     */
    private function routineById(string $userId, string $id): array {
        $routine = $this->formatRoutine(
            $this->ownedRow(self::ROUTINES, $userId, $id, 'Routine'),
        );
        $routine['items'] = array_map(
            $this->formatRoutineItem(...),
            $this->rows(self::ROUTINE_ITEMS, [
                'user_id' => $userId,
                'routine_id' => $id,
            ], [['order_index', 'ASC']]),
        );
        return $routine;
    }

    /**
     * @return array<string, mixed>
     */
    private function sessionById(string $userId, string $id): array {
        $session = $this->formatSession(
            $this->ownedRow(self::SESSIONS, $userId, $id, 'Session'),
        );
        $session['sets'] = array_map(
            $this->formatSet(...),
            $this->rows(self::SETS, [
                'user_id' => $userId,
                'session_id' => $id,
            ], [['logged_at', 'ASC']]),
        );
        $run = $this->firstRow(self::RUNS, [
            'user_id' => $userId,
            'session_id' => $id,
        ]);
        $session['running'] = $run === null ? null : $this->formatRun($run);
        $session['boulders'] = array_map(
            $this->formatBoulder(...),
            $this->rows(self::BOULDERS, [
                'user_id' => $userId,
                'session_id' => $id,
            ], [['created_at', 'ASC']]),
        );
        return $session;
    }

    /**
     * @return array<string, mixed>|null
     */
    private function exerciseByName(string $userId, string $name): ?array {
        $needle = mb_strtolower(trim($name));
        if ($needle === '') {
            return null;
        }
        foreach ($this->rows(self::EXERCISES, ['user_id' => $userId]) as $row) {
            if (mb_strtolower(trim((string)$row['name'])) === $needle) {
                return $row;
            }
        }
        return null;
    }

    private function refreshRoutineLastUsed(string $userId, string $routineId): void {
        $latest = null;
        foreach ($this->rows(self::SESSIONS, [
            'user_id' => $userId,
            'routine_id' => $routineId,
        ]) as $session) {
            if ($session['finished_at'] !== null) {
                $finishedAt = (int)$session['finished_at'];
                $latest = $latest === null ? $finishedAt : max($latest, $finishedAt);
            }
        }
        $this->updateRows(self::ROUTINES, ['last_used_at' => $latest], [
            'user_id' => $userId,
            'id' => $routineId,
        ]);
    }

    private function scheduleRoutineLastUsedRefresh(string $userId, string $routineId): void {
        if ($this->deferRoutineRefresh) {
            $this->pendingRoutineRefresh[$routineId] = true;
            return;
        }
        $this->refreshRoutineLastUsed($userId, $routineId);
    }

    /**
     * @param array<string, mixed> $row
     * @return array<string, mixed>
     */
    private function formatExercise(array $row): array {
        return [
            'id' => (string)$row['id'],
            'name' => (string)$row['name'],
            'category' => (string)$row['category'],
            'primaryMuscles' => $this->decodeList((string)$row['primary_muscles']),
            'secondaryMuscles' => $this->decodeList((string)$row['secondary_muscles']),
            'instructions' => (string)$row['instructions'],
            'isCustom' => (bool)$row['is_custom'],
            'createdAt' => (int)$row['created_at'],
        ];
    }

    /**
     * @param array<string, mixed> $row
     * @return array<string, mixed>
     */
    private function formatRoutine(array $row): array {
        return [
            'id' => (string)$row['id'],
            'name' => (string)$row['name'],
            'description' => (string)$row['description'],
            'createdAt' => (int)$row['created_at'],
            'lastUsedAt' => $row['last_used_at'] === null ? null : (int)$row['last_used_at'],
        ];
    }

    /**
     * @param array<string, mixed> $row
     * @return array<string, mixed>
     */
    private function formatRoutineItem(array $row): array {
        return [
            'id' => (string)$row['id'],
            'routineId' => (string)$row['routine_id'],
            'exerciseId' => (string)$row['exercise_id'],
            'orderIndex' => (int)$row['order_index'],
            'targetSets' => (int)$row['target_sets'],
            'targetReps' => (int)$row['target_reps'],
            'targetWeightKg' => $row['target_weight'] === null
                ? null
                : (float)$row['target_weight'],
            'targetWeightsPerSet' => $this->decodeList((string)$row['target_weights']),
            'targetRepsPerSet' => $this->decodeList((string)$row['target_reps_set']),
            'restSeconds' => (int)$row['rest_seconds'],
            'notes' => (string)$row['notes'],
        ];
    }

    /**
     * @param array<string, mixed> $row
     * @return array<string, mixed>
     */
    private function formatSession(array $row): array {
        return [
            'id' => (string)$row['id'],
            'routineId' => $row['routine_id'] === null ? null : (string)$row['routine_id'],
            'name' => (string)$row['name'],
            'startedAt' => (int)$row['started_at'],
            'finishedAt' => $row['finished_at'] === null ? null : (int)$row['finished_at'],
            'notes' => (string)$row['notes'],
            'sessionType' => (string)$row['session_type'],
        ];
    }

    /**
     * @param array<string, mixed> $row
     * @return array<string, mixed>
     */
    private function formatSet(array $row): array {
        return [
            'id' => (string)$row['id'],
            'sessionId' => (string)$row['session_id'],
            'exerciseId' => (string)$row['exercise_id'],
            'exerciseName' => (string)$row['exercise_name'],
            'setNumber' => (int)$row['set_number'],
            'reps' => (int)$row['reps'],
            'weightKg' => (float)$row['weight'],
            'isBodyweight' => (bool)$row['is_bodyweight'],
            'rpe' => $row['rpe'] === null ? null : (float)$row['rpe'],
            'loggedAt' => (int)$row['logged_at'],
        ];
    }

    /**
     * @param array<string, mixed> $row
     * @return array<string, mixed>
     */
    private function formatRun(array $row): array {
        return [
            'id' => (string)$row['id'],
            'sessionId' => (string)$row['session_id'],
            'distanceKm' => (float)$row['distance'],
            'durationSeconds' => (int)$row['duration'],
        ];
    }

    /**
     * @param array<string, mixed> $row
     * @return array<string, mixed>
     */
    private function formatBoulder(array $row): array {
        return [
            'id' => (string)$row['id'],
            'sessionId' => (string)$row['session_id'],
            'description' => (string)$row['description'],
            'isCompleted' => (bool)$row['is_completed'],
            'createdAt' => (int)$row['created_at'],
        ];
    }

    /**
     * @param array<string, mixed> $row
     * @return array<string, mixed>
     */
    private function formatWeight(array $row): array {
        return [
            'id' => (string)$row['id'],
            'date' => (int)$row['entry_date'],
            'weightKg' => (float)$row['weight'],
        ];
    }

    /**
     * @param array<string, mixed> $conditions
     * @param list<array{0: string, 1: string}> $order
     * @return list<array<string, mixed>>
     */
    private function rows(string $table, array $conditions = [], array $order = []): array {
        $query = $this->db->getQueryBuilder();
        $query->select('*')->from($table);
        $this->applyConditions($query, $conditions);
        foreach ($order as [$column, $direction]) {
            $query->addOrderBy($column, strtoupper($direction) === 'ASC' ? 'ASC' : 'DESC');
        }
        $result = $query->executeQuery();
        $rows = $result->fetchAllAssociative();
        $result->closeCursor();
        return $rows;
    }

    /**
     * @param array<string, mixed> $conditions
     * @return array<string, mixed>|null
     */
    private function firstRow(string $table, array $conditions): ?array {
        $query = $this->db->getQueryBuilder();
        $query->select('*')->from($table)->setMaxResults(1);
        $this->applyConditions($query, $conditions);
        $result = $query->executeQuery();
        $row = $result->fetchAssociative();
        $result->closeCursor();
        return $row === false ? null : $row;
    }

    /**
     * @return array<string, mixed>
     */
    private function ownedRow(
        string $table,
        string $userId,
        string $id,
        string $label,
    ): array {
        $row = $this->firstRow($table, ['user_id' => $userId, 'id' => $id]);
        if ($row === null) {
            throw new OutOfBoundsException($label . ' not found.');
        }
        return $row;
    }

    /**
     * @param array<string, mixed> $values
     */
    private function insertRow(string $table, array $values): void {
        $query = $this->db->getQueryBuilder();
        $parameters = [];
        foreach ($values as $column => $value) {
            $parameters[$column] = $this->parameter($query, $value);
        }
        $query->insert($table)->values($parameters)->executeStatement();
    }

    /**
     * @param array<string, mixed> $values
     * @param array<string, mixed> $conditions
     */
    private function updateRows(string $table, array $values, array $conditions): void {
        $query = $this->db->getQueryBuilder();
        $query->update($table);
        foreach ($values as $column => $value) {
            $query->set($column, $this->parameter($query, $value));
        }
        $this->applyConditions($query, $conditions);
        $query->executeStatement();
    }

    /**
     * @param array<string, mixed> $conditions
     */
    private function deleteRows(string $table, array $conditions): void {
        $query = $this->db->getQueryBuilder();
        $query->delete($table);
        $this->applyConditions($query, $conditions);
        $query->executeStatement();
    }

    /**
     * @param array<string, mixed> $conditions
     */
    private function applyConditions(IQueryBuilder $query, array $conditions): void {
        $first = true;
        foreach ($conditions as $column => $value) {
            $expression = $value === null
                ? $query->expr()->isNull($column)
                : $query->expr()->eq($column, $this->parameter($query, $value));
            if ($first) {
                $query->where($expression);
                $first = false;
            } else {
                $query->andWhere($expression);
            }
        }
    }

    private function parameter(IQueryBuilder $query, mixed $value): mixed {
        if ($value === null) {
            return $query->createNamedParameter(null, IQueryBuilder::PARAM_NULL);
        }
        if (is_bool($value)) {
            return $query->createNamedParameter($value, IQueryBuilder::PARAM_BOOL);
        }
        if (is_int($value)) {
            return $query->createNamedParameter($value, IQueryBuilder::PARAM_INT);
        }
        if (is_float($value)) {
            return $query->createNamedParameter((string)$value, IQueryBuilder::PARAM_STR);
        }
        return $query->createNamedParameter((string)$value, IQueryBuilder::PARAM_STR);
    }

    private function atomic(callable $callback): mixed {
        $isOutermost = $this->transactionDepth === 0;
        if ($isOutermost) {
            $this->db->beginTransaction();
        }
        $this->transactionDepth++;
        try {
            $result = $callback();
            if ($isOutermost) {
                $this->db->commit();
            }
            return $result;
        } catch (Throwable $exception) {
            if ($isOutermost) {
                try {
                    $this->db->rollBack();
                } catch (Throwable) {
                    // Preserve the original callback/commit exception.
                }
            }
            throw $exception;
        } finally {
            $this->transactionDepth--;
        }
    }

    private function now(): int {
        return (int)floor(microtime(true) * 1000);
    }

    private function newId(): string {
        return bin2hex(random_bytes(16));
    }

    private function requiredString(
        mixed $value,
        string $field,
        int $maximumLength,
    ): string {
        $text = $this->limitedString($value, $field, $maximumLength);
        if ($text === '') {
            throw new InvalidArgumentException($field . ' is required.');
        }
        return $text;
    }

    private function limitedString(
        mixed $value,
        string $field,
        int $maximumLength,
    ): string {
        if (!is_scalar($value) && $value !== null) {
            throw new InvalidArgumentException($field . ' must be text.');
        }
        $text = trim((string)($value ?? ''));
        if (mb_strlen($text) > $maximumLength) {
            throw new InvalidArgumentException($field . ' is too long.');
        }
        return $text;
    }

    private function optionalString(mixed $value): ?string {
        if ($value === null) {
            return null;
        }
        $text = trim((string)$value);
        return $text === '' ? null : $text;
    }

    private function integer(
        mixed $value,
        string $field,
        int $minimum,
        int $maximum,
    ): int {
        if (filter_var($value, FILTER_VALIDATE_INT) === false) {
            throw new InvalidArgumentException($field . ' must be an integer.');
        }
        $number = (int)$value;
        if ($number < $minimum || $number > $maximum) {
            throw new InvalidArgumentException(
                sprintf('%s must be between %d and %d.', $field, $minimum, $maximum),
            );
        }
        return $number;
    }

    private function number(
        mixed $value,
        string $field,
        float $minimum,
        float $maximum,
    ): float {
        if (is_string($value)) {
            $value = str_replace(',', '.', trim($value));
        }
        if (!is_numeric($value)) {
            throw new InvalidArgumentException($field . ' must be a number.');
        }
        $number = (float)$value;
        if (!is_finite($number) || $number < $minimum || $number > $maximum) {
            throw new InvalidArgumentException(
                sprintf('%s must be between %s and %s.', $field, $minimum, $maximum),
            );
        }
        return $number;
    }

    private function optionalNumber(
        mixed $value,
        string $field,
        float $minimum,
        float $maximum,
    ): ?float {
        if ($value === null || $value === '') {
            return null;
        }
        return $this->number($value, $field, $minimum, $maximum);
    }

    private function timestamp(mixed $value, string $field): int {
        return $this->integer($value, $field, 0, PHP_INT_MAX);
    }

    private function optionalTimestamp(mixed $value, string $field): ?int {
        if ($value === null || $value === '') {
            return null;
        }
        return $this->timestamp($value, $field);
    }

    /**
     * @param list<string> $allowed
     */
    private function assertEnum(string $value, array $allowed, string $field): void {
        if (!in_array($value, $allowed, true)) {
            throw new InvalidArgumentException($field . ' has an unsupported value.');
        }
    }

    /**
     * @return list<string>
     */
    private function muscles(mixed $value): array {
        $values = is_array($value) ? $value : [];
        $muscles = [];
        foreach ($values as $muscle) {
            $normalized = strtoupper(trim((string)$muscle));
            $this->assertEnum($normalized, self::MUSCLES, 'muscle');
            $muscles[$normalized] = true;
        }
        return array_keys($muscles);
    }

    /**
     * @return list<float>
     */
    private function numberList(mixed $value, float $minimum, float $maximum): array {
        if (is_string($value)) {
            $value = trim($value) === '' ? [] : explode(',', $value);
        }
        if (!is_array($value)) {
            return [];
        }
        $numbers = [];
        foreach ($value as $item) {
            if ($item === '' || $item === null) {
                continue;
            }
            $numbers[] = $this->number($item, 'per-set value', $minimum, $maximum);
        }
        return $numbers;
    }

    /**
     * @return list<int>
     */
    private function integerList(mixed $value, int $minimum, int $maximum): array {
        if (is_string($value)) {
            $value = trim($value) === '' ? [] : explode(',', $value);
        }
        if (!is_array($value)) {
            return [];
        }
        $numbers = [];
        foreach ($value as $item) {
            if ($item === '' || $item === null) {
                continue;
            }
            $numbers[] = $this->integer($item, 'per-set reps', $minimum, $maximum);
        }
        return $numbers;
    }

    private function json(array $value): string {
        $encoded = json_encode(array_values($value), JSON_THROW_ON_ERROR);
        return $encoded;
    }

    /**
     * @return list<mixed>
     */
    private function decodeList(string $value): array {
        $decoded = json_decode($value, true);
        return is_array($decoded) ? array_values($decoded) : [];
    }

    /**
     * @return list<array{name: string, primary: list<string>, secondary?: list<string>}>
     */
    private function defaultExercises(): array {
        return [
            ['name' => 'Brustpresse (01)', 'primary' => ['CHEST'], 'secondary' => ['TRICEPS', 'SHOULDERS']],
            ['name' => 'Schulterpresse (06)', 'primary' => ['SHOULDERS']],
            ['name' => 'Trizepsmaschine (23)', 'primary' => ['TRICEPS']],
            ['name' => 'Bankdrücken schräg (38)', 'primary' => ['CHEST'], 'secondary' => ['TRICEPS', 'SHOULDERS']],
            ['name' => 'Plate Loaded Seated Dip', 'primary' => ['TRICEPS'], 'secondary' => ['CHEST', 'SHOULDERS']],
            ['name' => 'Bankdrücken (25)', 'primary' => ['CHEST'], 'secondary' => ['TRICEPS', 'SHOULDERS']],
            ['name' => 'Trizepsstrecken beidarmig sitzend', 'primary' => ['TRICEPS']],
            ['name' => 'Seitheben (21)', 'primary' => ['SHOULDERS']],
            ['name' => 'Butterfly (02)', 'primary' => ['CHEST']],
            ['name' => 'Beinstreckung (14)', 'primary' => ['QUADRICEPS']],
            ['name' => 'Beinbeuger liegend', 'primary' => ['HAMSTRINGS']],
            ['name' => 'Adduktion (09)', 'primary' => ['QUADRICEPS']],
            ['name' => 'Abduktion (08)', 'primary' => ['GLUTES']],
            ['name' => 'Beinpresse (07)', 'primary' => ['QUADRICEPS'], 'secondary' => ['GLUTES', 'CALVES']],
            ['name' => 'Bauchmaschine (HS)', 'primary' => ['CORE']],
            ['name' => 'Wadenheben stehend', 'primary' => ['CALVES']],
            ['name' => 'Rumpfrotation (120)', 'primary' => ['CORE']],
            ['name' => 'Upper Back (03A)', 'primary' => ['BACK']],
            ['name' => 'Vertical Traction (05A)', 'primary' => ['BACK'], 'secondary' => ['BICEPS']],
            ['name' => 'Bizepsmaschine (22)', 'primary' => ['BICEPS']],
            ['name' => 'Reverse Fly', 'primary' => ['BACK'], 'secondary' => ['SHOULDERS']],
            ['name' => 'Rückenstreckung 45°', 'primary' => ['BACK'], 'secondary' => ['GLUTES', 'HAMSTRINGS']],
            ['name' => 'PL Latzug (50)', 'primary' => ['BACK'], 'secondary' => ['BICEPS']],
            ['name' => 'Rudern sitzend', 'primary' => ['BACK'], 'secondary' => ['BICEPS']],
            ['name' => 'Bizeps Curls stehend (95)', 'primary' => ['BICEPS']],
        ];
    }

    /**
     * @return array<string, list<string>>
     */
    private function defaultRoutines(): array {
        return [
            'Brust, Trizeps & Schultern' => [
                'Brustpresse (01)',
                'Bankdrücken schräg (38)',
                'Bankdrücken (25)',
                'Butterfly (02)',
                'Schulterpresse (06)',
                'Seitheben (21)',
                'Trizepsmaschine (23)',
                'Plate Loaded Seated Dip',
                'Trizepsstrecken beidarmig sitzend',
            ],
            'Beine & Core' => [
                'Beinpresse (07)',
                'Beinstreckung (14)',
                'Beinbeuger liegend',
                'Adduktion (09)',
                'Abduktion (08)',
                'Wadenheben stehend',
                'Bauchmaschine (HS)',
                'Rumpfrotation (120)',
            ],
            'Rücken & Bizeps' => [
                'Upper Back (03A)',
                'PL Latzug (50)',
                'Vertical Traction (05A)',
                'Rudern sitzend',
                'Reverse Fly',
                'Rückenstreckung 45°',
                'Bizepsmaschine (22)',
                'Bizeps Curls stehend (95)',
            ],
        ];
    }
}
