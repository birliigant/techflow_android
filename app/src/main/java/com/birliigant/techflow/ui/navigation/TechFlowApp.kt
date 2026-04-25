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
import com.birliigant.techflow.core.model.TagDetail
import com.birliigant.techflow.ui.ask.AskScreen
import com.birliigant.techflow.ui.ask.AskViewModel
import com.birliigant.techflow.ui.auth.RegisterScreen
import com.birliigant.techflow.ui.auth.RegisterViewModel
import com.birliigant.techflow.ui.detail.QuestionDetailScreen
import com.birliigant.techflow.ui.detail.QuestionDetailViewModel
import com.birliigant.techflow.ui.explore.TagFeedScreen
import com.birliigant.techflow.ui.explore.TagFeedViewModel
import com.birliigant.techflow.ui.explore.TagsScreen
import com.birliigant.techflow.ui.explore.TagsViewModel
import com.birliigant.techflow.ui.explore.UsersScreen
import com.birliigant.techflow.ui.explore.UsersViewModel
import com.birliigant.techflow.ui.home.HomeScreen
import com.birliigant.techflow.ui.home.HomeViewModel
import com.birliigant.techflow.ui.me.MeScreen
import com.birliigant.techflow.ui.me.MeViewModel
import com.birliigant.techflow.ui.profile.ProfileScreen
import com.birliigant.techflow.ui.profile.ProfileTab
import com.birliigant.techflow.ui.profile.ProfileViewModel
import com.birliigant.techflow.ui.search.SearchScreen
import com.birliigant.techflow.ui.search.SearchViewModel
import com.birliigant.techflow.ui.settings.SettingsScreen
import com.birliigant.techflow.ui.settings.SettingsViewModel

