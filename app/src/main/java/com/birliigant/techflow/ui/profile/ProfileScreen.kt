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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.AnswerItem
import com.birliigant.techflow.core.model.BadgeAward
import com.birliigant.techflow.core.model.PersonalCommentActivity
import com.birliigant.techflow.core.model.PublicUserProfile
import com.birliigant.techflow.core.model.QuestionSummary
import com.birliigant.techflow.core.model.ReputationActivity
import com.birliigant.techflow.core.model.VoteActivity
import com.birliigant.techflow.core.model.badgeLevelLabel
import com.birliigant.techflow.core.model.formatDisplayDate
import com.birliigant.techflow.core.model.markdownPreview
import com.birliigant.techflow.core.model.objectTypeLabel
import com.birliigant.techflow.core.model.reputationTypeLabel
import com.birliigant.techflow.core.model.voteTypeLabel
import com.birliigant.techflow.data.repository.SessionRepository
import com.birliigant.techflow.data.repository.UserRepository
import com.birliigant.techflow.ui.common.AvatarImage
import com.birliigant.techflow.ui.common.MarkdownText
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
    COLLECTIONS("collections", "收藏"),
    REPUTATION("reputation", "声望"),
    COMMENTS("comments", "评论"),
    VOTES("votes", "得票"),
    BADGES("badges", "徽章");

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
    val reputations: List<ReputationActivity> = emptyList(),
    val comments: List<PersonalCommentActivity> = emptyList(),
    val votes: List<VoteActivity> = emptyList(),
    val badges: List<BadgeAward> = emptyList(),
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
            val recentBadges = userRepository.getUserBadgeAwards(username, recentOnly = true).getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    answers = if (it.answers.isEmpty()) answers else it.answers,
                    questions = if (it.questions.isEmpty()) questions else it.questions,
                    badges = if (it.badges.isEmpty()) recentBadges else it.badges,
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

                ProfileTab.REPUTATION -> {
                    val result = userRepository.getPersonalRanks(username)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            reputations = result.getOrNull().orEmpty(),
                            errorMessage = result.exceptionOrNull()?.message,
                        )
                    }
                }

                ProfileTab.COMMENTS -> {
                    val result = userRepository.getPersonalComments(username)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            comments = result.getOrNull().orEmpty(),
                            errorMessage = result.exceptionOrNull()?.message,
                        )
                    }
                }

                ProfileTab.VOTES -> {
                    val currentUsername = _uiState.value.currentUsername
                    val result = if (currentUsername == username) {
                        userRepository.getPersonalVotes()
                    } else {
                        Result.success(emptyList())
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            votes = result.getOrNull().orEmpty(),
                            errorMessage = result.exceptionOrNull()?.message,
                        )
                    }
                }

                ProfileTab.BADGES -> {
                    val result = userRepository.getUserBadgeAwards(username)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            badges = result.getOrNull().orEmpty(),
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
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TechFlowTopBar(
            title = uiState.profile?.displayName ?: "用户主页",
            dense = true,
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

        ProfileContent(
            uiState = uiState,
            onTabSelected = viewModel::onTabSelected,
            onQuestionClick = onQuestionClick,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onTabSelected: (ProfileTab) -> Unit,
    onQuestionClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    showHeroEditButton: Boolean = true,
) {
    val isCurrentUser = uiState.currentUsername == uiState.profile?.username
    val visibleTabs = profileTabsFor(isCurrentUser)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.profile?.let { profile ->
            item {
                ProfileHeroCard(
                    profile = profile,
                    isCurrentUser = uiState.currentUsername == profile.username,
                    onOpenSettings = onOpenSettings,
                    showEditButton = showHeroEditButton,
                )
            }
            item {
                ProfileTabRow(
                    tabs = visibleTabs,
                    selectedTab = uiState.selectedTab,
                    onTabSelected = onTabSelected,
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
                            badges = uiState.badges.take(6),
                            onQuestionClick = onQuestionClick,
                        )
                    }
                }
            }

            ProfileTab.QUESTIONS -> {
                if (uiState.questions.isEmpty()) {
                    item { EmptyState(text = "这个用户还没有公开的问题内容。") }
                } else {
                    itemsIndexed(uiState.questions) { _, item ->
                        QuestionRow(item = item, onClick = { onQuestionClick(item.id) })
                    }
                }
            }

            ProfileTab.ANSWERS -> {
                if (uiState.answers.isEmpty()) {
                    item { EmptyState(text = "这个用户还没有公开的回答内容。") }
                } else {
                    itemsIndexed(uiState.answers) { _, item ->
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
                    itemsIndexed(uiState.collections) { _, item ->
                        QuestionRow(item = item, onClick = { onQuestionClick(item.id) })
                    }
                }
            }

            ProfileTab.REPUTATION -> {
                if (uiState.reputations.isEmpty()) {
                    item { EmptyState(text = "还没有声望变动记录。") }
                } else {
                    itemsIndexed(uiState.reputations) { _, item ->
                        ReputationRow(item = item, onQuestionClick = onQuestionClick)
                    }
                }
            }

            ProfileTab.COMMENTS -> {
                if (uiState.comments.isEmpty()) {
                    item { EmptyState(text = "这个用户还没有公开的评论内容。") }
                } else {
                    itemsIndexed(uiState.comments) { _, item ->
                        CommentRow(item = item, onQuestionClick = onQuestionClick)
                    }
                }
            }

            ProfileTab.VOTES -> {
                if (!isCurrentUser) {
                    item { EmptyState(text = "得票记录仅对当前登录用户开放。") }
                } else if (uiState.votes.isEmpty()) {
                    item { EmptyState(text = "还没有投票记录。") }
                } else {
                    itemsIndexed(uiState.votes) { _, item ->
                        VoteRow(item = item, onQuestionClick = onQuestionClick)
                    }
                }
            }

            ProfileTab.BADGES -> {
                if (uiState.badges.isEmpty()) {
                    item { EmptyState(text = "还没有获得徽章。") }
                } else {
                    itemsIndexed(uiState.badges) { _, item ->
                        BadgeRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeroCard(
    profile: PublicUserProfile,
    isCurrentUser: Boolean,
    onOpenSettings: () -> Unit,
    showEditButton: Boolean,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {}

            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.padding(top = 18.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AvatarImage(
                        imageUrl = profile.avatar,
                        fallbackText = profile.displayName,
                        modifier = Modifier.size(76.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "@${profile.username}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (profile.profession.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    text = profile.profession,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileStatChip(label = "声望", value = profile.rank.toString())
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
                if (isCurrentUser && showEditButton) {
                    Button(onClick = onOpenSettings) {
                        Text("编辑资料")
                    }
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
    tabs: List<ProfileTab>,
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(tabs, key = { it.routeValue }) { tab ->
            SectionSwitch(
                text = tab.label,
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
            )
        }
    }
}

@Composable
private fun OverviewSection(
    profile: PublicUserProfile,
    answers: List<AnswerItem>,
    questions: List<QuestionSummary>,
    badges: List<BadgeAward>,
    onQuestionClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OverviewCard(title = "关于我") {
            MarkdownText(profile.bio.ifBlank { "// Hello, World!" })
        }

        OverviewCard(title = "状态") {
            InfoLine(label = "加入时间", value = formatDisplayDate(profile.createdAt).ifBlank { "未知" })
            InfoLine(label = "最近登录", value = formatDisplayDate(profile.lastLoginAt).ifBlank { "未知" })
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

        OverviewCard(title = "最近的徽章") {
            if (badges.isEmpty()) {
                EmptyLabel(text = "还没有获得徽章。")
            } else {
                badges.forEach { badge ->
                    BadgeSummaryChip(item = badge)
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
                text = "${item.voteCount} 赞 · ${formatDisplayDate(item.createdAt).ifBlank { "刚刚" }}",
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
                text = "${item.voteCount} 点赞 · ${item.answerCount} 回答 · ${item.viewCount} 浏览 · ${formatDisplayDate(item.createdAt).ifBlank { "刚刚" }}",
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
                text = "${item.voteCount} 赞 · ${formatDisplayDate(item.createdAt).ifBlank { "刚刚" }}",
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

@Composable
private fun ReputationRow(
    item: ReputationActivity,
    onQuestionClick: (String) -> Unit,
) {
    TimelineCard(
        title = item.title.ifBlank { reputationTypeLabel(item.rankType) },
        content = item.content,
        meta = "${reputationTypeLabel(item.rankType)} · +${item.reputation} 声望 · ${formatDisplayDate(item.createdAt).ifBlank { "刚刚" }}",
        badge = objectTypeLabel(item.objectType),
        onClick = item.questionId.takeIf { it.isNotBlank() }?.let { { onQuestionClick(it) } },
    )
}

@Composable
private fun CommentRow(
    item: PersonalCommentActivity,
    onQuestionClick: (String) -> Unit,
) {
    TimelineCard(
        title = item.title.ifBlank { objectTypeLabel(item.objectType) },
        content = item.content,
        meta = "${objectTypeLabel(item.objectType)} · ${formatDisplayDate(item.createdAt).ifBlank { "刚刚" }}",
        badge = "评论",
        onClick = item.questionId.takeIf { it.isNotBlank() }?.let { { onQuestionClick(it) } },
    )
}

@Composable
private fun VoteRow(
    item: VoteActivity,
    onQuestionClick: (String) -> Unit,
) {
    TimelineCard(
        title = item.title.ifBlank { objectTypeLabel(item.objectType) },
        content = item.content,
        meta = "${voteTypeLabel(item.voteType)} · ${objectTypeLabel(item.objectType)} · ${formatDisplayDate(item.createdAt).ifBlank { "刚刚" }}",
        badge = voteTypeLabel(item.voteType),
        onClick = item.questionId.takeIf { it.isNotBlank() }?.let { { onQuestionClick(it) } },
    )
}

@Composable
private fun TimelineCard(
    title: String,
    content: String,
    meta: String,
    badge: String,
    onClick: (() -> Unit)? = null,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base },
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (content.isNotBlank()) {
                Text(
                    text = markdownPreview(content, maxLength = 180),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BadgeRow(item: BadgeAward) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarImage(
                imageUrl = item.icon?.takeIf(::isRemoteImageUrl),
                fallbackText = item.name,
                modifier = Modifier.size(56.dp),
                fallback = {
                    BadgeIcon(
                        icon = item.icon,
                        contentDescription = item.name,
                        modifier = Modifier.size(56.dp),
                    )
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${badgeLevelLabel(item.level)} · 已获得 ${item.earnedCount} 次",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun BadgeSummaryChip(item: BadgeAward) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarImage(
                imageUrl = item.icon?.takeIf(::isRemoteImageUrl),
                fallbackText = item.name,
                modifier = Modifier.size(42.dp),
                fallback = {
                    BadgeIcon(
                        icon = item.icon,
                        contentDescription = item.name,
                        modifier = Modifier.size(42.dp),
                    )
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = item.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = badgeLevelLabel(item.level),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun BadgeIcon(
    icon: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon.toBadgeVector(),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

private fun String?.toBadgeVector(): ImageVector {
    return when (orEmpty()) {
        "hand-thumbs-up-fill" -> Icons.Outlined.ThumbUp
        "check-circle-fill" -> Icons.Filled.CheckCircle
        "check-square-fill" -> Icons.Outlined.CheckBox
        else -> Icons.Outlined.EmojiEvents
    }
}

private fun isRemoteImageUrl(value: String): Boolean {
    return value.startsWith("http://") || value.startsWith("https://")
}

private fun profileTabsFor(isCurrentUser: Boolean): List<ProfileTab> {
    return if (isCurrentUser) {
        listOf(
            ProfileTab.OVERVIEW,
            ProfileTab.ANSWERS,
            ProfileTab.QUESTIONS,
            ProfileTab.COLLECTIONS,
            ProfileTab.REPUTATION,
            ProfileTab.COMMENTS,
            ProfileTab.VOTES,
            ProfileTab.BADGES,
        )
    } else {
        listOf(
            ProfileTab.OVERVIEW,
            ProfileTab.ANSWERS,
            ProfileTab.QUESTIONS,
            ProfileTab.REPUTATION,
            ProfileTab.COMMENTS,
            ProfileTab.BADGES,
        )
    }
}
