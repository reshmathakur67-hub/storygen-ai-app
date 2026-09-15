package com.example.data.local

import com.example.models.AspectRatio
import com.example.models.CameraMovement
import com.example.models.GeneratedVideoProject
import com.example.models.SceneGraphicType
import com.example.models.VideoDuration
import com.example.models.VideoScene
import com.example.models.VisualArtStyle
import com.example.models.VoiceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class VideoProjectRepository(private val dao: VideoProjectDao) {

    val allProjects: Flow<List<GeneratedVideoProject>> = dao.getAllProjects().map { entities ->
        entities.map { entity -> toModel(entity) }
    }

    suspend fun getProjectById(id: Long): GeneratedVideoProject? {
        val entity = dao.getProjectById(id) ?: return null
        return toModel(entity)
    }

    suspend fun saveProject(project: GeneratedVideoProject): Long {
        val entity = toEntity(project)
        return dao.insertProject(entity)
    }

    suspend fun deleteProject(id: Long) {
        dao.deleteProjectById(id)
    }

    private fun toModel(entity: VideoProjectEntity): GeneratedVideoProject {
        val scenes = parseScenesJson(entity.scenesJson)
        return GeneratedVideoProject(
            id = entity.id,
            title = entity.title,
            originalScript = entity.originalScript,
            characterName = entity.characterName,
            characterRole = entity.characterRole,
            aspectRatio = AspectRatio.fromKey(entity.aspectRatio),
            durationSec = entity.durationSec,
            style = VisualArtStyle.fromKey(entity.styleKey),
            voiceProfile = VoiceProfile.fromKey(entity.voiceProfileKey),
            scenes = scenes,
            createdAt = entity.createdAt
        )
    }

    private fun toEntity(model: GeneratedVideoProject): VideoProjectEntity {
        val jsonArray = JSONArray()
        model.scenes.forEach { scene ->
            val obj = JSONObject().apply {
                put("sceneNumber", scene.sceneNumber)
                put("title", scene.title)
                put("narration", scene.narration)
                put("visualDescription", scene.visualDescription)
                put("cameraMovement", scene.cameraMovement.name)
                put("durationMs", scene.durationMs)
                put("graphicType", scene.graphicType.name)
                put("primaryColorHex", scene.primaryColorHex)
                put("secondaryColorHex", scene.secondaryColorHex)
                put("accentColorHex", scene.accentColorHex)
            }
            jsonArray.put(obj)
        }

        return VideoProjectEntity(
            id = model.id,
            title = model.title,
            originalScript = model.originalScript,
            characterName = model.characterName,
            characterRole = model.characterRole,
            aspectRatio = model.aspectRatio.key,
            durationSec = model.durationSec,
            styleKey = model.style.key,
            voiceProfileKey = model.voiceProfile.key,
            scenesJson = jsonArray.toString(),
            createdAt = model.createdAt
        )
    }

    private fun parseScenesJson(jsonStr: String): List<VideoScene> {
        val result = mutableListOf<VideoScene>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    VideoScene(
                        sceneNumber = obj.optInt("sceneNumber", i + 1),
                        title = obj.optString("title", "Scene ${i + 1}"),
                        narration = obj.optString("narration", ""),
                        visualDescription = obj.optString("visualDescription", ""),
                        cameraMovement = CameraMovement.fromString(obj.optString("cameraMovement", "ZOOM_IN")),
                        durationMs = obj.optLong("durationMs", 3000L),
                        graphicType = try {
                            SceneGraphicType.valueOf(obj.optString("graphicType", "LANDSCAPE"))
                        } catch (e: Exception) {
                            SceneGraphicType.LANDSCAPE
                        },
                        primaryColorHex = obj.optString("primaryColorHex", "#121420"),
                        secondaryColorHex = obj.optString("secondaryColorHex", "#2D1E40"),
                        accentColorHex = obj.optString("accentColorHex", "#E0A96D")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return result
    }
}
