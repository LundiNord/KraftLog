<?php

declare(strict_types=1);

return [
    'routes' => [
        ['name' => 'page#index', 'url' => '/', 'verb' => 'GET'],

        ['name' => 'api#state', 'url' => '/api/state', 'verb' => 'GET'],
        ['name' => 'api#initialize', 'url' => '/api/initialize', 'verb' => 'POST'],

        ['name' => 'api#saveExercise', 'url' => '/api/exercises', 'verb' => 'POST'],
        ['name' => 'api#deleteExercise', 'url' => '/api/exercises/{id}', 'verb' => 'DELETE'],

        ['name' => 'api#saveRoutine', 'url' => '/api/routines', 'verb' => 'POST'],
        ['name' => 'api#deleteRoutine', 'url' => '/api/routines/{id}', 'verb' => 'DELETE'],

        ['name' => 'api#saveSession', 'url' => '/api/sessions', 'verb' => 'POST'],
        ['name' => 'api#deleteSession', 'url' => '/api/sessions/{id}', 'verb' => 'DELETE'],

        ['name' => 'api#saveWeight', 'url' => '/api/weights', 'verb' => 'POST'],
        ['name' => 'api#deleteWeight', 'url' => '/api/weights/{id}', 'verb' => 'DELETE'],

        ['name' => 'api#importData', 'url' => '/api/import', 'verb' => 'POST'],
    ],
];
