package com.birliigant.techflow.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.AnswerItem
import com.birliigant.techflow.core.model.PublicUserProfile
import com.birliigant.techflow.core.model.QuestionSummary
import com.birliigant.techflow.core.model.markdownPreview
import com.birliigant.techflow.data.repository.SessionRepository
import com.birliigant.techflow.data.repository.UserRepository
import com.birliigant.techflow.ui.common.AvatarImage
import com.birliigant.techflow.ui.common.MarkdownText
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
        refreshOverviewContent()
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

    private fun refreshOverviewContent() {
        viewModelScope.launch {
            val answers = userRepository.getPersonalAnswers(username, pageSize = 3).getOrDefault(emptyList())
            val questions = userRepository.getPersonalQuestions(username, pageSize = 3).getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    answers = if (it.answers.isEmpty()) answers else it.answers,
                    questions = if (it.questions.isEmpty()) questions else it.questions,
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            uiState.profile?.let { profile ->
                item {
                    ProfileHeroCard(profile = profile)
                }
                item {
                    ProfileTabRow(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = viewModel::onTabSelected,
                    )
                }
            }

            if (uiState.errorMessage != null) {
                item {
                    EmptyState(text = uiState.errorMessage.orEmpty())
                }
            }

            when (uiState.selectedTab) {
                ProfileTab.OVERVIEW -> {
                    uiState.profile?.let { profile ->
                        item {
                            OverviewSection(
                                profile = profile,
                                answers = uiState.answers.take(3),
                                questions = uiState.questions.take(3),
                                onQuestionClick = onQuestionClick,
                            )
                        }
                    }
                }

                ProfileTab.QUESTIONS -> {
                    if (uiState.questions.isEmpty()) {
                        item { EmptyState(text = "这个用户还没有公开的问题内容。") }
                    } else {
                        items(uiState.questions, key = { it.id }) { item ->
                            QuestionRow(item = item, onClick = { onQuestionClick(item.id) })
                        }
                    }
                }

                ProfileTab.ANSWERS -> {
                    if (uiState.answers.isEmpty()) {
                        item { EmptyState(text = "这个用户还没有公开的回答内容。") }
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
private fun ProfileHeroCard(profile: PublicUserProfile) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {}

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(top = 34.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AvatarImage(
                        imageUrl = profile.avatar,
                        fallbackText = profile.displayName,
                        modifier = Modifier.size(92.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "@${profile.username}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (profile.profession.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    text = profile.profession,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileStatChip(label = "声望", value = profile.followCount.toString())
                    ProfileStatChip(label = "回答", value = profile.answerCount.toString())
                    ProfileStatChip(label = "问题", value = profile.questionCount.toString())
                }

                if (profile.bio.isNotBlank()) {
                    Text(
                        text = markdownPreview(profile.bio, maxLength = 120),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatChip(
    label: String,
    value: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ProfileTabRow(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(ProfileTab.entries, key = { it.routeValue }) { tab ->
            Surface(
                color = if (tab == selectedTab) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = MaterialTheme.shapes.small,
                shadowElevation = if (tab == selectedTab) 2.dp else 0.dp,
                modifier = Modifier.clickable { onTabSelected(tab) },
            ) {
                Text(
                    text = tab.label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (tab == selectedTab) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun OverviewSection(
    profile: PublicUserProfile,
    answers: List<AnswerItem>,
    questions: List<QuestionSummary>,
    onQuestionClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OverviewCard(title = "关于我") {
            MarkdownText(profile.bio.ifBlank { "// Hello, World!" })
        }

        OverviewCard(title = "状态") {
            InfoLine(label = "加入时间", value = profile.createdAt.ifBlank { "未知" })
            InfoLine(label = "最近登录", value = profile.lastLoginAt.ifBlank { "未知" })
            if (profile.location.isNotBlank()) {
                InfoLine(label = "地区", value = profile.location)
            }
            if (profile.website.isNotBlank()) {
                InfoLine(label = "网站", value = profile.website)
            }
        }

        OverviewCard(title = "高分回答") {
            if (answers.isEmpty()) {
                EmptyLabel(text = "没有找到相关的回答内容。")
            } else {
                answers.forEach { answer ->
                    AnswerPreviewCard(item = answer)
                }
            }
        }

        OverviewCard(title = "高分问题") {
            if (questions.isEmpty()) {
                EmptyLabel(text = "没有找到相关的问题内容。")
            } else {
                questions.forEach { question ->
                    QuestionPreviewCard(
                        item = question,
                        onClick = { onQuestionClick(question.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                content()
            },
        )
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = 16.dp),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuestionPreviewCard(
    item: QuestionSummary,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.title,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${item.voteCount} 点赞 · ${item.answerCount} 回答 · ${item.viewCount} 浏览",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AnswerPreviewCard(item: AnswerItem) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (item.accepted) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "已采纳",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text = markdownPreview(item.content, maxLength = 140),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.voteCount} 赞 · ${item.createdAt.ifBlank { "刚刚" }}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
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
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (item.excerpt.isNotBlank()) {
                Text(
                    text = item.excerpt,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${item.voteCount} 点赞 · ${item.answerCount} 回答 · ${item.viewCount} 浏览 · ${item.createdAt.ifBlank { "刚刚" }}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AnswerRow(item: AnswerItem) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (item.accepted) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "已采纳回答",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            MarkdownText(markdownPreview(item.content, maxLength = 220))
            Text(
                text = "${item.voteCount} 赞 · ${item.createdAt.ifBlank { "刚刚" }}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun EmptyLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmptyState(text: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
