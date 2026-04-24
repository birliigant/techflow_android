package com.birliigant.techflow.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.UserProfile
import com.birliigant.techflow.data.repository.SessionRepository
import com.birliigant.techflow.data.repository.UserRepository
import com.birliigant.techflow.ui.common.AvatarBadge
import com.birliigant.techflow.ui.common.AvatarImage
import com.birliigant.techflow.ui.common.TechFlowFooter
import com.birliigant.techflow.ui.common.TechFlowTopBar
import com.birliigant.techflow.ui.common.TopBarFilledAction
import com.birliigant.techflow.ui.common.TopBarTextAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeUiState(
    val email: String = "",
    val password: String = "",
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

    fun updateEmail(value: String) = editorState.update { it.copy(email = value) }

    fun updatePassword(value: String) = editorState.update { it.copy(password = value) }

    fun consumeMessage() = editorState.update { it.copy(message = null) }

    fun login() {
        val state = uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            editorState.update { it.copy(message = "请输入邮箱和密码") }
            return
        }
        viewModelScope.launch {
            editorState.update { it.copy(busy = true, message = null) }
            val result = userRepository.loginWithEmail(state.email.trim(), state.password)
            editorState.update {
                it.copy(
                    busy = false,
                    password = "",
                    message = result.exceptionOrNull()?.message ?: "登录成功",
                )
            }
        }
    }

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
    onOpenProfile: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var userMenuExpanded by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

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
            title = "SIPC TechFlow",
            showMenu = true,
            onMenuClick = {},
        ) {
            if (uiState.user == null) {
                TopBarTextAction(text = "登录", onClick = {})
                TopBarFilledAction(text = "注册", onClick = {})
            } else {
                Box {
                    Row(
                        modifier = Modifier.clickable { userMenuExpanded = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AvatarBadge(text = uiState.user?.displayName ?: "我")
                        Text(
                            text = uiState.user?.displayName ?: "我的",
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "用户菜单",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    DropdownMenu(
                        expanded = userMenuExpanded,
                        onDismissRequest = { userMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("用户主页") },
                            onClick = {
                                userMenuExpanded = false
                                onOpenProfile()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("收藏夹") },
                            onClick = {
                                userMenuExpanded = false
                                onOpenCollections()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("账号设置") },
                            onClick = {
                                userMenuExpanded = false
                                onOpenSettings()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("退出") },
                            onClick = {
                                userMenuExpanded = false
                                viewModel.logout()
                            },
                        )
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                if (uiState.user == null) {
                    LoginContent(uiState = uiState, viewModel = viewModel)
                } else {
                    LoggedInContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenProfile = onOpenProfile,
                        onOpenCollections = onOpenCollections,
                        onOpenSettings = onOpenSettings,
                    )
                }
            }
            item {
                TechFlowFooter()
            }
        }
    }
}

@Composable
private fun LoginContent(
    uiState: MeUiState,
    viewModel: MeViewModel,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "欢迎来到 SIPC TechFlow",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::updateEmail,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("邮箱") },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        )
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::updatePassword,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("密码") },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        )
        Button(
            onClick = viewModel::login,
            enabled = !uiState.busy,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        ) {
            Text(if (uiState.busy) "登录中..." else "登录")
        }
        Text(
            text = "没有账户？先获取邀请码或在网页端完成注册后再登录。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoggedInContent(
    uiState: MeUiState,
    viewModel: MeViewModel,
    onOpenProfile: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val user = uiState.user ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "欢迎来到 SIPC TechFlow",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "你的账户信息会在每次进入页面时自动刷新，你可以继续浏览、提问和参与回答。",
            style = MaterialTheme.typography.bodyLarge,
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarImage(
                    imageUrl = user.avatar,
                    fallbackText = user.displayName,
                    modifier = Modifier.size(88.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProfileSummary(user)
                    if (user.profession.isNotBlank()) {
                        Text(
                            text = user.profession,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("更多功能", fontWeight = FontWeight.Bold)
                Button(
                    onClick = onOpenProfile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("进入用户主页")
                }
                Button(
                    onClick = onOpenCollections,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("查看收藏夹")
                }
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("打开账号设置")
                }
            }
        }
        Button(
            onClick = viewModel::logout,
            enabled = !uiState.busy,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        ) {
            Text("退出登录")
        }
    }
}

@Composable
private fun ProfileSummary(user: UserProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text("@${user.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (user.email.isNotBlank()) {
            Text(user.email)
        }
        Text("Rank ${user.rank} · ${user.questionCount} 提问 · ${user.answerCount} 回答")
    }
}
