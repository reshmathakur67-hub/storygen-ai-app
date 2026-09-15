package com.example.ui.creation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StoryPresets
import com.example.models.AspectRatio
import com.example.models.VideoDuration
import com.example.models.VisualArtStyle
import com.example.models.VoiceProfile
import com.example.ui.VideoStudioViewModel
import com.example.ui.export.ExportVideoDialog
import com.example.ui.history.VideoHistorySheet
import com.example.ui.player.CinematicVideoPlayer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoStudioScreen(
    viewModel: VideoStudioViewModel,
    modifier: Modifier = Modifier
) {
    val scriptText by viewModel.scriptText.collectAsStateWithLifecycle()
    val selectedRatio by viewModel.selectedRatio.collectAsStateWithLifecycle()
    val selectedDuration by viewModel.selectedDuration.collectAsStateWithLifecycle()
    val selectedStyle by viewModel.selectedStyle.collectAsStateWithLifecycle()
    val selectedVoice by viewModel.selectedVoice.collectAsStateWithLifecycle()
    val characterName by viewModel.characterName.collectAsStateWithLifecycle()
    val characterRole by viewModel.characterRole.collectAsStateWithLifecycle()

    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationStep by viewModel.generationStep.collectAsStateWithLifecycle()
    val generationProgress by viewModel.generationProgress.collectAsStateWithLifecycle()
    val isEnhancingScript by viewModel.isEnhancingScript.collectAsStateWithLifecycle()

    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentSceneIndex by viewModel.currentSceneIndex.collectAsStateWithLifecycle()
    val sceneProgress by viewModel.sceneProgress.collectAsStateWithLifecycle()
    val overallProgress by viewModel.overallProgress.collectAsStateWithLifecycle()
    val isAmbientAudioEnabled by viewModel.isAmbientAudioEnabled.collectAsStateWithLifecycle()
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()

    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val showHistorySheet by viewModel.showHistorySheet.collectAsStateWithLifecycle()
    val showCharacterDialog by viewModel.showCharacterDialog.collectAsStateWithLifecycle()

    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val exportProgress by viewModel.exportProgress.collectAsStateWithLifecycle()
    val exportStatus by viewModel.exportStatus.collectAsStateWithLifecycle()
    val exportResult by viewModel.exportResult.collectAsStateWithLifecycle()
    val exportError by viewModel.exportError.collectAsStateWithLifecycle()
    val showExportDialog by viewModel.showExportDialog.collectAsStateWithLifecycle()

    // Dialogs
    if (showExportDialog && activeProject != null) {
        val context = LocalContext.current
        ExportVideoDialog(
            project = activeProject!!,
            isExporting = isExporting,
            exportProgress = exportProgress,
            statusMessage = exportStatus,
            exportResult = exportResult,
            errorMessage = exportError,
            onOpenVideo = { uri, file -> viewModel.openExportedVideo(context, uri, file) },
            onShareVideo = { uri, file, title -> viewModel.shareExportedVideo(context, uri, file, title) },
            onSaveToDownloads = { viewModel.saveExportCopyDownloads() },
            onDismiss = { viewModel.dismissExportDialog() }
        )
    }

    if (showHistorySheet) {
        VideoHistorySheet(
            projects = savedProjects,
            onSelectProject = { proj -> viewModel.playProject(proj) },
            onDeleteProject = { id -> viewModel.deleteSavedProject(id) },
            onExportProject = { proj -> viewModel.startExport(proj) },
            onDismiss = { viewModel.showHistorySheet.value = false }
        )
    }

    if (showCharacterDialog) {
        CharacterSelectorDialog(
            currentCharacterName = characterName,
            currentCharacterRole = characterRole,
            onCharacterSelected = { name, role ->
                viewModel.characterName.value = name
                viewModel.characterRole.value = role
            },
            onDismiss = { viewModel.showCharacterDialog.value = false }
        )
    }

    if (isGenerating) {
        GenerationProgressDialog(
            stepDescription = generationStep,
            progress = generationProgress,
            style = selectedStyle,
            aspectRatio = selectedRatio
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = Color(0xFF07060E),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF7B2CBF),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MovieCreation,
                                    contentDescription = null,
                                    tint = Color(0xFF00F5D4),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI Video Generator",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Script to Video Studio",
                                color = Color(0xFF00F5D4),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startExport() },
                        modifier = Modifier.testTag("topbar_export_video_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export & Save Video",
                            tint = Color(0xFF00F5D4)
                        )
                    }

                    BadgedBox(
                        badge = {
                            if (savedProjects.isNotEmpty()) {
                                Badge(
                                    containerColor = Color(0xFFF72585),
                                    contentColor = Color.White
                                ) {
                                    Text(text = savedProjects.size.toString(), fontSize = 10.sp)
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.showHistorySheet.value = true },
                            modifier = Modifier.testTag("open_history_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = "Saved Videos",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF07060E)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 24.dp)
        ) {
            // 1. Live Video Player Canvas
            if (activeProject != null) {
                CinematicVideoPlayer(
                    project = activeProject!!,
                    currentSceneIndex = currentSceneIndex,
                    sceneProgress = sceneProgress,
                    overallProgress = overallProgress,
                    isPlaying = isPlaying,
                    isAmbientAudioEnabled = isAmbientAudioEnabled,
                    isVoiceEnabled = isVoiceEnabled,
                    isFullscreen = isFullscreen,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onReplay = { viewModel.replay() },
                    onNextScene = { viewModel.nextScene() },
                    onPrevScene = { viewModel.prevScene() },
                    onToggleAmbientAudio = { viewModel.toggleAmbientAudio() },
                    onToggleVoice = { viewModel.toggleVoice() },
                    onToggleFullscreen = { viewModel.toggleFullscreen() },
                    onToggleAspectRatio = { viewModel.toggleAspectRatio() },
                    onExportVideo = { viewModel.startExport() },
                    modifier = Modifier.testTag("cinematic_video_player")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Daily Script Inspiration Carousel
            Text(
                text = "Daily Story Starters / रोज़ की कहानियाँ",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(StoryPresets.dailyTemplates) { template ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF141224),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2844)),
                        modifier = Modifier
                            .clickable { viewModel.applyTemplate(template) }
                            .testTag("template_${template.category.replace(" ", "_")}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "✨", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = template.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${template.recommendedRatio.key} • ${template.recommendedDuration.seconds}s • ${template.recommendedStyle.title}",
                                    color = Color(0xFF00F5D4),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Script Input Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF100E20),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF221F38)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Script / Story (कोई भी स्क्रिप्ट)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // AI Enhance Button
                        Button(
                            onClick = { viewModel.enhanceScript() },
                            enabled = !isEnhancingScript && scriptText.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7B2CBF),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("enhance_script_btn")
                        ) {
                            if (isEnhancingScript) {
                                CircularProgressIndicator(
                                    color = Color(0xFF00F5D4),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Expanding...", fontSize = 11.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF00F5D4),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Enhance", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = scriptText,
                        onValueChange = { viewModel.scriptText.value = it },
                        placeholder = {
                            Text(
                                "कोई भी कहानी या स्क्रिप्ट लिखें (Hindi / English / Any genre)...",
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 13.sp
                            )
                        },
                        minLines = 3,
                        maxLines = 6,
                        trailingIcon = {
                            if (scriptText.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.scriptText.value = "" },
                                    modifier = Modifier.testTag("clear_script_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F5D4),
                            unfocusedBorderColor = Color(0xFF2B2844),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0A0914),
                            unfocusedContainerColor = Color(0xFF0A0914)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("script_input_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Character Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF17152B))
                            .clickable { viewModel.showCharacterDialog.value = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF00F5D4),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Character: ",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$characterName ($characterRole)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Change",
                            color = Color(0xFF00F5D4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Aspect Ratio & Duration Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ratio Card (9:16 vs 16:9)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF100E20),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF221F38)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Ratio (पहलू अनुपात)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // 9:16 Reel
                            val is916 = selectedRatio == AspectRatio.RATIO_9_16
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (is916) Color(0xFF7B2CBF) else Color(0xFF19172E))
                                    .border(
                                        1.dp,
                                        if (is916) Color(0xFF00F5D4) else Color(0xFF272445),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectedRatio.value = AspectRatio.RATIO_9_16 }
                                    .padding(vertical = 8.dp)
                                    .testTag("ratio_9_16_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = if (is916) Color(0xFF00F5D4) else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "9:16",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Reels",
                                        color = if (is916) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.4f),
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            // 16:9 Cinema
                            val is169 = selectedRatio == AspectRatio.RATIO_16_9
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (is169) Color(0xFF7B2CBF) else Color(0xFF19172E))
                                    .border(
                                        1.dp,
                                        if (is169) Color(0xFF00F5D4) else Color(0xFF272445),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectedRatio.value = AspectRatio.RATIO_16_9 }
                                    .padding(vertical = 8.dp)
                                    .testTag("ratio_16_9_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = if (is169) Color(0xFF00F5D4) else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "16:9",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "YouTube",
                                        color = if (is169) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.4f),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Duration Card (5s, 10s, 30s)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF100E20),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF221F38)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Duration (समय: 5, 10, 30s)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            VideoDuration.entries.forEach { dur ->
                                val isSelected = selectedDuration == dur
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF00F5D4) else Color(0xFF19172E))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF00F5D4) else Color(0xFF272445),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.selectedDuration.value = dur }
                                        .padding(vertical = 10.dp)
                                        .testTag("duration_${dur.seconds}s_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${dur.seconds}s",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${dur.sceneCount} cuts",
                                            color = if (isSelected) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.4f),
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Visual Art Styles Grid (Style bhi ho सारे)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF100E20),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF221F38)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Art Styles (सारे विज़ुअल स्टाइल्स)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedStyle.title,
                            color = Color(0xFF00F5D4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VisualArtStyle.entries.forEach { style ->
                            val isSelected = selectedStyle == style
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.linearGradient(
                                                listOf(Color(style.primaryColor), Color(style.secondaryColor))
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                listOf(Color(0xFF17152B), Color(0xFF141225))
                                            )
                                        }
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isSelected) Color(0xFF00F5D4) else Color(0xFF292648),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.selectedStyle.value = style }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                    .testTag("style_${style.key}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(style.accentColor))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = style.title,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Voice Profile Selection (voice ke sath)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF100E20),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF221F38)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = Color(0xFF00F5D4),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Voice Narration (वॉइस स्टाइल)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = selectedVoice.title,
                            color = Color(0xFFF72585),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VoiceProfile.entries.forEach { voice ->
                            val isSelected = selectedVoice == voice
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectedVoice.value = voice },
                                label = { Text(voice.title, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF7B2CBF),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF17152B),
                                    labelColor = Color.White.copy(alpha = 0.7f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color(0xFF292648),
                                    selectedBorderColor = Color(0xFF00F5D4),
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.dp
                                ),
                                modifier = Modifier.testTag("voice_${voice.key}")
                            )
                        }
                    }

                    Text(
                        text = selectedVoice.description,
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 7. BIG CTA GENERATE BUTTON
            Button(
                onClick = { viewModel.generateVideo() },
                enabled = !isGenerating && scriptText.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00F5D4),
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_video_cta_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MovieCreation,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Generate AI Video (${selectedRatio.key} • ${selectedDuration.seconds}s)",
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
