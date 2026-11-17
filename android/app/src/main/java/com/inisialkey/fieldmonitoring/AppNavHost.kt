package com.inisialkey.fieldmonitoring

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inisialkey.fieldmonitoring.auth.domain.Role
import com.inisialkey.fieldmonitoring.auth.domain.Session
import com.inisialkey.fieldmonitoring.auth.ui.LoginScreen
import com.inisialkey.fieldmonitoring.evaluation.ui.evaluate.EvaluateScreen
import com.inisialkey.fieldmonitoring.evaluation.ui.queue.EvaluationQueueScreen
import com.inisialkey.fieldmonitoring.report.domain.SyncState
import com.inisialkey.fieldmonitoring.report.ui.detail.ReportDetailScreen
import com.inisialkey.fieldmonitoring.report.ui.form.ReportFormScreen
import com.inisialkey.fieldmonitoring.report.ui.list.ReportListScreen
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data object Login

@Serializable
data object Reports

@Serializable
data class ReportForm(val reportId: String)

@Serializable
data class ReportDetail(val reportId: String)

@Serializable
data object Queue

@Serializable
data class Evaluate(val reportId: String)

@Composable
fun AppNavHost(initialSession: Session?, navController: NavHostController = rememberNavController()) {
    val start: Any = remember { initialSession?.role?.let(::homeOf) ?: Login }
    StatusBarAppearance(navController)
    NavHost(navController = navController, startDestination = start) {
        composable<Login> {
            LoginScreen(
                onSignedIn = { role ->
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(homeOf(role)) { popUpTo(navController.graph.id) { inclusive = true } }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Reports> {
            ReportListScreen(
                onCreateReport = { navController.navigateOnce(ReportForm(UUID.randomUUID().toString())) },
                onOpenReport = { report ->
                    navController.navigateOnce(
                        if (report.syncState == SyncState.DRAFT) ReportForm(report.id) else ReportDetail(report.id),
                    )
                },
                onSignIn = { navController.navigateOnce(Login) },
                onSignedOut = { navController.signOut() },
            )
        }
        composable<ReportForm> {
            ReportFormScreen(onBack = { navController.popBackStack() })
        }
        composable<ReportDetail> {
            ReportDetailScreen(onBack = { navController.popBackStack() })
        }
        composable<Queue> {
            EvaluationQueueScreen(
                onOpenReport = { navController.navigateOnce(Evaluate(it)) },
                onSignIn = { navController.navigateOnce(Login) },
                onSignedOut = { navController.signOut() },
            )
        }
        composable<Evaluate> {
            EvaluateScreen(
                onBack = { navController.popBackStack() },
                onOpenNext = { next -> navController.navigate(Evaluate(next)) { popUpTo<Evaluate> { inclusive = true } } },
                onSignIn = { navController.navigateOnce(Login) },
            )
        }
    }
}

@Composable
private fun StatusBarAppearance(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val onHero = destination == null || destination.hasRoute<Login>() || destination.hasRoute<Reports>() || destination.hasRoute<Queue>()
    val darkTheme = isSystemInDarkTheme()
    val window = LocalActivity.current?.window
    val view = LocalView.current
    LaunchedEffect(onHero, darkTheme, window) {
        if (window != null) WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !onHero && !darkTheme
    }
}

private fun homeOf(role: Role): Any = when (role) {
    Role.FIELD_OFFICER -> Reports
    Role.EVALUATOR -> Queue
}

private fun NavController.navigateOnce(route: Any) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) navigate(route)
}

private fun NavController.signOut() {
    navigate(Login) { popUpTo(graph.id) { inclusive = true } }
}
