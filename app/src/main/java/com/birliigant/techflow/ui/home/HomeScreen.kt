package com.birliigant.techflow.ui.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birliigant.techflow.core.model.QuestionSummary
import com.birliigant.techflow.core.model.SiteInfo
import com.birliigant.techflow.core.model.UserProfile
import com.birliigant.techflow.core.model.formatDisplayDate
import com.birliigant.techflow.data.repository.QuestionRepository
import com.birliigant.techflow.data.repository.SessionRepository
import com.birliigant.techflow.data.repository.SiteRepository
import com.birliigant.techflow.data.repository.UserRepository
import com.birliigant.techflow.ui.common.AvatarBadge
import com.birliigant.techflow.ui.common.AvatarImage
import com.birliigant.techflow.ui.common.SectionSwitch
import com.birliigant.techflow.ui.common.TechFlowTopBar
import com.birliigant.techflow.ui.common.TopBarFilledAction
import com.birliigant.techflow.ui.common.TopBarTextAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuestionOrder(
    val apiValue: String,
    val label: String,
) {
    NEWEST("newest", "最新"),
    ACTIVE("active", "活跃"),
    HOT("hot", "热门"),
    SCORE("score", "评分"),
    UNANSWERED("unanswered", "未回答"),
    RECOMMEND("recommend", "推荐"),
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val siteInfo: SiteInfo? = null,
    val questions: List<QuestionSummary> = emptyList(),
    val errorMessage: String? = null,
    val currentUser: UserProfile? = null,
    val selectedOrder: QuestionOrder = QuestionOrder.NEWEST,
)

