package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.models.AspectRatio
import com.example.models.CameraMovement
import com.example.models.SceneGraphicType
import com.example.models.VideoDuration
import com.example.models.VideoScene
import com.example.models.VisualArtStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiStoryService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateStoryboard(
        script: String,
        characterName: String,
        characterRole: String,
        style: VisualArtStyle,
        ratio: AspectRatio,
        duration: VideoDuration
    ): List<VideoScene> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (!apiKey.isNullOrBlank() && !apiKey.contains("MY_GEMINI_API_KEY")) {
            try {
                val scenesFromApi = requestGeminiStoryboard(
                    apiKey = apiKey,
                    script = script,
                    characterName = characterName,
                    characterRole = characterRole,
                    style = style,
                    ratio = ratio,
                    duration = duration
                )
                if (scenesFromApi.isNotEmpty()) {
                    return@withContext scenesFromApi
                }
            } catch (e: Exception) {
                Log.w("GeminiStoryService", "Gemini API call failed, using intelligent offline generator: ${e.message}")
            }
        }

        // Offline storyboard generation
        generateOfflineStoryboard(script, characterName, characterRole, style, duration)
    }

    suspend fun enhanceScript(
        ideaPrompt: String,
        characterName: String,
        style: VisualArtStyle
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (!apiKey.isNullOrBlank() && !apiKey.contains("MY_GEMINI_API_KEY")) {
            try {
                val enhanced = requestGeminiScriptEnhance(apiKey, ideaPrompt, characterName, style)
                if (enhanced.isNotBlank()) {
                    return@withContext enhanced
                }
            } catch (e: Exception) {
                Log.w("GeminiStoryService", "Gemini script enhance failed: ${e.message}")
            }
        }

        // Offline fallback enhancement
        generateOfflineEnhancedScript(ideaPrompt, characterName, style)
    }

    private fun requestGeminiStoryboard(
        apiKey: String,
        script: String,
        characterName: String,
        characterRole: String,
        style: VisualArtStyle,
        ratio: AspectRatio,
        duration: VideoDuration
    ): List<VideoScene> {
        val targetScenes = duration.sceneCount
        val sceneDurationMs = (duration.seconds * 1000L) / targetScenes

        val promptText = """
            You are a master cinematic director for an AI Video Generator app.
            Create exactly $targetScenes sequential video scenes for a ${duration.seconds}-second video.
            
            Video Parameters:
            - Original Script/Story: "$script"
            - Featured Character: "$characterName ($characterRole)"
            - Visual Style: "${style.title}" (${style.subtitle})
            - Framing Aspect Ratio: "${ratio.key}"
            - Target Duration: ${duration.seconds} seconds total ($sceneDurationMs ms per scene)
            
            Return ONLY a valid JSON array of objects with the exact fields below:
            [
              {
                "sceneNumber": 1,
                "title": "Scene title",
                "narration": "Exact spoken voiceover narration in the language of the script",
                "visualDescription": "High-fidelity visual prompt describing the camera shot, lighting, and action in style of ${style.title}",
                "cameraMovement": "ZOOM_IN" (one of: ZOOM_IN, ZOOM_OUT, PAN_LEFT, PAN_RIGHT, TILT_UP, PULSE),
                "graphicType": "CITYSCAPE" (one of: CITYSCAPE, LANDSCAPE, SPACE, PORTRAIT, INTERIOR, FANTASY_REALM, MYSTERY)
              }
            ]
        """.trimIndent()

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", promptText) })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                val respFormat = JSONObject().apply {
                    val textFormat = JSONObject().apply {
                        put("mimeType", "application/json")
                    }
                    put("text", textFormat)
                }
                put("responseFormat", respFormat)
            }
            put("generationConfig", genConfig)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val body = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(body).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        }

        val respString = response.body?.string() ?: throw Exception("Empty response")
        val jsonRoot = JSONObject(respString)
        val candidates = jsonRoot.optJSONArray("candidates")
        val content = candidates?.optJSONObject(0)?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text") ?: ""

        val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val jsonArray = JSONArray(cleanJson)

        val result = mutableListOf<VideoScene>()
        val primaryHex = String.format("#%06X", 0xFFFFFF and style.primaryColor.toInt())
        val secondaryHex = String.format("#%06X", 0xFFFFFF and style.secondaryColor.toInt())
        val accentHex = String.format("#%06X", 0xFFFFFF and style.accentColor.toInt())

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            result.add(
                VideoScene(
                    sceneNumber = obj.optInt("sceneNumber", i + 1),
                    title = obj.optString("title", "Scene ${i + 1}"),
                    narration = obj.optString("narration", ""),
                    visualDescription = obj.optString("visualDescription", ""),
                    cameraMovement = CameraMovement.fromString(obj.optString("cameraMovement", "ZOOM_IN")),
                    durationMs = sceneDurationMs,
                    graphicType = try {
                        SceneGraphicType.valueOf(obj.optString("graphicType", "LANDSCAPE"))
                    } catch (e: Exception) {
                        SceneGraphicType.LANDSCAPE
                    },
                    primaryColorHex = primaryHex,
                    secondaryColorHex = secondaryHex,
                    accentColorHex = accentHex
                )
            )
        }
        return result
    }

    private fun requestGeminiScriptEnhance(
        apiKey: String,
        ideaPrompt: String,
        characterName: String,
        style: VisualArtStyle
    ): String {
        val promptText = """
            You are a creative screenplay writer for viral cinematic short videos and reels.
            Convert the following brief prompt or story idea into an engaging, dramatic 2-to-3 sentence script
            featuring the character "$characterName" in a "${style.title}" artistic vibe.
            
            Story Idea: "$ideaPrompt"
            
            Keep the exact language (e.g. Hindi, English, or Hinglish) as provided by the user.
            Output ONLY the expanded script text, with no introductory text or quotes.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", promptText) })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val body = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(body).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return ""

        val respString = response.body?.string() ?: return ""
        val jsonRoot = JSONObject(respString)
        val candidates = jsonRoot.optJSONArray("candidates")
        val content = candidates?.optJSONObject(0)?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        return parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""
    }

    private fun generateOfflineStoryboard(
        script: String,
        characterName: String,
        characterRole: String,
        style: VisualArtStyle,
        duration: VideoDuration
    ): List<VideoScene> {
        val targetScenes = duration.sceneCount
        val sceneDurationMs = (duration.seconds * 1000L) / targetScenes

        // Split sentences cleanly
        val rawSentences = script
            .split(Regex("[.?!|।\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val cameraMoves = listOf(
            CameraMovement.ZOOM_IN,
            CameraMovement.PAN_RIGHT,
            CameraMovement.TILT_UP,
            CameraMovement.ZOOM_OUT,
            CameraMovement.PAN_LEFT,
            CameraMovement.PULSE
        )

        val graphicTypes = when (style) {
            VisualArtStyle.CYBERPUNK -> listOf(SceneGraphicType.CITYSCAPE, SceneGraphicType.PORTRAIT, SceneGraphicType.MYSTERY)
            VisualArtStyle.FANTASY -> listOf(SceneGraphicType.FANTASY_REALM, SceneGraphicType.LANDSCAPE, SceneGraphicType.PORTRAIT)
            VisualArtStyle.DARK_NOIR -> listOf(SceneGraphicType.MYSTERY, SceneGraphicType.CITYSCAPE, SceneGraphicType.PORTRAIT)
            VisualArtStyle.ANIME -> listOf(SceneGraphicType.LANDSCAPE, SceneGraphicType.PORTRAIT, SceneGraphicType.FANTASY_REALM)
            VisualArtStyle.DISNEY_3D -> listOf(SceneGraphicType.SPACE, SceneGraphicType.LANDSCAPE, SceneGraphicType.PORTRAIT)
            else -> listOf(SceneGraphicType.PORTRAIT, SceneGraphicType.LANDSCAPE, SceneGraphicType.CITYSCAPE)
        }

        val primaryHex = String.format("#%06X", 0xFFFFFF and style.primaryColor.toInt())
        val secondaryHex = String.format("#%06X", 0xFFFFFF and style.secondaryColor.toInt())
        val accentHex = String.format("#%06X", 0xFFFFFF and style.accentColor.toInt())

        val scenes = mutableListOf<VideoScene>()

        for (i in 0 until targetScenes) {
            val sentence = if (rawSentences.isNotEmpty()) {
                if (i < rawSentences.size) {
                    rawSentences[i]
                } else {
                    // Summarize or conclude
                    "${characterName} takes a decisive step forward as destiny unfolds."
                }
            } else {
                "${characterName} steps into the radiant scene."
            }

            val sceneTitle = when (i) {
                0 -> "Opening Hook: Encounter"
                1 -> "Rising Action: The Conflict"
                2 -> "Climax: Power Revealed"
                3 -> "Turning Point: Discovery"
                4 -> "Resolution: The Final Horizon"
                else -> "Scene ${i + 1}"
            }

            val visualPrompt = "${style.title} composition featuring $characterName as $characterRole. " +
                    "Intense cinematic focus, ${style.lightingDesc}. Highly detailed background."

            scenes.add(
                VideoScene(
                    sceneNumber = i + 1,
                    title = sceneTitle,
                    narration = sentence,
                    visualDescription = visualPrompt,
                    cameraMovement = cameraMoves[i % cameraMoves.size],
                    durationMs = sceneDurationMs,
                    graphicType = graphicTypes[i % graphicTypes.size],
                    primaryColorHex = primaryHex,
                    secondaryColorHex = secondaryHex,
                    accentColorHex = accentHex
                )
            )
        }

        return scenes
    }

    private fun generateOfflineEnhancedScript(
        ideaPrompt: String,
        characterName: String,
        style: VisualArtStyle
    ): String {
        val isHindi = ideaPrompt.any { it in '\u0900'..'\u097F' }
        return if (isHindi) {
            "जब $characterName ने कदम आगे बढ़ाया, तो पूरी दुनिया थम गई। $ideaPrompt का यह पल हमेशा के लिए इतिहास में दर्ज होने वाला था। सच्चाई अब सामने आने ही वाली थी।"
        } else {
            "As $characterName ventured deeper into the unknown, every heartbeat counted. With $ideaPrompt unfolding in radiant light, the ultimate legend was about to be forged."
        }
    }
}
