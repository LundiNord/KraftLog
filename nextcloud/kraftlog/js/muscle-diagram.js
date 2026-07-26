(function () {
    'use strict'

    const MUSCLES = [
        'CHEST', 'BACK', 'SHOULDERS', 'BICEPS', 'TRICEPS', 'FOREARMS',
        'CORE', 'GLUTES', 'QUADRICEPS', 'HAMSTRINGS', 'CALVES',
    ]
    const LABELS = {
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
    let diagramId = 0

    function escapeText(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;')
    }

    function effectiveSet(values) {
        const source = Array.isArray(values) ? values : []
        return new Set(source.includes('FULL_BODY') ? MUSCLES : source)
    }

    function renderMuscleDiagram(primaryMuscles, secondaryMuscles, options = {}) {
        const primary = effectiveSet(primaryMuscles)
        const secondary = effectiveSet(secondaryMuscles)
        const compact = options.compact === true
        const id = `kl-muscle-diagram-${++diagramId}`
        const state = muscle => primary.has(muscle)
            ? 'is-primary'
            : secondary.has(muscle) ? 'is-secondary' : ''
        const box = (left, top, right, bottom, className, radius = .018) => `
            <rect
                x="${left * 1000}" y="${top * 500}"
                width="${(right - left) * 1000}" height="${(bottom - top) * 500}"
                rx="${radius * 500}" class="${className}"
            />`
        const oval = (left, top, right, bottom, className) => `
            <ellipse
                cx="${(left + right) * 500}" cy="${(top + bottom) * 250}"
                rx="${(right - left) * 500}" ry="${(bottom - top) * 250}"
                class="${className}"
            />`
        const region = (left, top, right, bottom, muscle, radius) => box(
            left,
            top,
            right,
            bottom,
            `kl-muscle-region ${state(muscle)}`,
            radius,
        )
        const primaryNames = [...primary].map(muscle => LABELS[muscle] || muscle)
        const secondaryNames = [...secondary]
            .filter(muscle => !primary.has(muscle))
            .map(muscle => LABELS[muscle] || muscle)
        const description = [
            primaryNames.length ? `Primär: ${primaryNames.join(', ')}` : '',
            secondaryNames.length ? `Sekundär: ${secondaryNames.join(', ')}` : '',
        ].filter(Boolean).join('. ') || 'Keine Muskelgruppen ausgewählt.'

        return `
            <figure class="kl-muscle-diagram ${compact ? 'kl-muscle-diagram--compact' : ''}">
                <svg viewBox="0 0 1000 500" role="img" aria-labelledby="${id}">
                    <title id="${id}">${escapeText(description)}</title>
                    <line class="kl-muscle-divider" x1="500" y1="20" x2="500" y2="485" />

                    <g aria-hidden="true">
                        ${oval(.190, .030, .310, .125, 'kl-muscle-body')}
                        ${box(.228, .122, .272, .162, 'kl-muscle-body', .005)}
                        ${box(.175, .162, .325, .540, 'kl-muscle-body', .012)}
                        ${box(.095, .162, .175, .440, 'kl-muscle-body', .014)}
                        ${box(.325, .162, .405, .440, 'kl-muscle-body', .014)}
                        ${box(.098, .450, .172, .600, 'kl-muscle-body', .014)}
                        ${box(.328, .450, .402, .600, 'kl-muscle-body', .014)}
                        ${box(.175, .540, .248, .970, 'kl-muscle-body', .012)}
                        ${box(.252, .540, .325, .970, 'kl-muscle-body', .012)}
                        ${box(.095, .162, .215, .245, 'kl-muscle-body', .014)}
                        ${box(.285, .162, .405, .245, 'kl-muscle-body', .014)}

                        ${region(.097, .164, .213, .242, 'SHOULDERS')}
                        ${region(.287, .164, .403, .242, 'SHOULDERS')}
                        ${region(.180, .164, .320, .310, 'CHEST')}
                        ${region(.097, .164, .173, .435, 'BICEPS')}
                        ${region(.327, .164, .403, .435, 'BICEPS')}
                        ${region(.180, .310, .320, .535, 'CORE')}
                        ${region(.100, .452, .170, .597, 'FOREARMS')}
                        ${region(.330, .452, .400, .597, 'FOREARMS')}
                        ${region(.177, .537, .247, .808, 'QUADRICEPS')}
                        ${region(.253, .537, .323, .808, 'QUADRICEPS')}
                        ${region(.178, .820, .246, .968, 'CALVES')}
                        ${region(.254, .820, .322, .968, 'CALVES')}
                        ${oval(.190, .030, .310, .125, 'kl-muscle-body')}
                        ${box(.228, .120, .272, .164, 'kl-muscle-body', .005)}

                        ${oval(.690, .030, .810, .125, 'kl-muscle-body')}
                        ${box(.728, .122, .772, .162, 'kl-muscle-body', .005)}
                        ${box(.675, .162, .825, .620, 'kl-muscle-body', .012)}
                        ${box(.595, .162, .675, .440, 'kl-muscle-body', .014)}
                        ${box(.825, .162, .905, .440, 'kl-muscle-body', .014)}
                        ${box(.598, .450, .672, .600, 'kl-muscle-body', .014)}
                        ${box(.828, .450, .902, .600, 'kl-muscle-body', .014)}
                        ${box(.675, .620, .748, .970, 'kl-muscle-body', .012)}
                        ${box(.752, .620, .825, .970, 'kl-muscle-body', .012)}
                        ${box(.595, .162, .715, .245, 'kl-muscle-body', .014)}
                        ${box(.785, .162, .905, .245, 'kl-muscle-body', .014)}

                        ${region(.597, .164, .713, .242, 'SHOULDERS')}
                        ${region(.787, .164, .903, .242, 'SHOULDERS')}
                        ${region(.662, .242, .838, .492, 'BACK')}
                        ${region(.597, .164, .660, .435, 'TRICEPS')}
                        ${region(.840, .164, .903, .435, 'TRICEPS')}
                        ${region(.600, .452, .670, .597, 'FOREARMS')}
                        ${region(.830, .452, .900, .597, 'FOREARMS')}
                        ${region(.662, .492, .838, .622, 'GLUTES')}
                        ${region(.677, .622, .746, .808, 'HAMSTRINGS')}
                        ${region(.754, .622, .823, .808, 'HAMSTRINGS')}
                        ${region(.678, .820, .745, .968, 'CALVES')}
                        ${region(.755, .820, .822, .968, 'CALVES')}
                        ${oval(.690, .030, .810, .125, 'kl-muscle-body')}
                        ${box(.728, .120, .772, .164, 'kl-muscle-body', .005)}

                        <text class="kl-muscle-label" x="250" y="493">Vorne</text>
                        <text class="kl-muscle-label" x="750" y="493">Hinten</text>
                    </g>
                </svg>
                ${compact ? '' : `
                    <figcaption>
                        <span><i class="is-primary"></i>Primär</span>
                        <span><i class="is-secondary"></i>Sekundär</span>
                    </figcaption>
                `}
            </figure>
        `
    }

    window.KraftLogMuscleDiagram = Object.freeze({ render: renderMuscleDiagram })
})()