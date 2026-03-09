package com.voicediary.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicediary.app.data.local.VoiceEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToRecord: () -> Unit = {},
    viewModel: ListViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var entryToDelete by remember { mutableStateOf<VoiceEntry?>(null) }

    entryToDelete?.let { entry ->
        DeleteConfirmDialog(
            onConfirm = {
                viewModel.deleteEntry(entry)
                entryToDelete = null
            },
            onDismiss = { entryToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("기록 목록") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 검색 바
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::setSearchQuery
            )

            // 필터 칩 (검색 중이 아닐 때만)
            if (searchQuery.isBlank()) {
                FilterChipRow(
                    currentFilter = currentFilter,
                    onFilterSelected = viewModel::setFilter
                )
            }

            if (entries.isEmpty()) {
                if (searchQuery.isNotBlank()) {
                    // 검색 결과 없음
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "\"$searchQuery\"에 대한 검색 결과가 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    EmptyListContent(onNavigateToRecord = onNavigateToRecord)
                }
            } else {
                GroupedEntryList(
                    entries = entries,
                    searchQuery = searchQuery,
                    onEntryClick = { onNavigateToDetail(it.id) },
                    onEntryLongClick = { entryToDelete = it }
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("기록 검색...") },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = "검색")
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Filled.Clear, contentDescription = "지우기")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun FilterChipRow(
    currentFilter: ListFilter,
    onFilterSelected: (ListFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        items(ListFilter.entries) { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun EmptyListContent(onNavigateToRecord: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "아직 기록이 없습니다",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "음성으로 첫 번째 기록을 남겨보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onNavigateToRecord) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("첫 기록 남기기")
        }
    }
}

@Composable
private fun GroupedEntryList(
    entries: List<VoiceEntry>,
    searchQuery: String,
    onEntryClick: (VoiceEntry) -> Unit,
    onEntryLongClick: (VoiceEntry) -> Unit
) {
    val grouped = remember(entries) { groupByDate(entries) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (dateHeader, entriesForDate) ->
            item(key = "header_$dateHeader") {
                DateHeader(dateHeader)
            }
            items(entriesForDate, key = { it.id }) { entry ->
                VoiceEntryCard(
                    entry = entry,
                    searchQuery = searchQuery,
                    onClick = { onEntryClick(entry) },
                    onLongClick = { onEntryLongClick(entry) }
                )
            }
        }
    }
}

@Composable
private fun DateHeader(dateText: String) {
    Text(
        text = dateText,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VoiceEntryCard(
    entry: VoiceEntry,
    searchQuery: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    val typeIcon = if (entry.type == "diary") "\uD83D\uDCD4" else "\uD83D\uDCDD"
    val typeLabel = if (entry.type == "diary") "일기" else "메모"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = typeIcon, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                ReviewStatusBadge(status = entry.reviewStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 검색어 하이라이트가 적용된 텍스트
            HighlightedText(
                text = entry.correctedText.ifBlank { "(텍스트 없음)" },
                highlight = searchQuery,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = timeFormat.format(Date(entry.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    highlight: String,
    maxLines: Int = Int.MAX_VALUE
) {
    if (highlight.isBlank()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        return
    }

    val annotatedString = buildAnnotatedString {
        var start = 0
        val lowerText = text.lowercase()
        val lowerHighlight = highlight.lowercase()

        while (start < text.length) {
            val index = lowerText.indexOf(lowerHighlight, start)
            if (index == -1) {
                append(text.substring(start))
                break
            }
            // 매칭 전 텍스트
            append(text.substring(start, index))
            // 하이라이트
            withStyle(
                SpanStyle(
                    background = Color(0xFFFFEB3B),
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(text.substring(index, index + highlight.length))
            }
            start = index + highlight.length
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ReviewStatusBadge(status: String) {
    val isReviewed = status == "reviewed"
    val backgroundColor = if (isReviewed) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val textColor = if (isReviewed) Color(0xFF2E7D32) else Color(0xFFE65100)
    val label = if (isReviewed) "검토완료" else "검토전"

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("기록 삭제") },
        text = { Text("이 기록을 삭제하시겠습니까?\n삭제된 기록은 복구할 수 없습니다.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("삭제", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

private fun groupByDate(entries: List<VoiceEntry>): List<Pair<String, List<VoiceEntry>>> {
    val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
    val calendar = Calendar.getInstance()

    return entries
        .groupBy { entry ->
            calendar.timeInMillis = entry.createdAt
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        .toSortedMap(compareByDescending { it })
        .map { (dayMillis, entriesForDay) ->
            dateFormat.format(Date(dayMillis)) to entriesForDay
        }
}
