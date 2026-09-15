package com.example.models

import androidx.compose.ui.graphics.Color

enum class AspectRatio(val key: String, val label: String, val ratioFloat: Float, val iconDescription: String) {
    RATIO_9_16("9:16", "9:16 Reel / Story", 9f / 16f, "Vertical Reel (Shorts, TikTok, Insta)"),
    RATIO_16_9("16:9", "16:9 Cinema / YT", 16f / 9f, "Widescreen Cinema (YouTube, Film)");

    companion object {
        fun fromKey(key: String): AspectRatio {
            return entries.find { it.key == key } ?: RATIO_9_16
        }
    }
}

enum class VideoDuration(val seconds: Int, val sceneCount: Int, val label: String) {
    SEC_5(5, 2, "5s • Quick Hook"),
    SEC_10(10, 3, "10s • Story Reel"),
    SEC_30(30, 6, "30s • Full Short");

    companion object {
        fun fromSeconds(sec: Int): VideoDuration {
            return entries.find { it.seconds == sec } ?: SEC_10
        }
    }
}

enum class VisualArtStyle(
    val key: String,
    val title: String,
    val subtitle: String,
    val primaryColor: Long,
    val secondaryColor: Long,
    val accentColor: Long,
    val lightingDesc: String
) {
    CINEMATIC(
        key = "cinematic",
        title = "Cinematic 8K",
        subtitle = "Photorealistic, dramatic 35mm lighting & anamorphic flare",
        primaryColor = 0xFF121420,
        secondaryColor = 0xFF2D1E40,
        accentColor = 0xFFE0A96D,
        lightingDesc = "Warm rim light with atmospheric haze and anamorphic blue streaks"
    ),
    ANIME(
        key = "anime",
        title = "Anime / Ghibli",
        subtitle = "Luminous cel-shading, vibrant skies, hand-drawn detailing",
        primaryColor = 0xFF1D3557,
        secondaryColor = 0xFF457B9D,
        accentColor = 0xFFE63946,
        lightingDesc = "Soft pastel sunlight with floating sakura petals and clean lines"
    ),
    DISNEY_3D(
        key = "disney_3d",
        title = "3D Pixar Animation",
        subtitle = "Cute stylized character geometry, rich subsurface scattering",
        primaryColor = 0xFF3F37C9,
        secondaryColor = 0xFF4CC9F0,
        accentColor = 0xFFF72585,
        lightingDesc = "Warm whimsical key lights with glossy highlights and playful depth"
    ),
    CYBERPUNK(
        key = "cyberpunk",
        title = "Cyberpunk Neon",
        subtitle = "Rain-slicked asphalt, holographic billboards & laser trails",
        primaryColor = 0xFF0B091A,
        secondaryColor = 0xFF7209B7,
        accentColor = 0xFF00F5D4,
        lightingDesc = "High-contrast neon pink and electric cyan reflections in rain"
    ),
    FANTASY(
        key = "fantasy",
        title = "Mythic Watercolor",
        subtitle = "Dreamlike ethereal gradients, enchanted glow & magic motes",
        primaryColor = 0xFF1B4332,
        secondaryColor = 0xFF40916C,
        accentColor = 0xFFD8BBFF,
        lightingDesc = "Mystical golden hour sunbeams filtering through ancient ruins"
    ),
    RETRO_COMIC(
        key = "retro_comic",
        title = "Vintage Comic Book",
        subtitle = "Halftone dots, bold black inks, dynamic action frames",
        primaryColor = 0xFFD90429,
        secondaryColor = 0xFFEF233C,
        accentColor = 0xFFFFD166,
        lightingDesc = "Pop-art high contrast black shadows and punchy primary hues"
    ),
    DARK_NOIR(
        key = "dark_noir",
        title = "Moody Dark Noir",
        subtitle = "Venetian blind shadows, deep monochrome mystery",
        primaryColor = 0xFF111111,
        secondaryColor = 0xFF2B2D42,
        accentColor = 0xFF8D99AE,
        lightingDesc = "High-contrast hard shadows, silhouetted shapes, smoke trails"
    ),
    CLAYMATION(
        key = "claymation",
        title = "Claymation Stop-Motion",
        subtitle = "Handcrafted clay miniatures, textured tactile whimsy",
        primaryColor = 0xFF9C6644,
        secondaryColor = 0xFFDDB892,
        accentColor = 0xFF7F4F24,
        lightingDesc = "Soft studio desk lamp casting tactile shadows on miniature props"
    );

    companion object {
        fun fromKey(key: String): VisualArtStyle {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: CINEMATIC
        }
    }
}

enum class VoiceProfile(
    val key: String,
    val title: String,
    val description: String,
    val pitch: Float,
    val speechRate: Float
) {
    DRAMATIC("dramatic", "Deep & Dramatic", "Deep, resonant cinematic movie trailer voice", 0.78f, 0.90f),
    STORYTELLER("storyteller", "Epic Storyteller", "Warm, engaging, expressive narrative cadence", 1.0f, 0.95f),
    ENERGETIC("energetic", "Fast & Punchy", "High-energy, dynamic modern Reel creator tone", 1.15f, 1.18f),
    CALM("calm", "Calm & Mystical", "Soothing, gentle, whispering atmospheric narration", 0.88f, 0.85f),
    HEROIC("heroic", "Bold & Heroic", "Inspiring, powerful champion voice with great punch", 1.05f, 1.02f);

    companion object {
        fun fromKey(key: String): VoiceProfile {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: STORYTELLER
        }
    }
}

enum class CameraMovement(val label: String, val description: String) {
    ZOOM_IN("Zoom In", "Slow dramatic forward push into the subject"),
    ZOOM_OUT("Zoom Out", "Wide expansive pull back revealing scale"),
    PAN_LEFT("Pan Left", "Smooth cinematic slide towards the left"),
    PAN_RIGHT("Pan Right", "Dynamic sweeping motion towards the right"),
    TILT_UP("Tilt Up", "Upward heroic perspective shift"),
    PULSE("Dynamic Orbit", "Subtle breathing pulse focusing on character");

    companion object {
        fun fromString(str: String): CameraMovement {
            return entries.find { it.name.equals(str, ignoreCase = true) } ?: ZOOM_IN
        }
    }
}

enum class SceneGraphicType {
    CITYSCAPE,
    LANDSCAPE,
    SPACE,
    PORTRAIT,
    INTERIOR,
    FANTASY_REALM,
    MYSTERY
}

data class VideoScene(
    val sceneNumber: Int,
    val title: String,
    val narration: String,
    val visualDescription: String,
    val cameraMovement: CameraMovement,
    val durationMs: Long,
    val graphicType: SceneGraphicType,
    val primaryColorHex: String,
    val secondaryColorHex: String,
    val accentColorHex: String
)

data class CharacterPreset(
    val name: String,
    val role: String,
    val description: String,
    val emoji: String
)

data class ScriptTemplate(
    val title: String,
    val category: String,
    val characterName: String,
    val characterRole: String,
    val script: String,
    val recommendedStyle: VisualArtStyle,
    val recommendedRatio: AspectRatio,
    val recommendedDuration: VideoDuration
)

data class GeneratedVideoProject(
    val id: Long = 0,
    val title: String,
    val originalScript: String,
    val characterName: String,
    val characterRole: String,
    val aspectRatio: AspectRatio,
    val durationSec: Int,
    val style: VisualArtStyle,
    val voiceProfile: VoiceProfile,
    val scenes: List<VideoScene>,
    val createdAt: Long = System.currentTimeMillis()
)
