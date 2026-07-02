package cz.sobtech.hapBeer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cz.sobtech.hapBeer.ui.screens.diagnostics.DiagnosticsScreen
import cz.sobtech.hapBeer.ui.screens.eventdetail.EventDetailScreen
import cz.sobtech.hapBeer.ui.screens.eventlist.EventListScreen
import cz.sobtech.hapBeer.ui.screens.kegdetail.KegDetailScreen
import cz.sobtech.hapBeer.ui.screens.people.PeopleScreen

private sealed class Screen(val route: String) {
    object EventList : Screen("event_list")
    object People : Screen("people")
    object Diagnostics : Screen("diagnostics")
    object EventDetail : Screen("event_detail/{eventId}") {
        const val ARG = "eventId"
        fun createRoute(eventId: Long) = "event_detail/$eventId"
    }
    object KegDetail : Screen("keg_detail/{kegId}") {
        const val ARG = "kegId"
        fun createRoute(kegId: Long) = "keg_detail/$kegId"
    }
}

@Composable
fun PivoNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.EventList.route
    ) {
        composable(Screen.EventList.route) {
            EventListScreen(
                onEventClick = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                },
                onPeopleClick = {
                    navController.navigate(Screen.People.route)
                },
                onDiagnosticsClick = {
                    navController.navigate(Screen.Diagnostics.route)
                }
            )
        }

        composable(Screen.People.route) {
            PeopleScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(
                navArgument(Screen.EventDetail.ARG) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong(Screen.EventDetail.ARG) ?: -1L
            EventDetailScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() },
                onKegClick = { kegId ->
                    navController.navigate(Screen.KegDetail.createRoute(kegId))
                }
            )
        }

        composable(
            route = Screen.KegDetail.route,
            arguments = listOf(
                navArgument(Screen.KegDetail.ARG) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val kegId = backStackEntry.arguments?.getLong(Screen.KegDetail.ARG) ?: -1L
            KegDetailScreen(
                kegId = kegId,
                onBack = { navController.popBackStack() },
                onNavigateToPeople = {
                    navController.navigate(Screen.People.route)
                }
            )
        }

        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(onBack = { navController.popBackStack() })
        }
    }
}
