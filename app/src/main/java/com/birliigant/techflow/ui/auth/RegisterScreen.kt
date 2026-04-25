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
import com.birliigant.techflow.data.repository.UserRepository
import com.birliigant.techflow.ui.common.ProfessionDropdownField
import com.birliigant.techflow.ui.common.TechFlowFooter
import com.birliigant.techflow.ui.common.TechFlowTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val profession: String = "",
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val successMessage: String? = null,
)

class RegisterViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun updateName(value: String) = _uiState.update { it.copy(name = value) }

    fun updateProfession(value: String) = _uiState.update { it.copy(profession = value) }

    fun updateEmail(value: String) = _uiState.update { it.copy(email = value) }

    fun updatePassword(value: String) = _uiState.update { it.copy(password = value) }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    fun submit() {
        val state = _uiState.value
        if (state.name.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(message = "请填写姓名、邮箱和密码") }
            return
        }
        if (state.profession.isBlank()) {
            _uiState.update { it.copy(message = "请选择专业") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null, successMessage = null) }
            val result = userRepository.registerWithEmail(
                name = state.name.trim(),
                email = state.email.trim(),
                password = state.password,
                profession = state.profession,
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        password = "",
                        successMessage = "注册成功，请前往邮箱完成激活后再登录。",
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        message = result.exceptionOrNull()?.message ?: "注册失败",
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onBack: () -> Unit,
) {
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
            title = "注册",
            onBackClick = onBack,
        )
        SnackbarHost(hostState = snackbarHostState)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
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
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = viewModel::updateName,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("名字") },
                            singleLine = true,
                        )
                        ProfessionDropdownField(
                            value = uiState.profession,
                            onValueChange = viewModel::updateProfession,
                            label = "专业",
                        )
                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = viewModel::updateEmail,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("邮箱") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = viewModel::updatePassword,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("密码") },
                            singleLine = true,
                        )
                        Button(
                            onClick = viewModel::submit,
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (uiState.isSubmitting) "注册中..." else "注册")
                        }
                        Text(
                            text = uiState.successMessage ?: "注册后请留意邮箱中的激活邮件。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                TechFlowFooter()
            }
        }
    }
}
