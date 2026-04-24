package com.birliigant.techflow.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.birliigant.techflow.R
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
                HomeHeader(
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
private fun HomeHeader(
    siteInfo: SiteInfo?,
    baseUrl: String,
    onRefresh: () -> Unit,
    onOpenMe: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .padding(top = 10.dp, bottom = 18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_brand_mark),
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = siteInfo?.name ?: "Answer",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新", tint = MaterialTheme.colorScheme.onPrimary)
                }
                IconButton(onClick = onOpenMe) {
                    Icon(Icons.Outlined.Person, contentDescription = "我的", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                )
                Text(
                    text = "Search questions, answers, tags",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
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
                            text = siteInfo?.shortDescription?.ifBlank { "当前服务: $baseUrl" } ?: "当前服务: $baseUrl",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FilterStripButton(label = "More", icon = Icons.Outlined.MoreHoriz)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill(text = "Active", selected = true)
                    FilterPill(text = "Newest", selected = false)
                    FilterPill(text = "Frequent", selected = false)
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: QuestionSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = question.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${question.authorName} · ${question.voteCount} votes · ${question.answerCount} answers · ${question.viewCount} views",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (question.excerpt.isNotBlank()) {
                Text(
                    text = question.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                question.tags.take(4).forEachIndexed { index, tag ->
                    TagPill(
                        text = tag.name,
                        paletteIndex = index,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean) {
    FilterChip(
        selected = selected,
        onClick = {},
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.background,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun RowScope.FilterStripButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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
