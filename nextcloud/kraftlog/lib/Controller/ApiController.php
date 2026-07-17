<?php

declare(strict_types=1);

namespace OCA\KraftLog\Controller;

use InvalidArgumentException;
use JsonException;
use OCA\KraftLog\Service\KraftLogService;
use OCP\AppFramework\Controller;
use OCP\AppFramework\Http;
use OCP\AppFramework\Http\Attribute\NoAdminRequired;
use OCP\AppFramework\Http\DataResponse;
use OCP\DB\Exception as DatabaseException;
use OCP\IRequest;
use OCP\IUserSession;
use OutOfBoundsException;
use Throwable;

final class ApiController extends Controller {
    public function __construct(
        string $appName,
        IRequest $request,
        private KraftLogService $service,
        private IUserSession $userSession,
    ) {
        parent::__construct($appName, $request);
    }

    #[NoAdminRequired]
    public function state(): DataResponse {
        return $this->respond(
            fn (): array => $this->service->getState($this->userId()),
        );
    }

    #[NoAdminRequired]
    public function initialize(): DataResponse {
        return $this->respond(
            fn (): array => $this->service->initialize($this->userId()),
        );
    }

    #[NoAdminRequired]
    public function saveExercise(): DataResponse {
        return $this->respond(
            fn (): array => $this->service->saveExercise($this->userId(), $this->input()),
        );
    }

    #[NoAdminRequired]
    public function deleteExercise(string $id): DataResponse {
        return $this->respond(function () use ($id): array {
            $this->service->deleteExercise($this->userId(), $id);
            return ['ok' => true];
        });
    }

    #[NoAdminRequired]
    public function saveRoutine(): DataResponse {
        return $this->respond(
            fn (): array => $this->service->saveRoutine($this->userId(), $this->input()),
        );
    }

    #[NoAdminRequired]
    public function deleteRoutine(string $id): DataResponse {
        return $this->respond(function () use ($id): array {
            $this->service->deleteRoutine($this->userId(), $id);
            return ['ok' => true];
        });
    }

    #[NoAdminRequired]
    public function saveSession(): DataResponse {
        return $this->respond(
            fn (): array => $this->service->saveSession($this->userId(), $this->input()),
        );
    }

    #[NoAdminRequired]
    public function deleteSession(string $id): DataResponse {
        return $this->respond(function () use ($id): array {
            $this->service->deleteSession($this->userId(), $id);
            return ['ok' => true];
        });
    }

    #[NoAdminRequired]
    public function saveWeight(): DataResponse {
        return $this->respond(
            fn (): array => $this->service->saveWeight($this->userId(), $this->input()),
        );
    }

    #[NoAdminRequired]
    public function deleteWeight(string $id): DataResponse {
        return $this->respond(function () use ($id): array {
            $this->service->deleteWeight($this->userId(), $id);
            return ['ok' => true];
        });
    }

    #[NoAdminRequired]
    public function importData(): DataResponse {
        return $this->respond(
            fn (): array => [
                'imported' => $this->service->importData($this->userId(), $this->input()),
            ],
        );
    }

    /**
     * @return array<string, mixed>
     */
    private function input(): array {
        $content = trim($this->request->getContent());
        if ($content !== '') {
            $decoded = json_decode($content, true, 512, JSON_THROW_ON_ERROR);
            if (!is_array($decoded)) {
                throw new InvalidArgumentException('The request body must be a JSON object.');
            }
            return $decoded;
        }

        $params = $this->request->getParams();
        return is_array($params) ? $params : [];
    }

    private function userId(): string {
        $user = $this->userSession->getUser();
        if ($user === null) {
            throw new OutOfBoundsException('User not found.');
        }
        return $user->getUID();
    }

    private function respond(callable $callback): DataResponse {
        try {
            return new DataResponse($callback(), Http::STATUS_OK);
        } catch (OutOfBoundsException $exception) {
            return new DataResponse(
                ['message' => $exception->getMessage()],
                Http::STATUS_NOT_FOUND,
            );
        } catch (InvalidArgumentException | JsonException $exception) {
            return new DataResponse(
                ['message' => $exception->getMessage()],
                Http::STATUS_BAD_REQUEST,
            );
        } catch (DatabaseException $exception) {
            if (in_array($exception->getReason(), [
                DatabaseException::REASON_CONSTRAINT_VIOLATION,
                DatabaseException::REASON_UNIQUE_CONSTRAINT_VIOLATION,
            ], true)) {
                return new DataResponse(
                    ['message' => 'The data changed concurrently. Please retry the request.'],
                    Http::STATUS_CONFLICT,
                );
            }
            return new DataResponse(
                ['message' => 'KraftLog could not access its database.'],
                Http::STATUS_INTERNAL_SERVER_ERROR,
            );
        } catch (Throwable) {
            return new DataResponse(
                ['message' => 'KraftLog could not complete the request.'],
                Http::STATUS_INTERNAL_SERVER_ERROR,
            );
        }
    }
}
