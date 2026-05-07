package com.birliigant.techflow.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.app.appViewModelFactory
import com.birliigant.techflow.core.model.UserProfile
import com.birliigant.techflow.data.repository.SessionRepository
import com.birliigant.techflow.data.repository.UserRepository
import com.birliigant.techflow.ui.common.TechFlowFooter
import com.birliigant.techflow.ui.common.TechFlowTopBar
import com.birliigant.techflow.ui.common.TopBarFilledAction
import com.birliigant.techflow.ui.common.TopBarTextAction
import com.birliigant.techflow.ui.profile.ProfileContent
import com.birliigant.techflow.ui.profile.ProfileTab
import com.birliigant.techflow.ui.profile.ProfileViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeUiState(
    val user: UserProfile? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

class MeViewModel(
    sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val editorState = MutableStateFlow(MeUiState())

    val uiState: StateFlow<MeUiState> = combine(
        editorState,
        sessionRepository.currentUser,
    ) { state, user ->
        state.copy(user = user)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = editorState.value,
    )

    fun consumeMessage() = editorState.update { it.copy(message = null) }

    fun refreshProfile(silent: Boolean = true) {
        viewModelScope.launch {
            val result = userRepository.refreshCurrentUser()
            if (!silent) {
                result.exceptionOrNull()?.message?.let { message ->
                    editorState.update { it.copy(message = message) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            editorState.update { it.copy(busy = true) }
            userRepository.logout()
            editorState.update { it.copy(busy = false, message = "已退出登录") }
        }
    }
}

@Composable
fun MeScreen(
    viewModel: MeViewModel,
    sessionRepository: SessionRepository,
    userRepository: UserRepository,
    initialTab: ProfileTab,
    onQuestionClick: (String) -> Unit,
    onOpenCollections: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenRegister: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var toolbarMenuExpanded by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val profileViewModel = uiState.user?.username?.takeIf { it.isNotBlank() }?.let { username ->
        androidx.lifecycle.viewmodel.compose.viewModel<ProfileViewModel>(
            key = "me-profile-$username-${initialTab.routeValue}",
            factory = appViewModelFactory {
                ProfileViewModel(
                    username = username,
                    initialTab = initialTab,
                    userRepository = userRepository,
                    sessionRepository = sessionRepository,
                )
            },
        )
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TechFlowTopBar(
            title = if (uiState.user == null) "我的" else "我的主页",
            dense = uiState.user != null,
        ) {
            if (uiState.user == null) {
                TopBarTextAction(text = "登录", onClick = onOpenLogin)
                TopBarFilledAction(text = "注册", onClick = onOpenRegister)
            } else {
                Box {
                    IconButton(onClick = { toolbarMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "更多操作",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    DropdownMenu(
                        expanded = toolbarMenuExpanded,
                        onDismissRequest = { toolbarMenuExpanded = false },
                        offset = DpOffset(x = 0.dp, y = 8.dp),
                    ) {
                        DropdownMenuItem(
                            text = { Text("编辑资料") },
                            onClick = {
                                toolbarMenuExpanded = false
                                onOpenSettings()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("查看收藏夹") },
                            onClick = {
                                toolbarMenuExpanded = false
                                onOpenCollections()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("退出登录") },
                            onClick = {
                                toolbarMenuExpanded = false
                                viewModel.logout()
                            },
                        )
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState)
        if (uiState.user == null) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    GuestMeContent(
                        onOpenLogin = onOpenLogin,
                        onOpenRegister = onOpenRegister,
                    )
                }
                item {
                    TechFlowFooter()
                }
            }
        } else if (profileViewModel != null) {
            val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
            if (profileUiState.isLoading && profileUiState.profile == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                ProfileContent(
                    uiState = profileUiState,
                    onTabSelected = profileViewModel::onTabSelected,
                    onQuestionClick = onQuestionClick,
                    onOpenSettings = onOpenSettings,
                    showHeroEditButton = false,
                )
            }
        }
    }
}

@Composable
private fun GuestMeContent(
    onOpenLogin: () -> Unit,
    onOpenRegister: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "登录后查看完整个人主页",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "我的页面只展示个人主页、收藏、声望、评论、得票和徽章等账户内容。登录和注册已经移到独立页面，避免和个人主页混在一起。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onOpenLogin,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                ) {
                    Text("去登录")
                }
                Button(
                    onClick = onOpenRegister,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("没有账户？去注册")
                }
            }
        }
    }
}
