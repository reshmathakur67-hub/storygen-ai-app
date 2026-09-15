package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.export.VideoExporter
import com.example.models.AspectRatio
import com.example.models.CameraMovement
import com.example.models.GeneratedVideoProject
import com.example.models.SceneGraphicType
import com.example.models.VideoScene
import com.example.models.VisualArtStyle
import com.example.models.VoiceProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VideoExportTest {

    @Test
    fun `test video export creates file and public media storage entry`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val testScenes = listOf(
            VideoScene(
                sceneNumber = 1,
                title = "Neon Awakening",
                narration = "A lone warrior gazes at the neon horizon.",
                visualDescription = "Cyberpunk street with rain reflections",
                cameraMovement = CameraMovement.ZOOM_IN,
                durationMs = 2000L,
                graphicType = SceneGraphicType.CITYSCAPE,
                primaryColorHex = "#121420",
                secondaryColorHex = "#2D1E40",
                accentColorHex = "#00F5D4"
            ),
            VideoScene(
                sceneNumber = 2,
                title = "Cosmic Dawn",
                narration = "The cybernetic dawn begins to awaken.",
                visualDescription = "Space nebula and orbiting planet",
                cameraMovement = CameraMovement.PAN_RIGHT,
                durationMs = 2000L,
                graphicType = SceneGraphicType.SPACE,
                primaryColorHex = "#0D0C1D",
                secondaryColorHex = "#4717F6",
                accentColorHex = "#F72585"
            )
        )

        val project = GeneratedVideoProject(
            title = "Cyber Awakening",
            originalScript = "Cyber warrior story",
            characterName = "Arjun",
            characterRole = "Cybernetic Warrior",
            aspectRatio = AspectRatio.RATIO_9_16,
            durationSec = 5,
            style = VisualArtStyle.CYBERPUNK,
            voiceProfile = VoiceProfile.DRAMATIC,
            scenes = testScenes
        )

        var progressReported = false
        val result = VideoExporter.exportVideoToPublicStorage(
            context = context,
            project = project,
            saveToDownloads = false,
            onProgress = { prog, status ->
                if (prog > 0f) progressReported = true
            }
        )

        assertNotNull("Exported result should not be null", result)
        assertTrue("Output file must exist on disk", result.file.exists())
        assertTrue("Output file size should be greater than 0 bytes", result.file.length() > 0)
        assertTrue("Export progress callback must be triggered", progressReported)
        assertTrue("Destination folder should mention Movies", result.destinationFolder.contains("Movies"))
    }
}
