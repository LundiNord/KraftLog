(function () {
    'use strict'

    const screen = new URL(window.location.href).searchParams.get('screen')
    if (!screen || screen === 'home') {
        return
    }
    window.setTimeout(() => {
        if (['routines', 'exercises', 'history', 'weight'].includes(screen)) {
            document.querySelector(`[data-action="navigate"][data-view="${screen}"]`)?.click()
        } else if (screen === 'strength') {
            document.querySelector('[data-action="start-routine"]')?.click()
        } else if (screen === 'running') {
            document.querySelector('[data-action="start-running"]')?.click()
        } else if (screen === 'bouldering') {
            document.querySelector('[data-action="start-bouldering"]')?.click()
        }
    }, 250)
})()
