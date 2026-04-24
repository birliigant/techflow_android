package com.birliigant.techflow.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.UserProfile
import com.birliigant.techflow.data.repository.ConfigRepository
import com.birliigant.techflow.data.repository.SessionRepository
import com.birliigant.techflow.data.repository.UserRepository
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
    val baseUrl: String = "",
    val tokenInput: String = "",
    val email: String = "",
    val password: String = "",
    val user: UserProfile? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

class MeViewModel(
    private val configRepository: ConfigRepository,
    sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val editorState = MutableStateFlow(
        MeUiState(baseUrl = configRepository.baseUrl.value),
    )

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

    init {
        refreshProfile()
    }

    fun updateBaseUrl(value: String) = editorState.update { it.copy(baseUrl = value) }

    fun updateToken(value: String) = editorState.update { it.copy(tokenInput = value) }

    fun updateEmail(value: String) = editorState.update { it.copy(email = value) }

    fun updatePassword(value: String) = editorState.update { it.copy(password = value) }

    fun consumeMessage() = editorState.update { it.copy(message = null) }

    fun saveBaseUrl() {
        configRepository.saveBaseUrl(uiState.value.baseUrl)
        editorState.update { it.copy(baseUrl = configRepository.baseUrl.value, message = "服务器地址已保存") }
    }

    fun saveManualToken() {
        val token = uiState.value.tokenInput.trim()
        if (token.isBlank()) {
            editorState.update { it.copy(message = "请先输入 token") }
            return
        }
        userRepository.saveManualToken(token)
        editorState.update { it.copy(tokenInput = "", message = "token 已保存，正在刷新用户信息...") }
        refreshProfile()
    }

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

    fun refreshProfile() {
        viewModelScope.launch {
            val result = userRepository.refreshCurrentUser()
            result.exceptionOrNull()?.message?.let { message ->
                editorState.update { it.copy(message = message) }
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
fun MeScreen(viewModel: MeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
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
                TopBarTextAction(text = uiState.user?.displayName ?: "我的", onClick = {})
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
                    LoggedInContent(uiState = uiState, viewModel = viewModel)
                }
            }
            item {
                DeveloperConfigSection(uiState = uiState, viewModel = viewModel)
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
) {
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
            text = "就差一步，我们已经连接到你的账户信息，你可以继续浏览、提问和参与回答。",
            style = MaterialTheme.typography.bodyLarge,
        )
        ProfileSummary(uiState.user!!)
        Button(
            onClick = viewModel::refreshProfile,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        ) {
            Text("刷新用户信息")
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
private fun DeveloperConfigSection(
    uiState: MeUiState,
    viewModel: MeViewModel,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("连接设置", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = uiState.baseUrl,
                onValueChange = viewModel::updateBaseUrl,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("服务根地址") },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            )
            Button(onClick = viewModel::saveBaseUrl, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
                Text("保存地址")
            }
            OutlinedTextField(
                value = uiState.tokenInput,
                onValueChange = viewModel::updateToken,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Access Token") },
                minLines = 3,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            )
            Button(onClick = viewModel::saveManualToken, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
                Text("保存 token")
            }
        }
    }
}

@Composable
private fun ProfileSummary(user: UserProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${user.displayName} (@${user.username})", style = MaterialTheme.typography.titleMedium)
        if (user.email.isNotBlank()) {
            Text(user.email)
        }
        Text("Rank ${user.rank} · ${user.questionCount} 提问 · ${user.answerCount} 回答")
    }
}
