package com.birliigant.techflow.ui.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.CommunityUser
import com.birliigant.techflow.core.model.SearchPostItem
import com.birliigant.techflow.core.model.TagDetail
import com.birliigant.techflow.core.model.formatDisplayDate
import com.birliigant.techflow.data.repository.QuestionRepository
import com.birliigant.techflow.data.repository.TagRepository
import com.birliigant.techflow.data.repository.UiPreferenceRepository
import com.birliigant.techflow.data.repository.UserRepository
import com.birliigant.techflow.ui.common.AvatarImage
import com.birliigant.techflow.ui.common.SectionSwitch
import com.birliigant.techflow.ui.common.TechFlowTopBar
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchTab(val label: String) {
    QUESTIONS("问题"),
    TAGS("标签"),
    USERS("用户"),
}

enum class SearchSort(val label: String, val apiValue: String) {
    RELEVANCE("相关性", "relevance"),
    NEWEST("最新的", "newest"),
    ACTIVE("活跃的", "active"),
    SCORE("评分", "score"),
}

data class SearchUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val isLoading: Boolean = false,
    val selectedTab: SearchTab = SearchTab.QUESTIONS,
    val selectedSort: SearchSort = SearchSort.RELEVANCE,
    val showHintsDialog: Boolean = false,
    val posts: List<SearchPostItem> = emptyList(),
    val tags: List<TagDetail> = emptyList(),
    val users: List<CommunityUser> = emptyList(),
    val errorMessage: String? = null,
)

class SearchViewModel(
    initialQuery: String,
    private val questionRepository: QuestionRepository,
    private val tagRepository: TagRepository,
    private val userRepository: UserRepository,
    private val uiPreferenceRepository: UiPreferenceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState(query = initialQuery))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        if (!uiPreferenceRepository.hasSeenSearchHints()) {
            _uiState.update { it.copy(showHintsDialog = true) }
            uiPreferenceRepository.markSearchHintsSeen()
        }
        if (initialQuery.isNotBlank()) {
            submitSearch()
        }
    }

    fun updateQuery(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun onTabSelected(tab: SearchTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSortSelected(sort: SearchSort) {
        val submittedQuery = _uiState.value.submittedQuery
        _uiState.update { it.copy(selectedSort = sort) }
        if (submittedQuery.isNotBlank()) {
            performSearch(query = submittedQuery, keepSelectedTab = true)
        }
    }

    fun openHints() {
        _uiState.update { it.copy(showHintsDialog = true) }
    }

    fun dismissHints() {
        _uiState.update { it.copy(showHintsDialog = false) }
    }

    fun submitSearch() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    submittedQuery = "",
                    posts = emptyList(),
                    tags = emptyList(),
                    users = emptyList(),
                    errorMessage = null,
                    isLoading = false,
                )
            }
            return
        }
        performSearch(query = query, keepSelectedTab = false)
    }

    private fun performSearch(
        query: String,
        keepSelectedTab: Boolean,
    ) {
        viewModelScope.launch {
            val requestedSort = _uiState.value.selectedSort
            val currentTab = _uiState.value.selectedTab
            _uiState.update {
                it.copy(
                    isLoading = true,
                    submittedQuery = query,
                    errorMessage = null,
                )
            }
            val signals = SearchSignals.from(query)
            val postsDeferred = async {
                questionRepository.searchPosts(
                    query = query,
                    order = requestedSort.apiValue,
                    pageSize = 30,
                )
            }
            val tagsDeferred = async { tagRepository.getAllTags() }
            val usersDeferred = async { userRepository.getCommunityUsers() }

            val postResult = postsDeferred.await()
            val tagResult = tagsDeferred.await()
            val userResult = usersDeferred.await()

            val filteredTags = tagResult.getOrNull().orEmpty().filter { tag ->
                signals.matchesTag(tag)
            }
            val filteredUsers = userResult.getOrNull().orEmpty().filter { user ->
                signals.matchesUser(user)
            }

            val preferredTab = when {
                signals.userFilter != null && filteredUsers.isNotEmpty() -> SearchTab.USERS
                signals.tagFilter != null && filteredTags.isNotEmpty() -> SearchTab.TAGS
                else -> SearchTab.QUESTIONS
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedTab = if (keepSelectedTab) currentTab else preferredTab,
                    posts = postResult.getOrNull().orEmpty(),
                    tags = filteredTags,
                    users = filteredUsers,
                    errorMessage = postResult.exceptionOrNull()?.message
                        ?: tagResult.exceptionOrNull()?.message
                        ?: userResult.exceptionOrNull()?.message,
                )
            }
        }
    }
}

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onQuestionClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onTagClick: (TagDetail) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TechFlowTopBar(
            title = "搜索结果",
            dense = true,
            onBackClick = onBack,
        ) {
            IconButton(onClick = viewModel::openHints) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "高级搜索提示",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        SearchEntryBar(
            query = uiState.query,
            onQueryChange = viewModel::updateQuery,
            onSearch = viewModel::submitSearch,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SearchTab.entries, key = { it.name }) { tab ->
                            SectionSwitch(
                                text = tab.label,
                                selected = uiState.selectedTab == tab,
                                onClick = { viewModel.onTabSelected(tab) },
                            )
                        }
                    }
                    Text(
                        text = when (uiState.selectedTab) {
                            SearchTab.QUESTIONS -> "${uiState.posts.size} 个结果"
                            SearchTab.TAGS -> "${uiState.tags.size} 个标签"
                            SearchTab.USERS -> "${uiState.users.size} 个用户"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (uiState.selectedTab == SearchTab.QUESTIONS) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SearchSort.entries, key = { it.name }) { sort ->
                            SectionSwitch(
                                text = sort.label,
                                selected = uiState.selectedSort == sort,
                                onClick = { viewModel.onSortSelected(sort) },
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (uiState.errorMessage != null) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }

            when (uiState.selectedTab) {
                SearchTab.QUESTIONS -> {
                    if (!uiState.isLoading && uiState.posts.isEmpty() && uiState.submittedQuery.isNotBlank() && uiState.errorMessage == null) {
                        item { EmptySearchState("没有找到相关的问题或回答内容。") }
                    }
                    itemsIndexed(uiState.posts) { _, post ->
                        SearchPostCard(
                            post = post,
                            onClick = { onQuestionClick(post.questionId) },
                            onUserClick = { onUserClick(post.authorUsername) },
                        )
                    }
                }

                SearchTab.TAGS -> {
                    if (!uiState.isLoading && uiState.tags.isEmpty() && uiState.submittedQuery.isNotBlank() && uiState.errorMessage == null) {
                        item { EmptySearchState("没有找到相关标签。") }
                    }
                    itemsIndexed(uiState.tags) { _, tag ->
                        SearchTagCard(
                            tag = tag,
                            onClick = { onTagClick(tag) },
                        )
                    }
                }

                SearchTab.USERS -> {
                    if (!uiState.isLoading && uiState.users.isEmpty() && uiState.submittedQuery.isNotBlank() && uiState.errorMessage == null) {
                        item { EmptySearchState("没有找到相关用户。") }
                    }
                    itemsIndexed(uiState.users) { _, user ->
                        SearchUserCard(
                            user = user,
                            onClick = { onUserClick(user.username) },
                        )
                    }
                }
            }
        }

        if (uiState.showHintsDialog) {
            SearchHintsDialog(onDismiss = viewModel::dismissHints)
        }
    }
}

