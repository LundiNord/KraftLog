package de.nyxnord.kraftlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.nyxnord.kraftlog.ui.navigation.KraftLogNavHost
import de.nyxnord.kraftlog.ui.navigation.TopLevelDestination
import de.nyxnord.kraftlog.ui.navigation.navigateToTopLevel
import de.nyxnord.kraftlog.ui.theme.KraftLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KraftLogApplication
        setContent {
            KraftLogTheme {
                KraftLogApp(app)
            }
        }
    }
}

@Composable
fun KraftLogApp(app: KraftLogApplication) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()

    val currentDestination = TopLevelDestination.entries.firstOrNull { dest ->
        currentEntry?.destination?.route?.startsWith(dest.route) == true
    } ?: TopLevelDestination.HOME

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
