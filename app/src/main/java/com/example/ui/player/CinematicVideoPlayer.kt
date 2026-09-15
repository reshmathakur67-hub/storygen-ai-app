package com.example.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.AspectRatio
import com.example.models.CameraMovement
import com.example.models.GeneratedVideoProject
import com.example.models.SceneGraphicType
import com.example.models.VideoScene
import com.example.models.VisualArtStyle
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CinematicVideoPlayer(
    project: GeneratedVideoProject,
    currentSceneIndex: Int,
    sceneProgress: Float, // 0f..1f for the current scene
    overallProgress: Float, // 0f..1f total
    isPlaying: Boolean,
    isAmbientAudioEnabled: Boolean,
    isVoiceEnabled: Boolean,
    isFullscreen: Boolean,
    onTogglePlayPause: () -> Unit,
    onReplay: () -> Unit,
    onNextScene: () -> Unit,
    onPrevScene: () -> Unit,
    onToggleAmbientAudio: () -> Unit,
    onToggleVoice: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleAspectRatio: () -> Unit,
    onExportVideo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scenes = project.scenes
    val activeScene = scenes.getOrNull(currentSceneIndex) ?: scenes.firstOrNull()

    val infiniteTransition = rememberInfiniteTransition(label = "player_particles")
    val particleAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0A0912))
            .border(1.dp, Color(0xFF26243A), RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Player Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color(0xFF00F5D4) else Color(0xFFFF5964))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "PLAYING (${project.aspectRatio.key})" else "PAUSED (${project.aspectRatio.key})",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color(project.style.primaryColor).copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(project.style.accentColor).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = project.style.title,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick aspect ratio switch
                IconButton(
                    onClick = onToggleAspectRatio,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("toggle_ratio_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Switch Aspect Ratio",
                        tint = Color(0xFF00F5D4),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Ambient Audio toggle
                IconButton(
                    onClick = onToggleAmbientAudio,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("toggle_ambient_btn")
                ) {
                    Icon(
                        imageVector = if (isAmbientAudioEnabled) Icons.Default.Audiotrack else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Ambient Score",
                        tint = if (isAmbientAudioEnabled) Color(0xFFFFD166) else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Voice narration toggle
                IconButton(
                    onClick = onToggleVoice,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("toggle_voice_btn")
                ) {
                    Icon(
                        imageVector = if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Voice Narration",
                        tint = if (isVoiceEnabled) Color(0xFFF72585) else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Fullscreen toggle
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("fullscreen_btn")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Export Video to Device Storage
                IconButton(
                    onClick = onExportVideo,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("player_export_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export Video to Public Storage",
                        tint = Color(0xFF00F5D4),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Video Viewport Canvas with Dynamic Aspect Ratio
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val ratio = project.aspectRatio.ratioFloat

            // Calculate height or constraint so it stays comfortably within phone viewport
            val videoBoxModifier = if (project.aspectRatio == AspectRatio.RATIO_9_16) {
                // Vertical reel: constrain height so it doesn't overflow screen
                val maxHeight = if (isFullscreen) maxHeight else 420.dp
                Modifier
                    .height(maxHeight)
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(12.dp))
            } else {
                // 16:9 widescreen
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(12.dp))
            }

            Box(
                modifier = videoBoxModifier
                    .background(Color(0xFF030308))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTogglePlayPause
                    )
                    .testTag("video_viewport")
            ) {
                if (activeScene != null) {
                    // Render Dynamic Scene with Ken Burns Camera Motion
                    SceneMotionCanvas(
                        scene = activeScene,
                        style = project.style,
                        cameraMovement = activeScene.cameraMovement,
                        progress = sceneProgress,
                        particlePhase = particleAnim,
                        glowPulse = glowPulse,
                        characterName = project.characterName,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Scene Badge Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "Scene ${activeScene.sceneNumber}/${scenes.size} • ${activeScene.cameraMovement.label}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Character Tag
                        Surface(
                            color = Color(0xFF7B2CBF).copy(alpha = 0.75f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF00F5D4).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${project.characterName} (${project.characterRole})",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Play indicator overlay when paused
                    if (!isPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF00F5D4).copy(alpha = 0.9f),
                                modifier = Modifier.size(54.dp),
                                shadowElevation = 8.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Karaoke Subtitle Card Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        KaraokeSubtitleOverlay(
                            narration = activeScene.narration,
                            progress = sceneProgress,
                            accentColor = Color(project.style.accentColor)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scene-segmented Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            scenes.forEachIndexed { index, scene ->
                val segmentProgress = when {
                    index < currentSceneIndex -> 1f
                    index == currentSceneIndex -> sceneProgress
                    else -> 0f
                }
                LinearProgressIndicator(
                    progress = { segmentProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF00F5D4),
                    trackColor = Color(0xFF232238)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time Counter & Playback Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time elapsed
            val elapsedSec = (overallProgress * project.durationSec).toInt()
            Text(
                text = String.format("%02d:%02d / %02d:%02d", elapsedSec / 60, elapsedSec % 60, project.durationSec / 60, project.durationSec % 60),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            // Center Control Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Replay
                IconButton(
                    onClick = onReplay,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("replay_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Prev Scene
                IconButton(
                    onClick = onPrevScene,
                    enabled = currentSceneIndex > 0,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("prev_scene_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Scene",
                        tint = if (currentSceneIndex > 0) Color.White else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Play / Pause main button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00F5D4),
                    modifier = Modifier
                        .size(46.dp)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("play_pause_btn"),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Next Scene
                IconButton(
                    onClick = onNextScene,
                    enabled = currentSceneIndex < scenes.size - 1,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("next_scene_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Scene",
                        tint = if (currentSceneIndex < scenes.size - 1) Color.White else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Style Indicator
            Text(
                text = "${project.durationSec}s • ${project.scenes.size} cuts",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Export / Save Action Bar
        Surface(
            onClick = onExportVideo,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .testTag("player_export_bar_btn"),
            color = Color(0xFF19172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F5D4).copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color(0xFF00F5D4),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export & Save to Device (MP4)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Movies/AIVideoStudio",
                    color = Color(0xFF00F5D4).copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Animated Canvas drawing the scene elements with Ken Burns pan/zoom/tilt camera moves.
 */
@Composable
private fun SceneMotionCanvas(
    scene: VideoScene,
    style: VisualArtStyle,
    cameraMovement: CameraMovement,
    progress: Float,
    particlePhase: Float,
    glowPulse: Float,
    characterName: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Calculate Ken Burns camera transformation
        var scaleFactor = 1.0f
        var translateX = 0f
        var translateY = 0f

        when (cameraMovement) {
            CameraMovement.ZOOM_IN -> {
                scaleFactor = 1.0f + (progress * 0.28f)
            }
            CameraMovement.ZOOM_OUT -> {
                scaleFactor = 1.28f - (progress * 0.28f)
            }
            CameraMovement.PAN_LEFT -> {
                scaleFactor = 1.15f
                translateX = (1f - progress) * 40.dp.toPx() - 20.dp.toPx()
            }
            CameraMovement.PAN_RIGHT -> {
                scaleFactor = 1.15f
                translateX = (progress - 0.5f) * 60.dp.toPx()
            }
            CameraMovement.TILT_UP -> {
                scaleFactor = 1.12f
                translateY = (0.5f - progress) * 50.dp.toPx()
            }
            CameraMovement.PULSE -> {
                scaleFactor = 1.05f + (sin(progress * Math.PI.toFloat()) * 0.12f)
            }
        }

        scale(scaleFactor, pivot = Offset(w / 2f, h / 2f)) {
            translate(left = translateX, top = translateY) {

                // 1. Scene Background Gradient Horizon
                val primaryColor = Color(style.primaryColor)
                val secondaryColor = Color(style.secondaryColor)
                val accentColor = Color(style.accentColor)

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.95f),
                            secondaryColor.copy(alpha = 0.85f),
                            Color(0xFF07060D)
                        ),
                        startY = 0f,
                        endY = h
                    ),
                    size = size
                )

                // 2. Scenic Composition depending on GraphicType
                when (scene.graphicType) {
                    SceneGraphicType.CITYSCAPE -> {
                        drawCityscape(w, h, accentColor, glowPulse)
                    }
                    SceneGraphicType.SPACE -> {
                        drawCosmicSpace(w, h, accentColor, glowPulse, particlePhase)
                    }
                    SceneGraphicType.FANTASY_REALM -> {
                        drawFantasyRealm(w, h, accentColor, glowPulse)
                    }
                    SceneGraphicType.PORTRAIT -> {
                        drawDramaticPortrait(w, h, accentColor, glowPulse)
                    }
                    SceneGraphicType.MYSTERY -> {
                        drawMysteryFog(w, h, accentColor, glowPulse)
                    }
                    else -> {
                        drawAtmosphericLandscape(w, h, accentColor, glowPulse)
                    }
                }

                // 3. Central Stylized Character Silhouette & Aura
                drawCharacterAura(w, h, accentColor, glowPulse, style)

                // 4. Floating Ambient Particles & Speed Lines
                drawFloatingParticles(w, h, accentColor, particlePhase)

                // 5. Cinematic Anamorphic Lens Flare Line
                val flareY = h * 0.38f
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = 0.35f * glowPulse),
                            Color.White.copy(alpha = 0.8f * glowPulse),
                            accentColor.copy(alpha = 0.35f * glowPulse),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, flareY),
                    end = Offset(w, flareY),
                    strokeWidth = 1.5.dp.toPx()
                )

                // 6. Cinematic Vignette (darkened edges)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        center = Offset(w / 2f, h / 2f),
                        radius = (w.coerceAtLeast(h)) * 0.7f
                    ),
                    size = size
                )
            }
        }
    }
}

// Drawing helpers for visual scenes
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCityscape(
    w: Float,
    h: Float,
    accentColor: Color,
    glowPulse: Float
) {
    // Glowing moon or billboard
    drawCircle(
        color = accentColor.copy(alpha = 0.22f * glowPulse),
        radius = w * 0.28f,
        center = Offset(w * 0.75f, h * 0.28f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = w * 0.12f,
        center = Offset(w * 0.75f, h * 0.28f)
    )

    // Silhouetted skyscrapers
    val buildings = listOf(
        Pair(0.05f, 0.42f),
        Pair(0.20f, 0.52f),
        Pair(0.38f, 0.36f),
        Pair(0.55f, 0.48f),
        Pair(0.72f, 0.40f),
        Pair(0.88f, 0.45f)
    )
    buildings.forEachIndexed { i, (xRatio, heightRatio) ->
        val bW = w * 0.16f
        val bH = h * heightRatio
        val bX = w * xRatio
        val bY = h - bH

        drawRect(
            color = Color(0xFF0F0E17),
            topLeft = Offset(bX, bY),
            size = Size(bW, bH)
        )

        // Windows
        val windowCols = 2
        val windowRows = 5
        for (r in 0 until windowRows) {
            for (c in 0 until windowCols) {
                if ((r + c + i) % 2 == 0) {
                    drawRect(
                        color = if ((r + i) % 3 == 0) accentColor.copy(alpha = 0.75f) else Color(0xFFFFD166).copy(alpha = 0.6f),
                        topLeft = Offset(bX + 4.dp.toPx() + (c * 8.dp.toPx()), bY + 8.dp.toPx() + (r * 14.dp.toPx())),
                        size = Size(4.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCosmicSpace(
    w: Float,
    h: Float,
    accentColor: Color,
    glowPulse: Float,
    particlePhase: Float
) {
    // Distant Nebula
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.45f * glowPulse), Color.Transparent),
            center = Offset(w * 0.4f, h * 0.35f),
            radius = w * 0.45f
        ),
        radius = w * 0.45f,
        center = Offset(w * 0.4f, h * 0.35f)
    )

    // Giant Ringed Planet
    val planetCenter = Offset(w * 0.5f, h * 0.32f)
    val planetRadius = w * 0.22f

    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF8338EC), Color(0xFF3A86FF), Color(0xFF0B091A))
        ),
        radius = planetRadius,
        center = planetCenter
    )

    // Planet Rings
    rotate(25f, pivot = planetCenter) {
        drawOval(
            color = accentColor.copy(alpha = 0.6f),
            topLeft = Offset(planetCenter.x - (planetRadius * 1.8f), planetCenter.y - (planetRadius * 0.35f)),
            size = Size(planetRadius * 3.6f, planetRadius * 0.7f),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFantasyRealm(
    w: Float,
    h: Float,
    accentColor: Color,
    glowPulse: Float
) {
    // Ethereal Sun/Moon behind mountain peak
    drawCircle(
        color = Color(0xFFFFE3A8).copy(alpha = 0.8f),
        radius = w * 0.16f,
        center = Offset(w * 0.5f, h * 0.35f)
    )

    // Mountain peaks
    val path = Path().apply {
        moveTo(0f, h * 0.65f)
        lineTo(w * 0.25f, h * 0.42f)
        lineTo(w * 0.5f, h * 0.52f)
        lineTo(w * 0.75f, h * 0.38f)
        lineTo(w, h * 0.60f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF241442), Color(0xFF0D081D)),
            startY = h * 0.38f,
            endY = h
        )
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDramaticPortrait(
    w: Float,
    h: Float,
    accentColor: Color,
    glowPulse: Float
) {
    // Halo light circle
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.38f * glowPulse), Color.Transparent),
            center = Offset(w / 2f, h * 0.45f),
            radius = w * 0.45f
        ),
        radius = w * 0.45f,
        center = Offset(w / 2f, h * 0.45f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMysteryFog(
    w: Float,
    h: Float,
    accentColor: Color,
    glowPulse: Float
) {
    // Street lamp cone
    val lampPath = Path().apply {
        moveTo(w * 0.78f, h * 0.2f)
        lineTo(w * 0.55f, h * 0.85f)
        lineTo(w * 0.95f, h * 0.85f)
        close()
    }
    drawPath(
        path = lampPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFD166).copy(alpha = 0.35f * glowPulse), Color.Transparent),
            startY = h * 0.2f,
            endY = h * 0.85f
        )
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAtmosphericLandscape(
    w: Float,
    h: Float,
    accentColor: Color,
    glowPulse: Float
) {
    // Rolling hills
    val hillPath = Path().apply {
        moveTo(0f, h * 0.55f)
        quadraticTo(w * 0.35f, h * 0.48f, w * 0.65f, h * 0.58f)
        quadraticTo(w * 0.85f, h * 0.64f, w, h * 0.56f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = hillPath,
        color = Color(0xFF140D26)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCharacterAura(
    w: Float,
    h: Float,
    accentColor: Color,
    glowPulse: Float,
    style: VisualArtStyle
) {
    val charCenterX = w * 0.5f
    val charCenterY = h * 0.62f

    // Dramatic back-light halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.5f * glowPulse), Color.Transparent),
            center = Offset(charCenterX, charCenterY),
            radius = w * 0.32f
        ),
        radius = w * 0.32f,
        center = Offset(charCenterX, charCenterY)
    )

    // Character Silhouette: Head
    drawCircle(
        color = Color(0xFF090710),
        radius = 16.dp.toPx(),
        center = Offset(charCenterX, charCenterY - 34.dp.toPx())
    )

    // Shoulders & Torso Silhouette
    val bodyPath = Path().apply {
        moveTo(charCenterX - 24.dp.toPx(), charCenterY + 45.dp.toPx())
        lineTo(charCenterX - 18.dp.toPx(), charCenterY - 14.dp.toPx())
        lineTo(charCenterX + 18.dp.toPx(), charCenterY - 14.dp.toPx())
        lineTo(charCenterX + 24.dp.toPx(), charCenterY + 45.dp.toPx())
        close()
    }
    drawPath(path = bodyPath, color = Color(0xFF090710))

    // Glowing Visor / Blade / Eye Accent
    when (style) {
        VisualArtStyle.CYBERPUNK -> {
            // Neon visor
            drawLine(
                color = accentColor,
                start = Offset(charCenterX - 8.dp.toPx(), charCenterY - 34.dp.toPx()),
                end = Offset(charCenterX + 8.dp.toPx(), charCenterY - 34.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Cyber blade
            drawLine(
                color = Color(0xFF00F5D4),
                start = Offset(charCenterX + 20.dp.toPx(), charCenterY + 25.dp.toPx()),
                end = Offset(charCenterX + 35.dp.toPx(), charCenterY - 40.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        VisualArtStyle.ANIME -> {
            // Anime scarf / wind hair trail
            val trailPath = Path().apply {
                moveTo(charCenterX - 8.dp.toPx(), charCenterY - 20.dp.toPx())
                quadraticTo(charCenterX - 35.dp.toPx(), charCenterY - 15.dp.toPx(), charCenterX - 45.dp.toPx(), charCenterY - 5.dp.toPx())
            }
            drawPath(
                path = trailPath,
                color = accentColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        else -> {
            // Stylized shoulder rim lights
            drawLine(
                color = accentColor.copy(alpha = 0.8f),
                start = Offset(charCenterX - 18.dp.toPx(), charCenterY - 14.dp.toPx()),
                end = Offset(charCenterX - 24.dp.toPx(), charCenterY + 12.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = accentColor.copy(alpha = 0.8f),
                start = Offset(charCenterX + 18.dp.toPx(), charCenterY - 14.dp.toPx()),
                end = Offset(charCenterX + 24.dp.toPx(), charCenterY + 12.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFloatingParticles(
    w: Float,
    h: Float,
    accentColor: Color,
    phase: Float
) {
    val count = 16
    for (i in 0 until count) {
        val seed = i.toFloat() / count
        val x = (w * ((seed * 7.3f + phase * 0.4f) % 1f))
        val y = (h * ((seed * 5.1f + phase * 0.7f) % 1f))
        val r = (1.5f + (i % 3)).dp.toPx()
        val alpha = 0.2f + (0.6f * ((sin((phase + seed) * Math.PI.toFloat() * 2f) + 1f) / 2f))

        drawCircle(
            color = if (i % 2 == 0) accentColor.copy(alpha = alpha) else Color.White.copy(alpha = alpha),
            radius = r,
            center = Offset(x, y)
        )
    }
}

/**
 * High-visibility frosted karaoke subtitle overlay with dynamic word highlight.
 */
@Composable
private fun KaraokeSubtitleOverlay(
    narration: String,
    progress: Float,
    accentColor: Color
) {
    Surface(
        color = Color(0xFF0C0A17).copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Words with karaoke highlight
            val words = remember(narration) { narration.split(" ").filter { it.isNotBlank() } }
            val activeWordIndex = (progress * words.size).toInt().coerceIn(0, (words.size - 1).coerceAtLeast(0))

            Text(
                text = narration,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle Progress Line
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = accentColor,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
        }
    }
}
