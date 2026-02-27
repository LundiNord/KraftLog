package de.nyxnord.kraftlog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.nyxnord.kraftlog.KraftLogApplication
import de.nyxnord.kraftlog.ui.exercises.ExerciseDetailScreen
import de.nyxnord.kraftlog.ui.exercises.ExercisesScreen
import de.nyxnord.kraftlog.ui.history.HistoryScreen
import de.nyxnord.kraftlog.ui.history.SessionDetailScreen
import de.nyxnord.kraftlog.ui.home.HomeScreen
import de.nyxnord.kraftlog.ui.routines.RoutineDetailScreen
import de.nyxnord.kraftlog.ui.routines.RoutineEditScreen
import de.nyxnord.kraftlog.ui.routines.RoutinesScreen
import de.nyxnord.kraftlog.ui.workout.ActiveWorkoutScreen
import de.nyxnord.kraftlog.ui.workout.BoulderingWorkoutScreen
import de.nyxnord.kraftlog.ui.workout.RunningWorkoutScreen
import de.nyxnord.kraftlog.ui.workout.WorkoutSummaryScreen

enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val route: String
) {
    HOME("Home", Icons.Default.Home, "home"),
    ROUTINES("Routines", Icons.Default.List, "routines"),
    EXERCISES("Exercises", Icons.Default.FitnessCenter, "exercises"),
    HISTORY("History", Icons.Default.History, "history")
}

fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun KraftLogNavHost(
    navController: NavHostController,
    app: KraftLogApplication
) {
    NavHost(navController = navController, startDestination = TopLevelDestination.HOME.route) {

        // ── Home ──────────────────────────────────────────────────────────────
        composable("home") {
            HomeScreen(
                app = app,
                onStartWorkout = { routineId ->
                    navController.navigate("active_workout/$routineId")
                },
                onStartRunning = {
                    navController.navigate("running_workout")
                },
                onStartBouldering = {
                    navController.navigate("bouldering_workout")
                }
            )
        }
        composable(
            route = "active_workout/{routineId}",
            arguments = listOf(navArgument("routineId") { type = NavType.LongType })
        ) { backStack ->
            val routineId = backStack.arguments?.getLong("routineId") ?: -1L
            ActiveWorkoutScreen(
                routineId = routineId,
                app = app,
                onFinished = { sessionId ->
                    navController.navigate("workout_summary/$sessionId") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onDiscarded = { navController.popBackStack() }
            )
        }

        composable(
            route = "workout_summary/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStack ->
            val sessionId = backStack.arguments?.getLong("sessionId") ?: return@composable
            WorkoutSummaryScreen(
                sessionId = sessionId,
                app = app,
                onDone = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("running_workout") {
            RunningWorkoutScreen(
                app = app,
                onFinished = { sessionId ->
                    navController.navigate("workout_summary/$sessionId") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onDiscarded = { navController.popBackStack() }
            )
        }

        composable("bouldering_workout") {
            BoulderingWorkoutScreen(
                app = app,
                onFinished = { sessionId ->
                    navController.navigate("workout_summary/$sessionId") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onDiscarded = { navController.popBackStack() }
            )
        }

        // ── Routines ──────────────────────────────────────────────────────────
        composable("routines") {
            RoutinesScreen(
                app = app,
                onRoutineClick = { id -> navController.navigate("routines/$id") },
                onCreateRoutine = { navController.navigate("routines/-1/edit") }
            )
        }
        composable(
            route = "routines/{routineId}",
            arguments = listOf(navArgument("routineId") { type = NavType.LongType })
        ) { backStack ->
            val routineId = backStack.arguments?.getLong("routineId") ?: return@composable
            RoutineDetailScreen(
                routineId = routineId,
                app = app,
                onStartWorkout = { navController.navigate("active_workout/$routineId") },
                onEdit = { navController.navigate("routines/$routineId/edit") },
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() }
            )
        }
        composable(
            route = "routines/{routineId}/edit",
            arguments = listOf(navArgument("routineId") { type = NavType.LongType })
        ) { backStack ->
            val routineId = backStack.arguments?.getLong("routineId") ?: return@composable
            RoutineEditScreen(
                routineId = routineId,
                app = app,
                onSaved = { savedId ->
                    navController.navigate("routines/$savedId") {
                        popUpTo("routines") { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Exercises ─────────────────────────────────────────────────────────
        composable("exercises") {
            ExercisesScreen(
                app = app,
                onExerciseClick = { id -> navController.navigate("exercises/$id") }
            )
        }
        composable(
            route = "exercises/{exerciseId}",
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
        ) { backStack ->
            val exerciseId = backStack.arguments?.getLong("exerciseId") ?: return@composable
            ExerciseDetailScreen(
                exerciseId = exerciseId,
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        // ── History ───────────────────────────────────────────────────────────
        composable("history") {
            HistoryScreen(
                app = app,
                onSessionClick = { id -> navController.navigate("history/$id") }
            )
        }
        composable(
            route = "history/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStack ->
            val sessionId = backStack.arguments?.getLong("sessionId") ?: return@composable
            SessionDetailScreen(
                sessionId = sessionId,
                app = app,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onRoutineCreated = { id -> navController.navigate("routines/$id") }
            )
        }

    }
}
