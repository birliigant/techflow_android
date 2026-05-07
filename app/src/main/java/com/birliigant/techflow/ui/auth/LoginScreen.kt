package com.birliigant.techflow.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.data.repository.UserRepository
import com.birliigant.techflow.ui.common.PasswordTextField
import com.birliigant.techflow.ui.common.TechFlowFooter
import com.birliigant.techflow.ui.common.TechFlowTopBar
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val message: String? = null,
)

sealed interface LoginEvent {
    data class LoginSucceeded(val message: String) : LoginEvent
}

class LoginViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun updateEmail(value: String) = _uiState.update { it.copy(email = value) }

    fun updatePassword(value: String) = _uiState.update { it.copy(password = value) }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(message = "请输入邮箱和密码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null) }
            val result = userRepository.loginWithEmail(state.email.trim(), state.password)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSubmitting = false, password = "") }
                _events.tryEmit(LoginEvent.LoginSucceeded("登录成功"))
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        message = result.exceptionOrNull()?.message ?: "登录失败",
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onOpenRegister: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.LoginSucceeded -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                    onLoggedIn()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TechFlowTopBar(
            title = "登录",
            onBackClick = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "欢迎来到 SIPC TechFlow",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "登录后即可继续浏览、提问、回答，并同步你的个人资料。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = viewModel::updateEmail,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("邮箱") },
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        )
                        PasswordTextField(
                            value = uiState.password,
                            onValueChange = viewModel::updatePassword,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = viewModel::submit,
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        ) {
                            Text(if (uiState.isSubmitting) "登录中..." else "登录")
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
            item {
                TechFlowFooter()
            }
        }
    }
}
