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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.PublicUserProfile
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
    val username: String? = null,
    val profile: PublicUserProfile? = null,
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

    fun refresh() {
        val username = _uiState.value.username ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = userRepository.getPublicProfile(username)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    profile = result.getOrNull(),
                    errorMessage = result.exceptionOrNull()?.message,
                )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TechFlowTopBar(
            title = "账号设置",
            onBackClick = onBack,
        )

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
                            ReadonlyField("显示名称", profile.displayName)
                            ReadonlyField("用户名", profile.username)
                            ReadonlyField("专业", profile.profession)
                            ReadonlyField("关于我", profile.bio)
                            ReadonlyField("网站", profile.website)
                            ReadonlyField("地区", profile.location)
                            ReadonlyField("加入时间", profile.createdAt)
                            ReadonlyField("最近登录", profile.lastLoginAt)
                        }
                    }
                }
                item {
                    ElevatedCard {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("当前能力说明", fontWeight = FontWeight.Bold)
                            Text(
                                "当前 App 已接入资料查看、用户主页、收藏夹、标签和用户列表。资料修改接口在公开文档里没有面向普通用户的稳定写接口，所以这里先做成安全的只读设置页。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = viewModel::refresh,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("刷新资料")
                            }
                        }
                    }
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
private fun ReadonlyField(
    label: String,
    value: String,
) {
    OutlinedTextField(
        value = value.ifBlank { "未填写" },
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        readOnly = true,
        enabled = false,
    )
}
