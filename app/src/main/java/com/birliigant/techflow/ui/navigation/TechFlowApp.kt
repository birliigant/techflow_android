package com.birliigant.techflow.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.birliigant.techflow.app.AppContainer
import com.birliigant.techflow.app.appViewModelFactory
import com.birliigant.techflow.ui.ask.AskScreen
import com.birliigant.techflow.ui.ask.AskViewModel
import com.birliigant.techflow.ui.detail.QuestionDetailScreen
import com.birliigant.techflow.ui.detail.QuestionDetailViewModel
import com.birliigant.techflow.ui.home.HomeScreen
import com.birliigant.techflow.ui.home.HomeViewModel
import com.birliigant.techflow.ui.me.MeScreen
import com.birliigant.techflow.ui.me.MeViewModel

private data class TopLevelRoute(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private object Routes {
    const val home = "home"
    const val ask = "ask"
    const val me = "me"
    const val detailPattern = "detail/{questionId}"

    fun detail(questionId: String): String = "detail/${Uri.encode(questionId)}"
}

private val topRoutes = listOf(
    TopLevelRoute(Routes.home, "首页", Icons.Outlined.Home),
    TopLevelRoute(Routes.ask, "提问", Icons.Outlined.AddCircle),
    TopLevelRoute(Routes.me, "我的", Icons.Outlined.AccountCircle),
)

@Composable
fun TechFlowApp(appContainer: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route != Routes.detailPattern

    fun navigateToTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topRoutes.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.home,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.home) {
                HomeScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = appViewModelFactory {
                            HomeViewModel(
                                siteRepository = appContainer.siteRepository,
                                questionRepository = appContainer.questionRepository,
                                sessionRepository = appContainer.sessionRepository,
                            )
                        },
                    ),
                    onQuestionClick = { id -> navController.navigate(Routes.detail(id)) },
                    onOpenMe = { navigateToTopLevel(Routes.me) },
                )
            }

            composable(Routes.ask) {
                AskScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = appViewModelFactory {
                            AskViewModel(
                                questionRepository = appContainer.questionRepository,
                                sessionRepository = appContainer.sessionRepository,
                            )
                        },
                    ),
                    onGoProfile = { navigateToTopLevel(Routes.me) },
                    onSubmitted = { navigateToTopLevel(Routes.home) },
                )
            }

            composable(Routes.me) {
                MeScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = appViewModelFactory {
                            MeViewModel(
                                sessionRepository = appContainer.sessionRepository,
                                userRepository = appContainer.userRepository,
                            )
                        },
                    ),
                )
            }

            composable(
                route = Routes.detailPattern,
                arguments = listOf(navArgument("questionId") { type = NavType.StringType }),
            ) { entry ->
                val questionId = Uri.decode(entry.arguments?.getString("questionId").orEmpty())
                QuestionDetailScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "detail-$questionId",
                        factory = appViewModelFactory {
                            QuestionDetailViewModel(
                                questionId = questionId,
                                questionRepository = appContainer.questionRepository,
                            )
                        },
                    ),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
