package com.birliigant.techflow.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.birliigant.techflow.core.model.QuestionSummary
import com.birliigant.techflow.core.model.SiteInfo
import com.birliigant.techflow.data.repository.ConfigRepository
import com.birliigant.techflow.data.repository.QuestionRepository
import com.birliigant.techflow.data.repository.SiteRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val siteInfo: SiteInfo? = null,
    val questions: List<QuestionSummary> = emptyList(),
    val errorMessage: String? = null,
    val baseUrl: String = "",
)

class HomeViewModel(
    private val siteRepository: SiteRepository,
    private val questionRepository: QuestionRepository,
    configRepository: ConfigRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(baseUrl = configRepository.baseUrl.value) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val siteDeferred = async { siteRepository.getSiteInfo() }
            val questionsDeferred = async { questionRepository.getQuestionPage() }

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
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onQuestionClick: (String) -> Unit,
    onOpenMe: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HomeHero(
                    siteInfo = uiState.siteInfo,
                    baseUrl = uiState.baseUrl,
                    onRefresh = viewModel::refresh,
                    onOpenMe = onOpenMe,
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
                            Text("可以先去“我的”里确认服务器地址和 token。")
                        }
                    }
                }
            }

            items(uiState.questions, key = { it.id }) { question ->
                QuestionCard(
                    question = question,
                    onClick = { onQuestionClick(question.id) },
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
                            text = "这里还没有内容，试试刷新或者发布你的第一个问题。",
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHero(
    siteInfo: SiteInfo?,
    baseUrl: String,
    onRefresh: () -> Unit,
    onOpenMe: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),
                        ),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = siteInfo?.name ?: "TechFlow",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = siteInfo?.shortDescription?.ifBlank { siteInfo.description }
                            ?: "基于 Apache Answer 接口文档生成的 Android 客户端",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                    )
                }
                Row {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onOpenMe) {
                        Icon(Icons.Outlined.Person, contentDescription = "我的", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Text(
                text = "当前服务: $baseUrl",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
private fun QuestionCard(
    question: QuestionSummary,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = question.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (question.excerpt.isNotBlank()) {
                Text(
                    text = question.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                question.tags.take(3).forEach { tag ->
                    AssistChip(
                        onClick = {},
                        label = { Text(tag.name) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${question.authorName}  ·  ${question.answerCount} 回答  ·  ${question.viewCount} 浏览",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
