package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SpeechSynthesizerManager
import com.example.data.StoryPresets
import com.example.data.local.VideoDatabase
import com.example.data.local.VideoProjectRepository
import com.example.data.remote.GeminiStoryService
import com.example.export.ExportResult
import com.example.export.VideoExporter
import com.example.models.AspectRatio
import com.example.models.CharacterPreset
import com.example.models.GeneratedVideoProject
import com.example.models.ScriptTemplate
import com.example.models.VideoDuration
import com.example.models.VideoScene
import com.example.models.VisualArtStyle
import com.example.models.VoiceProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VideoStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoProjectRepository
    private val geminiService = GeminiStoryService()
    val speechManager = SpeechSynthesizerManager(application)

    init {
        val db = VideoDatabase.getInstance(application)
        repository = VideoProjectRepository(db.videoProjectDao())
    }

    val savedProjects = repository.allProjects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // User Creation Inputs
    val scriptText = MutableStateFlow(StoryPresets.dailyTemplates.first().script)
    val selectedRatio = MutableStateFlow(AspectRatio.RATIO_9_16)
    val selectedDuration = MutableStateFlow(VideoDuration.SEC_10)
    val selectedStyle = MutableStateFlow(VisualArtStyle.CINEMATIC)
    val selectedVoice = MutableStateFlow(VoiceProfile.DRAMATIC)
    val characterName = MutableStateFlow("Arjun")
    val characterRole = MutableStateFlow("Cybernetic Warrior")

    // Generation State
    val isGenerating = MutableStateFlow(false)
    val generationStep = MutableStateFlow("")
    val generationProgress = MutableStateFlow(0f)
    val isEnhancingScript = MutableStateFlow(false)

    // Export State
    val isExporting = MutableStateFlow(false)
    val exportProgress = MutableStateFlow(0f)
    val exportStatus = MutableStateFlow("")
    val exportResult = MutableStateFlow<ExportResult?>(null)
    val exportError = MutableStateFlow<String?>(null)
    val showExportDialog = MutableStateFlow(false)

    // Active Playback State
    val activeProject = MutableStateFlow<GeneratedVideoProject?>(null)
    val isPlaying = MutableStateFlow(false)
    val currentSceneIndex = MutableStateFlow(0)
    val sceneProgress = MutableStateFlow(0f)
    val overallProgress = MutableStateFlow(0f)
    val isAmbientAudioEnabled = MutableStateFlow(true)
    val isVoiceEnabled = MutableStateFlow(true)
    val isFullscreen = MutableStateFlow(false)

    val showHistorySheet = MutableStateFlow(false)
    val showCharacterDialog = MutableStateFlow(false)

    private var playbackJob: Job? = null

    init {
        // Initialize with default demo project so the user sees a ready-to-play video on launch!
        val defaultTemplate = StoryPresets.dailyTemplates.first()
        viewModelScope.launch {
            val initialScenes = geminiService.generateStoryboard(
                script = defaultTemplate.script,
                characterName = defaultTemplate.characterName,
                characterRole = defaultTemplate.characterRole,
                style = defaultTemplate.recommendedStyle,
                ratio = defaultTemplate.recommendedRatio,
                duration = defaultTemplate.recommendedDuration
            )
            val demoProject = GeneratedVideoProject(
                title = defaultTemplate.title,
                originalScript = defaultTemplate.script,
                characterName = defaultTemplate.characterName,
                characterRole = defaultTemplate.characterRole,
                aspectRatio = defaultTemplate.recommendedRatio,
                durationSec = defaultTemplate.recommendedDuration.seconds,
                style = defaultTemplate.recommendedStyle,
                voiceProfile = VoiceProfile.DRAMATIC,
                scenes = initialScenes
            )
            activeProject.value = demoProject
        }
    }

    fun applyTemplate(template: ScriptTemplate) {
        scriptText.value = template.script
        characterName.value = template.characterName
        characterRole.value = template.characterRole
        selectedStyle.value = template.recommendedStyle
        selectedRatio.value = template.recommendedRatio
        selectedDuration.value = template.recommendedDuration
    }

    fun selectCharacter(preset: CharacterPreset) {
        characterName.value = preset.name
        characterRole.value = preset.role
    }

    fun enhanceScript() {
        if (scriptText.value.isBlank()) return
        viewModelScope.launch {
            isEnhancingScript.value = true
            val enhanced = geminiService.enhanceScript(
                ideaPrompt = scriptText.value,
                characterName = characterName.value,
                style = selectedStyle.value
            )
            if (enhanced.isNotBlank()) {
                scriptText.value = enhanced
            }
            isEnhancingScript.value = false
        }
    }

    fun generateVideo() {
        val script = scriptText.value.trim()
        if (script.isBlank()) return

        stopPlayback()

        viewModelScope.launch {
            isGenerating.value = true
            generationProgress.value = 0.1f
            generationStep.value = "Analyzing screenplay & theme..."

            delay(400)
            generationProgress.value = 0.35f
            generationStep.value = "Directing scenes & camera angles..."

            val scenes = geminiService.generateStoryboard(
                script = script,
                characterName = characterName.value.ifBlank { "Hero" },
                characterRole = characterRole.value.ifBlank { "Protagonist" },
                style = selectedStyle.value,
                ratio = selectedRatio.value,
                duration = selectedDuration.value
            )

            generationProgress.value = 0.70f
            generationStep.value = "Rendering ${selectedStyle.value.title} visuals in ${selectedRatio.value.key}..."
            delay(500)

            generationProgress.value = 0.90f
            generationStep.value = "Synchronizing ${selectedVoice.value.title} voice & score..."
            delay(400)

            val title = if (script.length > 30) script.take(30) + "..." else script
            val newProject = GeneratedVideoProject(
                title = title,
                originalScript = script,
                characterName = characterName.value.ifBlank { "Hero" },
                characterRole = characterRole.value.ifBlank { "Protagonist" },
                aspectRatio = selectedRatio.value,
                durationSec = selectedDuration.value.seconds,
                style = selectedStyle.value,
                voiceProfile = selectedVoice.value,
                scenes = scenes
            )

            // Save to Room DB
            val savedId = repository.saveProject(newProject)
            val finalProject = newProject.copy(id = savedId)

            generationProgress.value = 1.0f
            generationStep.value = "Video Generated Successfully!"
            delay(300)

            isGenerating.value = false
            activeProject.value = finalProject
            currentSceneIndex.value = 0
            sceneProgress.value = 0f
            overallProgress.value = 0f

            // Auto-play generated video
            startPlayback()
        }
    }

    fun playProject(project: GeneratedVideoProject) {
        stopPlayback()
        activeProject.value = project
        selectedRatio.value = project.aspectRatio
        selectedStyle.value = project.style
        selectedVoice.value = project.voiceProfile
        characterName.value = project.characterName
        characterRole.value = project.characterRole
        currentSceneIndex.value = 0
        sceneProgress.value = 0f
        overallProgress.value = 0f
        startPlayback()
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun replay() {
        stopPlayback()
        currentSceneIndex.value = 0
        sceneProgress.value = 0f
        overallProgress.value = 0f
        startPlayback()
    }

    fun nextScene() {
        val project = activeProject.value ?: return
        if (currentSceneIndex.value < project.scenes.size - 1) {
            val wasPlaying = isPlaying.value
            pausePlayback()
            currentSceneIndex.value += 1
            sceneProgress.value = 0f
            updateOverallProgress(project)
            if (wasPlaying) startPlayback()
        }
    }

    fun prevScene() {
        val project = activeProject.value ?: return
        if (currentSceneIndex.value > 0) {
            val wasPlaying = isPlaying.value
            pausePlayback()
            currentSceneIndex.value -= 1
            sceneProgress.value = 0f
            updateOverallProgress(project)
            if (wasPlaying) startPlayback()
        }
    }

    fun toggleAspectRatio() {
        val current = selectedRatio.value
        val next = if (current == AspectRatio.RATIO_9_16) AspectRatio.RATIO_16_9 else AspectRatio.RATIO_9_16
        selectedRatio.value = next
        activeProject.value?.let { proj ->
            activeProject.value = proj.copy(aspectRatio = next)
        }
    }

    fun toggleAmbientAudio() {
        val newVal = !isAmbientAudioEnabled.value
        isAmbientAudioEnabled.value = newVal
        if (isPlaying.value) {
            speechManager.startAmbientSound(newVal)
        } else {
            speechManager.stopAmbientSound()
        }
    }

    fun toggleVoice() {
        val newVal = !isVoiceEnabled.value
        isVoiceEnabled.value = newVal
        if (!newVal) {
            speechManager.stop()
        } else if (isPlaying.value) {
            speakCurrentScene()
        }
    }

    fun toggleFullscreen() {
        isFullscreen.value = !isFullscreen.value
    }

    fun deleteSavedProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    fun startExport(targetProject: GeneratedVideoProject? = activeProject.value) {
        val proj = targetProject ?: activeProject.value ?: return
        showExportDialog.value = true
        isExporting.value = true
        exportProgress.value = 0.05f
        exportStatus.value = "Initializing export engine..."
        exportResult.value = null
        exportError.value = null

        // Pause playback while exporting
        pausePlayback()

        viewModelScope.launch {
            try {
                val result = VideoExporter.exportVideoToPublicStorage(
                    context = getApplication(),
                    project = proj,
                    saveToDownloads = false,
                    onProgress = { prog, status ->
                        exportProgress.value = prog
                        exportStatus.value = status
                    }
                )
                exportResult.value = result
                isExporting.value = false
            } catch (e: Exception) {
                exportError.value = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
                isExporting.value = false
            }
        }
    }

    fun saveExportCopyDownloads() {
        val currentResult = exportResult.value ?: return
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            try {
                val uri = VideoExporter.saveToMediaStoreDownloads(
                    context = getApplication(),
                    sourceFile = currentResult.file,
                    title = proj.title
                )
                if (uri != null) {
                    exportStatus.value = "Saved copy to Downloads folder!"
                }
            } catch (e: Exception) {
                exportError.value = "Could not save to downloads: ${e.message}"
            }
        }
    }

    fun dismissExportDialog() {
        showExportDialog.value = false
        exportResult.value = null
        exportError.value = null
        isExporting.value = false
    }

    fun openExportedVideo(context: android.content.Context, uri: android.net.Uri?, file: java.io.File?) {
        VideoExporter.openVideo(context, uri, file)
    }

    fun shareExportedVideo(context: android.content.Context, uri: android.net.Uri?, file: java.io.File?, title: String) {
        VideoExporter.shareVideo(context, uri, file, title)
    }

    private fun startPlayback() {
        val project = activeProject.value ?: return
        if (project.scenes.isEmpty()) return

        isPlaying.value = true

        if (isAmbientAudioEnabled.value) {
            speechManager.startAmbientSound(true)
        }

        speakCurrentScene()

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val tickInterval = 50L // 20 fps smooth progress updates

            while (isPlaying.value) {
                val currentIdx = currentSceneIndex.value
                val scene = project.scenes.getOrNull(currentIdx) ?: break
                val sceneDuration = scene.durationMs.coerceAtLeast(1000L)
                val step = tickInterval.toFloat() / sceneDuration

                delay(tickInterval)

                val newProg = sceneProgress.value + step
                if (newProg >= 1f) {
                    // Advance to next scene
                    if (currentIdx < project.scenes.size - 1) {
                        currentSceneIndex.value = currentIdx + 1
                        sceneProgress.value = 0f
                        updateOverallProgress(project)
                        speakCurrentScene()
                    } else {
                        // End of video reached
                        sceneProgress.value = 1f
                        overallProgress.value = 1f
                        pausePlayback()
                        break
                    }
                } else {
                    sceneProgress.value = newProg
                    updateOverallProgress(project)
                }
            }
        }
    }

    private fun pausePlayback() {
        isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        speechManager.stop()
        speechManager.stopAmbientSound()
    }

    private fun stopPlayback() {
        isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        speechManager.stop()
        speechManager.stopAmbientSound()
        sceneProgress.value = 0f
        overallProgress.value = 0f
    }

    private fun speakCurrentScene() {
        if (!isVoiceEnabled.value) return
        val project = activeProject.value ?: return
        val scene = project.scenes.getOrNull(currentSceneIndex.value) ?: return
        if (scene.narration.isNotBlank()) {
            speechManager.speakScene(
                text = scene.narration,
                profile = project.voiceProfile,
                utteranceId = "scene_${scene.sceneNumber}"
            )
        }
    }

    private fun updateOverallProgress(project: GeneratedVideoProject) {
        val totalScenes = project.scenes.size.coerceAtLeast(1)
        val completedScenes = currentSceneIndex.value.toFloat()
        val currentContribution = sceneProgress.value / totalScenes
        overallProgress.value = ((completedScenes / totalScenes) + currentContribution).coerceIn(0f, 1f)
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        speechManager.release()
    }
}
