package com.example.data

import com.example.models.AspectRatio
import com.example.models.CharacterPreset
import com.example.models.ScriptTemplate
import com.example.models.VideoDuration
import com.example.models.VisualArtStyle

object StoryPresets {

    val defaultCharacters = listOf(
        CharacterPreset(
            name = "Arjun",
            role = "Cybernetic Warrior",
            description = "A futuristic guardian with glowing katana and neon visor",
            emoji = "⚡"
        ),
        CharacterPreset(
            name = "Aria",
            role = "Cosmic Explorer",
            description = "An adventurous stargaze pilot navigating uncharted galaxies",
            emoji = "🚀"
        ),
        CharacterPreset(
            name = "Inspector Kabir",
            role = "Shadow Detective",
            description = "A trench-coat investigator solving noir city enigmas",
            emoji = "🕵️‍♂️"
        ),
        CharacterPreset(
            name = "Milo",
            role = "Wise Forest Owl",
            description = "An ancient spirit owl wearing a tiny brass pocket watch",
            emoji = "🦉"
        ),
        CharacterPreset(
            name = "Mira",
            role = "Anime Sorceress",
            description = "Spirited magic prodigy who commands celestial flames",
            emoji = "✨"
        ),
        CharacterPreset(
            name = "Leo",
            role = "Courageous Lion King",
            description = "A golden-maned ruler protecting the emerald savannah",
            emoji = "🦁"
        )
    )

    val dailyTemplates = listOf(
        ScriptTemplate(
            title = "अंधेरी रात का रहस्य (Noir Mystery)",
            category = "Daily Mystery",
            characterName = "Inspector Kabir",
            characterRole = "Detective",
            script = "बारिश से भीगी सड़कें और एक खामोश शहर। हर परछाई में एक राज़ छिपा था। कबीर ने अपनी सिगार जलाई और उस पुरानी हवेली की तरफ बढ़ा जहां से आखिरी घड़ी की टिक-टिक सुनाई दे रही थी। क्या वो सच तक पहुँच पाएगा?",
            recommendedStyle = VisualArtStyle.DARK_NOIR,
            recommendedRatio = AspectRatio.RATIO_9_16,
            recommendedDuration = VideoDuration.SEC_10
        ),
        ScriptTemplate(
            title = "The Cyber Samurai's Dawn",
            category = "Sci-Fi Action",
            characterName = "Arjun",
            characterRole = "Cybernetic Warrior",
            script = "Neo-Tokyo, Year 2099. High above the smog, neon dragons danced across mega-screens. Arjun drew his plasma blade as the rogue AI sentinels surrounded the rooftop. With one swift breath, the sparks ignited the night.",
            recommendedStyle = VisualArtStyle.CYBERPUNK,
            recommendedRatio = AspectRatio.RATIO_16_9,
            recommendedDuration = VideoDuration.SEC_10
        ),
        ScriptTemplate(
            title = "आज का हौसला (Daily Motivation)",
            category = "Motivation",
            characterName = "The Dreamer",
            characterRole = "Protagonist",
            script = "जब तक सांस है, तब तक उम्मीद है। गिरने का मतलब हारना नहीं होता, बल्कि उठकर दोबारा दौड़ने की तैयारी है। अपने सपनों पर भरोसा रखो, क्योंकि आज का संघर्ष ही कल की चमक बनेगा।",
            recommendedStyle = VisualArtStyle.CINEMATIC,
            recommendedRatio = AspectRatio.RATIO_9_16,
            recommendedDuration = VideoDuration.SEC_10
        ),
        ScriptTemplate(
            title = "Journey Beyond the Stars",
            category = "Cosmic Adventure",
            characterName = "Aria",
            characterRole = "Starlight Navigator",
            script = "Through the crystal asteroid belt, the ship 'Nova' touched the edge of an uncharted nebula. Starlight reflected off Aria's helmet visor as a colossal ancient space station emerged from the stardust.",
            recommendedStyle = VisualArtStyle.DISNEY_3D,
            recommendedRatio = AspectRatio.RATIO_16_9,
            recommendedDuration = VideoDuration.SEC_30
        ),
        ScriptTemplate(
            title = "जादुई जंगल और परी (Fairy Fantasy)",
            category = "Fantasy Tale",
            characterName = "Mira",
            characterRole = "Sorceress",
            script = "नीले फूलों से भरे इस प्राचीन जंगल में हर पेड़ बात करता था। मीरा ने जैसे ही जादुई तावीज को छुआ, हवा में सुनहरी तितलियाँ उड़ने लगीं। एक नया अध्याय शुरू हो चुका था।",
            recommendedStyle = VisualArtStyle.FANTASY,
            recommendedRatio = AspectRatio.RATIO_9_16,
            recommendedDuration = VideoDuration.SEC_10
        ),
        ScriptTemplate(
            title = "Detective Whiskers: The Case of Lost Milk",
            category = "Comedy Reel",
            characterName = "Professor Paws",
            characterRole = "Genius Cat",
            script = "Someone drank the royal cream. The suspect left only a single paw print. Paws adjusted his bow-tie, inspected the fridge handle with his magnifying glass, and hissed: 'Elementary, my dear kitten!'",
            recommendedStyle = VisualArtStyle.RETRO_COMIC,
            recommendedRatio = AspectRatio.RATIO_9_16,
            recommendedDuration = VideoDuration.SEC_5
        )
    )
}
