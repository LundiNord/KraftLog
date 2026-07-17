<?php

declare(strict_types=1);

namespace OCA\KraftLog\Migration;

use Closure;
use OCP\DB\ISchemaWrapper;
use OCP\DB\Types;
use OCP\Migration\IOutput;
use OCP\Migration\SimpleMigrationStep;

final class Version1000Date20260717190000 extends SimpleMigrationStep {
    public function changeSchema(
        IOutput $output,
        Closure $schemaClosure,
        array $options,
    ): ?ISchemaWrapper {
        /** @var ISchemaWrapper $schema */
        $schema = $schemaClosure();

        if (!$schema->hasTable('kl_exercises')) {
            $table = $schema->createTable('kl_exercises');
            $table->addColumn('id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('user_id', Types::STRING, ['length' => 64, 'notnull' => true]);
            $table->addColumn('name', Types::STRING, ['length' => 255, 'notnull' => true]);
            $table->addColumn('category', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('primary_muscles', Types::TEXT, ['notnull' => true]);
            $table->addColumn('secondary_muscles', Types::TEXT, ['notnull' => true]);
            $table->addColumn('instructions', Types::TEXT, ['notnull' => true]);
            $table->addColumn('is_custom', Types::BOOLEAN, ['notnull' => true, 'default' => false]);
            $table->addColumn('created_at', Types::BIGINT, ['notnull' => true]);
            $table->setPrimaryKey(['id'], 'kl_exercises_pk');
            $table->addIndex(['user_id'], 'kl_exercises_user');
            $table->addUniqueIndex(['user_id', 'name'], 'kl_exercises_user_name');
        }

        if (!$schema->hasTable('kl_routines')) {
            $table = $schema->createTable('kl_routines');
            $table->addColumn('id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('user_id', Types::STRING, ['length' => 64, 'notnull' => true]);
            $table->addColumn('name', Types::STRING, ['length' => 255, 'notnull' => true]);
            $table->addColumn('description', Types::TEXT, ['notnull' => true]);
            $table->addColumn('created_at', Types::BIGINT, ['notnull' => true]);
            $table->addColumn('last_used_at', Types::BIGINT, ['notnull' => false]);
            $table->setPrimaryKey(['id'], 'kl_routines_pk');
            $table->addIndex(['user_id'], 'kl_routines_user');
        }

        if (!$schema->hasTable('kl_routine_items')) {
            $table = $schema->createTable('kl_routine_items');
            $table->addColumn('id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('user_id', Types::STRING, ['length' => 64, 'notnull' => true]);
            $table->addColumn('routine_id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('exercise_id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('order_index', Types::INTEGER, ['notnull' => true]);
            $table->addColumn('target_sets', Types::INTEGER, ['notnull' => true, 'default' => 3]);
            $table->addColumn('target_reps', Types::INTEGER, ['notnull' => true, 'default' => 10]);
            $table->addColumn('target_weight', Types::FLOAT, ['notnull' => false]);
            $table->addColumn('target_weights', Types::TEXT, ['notnull' => true]);
            $table->addColumn('target_reps_set', Types::TEXT, ['notnull' => true]);
            $table->addColumn('rest_seconds', Types::INTEGER, ['notnull' => true, 'default' => 90]);
            $table->addColumn('notes', Types::TEXT, ['notnull' => true]);
            $table->setPrimaryKey(['id'], 'kl_routine_items_pk');
            $table->addIndex(['user_id', 'routine_id'], 'kl_ritems_user_routine');
            $table->addIndex(['user_id', 'exercise_id'], 'kl_ritems_user_exercise');
            $table->addUniqueIndex(
                ['user_id', 'routine_id', 'exercise_id'],
                'kl_ritems_unique_exercise',
            );
        }

        if (!$schema->hasTable('kl_sessions')) {
            $table = $schema->createTable('kl_sessions');
            $table->addColumn('id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('user_id', Types::STRING, ['length' => 64, 'notnull' => true]);
            $table->addColumn('routine_id', Types::STRING, ['length' => 32, 'notnull' => false]);
            $table->addColumn('name', Types::STRING, ['length' => 255, 'notnull' => true]);
            $table->addColumn('started_at', Types::BIGINT, ['notnull' => true]);
            $table->addColumn('finished_at', Types::BIGINT, ['notnull' => false]);
            $table->addColumn('notes', Types::TEXT, ['notnull' => true]);
            $table->addColumn('session_type', Types::STRING, ['length' => 16, 'notnull' => true]);
            $table->addColumn('active_slot', Types::STRING, ['length' => 16, 'notnull' => false]);
            $table->setPrimaryKey(['id'], 'kl_sessions_pk');
            $table->addIndex(['user_id', 'started_at'], 'kl_sessions_user_start');
            $table->addIndex(['user_id', 'routine_id'], 'kl_sessions_user_routine');
            $table->addUniqueIndex(['user_id', 'started_at'], 'kl_sessions_unique_start');
            $table->addUniqueIndex(['user_id', 'active_slot'], 'kl_sessions_one_active');
        }

        if (!$schema->hasTable('kl_sets')) {
            $table = $schema->createTable('kl_sets');
            $table->addColumn('id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('user_id', Types::STRING, ['length' => 64, 'notnull' => true]);
            $table->addColumn('session_id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('exercise_id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('exercise_name', Types::STRING, ['length' => 255, 'notnull' => true]);
            $table->addColumn('set_number', Types::INTEGER, ['notnull' => true]);
            $table->addColumn('reps', Types::INTEGER, ['notnull' => true]);
            $table->addColumn('weight', Types::FLOAT, ['notnull' => true]);
            $table->addColumn('is_bodyweight', Types::BOOLEAN, ['notnull' => true, 'default' => false]);
            $table->addColumn('rpe', Types::FLOAT, ['notnull' => false]);
            $table->addColumn('logged_at', Types::BIGINT, ['notnull' => true]);
            $table->setPrimaryKey(['id'], 'kl_sets_pk');
            $table->addIndex(['user_id', 'session_id'], 'kl_sets_user_session');
            $table->addIndex(['user_id', 'exercise_id'], 'kl_sets_user_exercise');
        }

        if (!$schema->hasTable('kl_runs')) {
            $table = $schema->createTable('kl_runs');
            $table->addColumn('id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('user_id', Types::STRING, ['length' => 64, 'notnull' => true]);
            $table->addColumn('session_id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('distance', Types::FLOAT, ['notnull' => true]);
            $table->addColumn('duration', Types::BIGINT, ['notnull' => true]);
            $table->setPrimaryKey(['id'], 'kl_runs_pk');
            $table->addUniqueIndex(['user_id', 'session_id'], 'kl_runs_user_session');
        }

        if (!$schema->hasTable('kl_boulders')) {
            $table = $schema->createTable('kl_boulders');
            $table->addColumn('id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('user_id', Types::STRING, ['length' => 64, 'notnull' => true]);
            $table->addColumn('session_id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('description', Types::STRING, ['length' => 255, 'notnull' => true]);
            $table->addColumn('is_completed', Types::BOOLEAN, ['notnull' => true, 'default' => true]);
            $table->addColumn('created_at', Types::BIGINT, ['notnull' => true]);
            $table->setPrimaryKey(['id'], 'kl_boulders_pk');
            $table->addIndex(['user_id', 'session_id'], 'kl_boulders_user_session');
        }

        if (!$schema->hasTable('kl_weights')) {
            $table = $schema->createTable('kl_weights');
            $table->addColumn('id', Types::STRING, ['length' => 32, 'notnull' => true]);
            $table->addColumn('user_id', Types::STRING, ['length' => 64, 'notnull' => true]);
            $table->addColumn('entry_date', Types::BIGINT, ['notnull' => true]);
            $table->addColumn('weight', Types::FLOAT, ['notnull' => true]);
            $table->setPrimaryKey(['id'], 'kl_weights_pk');
            $table->addIndex(['user_id', 'entry_date'], 'kl_weights_user_date');
            $table->addUniqueIndex(['user_id', 'entry_date'], 'kl_weights_unique_date');
        }

        return $schema;
    }
}
