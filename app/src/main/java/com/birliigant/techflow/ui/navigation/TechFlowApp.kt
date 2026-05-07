package com.birliigant.techflow.ui.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.birliigant.techflow.ui.auth.LoginScreen
import com.birliigant.techflow.ui.auth.LoginViewModel
import com.birliigant.techflow.ui.auth.RegisterScreen
import com.birliigant.techflow.ui.auth.RegisterViewModel
import com.birliigant.techflow.ui.common.RuntimePermissionGate
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
    val routePattern: String,
    val destination: String,
    val label: String,
    val icon: ImageVector,
)

private object Routes {
    const val home = "home"
    const val ask = "ask"
    const val mePattern = "me?tab={tab}"
    const val login = "login"
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
    fun me(tab: String = ProfileTab.OVERVIEW.routeValue): String {
        return if (tab == ProfileTab.OVERVIEW.routeValue) {
            "me"
        } else {
            "me?tab=${Uri.encode(tab)}"
        }
    }
}

private val topRoutes = listOf(
    TopLevelRoute(Routes.home, Routes.home, "首页", Icons.Outlined.Home),
    TopLevelRoute(Routes.ask, Routes.ask, "提问", Icons.Outlined.AddCircle),
    TopLevelRoute(Routes.mePattern, Routes.me(), "我的", Icons.Outlined.AccountCircle),
)

@Composable
private fun CompactBottomBar(
    routes: List<TopLevelRoute>,
    currentDestinationRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                routes.forEach { item ->
                    val selected = currentDestinationRoute == item.routePattern
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onNavigate(item.destination) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
            Box(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
fun TechFlowApp(appContainer: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route in topRoutes.map { it.routePattern }.toSet()
    RuntimePermissionGate(uiPreferenceRepository = appContainer.uiPreferenceRepository)

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
        currentUser?.username?.takeIf { it.isNotBlank() }?.let {
            navigateToTopLevel(Routes.me(initialTab))
        } ?: navigateToTopLevel(Routes.me())
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
                CompactBottomBar(
                    routes = topRoutes,
                    currentDestinationRoute = currentDestination?.route,
                    onNavigate = { navigateToTopLevel(it) },
                )
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
                    onOpenMe = {
                        if (appContainer.sessionRepository.currentUser.value == null) {
                            navController.navigate(Routes.login)
                        } else {
                            navigateToTopLevel(Routes.me())
                        }
                    },
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
                    onGoProfile = { navigateToTopLevel(Routes.me()) },
                    onSubmitted = { questionId ->
                        if (questionId.isNotBlank()) {
                            navController.navigate(Routes.detail(questionId)) {
                                popUpTo(Routes.ask) { inclusive = true }
                            }
                        } else {
                            navigateToTopLevel(Routes.home)
                        }
                    },
                )
            }

            composable(
                route = Routes.mePattern,
                arguments = listOf(
                    navArgument("tab") {
                        type = NavType.StringType
                        defaultValue = ProfileTab.OVERVIEW.routeValue
                    },
                ),
            ) { entry ->
                val tab = ProfileTab.from(Uri.decode(entry.arguments?.getString("tab")))
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
                    initialTab = tab,
                    onQuestionClick = { id -> navController.navigate(Routes.detail(id)) },
                    onOpenCollections = { openCurrentUserProfile(ProfileTab.COLLECTIONS.routeValue) },
                    onOpenSettings = { navController.navigate(Routes.settings) },
                    onOpenLogin = { navController.navigate(Routes.login) },
                    onOpenRegister = { navController.navigate(Routes.register) },
                )
            }

            composable(Routes.login) {
                LoginScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = appViewModelFactory {
                            LoginViewModel(appContainer.userRepository)
                        },
                    ),
                    onBack = { navController.popBackStack() },
                    onOpenRegister = {
                        navController.navigate(Routes.register) {
                            popUpTo(Routes.login) { inclusive = true }
                        }
                    },
                    onLoggedIn = {
                        navController.navigate(Routes.me()) {
                            popUpTo(Routes.home) {
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
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
                    onOpenLogin = {
                        navController.navigate(Routes.login) {
                            popUpTo(Routes.register) { inclusive = true }
                        }
                    },
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
                                uiPreferenceRepository = appContainer.uiPreferenceRepository,
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
                                sessionRepository = appContainer.sessionRepository,
                            )
                        },
                    ),
                    onBack = { navController.popBackStack() },
                    onQuestionDeleted = {
                        navController.navigate(Routes.home) {
                            popUpTo(Routes.home) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
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
