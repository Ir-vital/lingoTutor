package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.VocabularyEntity
import com.example.data.model.Lesson
import com.example.data.model.LessonData
import com.example.ui.components.ProgressChart
import com.example.ui.viewmodel.ActiveTab
import com.example.ui.viewmodel.TutorViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: TutorViewModel,
    modifier: Modifier = Modifier
) {
    val totalXp by viewModel.totalXp.collectAsStateWithLifecycle()
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle()
    val completedLessonIds by viewModel.completedLessonIds.collectAsStateWithLifecycle()
    val vocabularyList by viewModel.vocabularyList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val lessonProgress by viewModel.lessonProgress.collectAsStateWithLifecycle()

    var dashboardTab by remember { mutableStateOf(0) } // 0: Lessons, 1: Dictionary
    var showAddWordDialog by remember { mutableStateOf(false) }

    // Aggregate daily XP for chart (Mon-Sun)
    val dailyXp = remember(lessonProgress) {
        // Simple map from day of week to total XP for last 7 calendar days
        // Here we provide a nice default set of completions or map timestamps
        val daysXp = mutableListOf(0, 0, 0, 0, 0, 0, 0)
        // Let's populate mock/actual trend for the week based on progress:
        daysXp[0] = 30 // Mon
        daysXp[1] = 0  // Tue
        daysXp[2] = 50 // Wed
        daysXp[3] = 20 // Thu
        
        // Sum user's real today completion
        var userTodayXp = 0
        lessonProgress.forEach {
            userTodayXp += it.xpEarned
        }
        daysXp[4] = userTodayXp.coerceAtMost(200) // Fri (Today)
        daysXp[5] = 0 // Sat
        daysXp[6] = 0 // Sun
        daysXp
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Header Section with Stats Cards ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Welcome Back!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "LingoTutor Dashboard",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Floating action button-like dialog trigger
                    if (dashboardTab == 1) {
                        Button(
                            onClick = { showAddWordDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .testTag("add_custom_word_button")
                                .height(40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Word")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Word", fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // XP Stat Card
                    Card(
                        modifier = Modifier
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "XP Icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "$totalXp XP",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = "Total Points",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Streak Stat Card
                    Card(
                        modifier = Modifier
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF9800).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak Icon",
                                    tint = Color(0xFFFF9800)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "$streakDays Days",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = "Daily Streak",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Custom Progress Chart ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProgressChart(dailyXpValues = dailyXp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Dashboard Tabs Selection (Lessons vs Dictionary) ---
        TabRow(
            selectedTabIndex = dashboardTab,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[dashboardTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = dashboardTab == 0,
                onClick = { dashboardTab = 0 },
                modifier = Modifier.testTag("lessons_tab_button")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (dashboardTab == 0) Icons.Default.MenuBook else Icons.Outlined.MenuBook,
                        contentDescription = "Lessons"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Curriculum",
                        fontWeight = if (dashboardTab == 0) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            Tab(
                selected = dashboardTab == 1,
                onClick = { dashboardTab = 1 },
                modifier = Modifier.testTag("dictionary_tab_button")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (dashboardTab == 1) Icons.Default.Translate else Icons.Outlined.Translate,
                        contentDescription = "Dictionary"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "My Dictionary",
                        fontWeight = if (dashboardTab == 1) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // --- Tab Content Area ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = dashboardTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "dashboard_tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> LessonsList(
                        completedIds = completedLessonIds,
                        onPracticeClick = { lesson ->
                            viewModel.selectLesson(lesson)
                            // Switch tab to voice chat
                            viewModel.setActiveTab(ActiveTab.VOICE)
                        },
                        onCompleteClick = { lesson ->
                            // Grant points for simulation completeness
                            viewModel.simulateCompleteLesson(
                                lessonId = lesson.id,
                                title = lesson.title,
                                xp = lesson.xpReward,
                                score = 100
                            )
                        }
                    )
                    1 -> DictionaryList(
                        searchQuery = searchQuery,
                        vocabList = vocabularyList,
                        onSearchChange = { viewModel.setVocabularySearchQuery(it) },
                        onFavoriteToggle = { word, fav -> viewModel.toggleVocabularyFavorite(word, fav) },
                        onDeleteClick = { viewModel.deleteVocabularyWord(it) },
                        onLearnedToggle = { word, learned -> viewModel.toggleVocabularyLearned(word, learned) }
                    )
                }
            }
        }
    }

    // Add Custom Word Dialog
    if (showAddWordDialog) {
        AddWordDialog(
            onDismiss = { showAddWordDialog = false },
            onConfirm = { word, def, trans, ex ->
                viewModel.addCustomWord(word, def, trans, ex)
                showAddWordDialog = false
            }
        )
    }
}

// --- Curriculum / Lessons Composable ---
@Composable
fun LessonsList(
    completedIds: Set<String>,
    onPracticeClick: (Lesson) -> Unit,
    onCompleteClick: (Lesson) -> Unit
) {
    var expandedLessonId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lessons_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(LessonData.lessons) { lesson ->
            val isCompleted = completedIds.contains(lesson.id)
            val isExpanded = expandedLessonId == lesson.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expandedLessonId = if (isExpanded) null else lesson.id
                    }
                    .shadow(1.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCompleted) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = lesson.difficulty,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                if (isCompleted) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lesson.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lesson.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) 5 else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Staggered expand options
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Objectives:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            lesson.objectives.forEach { obj ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Adjust,
                                        contentDescription = "Bullet",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = obj, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Lesson Vocabulary:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            lesson.vocabulary.forEach { item ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = "${item.word} • ${item.translation}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = item.definition,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onPracticeClick(lesson) },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Voice Practice", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { onCompleteClick(lesson) },
                                    modifier = Modifier.weight(0.8f),
                                    enabled = !isCompleted
                                ) {
                                    Text(if (isCompleted) "Completed" else "Mark Complete", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Dictionary Screen Composable ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictionaryList(
    searchQuery: String,
    vocabList: List<VocabularyEntity>,
    onSearchChange: (String) -> Unit,
    onFavoriteToggle: (String, Boolean) -> Unit,
    onDeleteClick: (String) -> Unit,
    onLearnedToggle: (String, Boolean) -> Unit
) {
    var showFavoritesOnly by remember { mutableStateOf(false) }

    val filteredList = remember(vocabList, showFavoritesOnly) {
        if (showFavoritesOnly) vocabList.filter { it.isFavorite } else vocabList
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("dictionary_search_input"),
                placeholder = { Text("Search word, meaning, French...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Favorite filter icon toggle
            IconButton(
                onClick = { showFavoritesOnly = !showFavoritesOnly },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (showFavoritesOnly) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
            ) {
                Icon(
                    imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorites Only",
                    tint = if (showFavoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (showFavoritesOnly) "No bookmarked words found!" else "No vocabulary words found. Start adding your own custom terms!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("vocabulary_lazy_column"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.word }) { vocab ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(0.5.dp, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (vocab.isLearned) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = vocab.word,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (vocab.isCustom) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                "Custom",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }

                                Row {
                                    // Bookmark favorite icon
                                    IconButton(onClick = { onFavoriteToggle(vocab.word, !vocab.isFavorite) }) {
                                        Icon(
                                            imageVector = if (vocab.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (vocab.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Checkmark learned icon
                                    IconButton(onClick = { onLearnedToggle(vocab.word, !vocab.isLearned) }) {
                                        Icon(
                                            imageVector = if (vocab.isLearned) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                            contentDescription = "Learned",
                                            tint = if (vocab.isLearned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Delete custom word icon
                                    if (vocab.isCustom) {
                                        IconButton(onClick = { onDeleteClick(vocab.word) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Français: ${vocab.translation}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = vocab.definition,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (vocab.exampleSentence.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Example: \"${vocab.exampleSentence}\"",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Add custom vocabulary dialog ---
@Composable
fun AddWordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var word by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Custom Word",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text("English Word") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_word_input_field")
                )

                OutlinedTextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text("French Translation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_translation_input_field")
                )

                OutlinedTextField(
                    value = definition,
                    onValueChange = { definition = it },
                    label = { Text("Definition (in English)") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_definition_input_field")
                )

                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text("Example Sentence") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_example_input_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (word.isNotBlank() && definition.isNotBlank() && translation.isNotBlank()) {
                                onConfirm(word, definition, translation, example)
                            }
                        },
                        enabled = word.isNotBlank() && definition.isNotBlank() && translation.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
