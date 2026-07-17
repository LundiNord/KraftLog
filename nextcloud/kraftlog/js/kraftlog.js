(function () {
    'use strict'

    const ICONS = {
        home: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 11.5 12 4l9 7.5v8a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/></svg>',
        routines: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 4h15v4H5zm0 6h15v4H5zm0 6h15v4H5zM2 5h2v2H2zm0 6h2v2H2zm0 6h2v2H2z"/></svg>',
        exercises: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 8h2v8H4V14H2v-4h2zm14 0h2v2h2v4h-2v2h-2zM7 10h10v4H7z"/></svg>',
        history: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3a9 9 0 1 1-8.5 6H1l3.5-4L8 9H5.6A7 7 0 1 0 12 5zm-1 3h2v5.6l3.5 2.1-1 1.7-4.5-2.7z"/></svg>',
        weight: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 3h14a2 2 0 0 1 2 2v16H3V5a2 2 0 0 1 2-2zm7 3a5 5 0 0 0-5 5h10a5 5 0 0 0-5-5zm0 2 2 2h-4z"/></svg>',
        run: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14.5 3a2 2 0 1 1 0 4 2 2 0 0 1 0-4zM11 8l4 1 2.5 3H22v2h-5.5L15 12.3l-1 3.2 3 2.5v3h-2v-2l-4-2.5-2 4.5H6.8l3.5-8L8 12l-2 3H3.5L7 9.5z"/></svg>',
        boulder: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 21 3 16l3-7 4-2 2-4 5 1 4 6-1 8-4 3zm3-8 3 2 3-3 3 2 2-4-3-4-3 1-2 4-3-1z"/></svg>',
        plus: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M11 4h2v7h7v2h-7v7h-2v-7H4v-2h7z"/></svg>',
        close: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5.6 4.2 6.4 6.4 6.4-6.4 1.4 1.4-6.4 6.4 6.4 6.4-1.4 1.4-6.4-6.4-6.4 6.4-1.4-1.4 6.4-6.4-6.4-6.4z"/></svg>',
        play: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 4v16l13-8z"/></svg>',
        edit: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 16.5 10.8-10.8 3.5 3.5L7.5 20H4zm12.2-12.2 1.4-1.4a1.4 1.4 0 0 1 2 0l1.5 1.5a1.4 1.4 0 0 1 0 2l-1.4 1.4z"/></svg>',
        trash: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 3h8l1 2h4v2H3V5h4zm-2 6h12l-1 12H7zm4 2v7h2v-7zm4 0v7h2v-7z"/></svg>',
        back: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m14.5 5-7 7 7 7 1.5-1.5-5.5-5.5 5.5-5.5z"/></svg>',
        download: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M11 3h2v10l3.5-3.5 1.4 1.4-5.9 5.9-5.9-5.9 1.4-1.4L11 13zM4 19h16v2H4z"/></svg>',
        upload: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M11 17h2V7l3.5 3.5 1.4-1.4L12 3.2 6.1 9.1l1.4 1.4L11 7zM4 19h16v2H4z"/></svg>',
        check: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9.2 17.2-5-5 1.6-1.5 3.4 3.4 9-9 1.6 1.6z"/></svg>',
    }

    const CATEGORY_LABELS = {
        STRENGTH: 'Kraft',
        CARDIO: 'Ausdauer',
        FLEXIBILITY: 'Beweglichkeit',
        PLYOMETRICS: 'Plyometrie',
        CALISTHENICS: 'Calisthenics',
    }

    const MUSCLE_LABELS = {
        CHEST: 'Brust',
        BACK: 'Rücken',
        SHOULDERS: 'Schultern',
        BICEPS: 'Bizeps',
        TRICEPS: 'Trizeps',
        FOREARMS: 'Unterarme',
        CORE: 'Core',
        GLUTES: 'Gesäß',
        QUADRICEPS: 'Quadrizeps',
        HAMSTRINGS: 'Beinbeuger',
        CALVES: 'Waden',
        FULL_BODY: 'Ganzkörper',
    }

    const SESSION_LABELS = {
        STRENGTH: 'Krafttraining',
        RUNNING: 'Laufen',
        BOULDERING: 'Bouldern',
    }

    const app = {
        root: null,
        apiBase: '',
        data: null,
        view: 'home',
        modal: null,
        confirm: null,
        exerciseQuery: '',
        exerciseCategory: '',
        routineDraft: null,
        active: null,
        restEndsAt: null,
        clock: null,
        busy: false,
        toastTimer: null,
    }

    function icon(name) {
        return '<span class="kl-icon">' + (ICONS[name] || '') + '</span>'
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;')
    }

    function number(value, fallback = 0) {
        const parsed = Number(String(value ?? '').replace(',', '.'))
        return Number.isFinite(parsed) ? parsed : fallback
    }

    function integer(value, fallback = 0) {
        const parsed = Number.parseInt(String(value ?? ''), 10)
        return Number.isFinite(parsed) ? parsed : fallback
    }

    function clamp(value, minimum, maximum) {
        return Math.min(maximum, Math.max(minimum, value))
    }

    function formatNumber(value, maximumFractionDigits = 1) {
        return new Intl.NumberFormat(undefined, {
            maximumFractionDigits,
            minimumFractionDigits: 0,
        }).format(number(value))
    }

    function formatKg(value) {
        return formatNumber(value, 1) + ' kg'
    }

    function formatDate(timestamp, options) {
        if (!timestamp) {
            return '–'
        }
        return new Intl.DateTimeFormat(undefined, options || {
            dateStyle: 'medium',
            timeStyle: 'short',
        }).format(new Date(timestamp))
    }

    function formatDay(timestamp) {
        return formatDate(timestamp, { weekday: 'short', day: '2-digit', month: 'short' })
    }

    function formatMonth(timestamp) {
        return formatDate(timestamp, { month: 'long', year: 'numeric' })
    }

    function formatDuration(totalSeconds) {
        const safe = Math.max(0, Math.floor(number(totalSeconds)))
        const hours = Math.floor(safe / 3600)
        const minutes = Math.floor((safe % 3600) / 60)
        const seconds = safe % 60
        if (hours > 0) {
            return [hours, minutes, seconds].map(part => String(part).padStart(2, '0')).join(':')
        }
        return [minutes, seconds].map(part => String(part).padStart(2, '0')).join(':')
    }

    function durationOf(session) {
        const end = session.finishedAt || Date.now()
        return Math.max(0, Math.floor((end - session.startedAt) / 1000))
    }

    function toDateTimeInput(timestamp) {
        const date = new Date(timestamp || Date.now())
        const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
        return local.toISOString().slice(0, 16)
    }

    function parsePerSetNumbers(value, integersOnly) {
        return String(value || '')
            .split(';')
            .map(item => item.trim().replace(',', '.'))
            .filter(Boolean)
            .map(item => integersOnly ? integer(item, NaN) : number(item, NaN))
            .filter(Number.isFinite)
    }

    function startOfWeek(timestamp = Date.now()) {
        const date = new Date(timestamp)
        const day = date.getDay() || 7
        date.setHours(0, 0, 0, 0)
        date.setDate(date.getDate() - day + 1)
        return date.getTime()
    }

    function startOfMonth(timestamp = Date.now()) {
        const date = new Date(timestamp)
        date.setDate(1)
        date.setHours(0, 0, 0, 0)
        return date.getTime()
    }

    function finishedSessions() {
        return (app.data?.sessions || []).filter(session => session.finishedAt !== null)
    }

    function exerciseById(id) {
        return (app.data?.exercises || []).find(exercise => exercise.id === id)
    }

    function routineById(id) {
        return (app.data?.routines || []).find(routine => routine.id === id)
    }

    function sessionById(id) {
        return (app.data?.sessions || []).find(session => session.id === id)
    }

    function sessionMetrics(session) {
        if (session.sessionType === 'RUNNING') {
            return {
                primary: session.running ? formatNumber(session.running.distanceKm, 2) + ' km' : '0 km',
                secondary: session.running
                    ? formatDuration(session.running.durationSeconds)
                    : formatDuration(durationOf(session)),
            }
        }
        if (session.sessionType === 'BOULDERING') {
            const completed = (session.boulders || []).filter(route => route.isCompleted).length
            return {
                primary: completed + ' geschafft',
                secondary: (session.boulders || []).length + ' Routen',
            }
        }
        const sets = session.sets || []
        const volume = sets.reduce((sum, set) => sum + set.weightKg * set.reps, 0)
        return {
            primary: sets.length + ' Sätze',
            secondary: formatKg(volume),
        }
    }

    function sessionTypeIcon(type) {
        if (type === 'RUNNING') {
            return icon('run')
        }
        if (type === 'BOULDERING') {
            return icon('boulder')
        }
        return icon('exercises')
    }

    async function request(path, method = 'GET', body = null) {
        const headers = {
            Accept: 'application/json',
            requesttoken: window.OC?.requestToken || '',
        }
        const options = {
            method,
            credentials: 'same-origin',
            headers,
        }
        if (body !== null) {
            headers['Content-Type'] = 'application/json'
            options.body = JSON.stringify(body)
        }

        const response = await fetch(app.apiBase + path, options)
        const contentType = response.headers.get('content-type') || ''
        const payload = contentType.includes('application/json')
            ? await response.json()
            : { message: await response.text() }
        if (!response.ok) {
            throw new Error(payload.message || 'Die Anfrage ist fehlgeschlagen.')
        }
        return payload
    }

    async function withBusy(task) {
        if (app.busy) {
            return null
        }
        app.busy = true
        updateBusy()
        try {
            return await task()
        } catch (error) {
            showToast(error instanceof Error ? error.message : 'Ein Fehler ist aufgetreten.', true)
            return null
        } finally {
            app.busy = false
            updateBusy()
        }
    }

    function updateBusy() {
        const overlay = app.root?.querySelector('.kl-busy')
        if (overlay) {
            overlay.hidden = !app.busy
        }
    }

    function showToast(message, isError = false) {
        let toast = app.root?.querySelector('.kl-toast')
        if (!toast) {
            toast = document.createElement('div')
            toast.className = 'kl-toast'
            toast.setAttribute('role', 'status')
            app.root?.appendChild(toast)
        }
        toast.textContent = message
        toast.classList.toggle('kl-toast--error', isError)
        toast.classList.add('kl-toast--visible')
        window.clearTimeout(app.toastTimer)
        app.toastTimer = window.setTimeout(() => {
            toast?.classList.remove('kl-toast--visible')
        }, 3500)
    }

    async function reloadData(renderAfter = true) {
        app.data = await request('/state')
        if (!app.data.initialized) {
            app.data = await request('/initialize', 'POST', {})
        }
        if (renderAfter) {
            render()
        }
    }

    function render() {
        if (!app.root || !app.data) {
            return
        }
        const normalView = app.view === 'workout' && !app.active ? 'home' : app.view
        app.view = normalView
        app.root.innerHTML = `
            <div class="kl-shell">
                ${renderNavigation()}
                <main class="kl-main">
                    ${app.active && app.view === 'workout' ? renderWorkout() : renderCurrentView()}
                </main>
            </div>
            ${renderModal()}
            <div class="kl-busy" ${app.busy ? '' : 'hidden'} aria-live="polite">
                <span class="kraftlog-spinner" aria-hidden="true"></span>
                <span>Wird gespeichert…</span>
            </div>
            <div class="kl-toast" role="status"></div>
        `
        startClock()
    }

    function renderNavigation() {
        const items = [
            ['home', 'home', 'Übersicht'],
            ['routines', 'routines', 'Routinen'],
            ['exercises', 'exercises', 'Übungen'],
            ['history', 'history', 'Verlauf'],
            ['weight', 'weight', 'Gewicht'],
        ]
        return `
            <aside class="kl-navigation" aria-label="KraftLog Navigation">
                <div class="kl-brand">
                    <span class="kl-brand__mark">${icon('exercises')}</span>
                    <span>KraftLog</span>
                </div>
                <nav class="kl-navigation__items">
                    ${items.map(([view, iconName, label]) => `
                        <button
                            type="button"
                            class="kl-nav-item ${app.view === view && !app.active ? 'is-active' : ''}"
                            data-action="navigate"
                            data-view="${view}"
                        >
                            ${icon(iconName)}
                            <span>${label}</span>
                        </button>
                    `).join('')}
                </nav>
                <button type="button" class="kl-quick-button" data-action="quick-menu">
                    ${icon('plus')}<span>Training starten</span>
                </button>
            </aside>
        `
    }

    function renderCurrentView() {
        switch (app.view) {
            case 'routines':
                return renderRoutines()
            case 'exercises':
                return renderExercises()
            case 'history':
                return renderHistory()
            case 'weight':
                return renderWeight()
            default:
                return renderHome()
        }
    }

    function pageHeader(eyebrow, title, description, action = '') {
        return `
            <header class="kl-page-header">
                <div>
                    <span class="kl-eyebrow">${escapeHtml(eyebrow)}</span>
                    <h1>${escapeHtml(title)}</h1>
                    ${description ? `<p>${escapeHtml(description)}</p>` : ''}
                </div>
                ${action}
            </header>
        `
    }

    function statCard(label, value, hint, tone = '') {
        return `
            <article class="kl-stat ${tone ? 'kl-stat--' + tone : ''}">
                <span>${escapeHtml(label)}</span>
                <strong>${escapeHtml(value)}</strong>
                <small>${escapeHtml(hint)}</small>
            </article>
        `
    }

    function renderHome() {
        const sessions = finishedSessions()
        const weekly = sessions.filter(session => session.startedAt >= startOfWeek()).length
        const monthly = sessions.filter(session => session.startedAt >= startOfMonth()).length
        const totalVolume = sessions.flatMap(session => session.sets || [])
            .reduce((sum, set) => sum + set.weightKg * set.reps, 0)
        const active = app.data.activeSession
        const routines = app.data.routines || []

        return `
            ${pageHeader(
                formatDate(Date.now(), { weekday: 'long', day: 'numeric', month: 'long' }),
                'Dein Training',
                'Alles im Blick – privat in deiner Nextcloud.',
            )}
            ${active ? `
                <section class="kl-resume-card">
                    <div class="kl-resume-card__icon">${sessionTypeIcon(active.sessionType)}</div>
                    <div>
                        <span class="kl-eyebrow">Aktives Training</span>
                        <h2>${escapeHtml(active.name)}</h2>
                        <p>Gestartet ${escapeHtml(formatDate(active.startedAt))} · ${formatDuration(durationOf(active))}</p>
                    </div>
                    <div class="kl-card-actions">
                        <button type="button" class="primary" data-action="resume-session" data-id="${escapeHtml(active.id)}">
                            ${icon('play')}Fortsetzen
                        </button>
                        <button type="button" data-action="confirm-discard-session" data-id="${escapeHtml(active.id)}">
                            Verwerfen
                        </button>
                    </div>
                </section>
            ` : ''}
            <section class="kl-stats-grid" aria-label="Trainingsstatistik">
                ${statCard('Diese Woche', String(weekly), weekly === 1 ? 'Training' : 'Trainings', 'accent')}
                ${statCard('Dieser Monat', String(monthly), monthly === 1 ? 'Einheit' : 'Einheiten')}
                ${statCard('Gesamt', String(sessions.length), 'abgeschlossene Einheiten')}
                ${statCard('Trainingsvolumen', formatKg(totalVolume), 'Gewicht × Wiederholungen')}
            </section>
            <section class="kl-section">
                <div class="kl-section-heading">
                    <div>
                        <span class="kl-eyebrow">Schnellstart</span>
                        <h2>Was steht heute an?</h2>
                    </div>
                </div>
                <div class="kl-workout-types">
                    <button type="button" class="kl-workout-type kl-workout-type--strength" data-action="start-strength">
                        <span>${icon('exercises')}</span>
                        <strong>Krafttraining</strong>
                        <small>Frei zusammenstellen</small>
                    </button>
                    <button type="button" class="kl-workout-type kl-workout-type--run" data-action="start-running">
                        <span>${icon('run')}</span>
                        <strong>Laufen</strong>
                        <small>Zeit, Distanz & Pace</small>
                    </button>
                    <button type="button" class="kl-workout-type kl-workout-type--boulder" data-action="start-bouldering">
                        <span>${icon('boulder')}</span>
                        <strong>Bouldern</strong>
                        <small>Versuche & Tops erfassen</small>
                    </button>
                </div>
            </section>
            <section class="kl-section">
                <div class="kl-section-heading">
                    <div>
                        <span class="kl-eyebrow">Routinen</span>
                        <h2>Direkt loslegen</h2>
                    </div>
                    <button type="button" data-action="navigate" data-view="routines">Alle Routinen</button>
                </div>
                ${routines.length ? `
                    <div class="kl-card-grid">
                        ${routines.slice(0, 3).map(renderRoutineCard).join('')}
                    </div>
                ` : renderEmpty('Noch keine Routinen', 'Lege deine erste Trainingsroutine an.', 'new-routine', 'Routine anlegen')}
            </section>
            <section class="kl-section">
                <div class="kl-section-heading">
                    <div>
                        <span class="kl-eyebrow">Zuletzt</span>
                        <h2>Deine letzten Einheiten</h2>
                    </div>
                    <button type="button" data-action="navigate" data-view="history">Zum Verlauf</button>
                </div>
                ${sessions.length ? `
                    <div class="kl-session-list">
                        ${sessions.slice(0, 5).map(renderSessionRow).join('')}
                    </div>
                ` : renderEmpty('Noch kein Verlauf', 'Dein erstes abgeschlossenes Training erscheint hier.')}
            </section>
        `
    }

    function renderRoutineCard(routine) {
        const muscleNames = [...new Set((routine.items || [])
            .flatMap(item => exerciseById(item.exerciseId)?.primaryMuscles || [])
            .map(muscle => MUSCLE_LABELS[muscle] || muscle))]
        return `
            <article class="kl-card kl-routine-card">
                <div class="kl-card__topline">
                    <span class="kl-badge">${routine.items.length} Übungen</span>
                    <button
                        type="button"
                        class="kl-icon-button"
                        data-action="edit-routine"
                        data-id="${escapeHtml(routine.id)}"
                        aria-label="Routine bearbeiten"
                    >${icon('edit')}</button>
                </div>
                <h3>${escapeHtml(routine.name)}</h3>
                <p>${escapeHtml(routine.description || muscleNames.slice(0, 4).join(' · ') || 'Individuelle Routine')}</p>
                <button type="button" class="primary kl-full-button" data-action="start-routine" data-id="${escapeHtml(routine.id)}">
                    ${icon('play')}Training starten
                </button>
            </article>
        `
    }

    function renderSessionRow(session) {
        const metrics = sessionMetrics(session)
        return `
            <button
                type="button"
                class="kl-session-row"
                data-action="session-detail"
                data-id="${escapeHtml(session.id)}"
            >
                <span class="kl-session-row__icon">${sessionTypeIcon(session.sessionType)}</span>
                <span class="kl-session-row__main">
                    <strong>${escapeHtml(session.name)}</strong>
                    <small>${escapeHtml(formatDate(session.startedAt))} · ${formatDuration(durationOf(session))}</small>
                </span>
                <span class="kl-session-row__metric">
                    <strong>${escapeHtml(metrics.primary)}</strong>
                    <small>${escapeHtml(metrics.secondary)}</small>
                </span>
                <span class="kl-chevron" aria-hidden="true">›</span>
            </button>
        `
    }

    function renderRoutines() {
        const routines = app.data.routines || []
        return `
            ${pageHeader(
                'Vorlagen',
                'Routinen',
                'Plane Sätze, Wiederholungen, Gewichte und Pausen.',
                `<button type="button" class="primary" data-action="new-routine">${icon('plus')}Neue Routine</button>`,
            )}
            ${routines.length ? `
                <div class="kl-card-grid kl-card-grid--wide">
                    ${routines.map(routine => `
                        <article class="kl-card kl-routine-card">
                            <div class="kl-card__topline">
                                <span class="kl-badge">${routine.items.length} Übungen</span>
                                <div class="kl-inline-actions">
                                    <button type="button" class="kl-icon-button" data-action="edit-routine" data-id="${escapeHtml(routine.id)}" aria-label="Bearbeiten">${icon('edit')}</button>
                                    <button type="button" class="kl-icon-button kl-danger" data-action="confirm-delete-routine" data-id="${escapeHtml(routine.id)}" aria-label="Löschen">${icon('trash')}</button>
                                </div>
                            </div>
                            <h3>${escapeHtml(routine.name)}</h3>
                            <p>${escapeHtml(routine.description || 'Keine Beschreibung')}</p>
                            <ol class="kl-routine-preview">
                                ${routine.items.slice(0, 5).map(item => `
                                    <li>${escapeHtml(exerciseById(item.exerciseId)?.name || 'Unbekannte Übung')}<span>${item.targetSets} × ${item.targetReps}</span></li>
                                `).join('')}
                                ${routine.items.length > 5 ? `<li class="kl-muted">+ ${routine.items.length - 5} weitere</li>` : ''}
                            </ol>
                            <button type="button" class="primary kl-full-button" data-action="start-routine" data-id="${escapeHtml(routine.id)}">
                                ${icon('play')}Training starten
                            </button>
                        </article>
                    `).join('')}
                </div>
            ` : renderEmpty('Noch keine Routinen', 'Erstelle eine wiederverwendbare Vorlage für dein Training.', 'new-routine', 'Routine anlegen')}
        `
    }

    function renderExercises() {
        const query = app.exerciseQuery.trim().toLocaleLowerCase()
        const category = app.exerciseCategory
        const exercises = (app.data.exercises || []).filter(exercise => {
            const queryMatches = !query || exercise.name.toLocaleLowerCase().includes(query)
            const categoryMatches = !category || exercise.category === category
            return queryMatches && categoryMatches
        })
        return `
            ${pageHeader(
                'Bibliothek',
                'Übungen',
                `${app.data.exercises.length} Übungen für deine Trainingsplanung.`,
                `<button type="button" class="primary" data-action="new-exercise">${icon('plus')}Neue Übung</button>`,
            )}
            <section class="kl-filter-bar">
                <label class="kl-search">
                    <span class="sr-only">Übung suchen</span>
                    <input
                        type="search"
                        placeholder="Übung suchen…"
                        value="${escapeHtml(app.exerciseQuery)}"
                        data-filter="exercise-query"
                    >
                </label>
                <label>
                    <span class="sr-only">Kategorie</span>
                    <select data-filter="exercise-category">
                        <option value="">Alle Kategorien</option>
                        ${Object.entries(CATEGORY_LABELS).map(([value, label]) => `
                            <option value="${value}" ${category === value ? 'selected' : ''}>${escapeHtml(label)}</option>
                        `).join('')}
                    </select>
                </label>
                <span class="kl-filter-count">${exercises.length} Treffer</span>
            </section>
            ${exercises.length ? `
                <div class="kl-exercise-grid">
                    ${exercises.map(exercise => {
                        const muscles = exercise.primaryMuscles.map(muscle => MUSCLE_LABELS[muscle] || muscle)
                        return `
                            <article class="kl-exercise-card">
                                <button type="button" class="kl-exercise-card__body" data-action="exercise-detail" data-id="${escapeHtml(exercise.id)}">
                                    <span class="kl-exercise-card__icon">${icon('exercises')}</span>
                                    <span>
                                        <strong>${escapeHtml(exercise.name)}</strong>
                                        <small>${escapeHtml(CATEGORY_LABELS[exercise.category] || exercise.category)} · ${escapeHtml(muscles.join(', ') || 'Keine Muskelgruppe')}</small>
                                    </span>
                                </button>
                                <button type="button" class="kl-icon-button" data-action="edit-exercise" data-id="${escapeHtml(exercise.id)}" aria-label="Übung bearbeiten">${icon('edit')}</button>
                            </article>
                        `
                    }).join('')}
                </div>
            ` : renderEmpty('Keine Übungen gefunden', 'Passe Suche oder Kategorie an.')}
        `
    }

    function renderHistory() {
        const sessions = finishedSessions()
        const allSets = sessions.flatMap(session => session.sets || [])
        const volume = allSets.reduce((sum, set) => sum + set.weightKg * set.reps, 0)
        const reps = allSets.reduce((sum, set) => sum + set.reps, 0)
        const grouped = new Map()
        sessions.forEach(session => {
            const month = formatMonth(session.startedAt)
            if (!grouped.has(month)) {
                grouped.set(month, [])
            }
            grouped.get(month).push(session)
        })
        return `
            ${pageHeader(
                'Fortschritt',
                'Verlauf',
                'Alle abgeschlossenen Einheiten und persönlichen Daten.',
                `<div class="kl-header-actions">
                    <button type="button" data-action="export-data">${icon('download')}Export</button>
                    <button type="button" data-action="open-import">${icon('upload')}Import</button>
                    <input type="file" accept="application/json,.json" data-import-file hidden>
                </div>`,
            )}
            <section class="kl-stats-grid">
                ${statCard('Einheiten', String(sessions.length), 'insgesamt', 'accent')}
                ${statCard('Sätze', String(allSets.length), 'geloggte Sätze')}
                ${statCard('Wiederholungen', formatNumber(reps, 0), 'insgesamt')}
                ${statCard('Volumen', formatKg(volume), 'insgesamt')}
            </section>
            ${sessions.length ? [...grouped.entries()].map(([month, monthSessions]) => `
                <section class="kl-section kl-history-group">
                    <div class="kl-section-heading"><h2>${escapeHtml(month)}</h2></div>
                    <div class="kl-session-list">
                        ${monthSessions.map(renderSessionRow).join('')}
                    </div>
                </section>
            `).join('') : renderEmpty('Noch keine Einheiten', 'Abgeschlossene Trainings erscheinen automatisch hier.')}
        `
    }

    function weightStats(entries) {
        if (!entries.length) {
            return null
        }
        const latest = entries[0]
        const oldest = entries[entries.length - 1]
        const values = entries.map(entry => entry.weightKg)
        const findNear = days => {
            const target = latest.date - days * 86400000
            return entries.slice(1).reduce((best, entry) => {
                if (!best) {
                    return entry
                }
                return Math.abs(entry.date - target) < Math.abs(best.date - target) ? entry : best
            }, null)
        }
        const seven = findNear(7)
        const thirty = findNear(30)
        return {
            latest,
            change7: seven ? latest.weightKg - seven.weightKg : null,
            change30: thirty ? latest.weightKg - thirty.weightKg : null,
            total: latest.weightKg - oldest.weightKg,
            min: Math.min(...values),
            max: Math.max(...values),
            average: values.reduce((sum, value) => sum + value, 0) / values.length,
        }
    }

    function renderWeightChart(entries) {
        const points = [...entries].reverse().slice(-60)
        if (points.length < 2) {
            return `<div class="kl-chart-empty">Mindestens zwei Einträge zeigen den Trend.</div>`
        }
        const weights = points.map(entry => entry.weightKg)
        const minimum = Math.min(...weights)
        const maximum = Math.max(...weights)
        const range = Math.max(maximum - minimum, 1)
        const polyline = points.map((entry, index) => {
            const x = 5 + (index / (points.length - 1)) * 90
            const y = 92 - ((entry.weightKg - minimum) / range) * 78
            return `${x.toFixed(2)},${y.toFixed(2)}`
        }).join(' ')
        const area = `5,95 ${polyline} 95,95`
        return `
            <svg class="kl-weight-chart" viewBox="0 0 100 100" role="img" aria-label="Gewichtsverlauf">
                <line x1="5" y1="14" x2="95" y2="14"></line>
                <line x1="5" y1="53" x2="95" y2="53"></line>
                <line x1="5" y1="92" x2="95" y2="92"></line>
                <polygon points="${area}"></polygon>
                <polyline points="${polyline}"></polyline>
            </svg>
            <div class="kl-chart-labels"><span>${formatKg(minimum)}</span><span>${formatKg(maximum)}</span></div>
        `
    }

    function renderDelta(value) {
        if (value === null) {
            return '–'
        }
        const prefix = value > 0 ? '+' : ''
        return prefix + formatNumber(value, 1) + ' kg'
    }

    function renderWeight() {
        const entries = app.data.weights || []
        const stats = weightStats(entries)
        return `
            ${pageHeader(
                'Körperdaten',
                'Gewicht',
                'Trend und Veränderungen auf einen Blick.',
            )}
            <section class="kl-weight-layout">
                <article class="kl-card kl-weight-entry-card">
                    <span class="kl-eyebrow">Neuer Eintrag</span>
                    <h2>Gewicht erfassen</h2>
                    <form data-form="weight" class="kl-form">
                        <label>
                            <span>Gewicht in kg</span>
                            <input name="weightKg" type="number" inputmode="decimal" min="1" max="1000" step="0.1" required placeholder="75,0">
                        </label>
                        <label>
                            <span>Datum und Uhrzeit</span>
                            <input name="date" type="datetime-local" required value="${toDateTimeInput(Date.now())}">
                        </label>
                        <button type="submit" class="primary">${icon('plus')}Eintrag speichern</button>
                    </form>
                </article>
                <article class="kl-card kl-weight-chart-card">
                    <div class="kl-card__topline">
                        <div>
                            <span class="kl-eyebrow">Aktuell</span>
                            <h2>${stats ? formatKg(stats.latest.weightKg) : 'Noch kein Eintrag'}</h2>
                        </div>
                        ${stats ? `<span class="kl-badge">${entries.length} Messungen</span>` : ''}
                    </div>
                    ${renderWeightChart(entries)}
                </article>
            </section>
            ${stats ? `
                <section class="kl-stats-grid kl-stats-grid--six">
                    ${statCard('7 Tage', renderDelta(stats.change7), 'Veränderung')}
                    ${statCard('30 Tage', renderDelta(stats.change30), 'Veränderung')}
                    ${statCard('Gesamt', renderDelta(stats.total), 'seit Beginn')}
                    ${statCard('Minimum', formatKg(stats.min), 'Tiefstwert')}
                    ${statCard('Durchschnitt', formatKg(stats.average), 'Mittelwert')}
                    ${statCard('Maximum', formatKg(stats.max), 'Höchstwert')}
                </section>
            ` : ''}
            <section class="kl-section">
                <div class="kl-section-heading"><h2>Messverlauf</h2></div>
                ${entries.length ? `
                    <div class="kl-table-wrap">
                        <table class="kl-table">
                            <thead><tr><th>Datum</th><th>Gewicht</th><th></th></tr></thead>
                            <tbody>
                                ${entries.map(entry => `
                                    <tr>
                                        <td>${escapeHtml(formatDate(entry.date))}</td>
                                        <td><strong>${escapeHtml(formatKg(entry.weightKg))}</strong></td>
                                        <td><button type="button" class="kl-icon-button kl-danger" data-action="confirm-delete-weight" data-id="${escapeHtml(entry.id)}" aria-label="Eintrag löschen">${icon('trash')}</button></td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                ` : renderEmpty('Noch keine Gewichtsdaten', 'Nutze das Formular, um deinen ersten Messwert zu speichern.')}
            </section>
        `
    }

    function renderEmpty(title, text, action = '', actionLabel = '') {
        return `
            <div class="kl-empty">
                <span class="kl-empty__icon">${icon('exercises')}</span>
                <h3>${escapeHtml(title)}</h3>
                <p>${escapeHtml(text)}</p>
                ${action ? `<button type="button" class="primary" data-action="${action}">${icon('plus')}${escapeHtml(actionLabel)}</button>` : ''}
            </div>
        `
    }

    function modalShell(title, content, wide = false) {
        return `
            <div class="kl-modal-backdrop" data-action="close-modal">
                <section class="kl-modal ${wide ? 'kl-modal--wide' : ''}" role="dialog" aria-modal="true" aria-label="${escapeHtml(title)}" data-modal-panel>
                    <header class="kl-modal__header">
                        <h2>${escapeHtml(title)}</h2>
                        <button type="button" class="kl-icon-button" data-action="close-modal" aria-label="Schließen">${icon('close')}</button>
                    </header>
                    <div class="kl-modal__body">${content}</div>
                </section>
            </div>
        `
    }

    function renderModal() {
        if (app.confirm) {
            return renderConfirm()
        }
        if (!app.modal) {
            return ''
        }
        switch (app.modal.type) {
            case 'quick':
                return renderQuickModal()
            case 'exercise-editor':
                return renderExerciseEditor()
            case 'exercise-detail':
                return renderExerciseDetail()
            case 'routine-editor':
                return renderRoutineEditor()
            case 'session-detail':
                return renderSessionDetail()
            case 'add-workout-exercise':
                return renderAddWorkoutExercise()
            default:
                return ''
        }
    }

    function renderConfirm() {
        return modalShell(app.confirm.title, `
            <p class="kl-confirm-text">${escapeHtml(app.confirm.text)}</p>
            <div class="kl-modal__actions">
                <button type="button" data-action="cancel-confirm">Abbrechen</button>
                <button type="button" class="primary kl-button--danger" data-action="execute-confirm">${escapeHtml(app.confirm.label || 'Löschen')}</button>
            </div>
        `)
    }

    function renderQuickModal() {
        const routines = app.data.routines || []
        return modalShell('Training starten', `
            <div class="kl-quick-modal">
                <button type="button" class="kl-quick-choice" data-action="start-strength">
                    ${icon('exercises')}<span><strong>Freies Krafttraining</strong><small>Übungen während des Trainings hinzufügen</small></span>
                </button>
                <button type="button" class="kl-quick-choice" data-action="start-running">
                    ${icon('run')}<span><strong>Laufen</strong><small>Zeit und Distanz aufzeichnen</small></span>
                </button>
                <button type="button" class="kl-quick-choice" data-action="start-bouldering">
                    ${icon('boulder')}<span><strong>Bouldern</strong><small>Routen und Versuche loggen</small></span>
                </button>
                ${routines.length ? `
                    <div class="kl-divider-label"><span>Oder aus einer Routine</span></div>
                    ${routines.map(routine => `
                        <button type="button" class="kl-quick-choice" data-action="start-routine" data-id="${escapeHtml(routine.id)}">
                            ${icon('play')}<span><strong>${escapeHtml(routine.name)}</strong><small>${routine.items.length} Übungen</small></span>
                        </button>
                    `).join('')}
                ` : ''}
            </div>
        `)
    }

    function renderExerciseEditor() {
        const exercise = app.modal.exercise
        const currentPrimary = exercise?.primaryMuscles || []
        const currentSecondary = exercise?.secondaryMuscles || []
        return modalShell(exercise ? 'Übung bearbeiten' : 'Neue Übung', `
            <form data-form="exercise" class="kl-form">
                <input type="hidden" name="id" value="${escapeHtml(exercise?.id || '')}">
                <label>
                    <span>Name</span>
                    <input name="name" maxlength="255" required value="${escapeHtml(exercise?.name || '')}" placeholder="z. B. Kniebeuge">
                </label>
                <label>
                    <span>Kategorie</span>
                    <select name="category">
                        ${Object.entries(CATEGORY_LABELS).map(([value, label]) => `
                            <option value="${value}" ${exercise?.category === value ? 'selected' : ''}>${escapeHtml(label)}</option>
                        `).join('')}
                    </select>
                </label>
                <fieldset>
                    <legend>Primäre Muskelgruppen</legend>
                    <div class="kl-checkbox-grid">
                        ${Object.entries(MUSCLE_LABELS).map(([value, label]) => `
                            <label class="kl-check">
                                <input type="checkbox" name="primaryMuscles" value="${value}" ${currentPrimary.includes(value) ? 'checked' : ''}>
                                <span>${escapeHtml(label)}</span>
                            </label>
                        `).join('')}
                    </div>
                </fieldset>
                <fieldset>
                    <legend>Sekundäre Muskelgruppen</legend>
                    <div class="kl-checkbox-grid">
                        ${Object.entries(MUSCLE_LABELS).map(([value, label]) => `
                            <label class="kl-check">
                                <input type="checkbox" name="secondaryMuscles" value="${value}" ${currentSecondary.includes(value) ? 'checked' : ''}>
                                <span>${escapeHtml(label)}</span>
                            </label>
                        `).join('')}
                    </div>
                </fieldset>
                <label>
                    <span>Anleitung oder Notizen</span>
                    <textarea name="instructions" rows="4" maxlength="10000" placeholder="Ausführung, Setup, Hinweise…">${escapeHtml(exercise?.instructions || '')}</textarea>
                </label>
                <div class="kl-modal__actions">
                    ${exercise?.isCustom ? `<button type="button" class="kl-button--danger" data-action="confirm-delete-exercise" data-id="${escapeHtml(exercise.id)}">${icon('trash')}Löschen</button>` : '<span></span>'}
                    <button type="submit" class="primary">${icon('check')}Speichern</button>
                </div>
            </form>
        `, true)
    }

    function exerciseRecords(exercise) {
        const sets = finishedSessions()
            .flatMap(session => session.sets || [])
            .filter(set => set.exerciseId === exercise.id)
        const weighted = sets.filter(set => !set.isBodyweight && set.weightKg > 0)
        const volume = sets.reduce((sum, set) => sum + set.weightKg * set.reps, 0)
        return {
            sets,
            bestWeight: weighted.length ? Math.max(...weighted.map(set => set.weightKg)) : null,
            estimatedOneRepMax: weighted.length
                ? Math.max(...weighted.map(set => set.weightKg * (1 + set.reps / 30)))
                : null,
            maxReps: sets.length ? Math.max(...sets.map(set => set.reps)) : null,
            sessions: new Set(sets.map(set => set.sessionId)).size,
            volume,
        }
    }

    function renderExerciseDetail() {
        const exercise = app.modal.exercise
        const records = exerciseRecords(exercise)
        const recent = [...records.sets].sort((a, b) => b.loggedAt - a.loggedAt).slice(0, 12)
        return modalShell(exercise.name, `
            <div class="kl-detail-lead">
                <span class="kl-detail-lead__icon">${icon('exercises')}</span>
                <div>
                    <span class="kl-badge">${escapeHtml(CATEGORY_LABELS[exercise.category] || exercise.category)}</span>
                    <p>${escapeHtml(exercise.instructions || 'Noch keine Anleitung hinterlegt.')}</p>
                </div>
            </div>
            <div class="kl-muscle-tags">
                ${exercise.primaryMuscles.map(muscle => `<span>${escapeHtml(MUSCLE_LABELS[muscle] || muscle)}</span>`).join('')}
                ${exercise.secondaryMuscles.map(muscle => `<span class="secondary">${escapeHtml(MUSCLE_LABELS[muscle] || muscle)}</span>`).join('')}
            </div>
            <div class="kl-stats-grid kl-stats-grid--compact">
                ${statCard('Bestgewicht', records.bestWeight === null ? '–' : formatKg(records.bestWeight), 'persönlicher Rekord')}
                ${statCard('Geschätztes 1RM', records.estimatedOneRepMax === null ? '–' : formatKg(records.estimatedOneRepMax), 'Epley-Formel')}
                ${statCard('Einheiten', String(records.sessions), 'mit dieser Übung')}
                ${statCard('Volumen', formatKg(records.volume), 'insgesamt')}
            </div>
            <div class="kl-section-heading"><h3>Letzte Sätze</h3></div>
            ${recent.length ? `
                <div class="kl-table-wrap">
                    <table class="kl-table">
                        <thead><tr><th>Datum</th><th>Gewicht</th><th>Wdh.</th><th>RPE</th></tr></thead>
                        <tbody>${recent.map(set => `
                            <tr>
                                <td>${escapeHtml(formatDay(set.loggedAt))}</td>
                                <td>${set.isBodyweight ? 'Körpergewicht' : escapeHtml(formatKg(set.weightKg))}</td>
                                <td>${set.reps}</td>
                                <td>${set.rpe ?? '–'}</td>
                            </tr>
                        `).join('')}</tbody>
                    </table>
                </div>
            ` : renderEmpty('Noch keine Sätze', 'Sobald du diese Übung trainierst, erscheinen die Werte hier.')}
            <div class="kl-modal__actions">
                <span></span>
                <button type="button" class="primary" data-action="edit-exercise" data-id="${escapeHtml(exercise.id)}">${icon('edit')}Bearbeiten</button>
            </div>
        `, true)
    }

    function renderRoutineEditor() {
        const draft = app.routineDraft
        const exercises = app.data.exercises || []
        return modalShell(draft.id ? 'Routine bearbeiten' : 'Neue Routine', `
            <form data-form="routine" class="kl-form">
                <div class="kl-form-row">
                    <label>
                        <span>Name</span>
                        <input
                            required
                            maxlength="255"
                            value="${escapeHtml(draft.name)}"
                            data-routine-field="name"
                            placeholder="z. B. Push Day"
                        >
                    </label>
                    <label>
                        <span>Beschreibung</span>
                        <input
                            maxlength="10000"
                            value="${escapeHtml(draft.description)}"
                            data-routine-field="description"
                            placeholder="Optional"
                        >
                    </label>
                </div>
                <div class="kl-routine-editor-heading">
                    <div>
                        <span class="kl-eyebrow">Trainingsplan</span>
                        <h3>${draft.items.length} Übungen</h3>
                    </div>
                    <button type="button" data-action="routine-add-item" ${exercises.length ? '' : 'disabled'}>${icon('plus')}Übung hinzufügen</button>
                </div>
                <div class="kl-routine-items">
                    ${draft.items.length ? draft.items.map((item, index) => `
                        <article class="kl-routine-item">
                            <div class="kl-routine-item__head">
                                <span class="kl-drag-number">${index + 1}</span>
                                <select data-routine-index="${index}" data-item-field="exerciseId" aria-label="Übung">
                                    ${exercises.map(exercise => `
                                        <option value="${escapeHtml(exercise.id)}" ${exercise.id === item.exerciseId ? 'selected' : ''}>${escapeHtml(exercise.name)}</option>
                                    `).join('')}
                                </select>
                                <div class="kl-inline-actions">
                                    <button type="button" class="kl-icon-button" data-action="routine-move-up" data-index="${index}" ${index === 0 ? 'disabled' : ''} aria-label="Nach oben">↑</button>
                                    <button type="button" class="kl-icon-button" data-action="routine-move-down" data-index="${index}" ${index === draft.items.length - 1 ? 'disabled' : ''} aria-label="Nach unten">↓</button>
                                    <button type="button" class="kl-icon-button kl-danger" data-action="routine-remove-item" data-index="${index}" aria-label="Entfernen">${icon('trash')}</button>
                                </div>
                            </div>
                            <div class="kl-routine-item__fields">
                                <label><span>Sätze</span><input type="number" min="1" max="20" value="${item.targetSets}" data-routine-index="${index}" data-item-field="targetSets"></label>
                                <label><span>Wdh.</span><input type="number" min="0" max="1000" value="${item.targetReps}" data-routine-index="${index}" data-item-field="targetReps"></label>
                                <label><span>Gewicht kg</span><input type="number" min="0" max="10000" step="0.1" value="${escapeHtml(item.targetWeightKg ?? '')}" data-routine-index="${index}" data-item-field="targetWeightKg" placeholder="optional"></label>
                                <label><span>Pause Sek.</span><input type="number" min="0" max="3600" value="${item.restSeconds}" data-routine-index="${index}" data-item-field="restSeconds"></label>
                            </div>
                            <details>
                                <summary>Ziele pro Satz & Notizen</summary>
                                <div class="kl-routine-item__details">
                                    <label>
                                        <span>Gewichte pro Satz (mit ; trennen)</span>
                                        <input value="${escapeHtml((item.targetWeightsPerSet || []).join('; '))}" data-routine-index="${index}" data-item-field="targetWeightsPerSet" placeholder="40; 45; 45">
                                    </label>
                                    <label>
                                        <span>Wdh. pro Satz (mit ; trennen)</span>
                                        <input value="${escapeHtml((item.targetRepsPerSet || []).join('; '))}" data-routine-index="${index}" data-item-field="targetRepsPerSet" placeholder="12; 10; 8">
                                    </label>
                                    <label>
                                        <span>Notiz</span>
                                        <input maxlength="5000" value="${escapeHtml(item.notes || '')}" data-routine-index="${index}" data-item-field="notes" placeholder="Griff, Tempo, Technik…">
                                    </label>
                                </div>
                            </details>
                        </article>
                    `).join('') : renderEmpty('Leere Routine', 'Füge Übungen hinzu oder speichere eine leere Vorlage.')}
                </div>
                <div class="kl-modal__actions">
                    ${draft.id ? `<button type="button" class="kl-button--danger" data-action="confirm-delete-routine" data-id="${escapeHtml(draft.id)}">${icon('trash')}Löschen</button>` : '<span></span>'}
                    <button type="submit" class="primary">${icon('check')}Routine speichern</button>
                </div>
            </form>
        `, true)
    }

    function renderSessionDetail() {
        const session = app.modal.session
        const metrics = sessionMetrics(session)
        const setGroups = new Map()
        ;(session.sets || []).forEach(set => {
            const key = set.exerciseId + ':' + set.exerciseName
            if (!setGroups.has(key)) {
                setGroups.set(key, [])
            }
            setGroups.get(key).push(set)
        })
        let detail = ''
        if (session.sessionType === 'STRENGTH') {
            detail = [...setGroups.values()].map(sets => `
                <article class="kl-history-exercise">
                    <h3>${escapeHtml(sets[0].exerciseName)}</h3>
                    <div class="kl-set-summary-list">
                        ${sets.sort((a, b) => a.setNumber - b.setNumber).map(set => `
                            <div><span>Satz ${set.setNumber}</span><strong>${set.isBodyweight ? 'Körpergewicht' : formatKg(set.weightKg)} × ${set.reps}</strong>${set.rpe === null ? '' : `<small>RPE ${set.rpe}</small>`}</div>
                        `).join('')}
                    </div>
                </article>
            `).join('')
        } else if (session.sessionType === 'RUNNING') {
            const running = session.running
            const paceSeconds = running && running.distanceKm > 0
                ? running.durationSeconds / running.distanceKm
                : null
            detail = `
                <div class="kl-detail-feature">
                    ${icon('run')}
                    <div><span>Distanz</span><strong>${running ? formatNumber(running.distanceKm, 2) + ' km' : '–'}</strong></div>
                    <div><span>Zeit</span><strong>${running ? formatDuration(running.durationSeconds) : '–'}</strong></div>
                    <div><span>Pace</span><strong>${paceSeconds ? formatDuration(paceSeconds) + ' /km' : '–'}</strong></div>
                </div>
            `
        } else {
            const completed = (session.boulders || []).filter(route => route.isCompleted).length
            detail = `
                <div class="kl-detail-feature">
                    ${icon('boulder')}
                    <div><span>Routen</span><strong>${session.boulders.length}</strong></div>
                    <div><span>Geschafft</span><strong>${completed}</strong></div>
                    <div><span>Versucht</span><strong>${session.boulders.length - completed}</strong></div>
                </div>
                <div class="kl-boulder-history">
                    ${(session.boulders || []).map(route => `
                        <div><span class="${route.isCompleted ? 'is-complete' : ''}">${route.isCompleted ? icon('check') : '·'}</span><strong>${escapeHtml(route.description)}</strong><small>${route.isCompleted ? 'Geschafft' : 'Versucht'}</small></div>
                    `).join('')}
                </div>
            `
        }

        return modalShell(session.name, `
            <div class="kl-session-detail-head">
                <span class="kl-session-row__icon">${sessionTypeIcon(session.sessionType)}</span>
                <div>
                    <span class="kl-badge">${escapeHtml(SESSION_LABELS[session.sessionType] || session.sessionType)}</span>
                    <p>${escapeHtml(formatDate(session.startedAt))} · ${formatDuration(durationOf(session))}</p>
                </div>
                <div><strong>${escapeHtml(metrics.primary)}</strong><small>${escapeHtml(metrics.secondary)}</small></div>
            </div>
            ${session.notes ? `<blockquote class="kl-notes">${escapeHtml(session.notes)}</blockquote>` : ''}
            ${detail}
            <div class="kl-modal__actions">
                <button type="button" class="kl-button--danger" data-action="confirm-delete-session" data-id="${escapeHtml(session.id)}">${icon('trash')}Löschen</button>
                ${session.sessionType === 'STRENGTH' && session.sets.length ? `
                    <button type="button" class="primary" data-action="session-to-routine" data-id="${escapeHtml(session.id)}">${icon('plus')}Als Routine speichern</button>
                ` : '<span></span>'}
            </div>
        `, true)
    }

    function renderAddWorkoutExercise() {
        const used = new Set((app.active?.exercises || []).map(exercise => exercise.exerciseId))
        const choices = (app.data.exercises || []).filter(exercise => !used.has(exercise.id))
        return modalShell('Übung hinzufügen', choices.length ? `
            <div class="kl-exercise-picker">
                ${choices.map(exercise => `
                    <button type="button" data-action="add-active-exercise" data-id="${escapeHtml(exercise.id)}">
                        <span class="kl-exercise-card__icon">${icon('exercises')}</span>
                        <span><strong>${escapeHtml(exercise.name)}</strong><small>${escapeHtml((exercise.primaryMuscles || []).map(muscle => MUSCLE_LABELS[muscle] || muscle).join(', ') || CATEGORY_LABELS[exercise.category])}</small></span>
                        ${icon('plus')}
                    </button>
                `).join('')}
            </div>
        ` : renderEmpty('Alle Übungen hinzugefügt', 'In diesem Training sind bereits alle Übungen enthalten.'))
    }

    function renderWorkout() {
        if (app.active.type === 'RUNNING') {
            return renderRunningWorkout()
        }
        if (app.active.type === 'BOULDERING') {
            return renderBoulderingWorkout()
        }
        return renderStrengthWorkout()
    }

    function workoutHeader(typeIcon, eyebrow, title) {
        return `
            <header class="kl-workout-header">
                <button type="button" class="kl-icon-button" data-action="leave-workout" aria-label="Zur Übersicht">${icon('back')}</button>
                <span class="kl-workout-header__icon">${icon(typeIcon)}</span>
                <div>
                    <span class="kl-eyebrow">${escapeHtml(eyebrow)}</span>
                    <h1>${escapeHtml(title)}</h1>
                </div>
                <div class="kl-live-time" data-live-timer>${formatDuration((Date.now() - app.active.startedAt) / 1000)}</div>
            </header>
        `
    }

    function renderStrengthWorkout() {
        const active = app.active
        const logged = active.exercises.flatMap(exercise => exercise.sets).filter(set => set.logged)
        const volume = logged.reduce((sum, set) => sum + number(set.weight) * integer(set.reps), 0)
        return `
            ${workoutHeader('exercises', 'Aktives Krafttraining', active.name)}
            <section class="kl-live-summary">
                <div><span>Übungen</span><strong>${active.exercises.length}</strong></div>
                <div><span>Sätze</span><strong>${logged.length}</strong></div>
                <div><span>Volumen</span><strong>${formatKg(volume)}</strong></div>
                <button type="button" data-action="add-workout-exercise">${icon('plus')}Übung</button>
            </section>
            ${app.restEndsAt ? `
                <section class="kl-rest-bar">
                    <span>Pause</span>
                    <strong data-rest-timer>${formatDuration((app.restEndsAt - Date.now()) / 1000)}</strong>
                    <button type="button" data-action="dismiss-rest">Überspringen</button>
                </section>
            ` : ''}
            <div class="kl-active-exercises">
                ${active.exercises.length ? active.exercises.map((exercise, exerciseIndex) => `
                    <article class="kl-active-exercise">
                        <header>
                            <div>
                                <span class="kl-drag-number">${exerciseIndex + 1}</span>
                                <div><h2>${escapeHtml(exercise.name)}</h2><small>${exercise.restSeconds} Sek. Pause</small></div>
                            </div>
                            <button type="button" class="kl-icon-button kl-danger" data-action="remove-active-exercise" data-index="${exerciseIndex}" aria-label="Übung entfernen">${icon('trash')}</button>
                        </header>
                        ${exercise.lastSets?.length ? `
                            <div class="kl-last-session">
                                <span>Letztes Mal</span>
                                ${exercise.lastSets.map(set => `<small>${set.setNumber}. ${set.isBodyweight ? 'KG' : formatKg(set.weightKg)} × ${set.reps}</small>`).join('')}
                            </div>
                        ` : ''}
                        <div class="kl-set-table">
                            <div class="kl-set-table__head"><span>Satz</span><span>kg</span><span>Wdh.</span><span>RPE</span><span>KG</span><span></span></div>
                            ${exercise.sets.map((set, setIndex) => `
                                <div class="kl-set-row ${set.logged ? 'is-logged' : ''}">
                                    <strong>${set.setNumber}</strong>
                                    <input
                                        type="number"
                                        inputmode="decimal"
                                        step="0.1"
                                        min="0"
                                        max="100000"
                                        value="${escapeHtml(set.weight)}"
                                        data-active-exercise="${exerciseIndex}"
                                        data-active-set="${setIndex}"
                                        data-active-set-field="weight"
                                        ${set.logged || set.isBodyweight ? 'disabled' : ''}
                                        aria-label="Gewicht"
                                    >
                                    <input
                                        type="number"
                                        inputmode="numeric"
                                        min="0"
                                        max="100000"
                                        value="${escapeHtml(set.reps)}"
                                        data-active-exercise="${exerciseIndex}"
                                        data-active-set="${setIndex}"
                                        data-active-set-field="reps"
                                        ${set.logged ? 'disabled' : ''}
                                        aria-label="Wiederholungen"
                                    >
                                    <input
                                        type="number"
                                        inputmode="decimal"
                                        step="0.5"
                                        min="0"
                                        max="10"
                                        value="${escapeHtml(set.rpe ?? '')}"
                                        data-active-exercise="${exerciseIndex}"
                                        data-active-set="${setIndex}"
                                        data-active-set-field="rpe"
                                        ${set.logged ? 'disabled' : ''}
                                        aria-label="RPE"
                                    >
                                    <label class="kl-switch-check">
                                        <input
                                            type="checkbox"
                                            ${set.isBodyweight ? 'checked' : ''}
                                            data-active-exercise="${exerciseIndex}"
                                            data-active-set="${setIndex}"
                                            data-active-set-field="isBodyweight"
                                            ${set.logged ? 'disabled' : ''}
                                            aria-label="Körpergewicht"
                                        >
                                        <span></span>
                                    </label>
                                    <button
                                        type="button"
                                        class="${set.logged ? 'kl-set-done' : 'kl-set-log'}"
                                        data-action="toggle-active-set"
                                        data-exercise-index="${exerciseIndex}"
                                        data-set-index="${setIndex}"
                                        aria-label="${set.logged ? 'Satz entsperren' : 'Satz speichern'}"
                                    >${set.logged ? icon('check') : 'Log'}</button>
                                </div>
                            `).join('')}
                        </div>
                        <button type="button" class="kl-add-set" data-action="add-active-set" data-index="${exerciseIndex}">${icon('plus')}Satz hinzufügen</button>
                    </article>
                `).join('') : renderEmpty('Noch keine Übung', 'Füge eine Übung hinzu, um dein Training zu beginnen.', 'add-workout-exercise', 'Übung hinzufügen')}
            </div>
            <section class="kl-workout-finish">
                <label><span>Trainingsnotiz</span><textarea data-active-field="notes" rows="2" placeholder="Wie lief das Training?">${escapeHtml(active.notes || '')}</textarea></label>
                <button type="button" class="primary" data-action="finish-workout">${icon('check')}Training abschließen</button>
                <button type="button" class="kl-button--danger" data-action="confirm-discard-workout">Training verwerfen</button>
            </section>
        `
    }

    function renderRunningWorkout() {
        const active = app.active
        const autoSeconds = Math.max(0, Math.floor((Date.now() - active.startedAt) / 1000))
        const hasManual = [active.hours, active.minutes, active.seconds].some(value => String(value).trim() !== '')
        const duration = hasManual
            ? integer(active.hours) * 3600 + integer(active.minutes) * 60 + integer(active.seconds)
            : autoSeconds
        const pace = number(active.distance) > 0 && duration > 0
            ? formatDuration(duration / number(active.distance)) + ' /km'
            : '–'
        return `
            ${workoutHeader('run', 'Aktiver Lauf', active.name)}
            <section class="kl-run-layout">
                <article class="kl-run-hero">
                    <span>Zeit</span>
                    <strong data-live-timer>${formatDuration(autoSeconds)}</strong>
                    <small>Live-Timer</small>
                </article>
                <article class="kl-card">
                    <div class="kl-form">
                        <label>
                            <span>Distanz in km</span>
                            <input type="number" min="0" max="100000" step="0.01" inputmode="decimal" value="${escapeHtml(active.distance)}" data-active-field="distance" placeholder="5,00">
                        </label>
                        <fieldset>
                            <legend>Manuelle Zeit (optional)</legend>
                            <div class="kl-time-inputs">
                                <label><span>Stunden</span><input type="number" min="0" value="${escapeHtml(active.hours)}" data-active-field="hours" placeholder="0"></label>
                                <label><span>Minuten</span><input type="number" min="0" value="${escapeHtml(active.minutes)}" data-active-field="minutes" placeholder="0"></label>
                                <label><span>Sekunden</span><input type="number" min="0" value="${escapeHtml(active.seconds)}" data-active-field="seconds" placeholder="0"></label>
                            </div>
                        </fieldset>
                        <label><span>Notiz</span><textarea rows="3" data-active-field="notes" placeholder="Strecke, Gefühl, Wetter…">${escapeHtml(active.notes || '')}</textarea></label>
                    </div>
                </article>
                <article class="kl-run-pace">
                    <span>Aktuelle Pace</span>
                    <strong data-run-pace>${escapeHtml(pace)}</strong>
                    <small>basierend auf Distanz und Zeit</small>
                </article>
            </section>
            <section class="kl-workout-finish">
                <button type="button" class="primary" data-action="finish-workout">${icon('check')}Lauf abschließen</button>
                <button type="button" class="kl-button--danger" data-action="confirm-discard-workout">Lauf verwerfen</button>
            </section>
        `
    }

    function renderBoulderingWorkout() {
        const active = app.active
        const completed = active.routes.filter(route => route.isCompleted).length
        return `
            ${workoutHeader('boulder', 'Aktive Session', active.name)}
            <section class="kl-live-summary">
                <div><span>Routen</span><strong>${active.routes.length}</strong></div>
                <div><span>Geschafft</span><strong>${completed}</strong></div>
                <div><span>Versucht</span><strong>${active.routes.length - completed}</strong></div>
            </section>
            <section class="kl-boulder-layout">
                <article class="kl-card">
                    <span class="kl-eyebrow">Route loggen</span>
                    <h2>Was hast du probiert?</h2>
                    <form data-form="boulder" class="kl-form">
                        <label>
                            <span>Beschreibung oder Grad</span>
                            <input name="description" maxlength="255" required placeholder="z. B. Rot 6b, Überhang">
                        </label>
                        <div class="kl-form-row">
                            <button type="submit" name="outcome" value="attempted">Versucht</button>
                            <button type="submit" name="outcome" value="completed" class="primary">${icon('check')}Geschafft</button>
                        </div>
                    </form>
                </article>
                <div class="kl-boulder-routes">
                    ${active.routes.length ? active.routes.map((route, index) => `
                        <article class="kl-boulder-route ${route.isCompleted ? 'is-complete' : ''}">
                            <span>${route.isCompleted ? icon('check') : '·'}</span>
                            <div><strong>${escapeHtml(route.description)}</strong><small>${route.isCompleted ? 'Geschafft' : 'Versucht'}</small></div>
                            <button type="button" class="kl-icon-button" data-action="remove-boulder-route" data-index="${index}" aria-label="Route entfernen">${icon('close')}</button>
                        </article>
                    `).join('') : renderEmpty('Noch keine Route', 'Deine Versuche erscheinen hier.')}
                </div>
            </section>
            <section class="kl-workout-finish">
                <label><span>Session-Notiz</span><textarea data-active-field="notes" rows="2" placeholder="Projekt, Halle, Fortschritt…">${escapeHtml(active.notes || '')}</textarea></label>
                <button type="button" class="primary" data-action="finish-workout">${icon('check')}Session abschließen</button>
                <button type="button" class="kl-button--danger" data-action="confirm-discard-workout">Session verwerfen</button>
            </section>
        `
    }

    function openRoutineEditor(routine = null) {
        app.routineDraft = {
            id: routine?.id || null,
            name: routine?.name || '',
            description: routine?.description || '',
            items: (routine?.items || []).map(item => ({
                exerciseId: item.exerciseId,
                targetSets: item.targetSets ?? 3,
                targetReps: item.targetReps ?? 10,
                targetWeightKg: item.targetWeightKg,
                targetWeightsPerSet: [...(item.targetWeightsPerSet || [])],
                targetRepsPerSet: [...(item.targetRepsPerSet || [])],
                restSeconds: item.restSeconds ?? 90,
                notes: item.notes || '',
            })),
        }
        app.modal = { type: 'routine-editor' }
        render()
    }

    function lastSetsForExercise(exerciseId, excludedSessionId = null) {
        const previous = finishedSessions()
            .filter(session => session.id !== excludedSessionId)
            .find(session => (session.sets || []).some(set => set.exerciseId === exerciseId))
        return previous
            ? previous.sets
                .filter(set => set.exerciseId === exerciseId)
                .sort((a, b) => a.setNumber - b.setNumber)
            : []
    }

    function liveExercise(exerciseId, routineItem = null, savedSets = [], excludedSessionId = null) {
        const exercise = exerciseById(exerciseId)
        if (!exercise) {
            return null
        }
        const previousSets = lastSetsForExercise(exerciseId, excludedSessionId)
        const highestSaved = savedSets.reduce((max, set) => Math.max(max, set.setNumber), 0)
        const highestPrevious = previousSets.reduce((max, set) => Math.max(max, set.setNumber), 0)
        const targetCount = routineItem?.targetSets || 1
        const count = Math.max(targetCount, highestSaved, highestPrevious, 1)
        const sets = Array.from({ length: count }, (_, index) => {
            const setNumber = index + 1
            const saved = savedSets.find(set => set.setNumber === setNumber)
            const previous = previousSets.find(set => set.setNumber === setNumber)
            const targetWeight = routineItem?.targetWeightsPerSet?.[index]
                ?? routineItem?.targetWeightKg
                ?? ''
            const targetReps = routineItem?.targetRepsPerSet?.[index]
                ?? routineItem?.targetReps
                ?? ''
            return {
                setNumber,
                reps: String(saved?.reps ?? previous?.reps ?? targetReps),
                weight: String(saved?.weightKg ?? previous?.weightKg ?? targetWeight),
                isBodyweight: saved?.isBodyweight ?? previous?.isBodyweight ?? false,
                rpe: saved?.rpe ?? '',
                logged: Boolean(saved),
                loggedAt: saved?.loggedAt || null,
            }
        })
        return {
            exerciseId,
            name: exercise.name,
            restSeconds: routineItem?.restSeconds ?? 90,
            notes: routineItem?.notes || '',
            lastSets: previousSets,
            sets,
        }
    }

    function buildStrengthActive(session, routine) {
        const savedSets = session.sets || []
        const exercises = []
        const included = new Set()
        ;(routine?.items || []).forEach(item => {
            const live = liveExercise(
                item.exerciseId,
                item,
                savedSets.filter(set => set.exerciseId === item.exerciseId),
                session.id,
            )
            if (live) {
                exercises.push(live)
                included.add(item.exerciseId)
            }
        })
        savedSets.forEach(set => {
            if (included.has(set.exerciseId)) {
                return
            }
            const live = liveExercise(
                set.exerciseId,
                null,
                savedSets.filter(candidate => candidate.exerciseId === set.exerciseId),
                session.id,
            )
            if (live) {
                exercises.push(live)
                included.add(set.exerciseId)
            }
        })
        return {
            sessionId: session.id,
            type: 'STRENGTH',
            routineId: session.routineId,
            name: session.name,
            startedAt: session.startedAt,
            notes: session.notes || '',
            exercises,
        }
    }

    function buildActiveFromSession(session) {
        if (session.sessionType === 'RUNNING') {
            const running = session.running
            return {
                sessionId: session.id,
                type: 'RUNNING',
                routineId: null,
                name: session.name,
                startedAt: session.startedAt,
                notes: session.notes || '',
                distance: running ? String(running.distanceKm) : '',
                hours: '',
                minutes: '',
                seconds: running ? String(running.durationSeconds) : '',
            }
        }
        if (session.sessionType === 'BOULDERING') {
            return {
                sessionId: session.id,
                type: 'BOULDERING',
                routineId: null,
                name: session.name,
                startedAt: session.startedAt,
                notes: session.notes || '',
                routes: (session.boulders || []).map(route => ({ ...route })),
            }
        }
        return buildStrengthActive(session, routineById(session.routineId))
    }

    async function beginSession(type, routineId = null, replaceExisting = false) {
        const existing = app.data.activeSession
        if (existing && !replaceExisting) {
            app.confirm = {
                title: 'Aktives Training ersetzen?',
                text: `${existing.name} ist noch nicht abgeschlossen. Wenn du fortfährst, wird diese Einheit verworfen.`,
                label: 'Verwerfen & starten',
                action: 'replace-active',
                payload: { type, routineId, existingId: existing.id },
            }
            app.modal = null
            render()
            return
        }

        const routine = routineId ? routineById(routineId) : null
        const name = type === 'RUNNING'
            ? 'Running'
            : type === 'BOULDERING'
                ? 'Bouldering'
                : routine?.name || 'Ad-hoc Workout'
        const saved = await withBusy(() => request('/sessions', 'POST', {
            routineId,
            name,
            startedAt: Date.now(),
            finishedAt: null,
            notes: '',
            sessionType: type,
            sets: [],
            boulders: [],
        }))
        if (!saved) {
            return
        }
        app.active = buildActiveFromSession(saved)
        if (type === 'STRENGTH' && routine) {
            app.active = buildStrengthActive(saved, routine)
        }
        app.modal = null
        app.view = 'workout'
        app.data.activeSession = saved
        render()
    }

    function resumeSession(id) {
        const session = sessionById(id) || app.data.activeSession
        if (!session) {
            showToast('Das aktive Training wurde nicht gefunden.', true)
            return
        }
        app.active = buildActiveFromSession(session)
        app.modal = null
        app.view = 'workout'
        render()
    }

    function activeDurationSeconds() {
        return Math.max(0, Math.floor((Date.now() - app.active.startedAt) / 1000))
    }

    function runningDurationSeconds() {
        const hasManual = [app.active.hours, app.active.minutes, app.active.seconds]
            .some(value => String(value).trim() !== '')
        return hasManual
            ? integer(app.active.hours) * 3600
                + integer(app.active.minutes) * 60
                + integer(app.active.seconds)
            : activeDurationSeconds()
    }

    function activePayload(finishedAt = null) {
        const active = app.active
        const payload = {
            id: active.sessionId,
            routineId: active.routineId,
            name: active.name,
            startedAt: active.startedAt,
            finishedAt,
            notes: active.notes || '',
            sessionType: active.type,
        }
        if (active.type === 'STRENGTH') {
            payload.sets = active.exercises.flatMap(exercise => exercise.sets
                .filter(set => set.logged)
                .map(set => ({
                    exerciseId: exercise.exerciseId,
                    exerciseName: exercise.name,
                    setNumber: set.setNumber,
                    reps: Math.max(0, integer(set.reps)),
                    weightKg: set.isBodyweight ? 0 : Math.max(0, number(set.weight)),
                    isBodyweight: Boolean(set.isBodyweight),
                    rpe: String(set.rpe).trim() === '' ? null : clamp(number(set.rpe), 0, 10),
                    loggedAt: set.loggedAt || Date.now(),
                })))
        } else if (active.type === 'RUNNING') {
            payload.running = {
                distanceKm: Math.max(0, number(active.distance)),
                durationSeconds: runningDurationSeconds(),
            }
        } else {
            payload.boulders = active.routes.map(route => ({
                description: route.description,
                isCompleted: route.isCompleted,
                createdAt: route.createdAt || Date.now(),
            }))
        }
        return payload
    }

    async function persistActive(finishedAt = null) {
        if (!app.active) {
            return null
        }
        const saved = await request('/sessions', 'POST', activePayload(finishedAt))
        app.active.sessionId = saved.id
        if (app.active.type === 'BOULDERING') {
            app.active.routes = saved.boulders.map(route => ({ ...route }))
        }
        app.data.activeSession = finishedAt === null ? saved : null
        return saved
    }

    async function finishWorkout() {
        const saved = await withBusy(() => persistActive(Date.now()))
        if (!saved) {
            return
        }
        app.active = null
        app.restEndsAt = null
        app.view = 'history'
        await withBusy(() => reloadData(false))
        const finished = sessionById(saved.id)
        if (finished) {
            app.modal = { type: 'session-detail', session: finished }
        }
        render()
        showToast('Training gespeichert.')
    }

    async function leaveWorkout() {
        if (!app.active) {
            return
        }
        await withBusy(() => persistActive(null))
        app.active = null
        app.restEndsAt = null
        app.view = 'home'
        await withBusy(() => reloadData(false))
        render()
    }

    function setConfirmation(title, text, action, payload, label = 'Löschen') {
        app.confirm = { title, text, action, payload, label }
        render()
    }

    async function executeConfirmation() {
        const confirmation = app.confirm
        app.confirm = null
        if (!confirmation) {
            return
        }
        const { action, payload } = confirmation
        if (action === 'replace-active') {
            const deleted = await withBusy(() => request('/sessions/' + encodeURIComponent(payload.existingId), 'DELETE'))
            if (deleted) {
                app.data.activeSession = null
                await beginSession(payload.type, payload.routineId, true)
            }
            return
        }
        if (action === 'discard-workout') {
            const deleted = await withBusy(() => request('/sessions/' + encodeURIComponent(app.active.sessionId), 'DELETE'))
            if (deleted) {
                app.active = null
                app.restEndsAt = null
                app.view = 'home'
                await withBusy(() => reloadData(false))
                render()
                showToast('Training verworfen.')
            }
            return
        }
        if (action === 'discard-session' || action === 'delete-session') {
            const deleted = await withBusy(() => request('/sessions/' + encodeURIComponent(payload.id), 'DELETE'))
            if (deleted) {
                app.modal = null
                await withBusy(() => reloadData(false))
                render()
                showToast('Einheit gelöscht.')
            }
            return
        }
        if (action === 'delete-routine') {
            const deleted = await withBusy(() => request('/routines/' + encodeURIComponent(payload.id), 'DELETE'))
            if (deleted) {
                app.modal = null
                app.routineDraft = null
                await withBusy(() => reloadData(false))
                render()
                showToast('Routine gelöscht.')
            }
            return
        }
        if (action === 'delete-exercise') {
            const deleted = await withBusy(() => request('/exercises/' + encodeURIComponent(payload.id), 'DELETE'))
            if (deleted) {
                app.modal = null
                await withBusy(() => reloadData(false))
                render()
                showToast('Übung gelöscht.')
            }
            return
        }
        if (action === 'delete-weight') {
            const deleted = await withBusy(() => request('/weights/' + encodeURIComponent(payload.id), 'DELETE'))
            if (deleted) {
                await withBusy(() => reloadData(false))
                render()
                showToast('Messwert gelöscht.')
            }
            return
        }
        if (action === 'remove-active-exercise') {
            app.active.exercises.splice(payload.index, 1)
            await withBusy(() => persistActive(null))
            render()
        }
    }

    function openSessionAsRoutine(session) {
        const order = []
        const groups = new Map()
        ;(session.sets || []).forEach(set => {
            if (!groups.has(set.exerciseId)) {
                order.push(set.exerciseId)
                groups.set(set.exerciseId, [])
            }
            groups.get(set.exerciseId).push(set)
        })
        app.routineDraft = {
            id: null,
            name: session.name + ' – Routine',
            description: 'Erstellt aus dem Training vom ' + formatDate(session.startedAt, { dateStyle: 'medium' }),
            items: order.map(exerciseId => {
                const sets = groups.get(exerciseId).sort((a, b) => a.setNumber - b.setNumber)
                const weighted = sets.filter(set => !set.isBodyweight)
                return {
                    exerciseId,
                    targetSets: sets.length,
                    targetReps: sets[0]?.reps || 10,
                    targetWeightKg: weighted.length ? Math.max(...weighted.map(set => set.weightKg)) : null,
                    targetWeightsPerSet: sets.map(set => set.isBodyweight ? 0 : set.weightKg),
                    targetRepsPerSet: sets.map(set => set.reps),
                    restSeconds: 90,
                    notes: '',
                }
            }),
        }
        app.modal = { type: 'routine-editor' }
        render()
    }

    function downloadExport() {
        const exportPayload = {
            version: 1,
            exportedAt: Date.now(),
            source: 'KraftLog Nextcloud',
            exercises: app.data.exercises,
            routines: app.data.routines,
            sessions: app.data.sessions,
            weights: app.data.weights,
        }
        const blob = new Blob([JSON.stringify(exportPayload, null, 2)], {
            type: 'application/json',
        })
        const link = document.createElement('a')
        link.href = URL.createObjectURL(blob)
        link.download = `kraftlog-export-${new Date().toISOString().slice(0, 10)}.json`
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.setTimeout(() => URL.revokeObjectURL(link.href), 1000)
    }

    async function importFile(file) {
        if (!file) {
            return
        }
        try {
            const data = JSON.parse(await file.text())
            const result = await withBusy(() => request('/import', 'POST', { data }))
            if (!result) {
                return
            }
            await withBusy(() => reloadData(false))
            render()
            const counts = result.imported
            showToast(`Import abgeschlossen: ${counts.sessions} Einheiten, ${counts.routines} Routinen.`)
        } catch (error) {
            showToast(error instanceof Error ? error.message : 'Die Datei konnte nicht importiert werden.', true)
        }
    }

    function startClock() {
        if (app.clock) {
            return
        }
        app.clock = window.setInterval(() => {
            if (!app.active || !app.root) {
                return
            }
            const elapsed = activeDurationSeconds()
            app.root.querySelectorAll('[data-live-timer]').forEach(element => {
                element.textContent = formatDuration(elapsed)
            })
            if (app.active.type === 'RUNNING') {
                const paceElement = app.root.querySelector('[data-run-pace]')
                const duration = runningDurationSeconds()
                paceElement && (paceElement.textContent = number(app.active.distance) > 0 && duration > 0
                    ? formatDuration(duration / number(app.active.distance)) + ' /km'
                    : '–')
            }
            if (app.restEndsAt) {
                const remaining = Math.ceil((app.restEndsAt - Date.now()) / 1000)
                const timer = app.root.querySelector('[data-rest-timer]')
                if (remaining <= 0) {
                    app.restEndsAt = null
                    render()
                    showToast('Pause beendet.')
                } else if (timer) {
                    timer.textContent = formatDuration(remaining)
                }
            }
        }, 1000)
    }

    async function handleClick(event) {
        const trigger = event.target.closest('[data-action]')
        if (!trigger || !app.root.contains(trigger)) {
            return
        }
        const action = trigger.dataset.action
        if (action === 'close-modal' && event.target.closest('[data-modal-panel]')) {
            return
        }
        event.preventDefault()

        if (action === 'navigate') {
            if (app.active) {
                await withBusy(() => persistActive(null))
                app.active = null
                app.restEndsAt = null
            }
            app.modal = null
            app.confirm = null
            app.view = trigger.dataset.view || 'home'
            render()
            return
        }
        if (action === 'quick-menu') {
            app.modal = { type: 'quick' }
            render()
            return
        }
        if (action === 'close-modal') {
            app.modal = null
            app.confirm = null
            render()
            return
        }
        if (action === 'cancel-confirm') {
            app.confirm = null
            render()
            return
        }
        if (action === 'execute-confirm') {
            await executeConfirmation()
            return
        }
        if (action === 'start-strength') {
            await beginSession('STRENGTH')
            return
        }
        if (action === 'start-running') {
            await beginSession('RUNNING')
            return
        }
        if (action === 'start-bouldering') {
            await beginSession('BOULDERING')
            return
        }
        if (action === 'start-routine') {
            await beginSession('STRENGTH', trigger.dataset.id)
            return
        }
        if (action === 'resume-session') {
            resumeSession(trigger.dataset.id)
            return
        }
        if (action === 'confirm-discard-session') {
            setConfirmation(
                'Aktives Training verwerfen?',
                'Alle bereits gespeicherten Werte dieser Einheit werden dauerhaft gelöscht.',
                'discard-session',
                { id: trigger.dataset.id },
                'Verwerfen',
            )
            return
        }
        if (action === 'new-routine') {
            openRoutineEditor()
            return
        }
        if (action === 'edit-routine') {
            openRoutineEditor(routineById(trigger.dataset.id))
            return
        }
        if (action === 'confirm-delete-routine') {
            const routine = routineById(trigger.dataset.id)
            setConfirmation(
                'Routine löschen?',
                `${routine?.name || 'Diese Routine'} wird dauerhaft gelöscht. Dein Trainingsverlauf bleibt erhalten.`,
                'delete-routine',
                { id: trigger.dataset.id },
            )
            return
        }
        if (action === 'routine-add-item') {
            const used = new Set(app.routineDraft.items.map(item => item.exerciseId))
            const exercise = app.data.exercises.find(candidate => !used.has(candidate.id))
                || app.data.exercises[0]
            if (exercise) {
                app.routineDraft.items.push({
                    exerciseId: exercise.id,
                    targetSets: 3,
                    targetReps: 10,
                    targetWeightKg: null,
                    targetWeightsPerSet: [],
                    targetRepsPerSet: [],
                    restSeconds: 90,
                    notes: '',
                })
                render()
            }
            return
        }
        if (action === 'routine-remove-item') {
            app.routineDraft.items.splice(integer(trigger.dataset.index), 1)
            render()
            return
        }
        if (action === 'routine-move-up' || action === 'routine-move-down') {
            const from = integer(trigger.dataset.index)
            const to = action === 'routine-move-up' ? from - 1 : from + 1
            if (to >= 0 && to < app.routineDraft.items.length) {
                const [item] = app.routineDraft.items.splice(from, 1)
                app.routineDraft.items.splice(to, 0, item)
                render()
            }
            return
        }
        if (action === 'new-exercise') {
            app.modal = { type: 'exercise-editor', exercise: null }
            render()
            return
        }
        if (action === 'edit-exercise') {
            app.modal = { type: 'exercise-editor', exercise: exerciseById(trigger.dataset.id) }
            render()
            return
        }
        if (action === 'exercise-detail') {
            app.modal = { type: 'exercise-detail', exercise: exerciseById(trigger.dataset.id) }
            render()
            return
        }
        if (action === 'confirm-delete-exercise') {
            const exercise = exerciseById(trigger.dataset.id)
            setConfirmation(
                'Übung löschen?',
                `${exercise?.name || 'Diese Übung'} wird aus Bibliothek und Routinen entfernt.`,
                'delete-exercise',
                { id: trigger.dataset.id },
            )
            return
        }
        if (action === 'session-detail') {
            app.modal = { type: 'session-detail', session: sessionById(trigger.dataset.id) }
            render()
            return
        }
        if (action === 'confirm-delete-session') {
            setConfirmation(
                'Einheit löschen?',
                'Die Einheit und alle zugehörigen Sätze werden dauerhaft gelöscht.',
                'delete-session',
                { id: trigger.dataset.id },
            )
            return
        }
        if (action === 'session-to-routine') {
            openSessionAsRoutine(sessionById(trigger.dataset.id))
            return
        }
        if (action === 'confirm-delete-weight') {
            setConfirmation(
                'Messwert löschen?',
                'Dieser Gewichtseintrag wird dauerhaft gelöscht.',
                'delete-weight',
                { id: trigger.dataset.id },
            )
            return
        }
        if (action === 'export-data') {
            downloadExport()
            return
        }
        if (action === 'open-import') {
            app.root.querySelector('[data-import-file]')?.click()
            return
        }
        if (action === 'leave-workout') {
            await leaveWorkout()
            return
        }
        if (action === 'add-workout-exercise') {
            app.modal = { type: 'add-workout-exercise' }
            render()
            return
        }
        if (action === 'add-active-exercise') {
            const live = liveExercise(trigger.dataset.id, null, [], app.active.sessionId)
            if (live) {
                app.active.exercises.push(live)
                app.modal = null
                render()
            }
            return
        }
        if (action === 'remove-active-exercise') {
            const index = integer(trigger.dataset.index)
            const live = app.active.exercises[index]
            setConfirmation(
                'Übung entfernen?',
                `${live?.name || 'Diese Übung'} und ihre geloggten Sätze werden aus der aktiven Einheit entfernt.`,
                'remove-active-exercise',
                { index },
                'Entfernen',
            )
            return
        }
        if (action === 'add-active-set') {
            const exercise = app.active.exercises[integer(trigger.dataset.index)]
            const previous = exercise.sets[exercise.sets.length - 1]
            exercise.sets.push({
                setNumber: exercise.sets.length + 1,
                reps: previous?.reps || '',
                weight: previous?.weight || '',
                isBodyweight: previous?.isBodyweight || false,
                rpe: '',
                logged: false,
                loggedAt: null,
            })
            render()
            return
        }
        if (action === 'toggle-active-set') {
            const exercise = app.active.exercises[integer(trigger.dataset.exerciseIndex)]
            const set = exercise.sets[integer(trigger.dataset.setIndex)]
            const old = set.logged
            set.logged = !set.logged
            set.loggedAt = set.logged ? Date.now() : null
            const saved = await withBusy(() => persistActive(null))
            if (!saved) {
                set.logged = old
            } else if (set.logged && exercise.restSeconds > 0) {
                app.restEndsAt = Date.now() + exercise.restSeconds * 1000
            }
            render()
            return
        }
        if (action === 'dismiss-rest') {
            app.restEndsAt = null
            render()
            return
        }
        if (action === 'remove-boulder-route') {
            app.active.routes.splice(integer(trigger.dataset.index), 1)
            await withBusy(() => persistActive(null))
            render()
            return
        }
        if (action === 'finish-workout') {
            await finishWorkout()
            return
        }
        if (action === 'confirm-discard-workout') {
            setConfirmation(
                'Training verwerfen?',
                'Diese Einheit und alle bereits geloggten Daten werden dauerhaft gelöscht.',
                'discard-workout',
                {},
                'Verwerfen',
            )
        }
    }

    async function handleSubmit(event) {
        const form = event.target.closest('[data-form]')
        if (!form) {
            return
        }
        event.preventDefault()
        const type = form.dataset.form
        if (type === 'exercise') {
            const data = new FormData(form)
            const payload = {
                id: data.get('id') || null,
                name: data.get('name'),
                category: data.get('category'),
                primaryMuscles: data.getAll('primaryMuscles'),
                secondaryMuscles: data.getAll('secondaryMuscles'),
                instructions: data.get('instructions'),
            }
            const saved = await withBusy(() => request('/exercises', 'POST', payload))
            if (saved) {
                app.modal = null
                await withBusy(() => reloadData(false))
                render()
                showToast('Übung gespeichert.')
            }
            return
        }
        if (type === 'routine') {
            const duplicate = app.routineDraft.items.some((item, index, items) =>
                items.findIndex(candidate => candidate.exerciseId === item.exerciseId) !== index)
            if (duplicate) {
                showToast('Eine Übung darf nur einmal in einer Routine vorkommen.', true)
                return
            }
            const saved = await withBusy(() => request('/routines', 'POST', app.routineDraft))
            if (saved) {
                app.modal = null
                app.routineDraft = null
                await withBusy(() => reloadData(false))
                render()
                showToast('Routine gespeichert.')
            }
            return
        }
        if (type === 'weight') {
            const data = new FormData(form)
            const date = new Date(String(data.get('date'))).getTime()
            const saved = await withBusy(() => request('/weights', 'POST', {
                weightKg: number(data.get('weightKg'), NaN),
                date,
            }))
            if (saved) {
                await withBusy(() => reloadData(false))
                render()
                showToast('Gewicht gespeichert.')
            }
            return
        }
        if (type === 'boulder') {
            const data = new FormData(form)
            const submitter = event.submitter
            const description = String(data.get('description') || '').trim()
            if (!description) {
                return
            }
            app.active.routes.push({
                description,
                isCompleted: submitter?.value === 'completed',
                createdAt: Date.now(),
            })
            const saved = await withBusy(() => persistActive(null))
            if (saved) {
                render()
            }
        }
    }

    function handleInput(event) {
        const input = event.target
        if (input.matches('[data-filter="exercise-query"]')) {
            app.exerciseQuery = input.value
            render()
            const replacement = app.root.querySelector('[data-filter="exercise-query"]')
            replacement?.focus()
            replacement?.setSelectionRange(app.exerciseQuery.length, app.exerciseQuery.length)
            return
        }
        if (input.matches('[data-routine-field]')) {
            app.routineDraft[input.dataset.routineField] = input.value
            return
        }
        if (input.matches('[data-item-field]')) {
            const item = app.routineDraft.items[integer(input.dataset.routineIndex)]
            const field = input.dataset.itemField
            if (field === 'targetWeightsPerSet') {
                item[field] = parsePerSetNumbers(input.value, false)
            } else if (field === 'targetRepsPerSet') {
                item[field] = parsePerSetNumbers(input.value, true)
            } else if (['targetSets', 'targetReps', 'restSeconds'].includes(field)) {
                item[field] = integer(input.value)
            } else if (field === 'targetWeightKg') {
                item[field] = input.value === '' ? null : number(input.value)
            } else {
                item[field] = input.value
            }
            return
        }
        if (input.matches('[data-active-set-field]')) {
            const exercise = app.active.exercises[integer(input.dataset.activeExercise)]
            const set = exercise.sets[integer(input.dataset.activeSet)]
            const field = input.dataset.activeSetField
            set[field] = input.type === 'checkbox' ? input.checked : input.value
            if (field === 'isBodyweight' && input.checked) {
                set.weight = ''
            }
            return
        }
        if (input.matches('[data-active-field]')) {
            app.active[input.dataset.activeField] = input.value
        }
    }

    function handleChange(event) {
        const input = event.target
        if (input.matches('[data-filter="exercise-category"]')) {
            app.exerciseCategory = input.value
            render()
            return
        }
        if (input.matches('[data-import-file]')) {
            importFile(input.files?.[0])
        }
    }

    async function boot() {
        app.root = document.getElementById('kraftlog-app')
        if (!app.root) {
            return
        }
        app.apiBase = app.root.dataset.apiBase || ''
        app.root.addEventListener('click', handleClick)
        app.root.addEventListener('submit', handleSubmit)
        app.root.addEventListener('input', handleInput)
        app.root.addEventListener('change', handleChange)
        try {
            await reloadData()
        } catch (error) {
            app.root.innerHTML = `
                <div class="kl-fatal">
                    <h1>KraftLog konnte nicht geladen werden</h1>
                    <p>${escapeHtml(error instanceof Error ? error.message : 'Unbekannter Fehler')}</p>
                    <button type="button" data-reload>Neu laden</button>
                </div>
            `
            app.root.querySelector('[data-reload]')?.addEventListener('click', () => {
                window.location.reload()
            })
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot, { once: true })
    } else {
        boot()
    }
})()
