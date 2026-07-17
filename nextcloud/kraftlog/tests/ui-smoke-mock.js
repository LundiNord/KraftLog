(function () {
    'use strict'

    window.OC = { requestToken: 'smoke-test-token' }
    const now = Date.now()
    const exercises = [
        {
            id: 'exercise-1',
            name: 'Bankdrücken',
            category: 'STRENGTH',
            primaryMuscles: ['CHEST'],
            secondaryMuscles: ['TRICEPS', 'SHOULDERS'],
            instructions: 'Schulterblätter zurückziehen und kontrolliert absenken.',
            isCustom: false,
            createdAt: now - 100000,
        },
        {
            id: 'exercise-2',
            name: 'Rudern sitzend',
            category: 'STRENGTH',
            primaryMuscles: ['BACK'],
            secondaryMuscles: ['BICEPS'],
            instructions: '',
            isCustom: false,
            createdAt: now - 90000,
        },
        {
            id: 'exercise-3',
            name: 'Beinpresse',
            category: 'STRENGTH',
            primaryMuscles: ['QUADRICEPS'],
            secondaryMuscles: ['GLUTES', 'CALVES'],
            instructions: '',
            isCustom: true,
            createdAt: now - 80000,
        },
    ]
    const state = {
        version: 1,
        initialized: true,
        exercises,
        routines: [{
            id: 'routine-1',
            name: 'Ganzkörper A',
            description: 'Kompaktes Training für den Wochenstart',
            createdAt: now - 500000,
            lastUsedAt: now - 86400000,
            items: [
                { id: 'item-1', routineId: 'routine-1', exerciseId: 'exercise-1', orderIndex: 0, targetSets: 3, targetReps: 8, targetWeightKg: 70, targetWeightsPerSet: [], targetRepsPerSet: [], restSeconds: 120, notes: '' },
                { id: 'item-2', routineId: 'routine-1', exerciseId: 'exercise-2', orderIndex: 1, targetSets: 3, targetReps: 10, targetWeightKg: 60, targetWeightsPerSet: [], targetRepsPerSet: [], restSeconds: 90, notes: '' },
                { id: 'item-3', routineId: 'routine-1', exerciseId: 'exercise-3', orderIndex: 2, targetSets: 4, targetReps: 12, targetWeightKg: 140, targetWeightsPerSet: [], targetRepsPerSet: [], restSeconds: 120, notes: '' },
            ],
        }],
        sessions: [
            {
                id: 'session-1',
                routineId: 'routine-1',
                name: 'Ganzkörper A',
                startedAt: now - 86400000,
                finishedAt: now - 86400000 + 3900000,
                notes: 'Gute Einheit.',
                sessionType: 'STRENGTH',
                sets: [
                    { id: 'set-1', sessionId: 'session-1', exerciseId: 'exercise-1', exerciseName: 'Bankdrücken', setNumber: 1, reps: 8, weightKg: 70, isBodyweight: false, rpe: 8, loggedAt: now - 86300000 },
                    { id: 'set-2', sessionId: 'session-1', exerciseId: 'exercise-1', exerciseName: 'Bankdrücken', setNumber: 2, reps: 8, weightKg: 70, isBodyweight: false, rpe: 8.5, loggedAt: now - 86200000 },
                    { id: 'set-3', sessionId: 'session-1', exerciseId: 'exercise-2', exerciseName: 'Rudern sitzend', setNumber: 1, reps: 10, weightKg: 60, isBodyweight: false, rpe: null, loggedAt: now - 86100000 },
                ],
                running: null,
                boulders: [],
            },
            {
                id: 'session-2',
                routineId: null,
                name: 'Running',
                startedAt: now - 3 * 86400000,
                finishedAt: now - 3 * 86400000 + 1800000,
                notes: 'Lockerer Lauf am Fluss.',
                sessionType: 'RUNNING',
                sets: [],
                running: { id: 'run-1', sessionId: 'session-2', distanceKm: 5.2, durationSeconds: 1800 },
                boulders: [],
            },
            {
                id: 'session-3',
                routineId: null,
                name: 'Bouldering',
                startedAt: now - 6 * 86400000,
                finishedAt: now - 6 * 86400000 + 5400000,
                notes: '',
                sessionType: 'BOULDERING',
                sets: [],
                running: null,
                boulders: [
                    { id: 'b-1', sessionId: 'session-3', description: 'Blau 6a+', isCompleted: true, createdAt: now - 6 * 86400000 },
                    { id: 'b-2', sessionId: 'session-3', description: 'Schwarz 6c', isCompleted: false, createdAt: now - 6 * 86400000 + 60000 },
                ],
            },
        ],
        activeSession: null,
        weights: [
            { id: 'weight-1', date: now, weightKg: 78.2 },
            { id: 'weight-2', date: now - 7 * 86400000, weightKg: 78.8 },
            { id: 'weight-3', date: now - 30 * 86400000, weightKg: 80.1 },
        ],
    }

    window.fetch = async function (url, options = {}) {
        const method = options.method || 'GET'
        let responseData = state
        if (method === 'POST' && String(url).endsWith('/sessions')) {
            const input = JSON.parse(options.body || '{}')
            responseData = {
                ...input,
                id: input.id || 'active-session',
                sets: (input.sets || []).map((set, index) => ({
                    ...set,
                    id: 'active-set-' + index,
                    sessionId: input.id || 'active-session',
                })),
                running: input.running
                    ? { ...input.running, id: 'active-run', sessionId: input.id || 'active-session' }
                    : null,
                boulders: (input.boulders || []).map((route, index) => ({
                    ...route,
                    id: 'active-boulder-' + index,
                    sessionId: input.id || 'active-session',
                })),
            }
        }
        return new Response(JSON.stringify(responseData), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
        })
    }
})()