class HomeViewModel(
    private val siteRepository: SiteRepository,
    private val questionRepository: QuestionRepository,
    private val userRepository: UserRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            sessionRepository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
        refresh()
    }

    fun onOrderSelected(order: QuestionOrder) {
        if (_uiState.value.selectedOrder == order) return
        _uiState.update { it.copy(selectedOrder = order) }
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val selectedOrder = _uiState.value.selectedOrder
            val siteDeferred = async { siteRepository.getSiteInfo() }
            val questionsDeferred = async {
                questionRepository.getQuestionPage(order = selectedOrder.apiValue)
            }

            val siteResult = siteDeferred.await()
            val questionResult = questionsDeferred.await()

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    siteInfo = siteResult.getOrNull() ?: current.siteInfo,
                    questions = questionResult.getOrNull() ?: current.questions,
                    errorMessage = siteResult.exceptionOrNull()?.message
                        ?: questionResult.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onQuestionClick: (String) -> Unit,
    onOpenMe: () -> Unit,
    onOpenRegister: () -> Unit,
    onOpenSearch: (String) -> Unit,
    onOpenTags: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = viewModel::refresh,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HomeHeader(
                    siteInfo = uiState.siteInfo,
                    currentUser = uiState.currentUser,
                    selectedOrder = uiState.selectedOrder,
                    onOrderSelected = viewModel::onOrderSelected,
                    onOpenMe = onOpenMe,
                    onOpenRegister = onOpenRegister,
                    onOpenSearch = onOpenSearch,
                    onOpenTags = onOpenTags,
                    onOpenUsers = onOpenUsers,
                    onOpenProfile = onOpenProfile,
                    onOpenCollections = onOpenCollections,
                    onOpenSettings = onOpenSettings,
                    onLogout = viewModel::logout,
                )
            }

            item {
                if (uiState.isLoading && uiState.questions.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.errorMessage != null && uiState.questions.isEmpty()) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("暂时拉不到问题列表", style = MaterialTheme.typography.titleMedium)
                            Text(uiState.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("请稍后重试，或检查网络连接与账号状态。")
                        }
                    }
                }
            }

            items(uiState.questions, key = { it.id }) { question ->
                QuestionCard(
                    question = question,
                    onClick = { onQuestionClick(question.id) },
                    onAuthorClick = { onOpenUserProfile(question.authorUsername) },
                )
            }

            if (uiState.questions.isEmpty() && !uiState.isLoading && uiState.errorMessage == null) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        Text(
                            text = if (uiState.selectedOrder == QuestionOrder.ACTIVE) {
                                "当前“活跃”分类下还没有内容，试试切换到其他排序。"
                            } else {
                                "这里还没有内容，试试切换排序或发布你的第一个问题。"
                            },
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    siteInfo: SiteInfo?,
    currentUser: UserProfile?,
    selectedOrder: QuestionOrder,
    onOrderSelected: (QuestionOrder) -> Unit,
    onOpenMe: () -> Unit,
    onOpenRegister: () -> Unit,
    onOpenSearch: (String) -> Unit,
    onOpenTags: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    var navigationMenuExpanded by remember { mutableStateOf(false) }
    var userMenuExpanded by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(bottom = 18.dp),
    ) {
        TechFlowTopBar(
            title = siteInfo?.name ?: "SIPC TechFlow",
            showMenu = true,
            onMenuClick = { navigationMenuExpanded = true },
            menuContent = {
                DropdownMenu(
                    expanded = navigationMenuExpanded,
                    onDismissRequest = { navigationMenuExpanded = false },
                    offset = DpOffset(x = 0.dp, y = 8.dp),
                ) {
                    DropdownMenuItem(
                        text = { Text("问题") },
                        onClick = { navigationMenuExpanded = false },
                    )
                    DropdownMenuItem(
                        text = { Text("标签") },
                        onClick = {
                            navigationMenuExpanded = false
                            onOpenTags()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("用户") },
                        onClick = {
                            navigationMenuExpanded = false
                            onOpenUsers()
                        },
                    )
                }
            },
        ) {
            if (currentUser == null) {
                TopBarTextAction(text = "登录", onClick = onOpenMe)
                TopBarFilledAction(text = "注册", onClick = onOpenRegister)
            } else {
                Box {
                    Row(
                        modifier = Modifier.clickable { userMenuExpanded = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AvatarBadge(text = currentUser.displayName)
                        Text(
                            text = currentUser.displayName,
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
                        offset = DpOffset(x = 0.dp, y = 8.dp),
                    ) {
                        DropdownMenuItem(
                            text = { Text("我的主页") },
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
                                onLogout()
                            },
                        )
                    }
                }
            }
        }

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            placeholder = {
                Text(
                    text = "搜索问题、标签、用户",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                )
            },
            trailingIcon = {
                Text(
                    text = "搜索",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        searchQuery.trim().takeIf { it.isNotBlank() }?.let(onOpenSearch)
                    },
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = {
                    searchQuery.trim().takeIf { it.isNotBlank() }?.let(onOpenSearch)
                },
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                disabledContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                focusedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
                cursorColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "All Questions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = siteInfo?.shortDescription?.ifBlank {
                                siteInfo?.description?.ifBlank { "欢迎来到 SIPC TechFlow 学术问答社区" }
                                    ?: "欢迎来到 SIPC TechFlow 学术问答社区"
                            } ?: "欢迎来到 SIPC TechFlow 学术问答社区",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QuestionOrder.entries) { order ->
                        FilterPill(
                            text = order.label,
                            selected = order == selectedOrder,
                            onClick = { onOrderSelected(order) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: QuestionSummary,
    onClick: () -> Unit,
    onAuthorClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = question.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (question.excerpt.isNotBlank()) {
            Text(
                text = question.excerpt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AvatarImage(
                imageUrl = question.authorAvatar,
                fallbackText = question.authorName,
                modifier = Modifier.size(42.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = question.authorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onAuthorClick),
                )
                Text(
                    text = "@${question.authorUsername}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "提问于 ${formatDisplayDate(question.createdAt).ifBlank { "刚刚" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatPill(text = "${question.voteCount} 个点赞")
            AcceptedAnswerStat(
                answerCount = question.answerCount,
                accepted = question.accepted,
            )
            StatPill(text = "${question.viewCount} 次浏览")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            question.tags.take(2).forEachIndexed { index, tag ->
                TagPill(
                    text = tag.name,
                    paletteIndex = index,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun StatPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AcceptedAnswerStat(
    answerCount: Int,
    accepted: Boolean,
) {
    val acceptedColor = Color(0xFF1E8E5A)
    Surface(
        color = if (accepted) {
            acceptedColor.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        },
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (accepted) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "已有最佳回答",
                    tint = acceptedColor,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = "${answerCount} 个回答",
                style = MaterialTheme.typography.bodySmall,
                color = if (accepted) acceptedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (accepted) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SectionSwitch(text = text, selected = selected, onClick = onClick)
}

@Composable
private fun TagPill(
    text: String,
    paletteIndex: Int,
) {
    val background = when (paletteIndex % 4) {
        0 -> MaterialTheme.colorScheme.surfaceVariant
        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        2 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f)
        else -> MaterialTheme.colorScheme.background
    }
    val content = when (paletteIndex % 4) {
        0 -> MaterialTheme.colorScheme.onSurfaceVariant
        1 -> MaterialTheme.colorScheme.primary
        2 -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = background,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}
