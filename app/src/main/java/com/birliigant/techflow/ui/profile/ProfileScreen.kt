package com.birliigant.techflow.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.AnswerItem
import com.birliigant.techflow.core.model.PublicUserProfile
import com.birliigant.techflow.core.model.QuestionSummary
import com.birliigant.techflow.data.repository.SessionRepository
import com.birliigant.techflow.data.repository.UserRepository
import com.birliigant.techflow.ui.common.AvatarImage
import com.birliigant.techflow.ui.common.SectionSwitch
import com.birliigant.techflow.ui.common.TechFlowTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileTab(val routeValue: String, val label: String) {
    OVERVIEW("overview", "概览"),
    ANSWERS("answers", "回答"),
    QUESTIONS("questions", "问题"),
    COLLECTIONS("collections", "收藏");

    companion object {
        fun from(value: String?): ProfileTab {
            return entries.firstOrNull { it.routeValue == value } ?: OVERVIEW
        }
    }
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: PublicUserProfile? = null,
    val selectedTab: ProfileTab = ProfileTab.OVERVIEW,
    val questions: List<QuestionSummary> = emptyList(),
    val answers: List<AnswerItem> = emptyList(),
    val collections: List<QuestionSummary> = emptyList(),
    val currentUsername: String? = null,
    val errorMessage: String? = null,
)

class ProfileViewModel(
    private val username: String,
    private val initialTab: ProfileTab,
    private val userRepository: UserRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(selectedTab = initialTab))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.currentUser.collect { current ->
                _uiState.update { it.copy(currentUsername = current?.username) }
            }
        }
        refreshProfile()
        loadTab(initialTab)
    }

    fun onTabSelected(tab: ProfileTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab) }
        loadTab(tab)
    }

    private fun refreshProfile() {
        viewModelScope.launch {
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

    private fun loadTab(tab: ProfileTab) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (tab) {
                ProfileTab.OVERVIEW -> {
                    _uiState.update { it.copy(isLoading = false) }
                }

                ProfileTab.QUESTIONS -> {
                    val result = userRepository.getPersonalQuestions(username)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            questions = result.getOrNull().orEmpty(),
                            errorMessage = result.exceptionOrNull()?.message,
                        )
                    }
                }

                ProfileTab.ANSWERS -> {
                    val result = userRepository.getPersonalAnswers(username)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            answers = result.getOrNull().orEmpty(),
                            errorMessage = result.exceptionOrNull()?.message,
                        )
                    }
                }

                ProfileTab.COLLECTIONS -> {
                    val currentUsername = _uiState.value.currentUsername
                    val result = if (currentUsername == username) {
                        userRepository.getPersonalCollections()
                    } else {
                        Result.success(emptyList())
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            collections = result.getOrNull().orEmpty(),
                            errorMessage = result.exceptionOrNull()?.message,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onQuestionClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TechFlowTopBar(
            title = uiState.profile?.displayName ?: "用户主页",
            onBackClick = onBack,
        )

        if (uiState.isLoading && uiState.profile == null) {
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
                    ProfileHeader(profile = profile)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileTab.entries.forEach { tab ->
                            SectionSwitch(
                                text = tab.label,
                                selected = uiState.selectedTab == tab,
                                onClick = { viewModel.onTabSelected(tab) },
                            )
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                item {
                    ElevatedCard {
                        Text(uiState.errorMessage.orEmpty(), modifier = Modifier.padding(20.dp))
                    }
                }
            }

            when (uiState.selectedTab) {
                ProfileTab.OVERVIEW -> {
                    uiState.profile?.let { profile ->
                        item { OverviewSection(profile) }
                    }
                }

                ProfileTab.QUESTIONS -> {
                    if (uiState.questions.isEmpty()) {
                        item { EmptyState(text = "没有找到相关的问题内容。") }
                    } else {
                        items(uiState.questions, key = { it.id }) { item ->
                            QuestionRow(item = item, onClick = { onQuestionClick(item.id) })
                        }
                    }
                }

                ProfileTab.ANSWERS -> {
                    if (uiState.answers.isEmpty()) {
                        item { EmptyState(text = "没有找到相关的回答内容。") }
                    } else {
                        items(uiState.answers, key = { it.id }) { item ->
                            AnswerRow(item = item)
                        }
                    }
                }

                ProfileTab.COLLECTIONS -> {
                    if (uiState.currentUsername != uiState.profile?.username) {
                        item { EmptyState(text = "收藏夹仅对当前登录用户开放。") }
                    } else if (uiState.collections.isEmpty()) {
                        item { EmptyState(text = "还没有收藏内容。") }
                    } else {
                        items(uiState.collections, key = { it.id }) { item ->
                            QuestionRow(item = item, onClick = { onQuestionClick(item.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: PublicUserProfile) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AvatarImage(
            imageUrl = profile.avatar,
            fallbackText = profile.displayName,
            modifier = Modifier.size(88.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(profile.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("@${profile.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${profile.followCount} 声望  ${profile.answerCount} 个回答  ${profile.questionCount} 个问题",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (profile.profession.isNotBlank()) {
                Text(profile.profession, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OverviewSection(profile: PublicUserProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ElevatedCard {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("关于我", fontWeight = FontWeight.Bold)
                Text(profile.bio.ifBlank { "// Hello, World!" })
            }
        }
        ElevatedCard {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("状态", fontWeight = FontWeight.Bold)
                Text("加入于 ${profile.createdAt.ifBlank { "未知" }}，最近登录 ${profile.lastLoginAt.ifBlank { "未知" }}")
                if (profile.location.isNotBlank()) {
                    Text("地区：${profile.location}")
                }
                if (profile.website.isNotBlank()) {
                    Text("网站：${profile.website}")
                }
            }
        }
    }
}

@Composable
private fun QuestionRow(
    item: QuestionSummary,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(item.title, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${item.answerCount} 回答 · ${item.viewCount} 浏览",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AnswerRow(item: AnswerItem) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(item.content)
            Text(
                text = "${item.voteCount} 赞 · ${item.createdAt.ifBlank { "刚刚" }}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    ElevatedCard {
        Text(
            text = text,
            modifier = Modifier.padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
