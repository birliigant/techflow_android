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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.QuestionSummary
import com.birliigant.techflow.data.repository.QuestionRepository
import com.birliigant.techflow.ui.common.AvatarImage
import com.birliigant.techflow.ui.common.TechFlowTopBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val isLoading: Boolean = false,
    val results: List<QuestionSummary> = emptyList(),
    val errorMessage: String? = null,
)

class SearchViewModel(
    initialQuery: String,
    private val questionRepository: QuestionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState(query = initialQuery))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        if (initialQuery.isNotBlank()) {
            submitSearch()
        }
    }

    fun updateQuery(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun submitSearch() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    submittedQuery = "",
                    results = emptyList(),
                    errorMessage = null,
                    isLoading = false,
                )
            }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    submittedQuery = query,
                    errorMessage = null,
                )
            }
            val result = questionRepository.searchQuestions(query = query, pageSize = 30)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    results = result.getOrNull().orEmpty(),
                    errorMessage = result.exceptionOrNull()?.message,
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
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TechFlowTopBar(
            title = "搜索",
            onBackClick = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = uiState.query,
                            onValueChange = viewModel::updateQuery,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("搜索问题") },
                            placeholder = { Text("输入关键词，例如：机器学习、前端、保研") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = { viewModel.submitSearch() },
                            ),
                        )
                        Button(
                            onClick = viewModel::submitSearch,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text("开始搜索")
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

            if (!uiState.isLoading && uiState.submittedQuery.isBlank()) {
                item {
                    EmptySearchState("输入关键词后即可搜索问题内容。")
                }
            }

            if (!uiState.isLoading && uiState.submittedQuery.isNotBlank() && uiState.results.isEmpty() && uiState.errorMessage == null) {
                item {
                    EmptySearchState("没有找到与“${uiState.submittedQuery}”相关的问题。")
                }
            }

            itemsIndexed(uiState.results) { _, question ->
                SearchResultCard(
                    question = question,
                    onClick = { onQuestionClick(question.id) },
                    onUserClick = { onUserClick(question.authorUsername) },
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    question: QuestionSummary,
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
            Text(
                text = question.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarImage(
                    imageUrl = question.authorAvatar,
                    fallbackText = question.authorName,
                    modifier = Modifier.size(40.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = question.authorName,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onUserClick),
                    )
                    Text(
                        text = "@${question.authorUsername} · ${question.createdAt.ifBlank { "刚刚" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (question.excerpt.isNotBlank()) {
                Text(
                    text = question.excerpt,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${question.voteCount} 点赞 · ${question.answerCount} 回答 · ${question.viewCount} 浏览",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