@Composable
private fun SearchEntryBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 18.dp, vertical = 10.dp),
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
                modifier = Modifier.clickable(onClick = onSearch),
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { onSearch() },
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
            unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
            disabledContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun SearchPostCard(
    post: SearchPostItem,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = if (post.objectType == "answer") {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = if (post.objectType == "answer") "回答" else "问题",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (post.objectType == "answer") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarImage(
                    imageUrl = post.authorAvatar,
                    fallbackText = post.authorName,
                    modifier = Modifier.size(40.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = post.authorName,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onUserClick),
                    )
                    Text(
                        text = "@${post.authorUsername} · ${formatDisplayDate(post.createdAt).ifBlank { "刚刚" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (post.excerpt.isNotBlank()) {
                Text(
                    text = post.excerpt,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (post.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(post.tags.take(3), key = { it.slug.ifBlank { it.name } }) { tag ->
                        SearchTagChip(tag.name)
                    }
                }
            }
            Text(
                text = "${post.voteCount} 点赞 · ${post.answerCount} 回答 · ${post.viewCount} 浏览",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchTagCard(
    tag: TagDetail,
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
                text = tag.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (tag.description.isNotBlank()) {
                Text(
                    text = tag.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${tag.questionCount} 帖子 · ${tag.followCount} 关注",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchUserCard(
    user: CommunityUser,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AvatarImage(
                imageUrl = user.avatar,
                fallbackText = user.displayName,
                modifier = Modifier.size(48.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("@${user.username}", color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "${user.rank} 声望值",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SearchHintsCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        SearchHintsContent(modifier = Modifier.padding(18.dp))
    }
}

@Composable
private fun SearchHintsDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        },
        title = {
            Text("高级搜索提示", fontWeight = FontWeight.Bold)
        },
        text = {
            SearchHintsContent()
        },
    )
}

@Composable
private fun SearchHintsContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SearchHintLine("[tag]", "在指定标签中搜索")
        SearchHintLine("user:username", "根据作者搜索")
        SearchHintLine("answers:0", "搜索未回答的问题")
        SearchHintLine("score:3", "评分 3+ 的帖子")
        SearchHintLine("is:question", "搜索问题")
        SearchHintLine("is:answer", "搜索回答")
    }
}

@Composable
private fun SearchHintLine(keyword: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = keyword,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchTagChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun EmptySearchState(text: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class SearchSignals(
    val raw: String,
    val normalized: String,
    val tagFilter: String? = null,
    val userFilter: String? = null,
) {
    fun matchesTag(tag: TagDetail): Boolean {
        val candidate = tagFilter ?: normalized
        if (candidate.isBlank()) return false
        return tag.name.contains(candidate, ignoreCase = true) ||
            tag.slug.contains(candidate, ignoreCase = true) ||
            tag.description.contains(candidate, ignoreCase = true)
    }

    fun matchesUser(user: CommunityUser): Boolean {
        val candidate = userFilter ?: normalized
        if (candidate.isBlank()) return false
        return user.username.contains(candidate, ignoreCase = true) ||
            user.displayName.contains(candidate, ignoreCase = true)
    }

    companion object {
        fun from(raw: String): SearchSignals {
            val tagMatch = Regex("\\[(.*?)]").find(raw)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
            val userMatch = Regex("user:([^\\s]+)").find(raw)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
            val normalized = raw
                .replace(Regex("\\[(.*?)]"), " ")
                .replace(Regex("user:([^\\s]+)"), " ")
                .replace(Regex("answers:\\d+"), " ")
                .replace(Regex("score:\\d+"), " ")
                .replace(Regex("is:(question|answer)"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            return SearchSignals(
                raw = raw,
                normalized = normalized,
                tagFilter = tagMatch,
                userFilter = userMatch,
            )
        }
    }
}
