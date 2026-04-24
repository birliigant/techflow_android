package com.birliigant.techflow.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.PublicUserProfile
import com.birliigant.techflow.core.model.UserProfileUpdate
import com.birliigant.techflow.data.repository.SessionRepository
import com.birliigant.techflow.data.repository.UserRepository
import com.birliigant.techflow.ui.common.AvatarImage
import com.birliigant.techflow.ui.common.TechFlowTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val username: String? = null,
    val profile: PublicUserProfile? = null,
    val displayName: String = "",
    val editedUsername: String = "",
    val profession: String = "",
    val bio: String = "",
    val website: String = "",
    val location: String = "",
    val message: String? = null,
    val errorMessage: String? = null,
)

class SettingsViewModel(
    private val userRepository: UserRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.currentUser.collect { currentUser ->
                _uiState.update { it.copy(username = currentUser?.username) }
                if (currentUser?.username.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = null,
                            errorMessage = "请先登录后再查看账号设置。",
                        )
                    }
                } else {
                    refresh()
                }
            }
        }
    }

    fun updateDisplayName(value: String) = _uiState.update { it.copy(displayName = value) }

    fun updateEditedUsername(value: String) = _uiState.update { it.copy(editedUsername = value) }

    fun updateProfession(value: String) = _uiState.update { it.copy(profession = value) }

    fun updateBio(value: String) = _uiState.update { it.copy(bio = value) }

    fun updateWebsite(value: String) = _uiState.update { it.copy(website = value) }

    fun updateLocation(value: String) = _uiState.update { it.copy(location = value) }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    fun refresh() {
        val username = _uiState.value.username ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = userRepository.getPublicProfile(username)
            _uiState.update { state ->
                val profile = result.getOrNull()
                if (profile != null) {
                    state.copy(
                        isLoading = false,
                        profile = profile,
                        displayName = profile.displayName,
                        editedUsername = profile.username,
                        profession = profile.profession,
                        bio = profile.bio,
                        website = profile.website,
                        location = profile.location,
                        errorMessage = null,
                    )
                } else {
                    state.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message,
                    )
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.editedUsername.isBlank()) {
            _uiState.update { it.copy(message = "用户名不能为空") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, message = null) }
            val result = userRepository.updateProfile(
                UserProfileUpdate(
                    displayName = state.displayName.trim(),
                    username = state.editedUsername.trim(),
                    bio = state.bio.trim(),
                    location = state.location.trim(),
                    website = state.website.trim(),
                    profession = state.profession.trim(),
                ),
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        username = state.editedUsername.trim(),
                        message = "资料已保存",
                    )
                }
                refresh()
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "保存失败",
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
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
                viewModel.refresh()
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
            title = "账号设置",
            onBackClick = onBack,
        )
        SnackbarHost(hostState = snackbarHostState)

        if (uiState.isLoading && uiState.profile == null && uiState.errorMessage == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(horizontal = 24.dp))
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            uiState.profile?.let { profile ->
                item {
                    EditableProfileCard(
                        profile = profile,
                        uiState = uiState,
                        viewModel = viewModel,
                    )
                }
            }

            if (uiState.errorMessage != null) {
                item {
                    ElevatedCard {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("暂时无法加载账号设置", fontWeight = FontWeight.Bold)
                            Text(uiState.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableProfileCard(
    profile: PublicUserProfile,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("个人资料", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            AvatarImage(
                imageUrl = profile.avatar,
                fallbackText = profile.displayName,
                modifier = Modifier.size(92.dp),
            )
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::updateDisplayName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("显示名称") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.editedUsername,
                onValueChange = viewModel::updateEditedUsername,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("用户名") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.profession,
                onValueChange = viewModel::updateProfession,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("专业") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.location,
                onValueChange = viewModel::updateLocation,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("地区") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.website,
                onValueChange = viewModel::updateWebsite,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("网站") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.bio,
                onValueChange = viewModel::updateBio,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("关于我") },
                minLines = 4,
            )
            Text(
                text = "头像当前为只读展示；显示名、用户名、专业、地区、网站和简介可直接保存。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSaving) "保存中..." else "保存修改")
            }
        }
    }
}
