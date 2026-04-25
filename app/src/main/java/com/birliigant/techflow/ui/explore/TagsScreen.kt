package com.birliigant.techflow.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.birliigant.techflow.core.model.TagDetail
import com.birliigant.techflow.core.model.TagSection
import com.birliigant.techflow.data.repository.TagRepository
import com.birliigant.techflow.ui.common.SectionSwitch
import com.birliigant.techflow.ui.common.TechFlowTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TagsUiState(
    val isLoading: Boolean = true,
    val sections: List<TagSection> = emptyList(),
    val errorMessage: String? = null,
)

class TagsViewModel(
    private val tagRepository: TagRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TagsUiState())
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = tagRepository.getTagSections()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    sections = result.getOrNull().orEmpty(),
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }
        }
    }
}

@Composable
fun TagsScreen(
    viewModel: TagsViewModel,
    onBack: () -> Unit,
    onTagClick: (TagDetail) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TechFlowTopBar(
            title = "标签",
            onBackClick = onBack,
        )

        if (uiState.isLoading && uiState.sections.isEmpty()) {
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (uiState.errorMessage != null && uiState.sections.isEmpty()) {
                item {
                    ElevatedCard {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }

            items(uiState.sections, key = { it.title }) { section ->
                TagSectionCard(
                    section = section,
                    onTagClick = onTagClick,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TagSectionCard(
    section: TagSection,
    onTagClick: (TagDetail) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (section.tags.isEmpty()) {
            Text("该分区还没有标签。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                section.tags.forEach { tag ->
                    TagCard(
                        tag = tag,
                        onClick = { onTagClick(tag) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TagCard(
    tag: TagDetail,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(0.48f)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = tag.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(tag.description.ifBlank { tag.name }, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${tag.questionCount} 帖子  ·  ${tag.followCount} 关注",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            if (tag.partition.isNotBlank()) {
                SectionSwitch(
                    text = "查看 ${tag.partition}",
                    selected = false,
                    onClick = onClick,
                )
            }
        }
    }
}