private data class TopLevelRoute(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private object Routes {
    const val home = "home"
    const val ask = "ask"
    const val me = "me"
    const val register = "register"
    const val searchPattern = "search?q={q}"
    const val tags = "tags"
    const val tagPattern = "tag/{slug}?name={name}&partition={partition}&questionCount={questionCount}&followCount={followCount}"
    const val users = "users"
    const val settings = "settings"
    const val detailPattern = "detail/{questionId}"
    const val profilePattern = "profile/{username}?tab={tab}"

    fun detail(questionId: String): String = "detail/${Uri.encode(questionId)}"
    fun search(query: String = ""): String = "search?q=${Uri.encode(query)}"
    fun tag(tag: TagDetail): String {
        return buildString {
            append("tag/${Uri.encode(tag.slug)}")
            append("?name=${Uri.encode(tag.name)}")
            append("&partition=${Uri.encode(tag.partition)}")
            append("&questionCount=${tag.questionCount}")
            append("&followCount=${tag.followCount}")
        }
    }
    fun profile(username: String, tab: String = ProfileTab.OVERVIEW.routeValue): String {
        return "profile/${Uri.encode(username)}?tab=${Uri.encode(tab)}"
    }
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

    fun openCurrentUserProfile(initialTab: String = ProfileTab.OVERVIEW.routeValue) {
        val currentUser = appContainer.sessionRepository.currentUser.value
        currentUser?.username?.takeIf { it.isNotBlank() }?.let { username ->
            navController.navigate(Routes.profile(username, initialTab))
        } ?: navigateToTopLevel(Routes.me)
    }

    fun openUserProfile(username: String) {
        username.takeIf { it.isNotBlank() }?.let {
            navController.navigate(Routes.profile(it))
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
                                userRepository = appContainer.userRepository,
                                sessionRepository = appContainer.sessionRepository,
                            )
                        },
                    ),
                    onQuestionClick = { id -> navController.navigate(Routes.detail(id)) },
                    onOpenMe = { navigateToTopLevel(Routes.me) },
                    onOpenRegister = { navController.navigate(Routes.register) },
                    onOpenSearch = { query -> navController.navigate(Routes.search(query)) },
                    onOpenTags = { navController.navigate(Routes.tags) },
                    onOpenUsers = { navController.navigate(Routes.users) },
                    onOpenUserProfile = ::openUserProfile,
                    onOpenProfile = { openCurrentUserProfile() },
                    onOpenCollections = { openCurrentUserProfile(ProfileTab.COLLECTIONS.routeValue) },
                    onOpenSettings = { navController.navigate(Routes.settings) },
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
                    sessionRepository = appContainer.sessionRepository,
                    userRepository = appContainer.userRepository,
                    onOpenProfile = { openCurrentUserProfile() },
                    onQuestionClick = { id -> navController.navigate(Routes.detail(id)) },
                    onOpenCollections = { openCurrentUserProfile(ProfileTab.COLLECTIONS.routeValue) },
                    onOpenSettings = { navController.navigate(Routes.settings) },
                    onOpenRegister = { navController.navigate(Routes.register) },
                )
            }

            composable(Routes.register) {
                RegisterScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = appViewModelFactory {
                            RegisterViewModel(appContainer.userRepository)
                        },
                    ),
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.searchPattern,
                arguments = listOf(
                    navArgument("q") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val query = Uri.decode(entry.arguments?.getString("q").orEmpty())
                SearchScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "search-$query",
                        factory = appViewModelFactory {
                            SearchViewModel(
                                initialQuery = query,
                                questionRepository = appContainer.questionRepository,
                                tagRepository = appContainer.tagRepository,
                                userRepository = appContainer.userRepository,
                            )
                        },
                    ),
                    onBack = { navController.popBackStack() },
                    onQuestionClick = { id -> navController.navigate(Routes.detail(id)) },
                    onUserClick = ::openUserProfile,
                    onTagClick = { tag -> navController.navigate(Routes.tag(tag)) },
                )
            }

            composable(Routes.settings) {
                SettingsScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = appViewModelFactory {
                            SettingsViewModel(
                                userRepository = appContainer.userRepository,
                                sessionRepository = appContainer.sessionRepository,
                            )
                        },
                    ),
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.tags) {
                TagsScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = appViewModelFactory {
                            TagsViewModel(appContainer.tagRepository)
                        },
                    ),
                    onBack = { navController.popBackStack() },
                    onTagClick = { tag -> navController.navigate(Routes.tag(tag)) },
                )
            }

            composable(
                route = Routes.tagPattern,
                arguments = listOf(
                    navArgument("slug") { type = NavType.StringType },
                    navArgument("name") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("partition") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("questionCount") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                    navArgument("followCount") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) { entry ->
                val slug = Uri.decode(entry.arguments?.getString("slug").orEmpty())
                val name = Uri.decode(entry.arguments?.getString("name").orEmpty())
                val partition = Uri.decode(entry.arguments?.getString("partition").orEmpty())
                val tag = TagDetail(
                    id = slug,
                    name = name.ifBlank { slug },
                    slug = slug,
                    description = "",
                    followCount = entry.arguments?.getInt("followCount") ?: 0,
                    questionCount = entry.arguments?.getInt("questionCount") ?: 0,
                    partition = partition,
                )
                TagFeedScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "tag-${tag.slug}",
                        factory = appViewModelFactory {
                            TagFeedViewModel(
                                tag = tag,
                                questionRepository = appContainer.questionRepository,
                            )
                        },
                    ),
                    onBack = { navController.popBackStack() },
                    onQuestionClick = { id -> navController.navigate(Routes.detail(id)) },
                    onUserClick = ::openUserProfile,
                )
            }

            composable(Routes.users) {
                UsersScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = appViewModelFactory {
                            UsersViewModel(appContainer.userRepository)
                        },
                    ),
                    onBack = { navController.popBackStack() },
                    onUserClick = { username ->
                        navController.navigate(Routes.profile(username))
                    },
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
                    onOpenUserProfile = ::openUserProfile,
                )
            }

            composable(
                route = Routes.profilePattern,
                arguments = listOf(
                    navArgument("username") { type = NavType.StringType },
                    navArgument("tab") {
                        type = NavType.StringType
                        defaultValue = ProfileTab.OVERVIEW.routeValue
                    },
                ),
            ) { entry ->
                val username = Uri.decode(entry.arguments?.getString("username").orEmpty())
                val tab = ProfileTab.from(Uri.decode(entry.arguments?.getString("tab")))
                ProfileScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "profile-$username-${tab.routeValue}",
                        factory = appViewModelFactory {
                            ProfileViewModel(
                                username = username,
                                initialTab = tab,
                                userRepository = appContainer.userRepository,
                                sessionRepository = appContainer.sessionRepository,
                            )
                        },
                    ),
                    onBack = { navController.popBackStack() },
                    onQuestionClick = { id -> navController.navigate(Routes.detail(id)) },
                    onOpenSettings = { navController.navigate(Routes.settings) },
                )
            }
        }
    }
}
