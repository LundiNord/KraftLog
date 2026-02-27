package de.nyxnord.kraftlog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.nyxnord.kraftlog.ui.navigation.KraftLogNavHost
import de.nyxnord.kraftlog.ui.navigation.TopLevelDestination
import de.nyxnord.kraftlog.ui.navigation.navigateToTopLevel
import de.nyxnord.kraftlog.ui.theme.KraftLogTheme
import de.nyxnord.kraftlog.widget.EXTRA_START_ROUTINE_ID
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val pendingRoutineId = MutableStateFlow(Long.MIN_VALUE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KraftLogApplication
        extractWidgetIntent(intent)
        setContent {
            val routineId by pendingRoutineId.collectAsState()
            KraftLogTheme {
                KraftLogApp(app = app, widgetRoutineId = routineId) {
                    pendingRoutineId.value = Long.MIN_VALUE
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractWidgetIntent(intent)
    }

    private fun extractWidgetIntent(intent: Intent?) {
        val routineId = intent?.getLongExtra(EXTRA_START_ROUTINE_ID, Long.MIN_VALUE)
            ?: Long.MIN_VALUE
        if (routineId != Long.MIN_VALUE) {
            pendingRoutineId.value = routineId
        }
    }
}

@Composable
fun KraftLogApp(
    app: KraftLogApplication,
    widgetRoutineId: Long = Long.MIN_VALUE,
    onWidgetNavigated: () -> Unit = {}
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()

    val currentDestination = TopLevelDestination.entries.firstOrNull { dest ->
        currentEntry?.destination?.route?.startsWith(dest.route) == true
    } ?: TopLevelDestination.HOME

    LaunchedEffect(widgetRoutineId) {
        if (widgetRoutineId != Long.MIN_VALUE) {
            navController.navigate("active_workout/$widgetRoutineId")
            onWidgetNavigated()
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { dest ->
                item(
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label) },
                    selected = dest == currentDestination,
                    onClick = { navController.navigateToTopLevel(dest) }
                )
            }
        }
    ) {
        KraftLogNavHost(navController = navController, app = app)
    }
}
