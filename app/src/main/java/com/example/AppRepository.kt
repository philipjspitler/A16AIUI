package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppCategory(val title: String) {
    ALL("All Apps"),
    AI_TOOLS("AI & LLM"),
    PRODUCTIVITY("Productivity"),
    UTILITIES("Utilities"),
    MEDIA("Media & Audio"),
    CUSTOM_WEB("Web Apps")
}

data class AppCatalogItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val category: AppCategory,
    val iconName: String, // Icon key
    val badge: String? = null,
    val sizeMb: Double = 1.2,
    val rating: Double = 4.9,
    val isInstalledByDefault: Boolean = false,
    val webUrl: String? = null,
    val accentColorHex: Long = 0xFF4285F4
)

object DefaultAppCatalog {
    val catalog = listOf(
        AppCatalogItem(
            id = "app_chat",
            name = "Ultra Chat AI",
            subtitle = "Advanced AI Assistant",
            description = "Your ultra-advanced AI engine, powered directly via the Gemini API.",
            category = AppCategory.AI_TOOLS,
            iconName = "chat",
            badge = "CORE",
            sizeMb = 4.2,
            rating = 5.0,
            isInstalledByDefault = true,
            accentColorHex = 0xFF4285F4
        ),
        AppCatalogItem(
            id = "app_aide",
            name = "Android Aide",
            subtitle = "Web & Phone Agent",
            description = "Autonomous task planner that navigates the web, executes phone calls, sets alarms, and handles system intents.",
            category = AppCategory.AI_TOOLS,
            iconName = "phone",
            badge = "AGENT",
            sizeMb = 6.8,
            rating = 4.9,
            isInstalledByDefault = true,
            accentColorHex = 0xFF34A853
        ),
        AppCatalogItem(
            id = "app_code_studio",
            name = "Fast Code Studio",
            subtitle = "Turbo App & Code Maker",
            description = "Super-fast AI app generator, live sandbox preview, interactive HTML5/JS compiler, and 1-tap launcher installer.",
            category = AppCategory.AI_TOOLS,
            iconName = "code",
            badge = "TURBO",
            sizeMb = 3.4,
            rating = 5.0,
            isInstalledByDefault = true,
            accentColorHex = 0xFF00E5FF
        ),
        AppCatalogItem(
            id = "app_image_studio",
            name = "Image Studio Pro",
            subtitle = "Text-to-Image & Video",
            description = "Creative AI studio with local diffusion style models, image-to-video animation, and high-res asset generation.",
            category = AppCategory.MEDIA,
            iconName = "image",
            badge = "STUDIO",
            sizeMb = 8.5,
            rating = 4.8,
            isInstalledByDefault = true,
            accentColorHex = 0xFFEA4335
        ),
        AppCatalogItem(
            id = "app_model_hub",
            name = "Model Hub",
            subtitle = "GGUF Quantized Models",
            description = "Download, manage, and benchmark quantized local LLM weights (SmolLM2, Qwen 2.5, Phi-3, TinyLlama).",
            category = AppCategory.AI_TOOLS,
            iconName = "storage",
            badge = "HUB",
            sizeMb = 2.1,
            rating = 4.9,
            isInstalledByDefault = true,
            accentColorHex = 0xFFFBBC05
        ),
        AppCatalogItem(
            id = "app_notes",
            name = "Offline Notes & Tasks",
            subtitle = "Markdown Memo & Checklist",
            description = "Secure offline scratchpad with Markdown formatting, task checklists, and instant local device backup.",
            category = AppCategory.PRODUCTIVITY,
            iconName = "edit",
            badge = "NEW",
            sizeMb = 1.4,
            rating = 4.7,
            isInstalledByDefault = true,
            accentColorHex = 0xFF9C27B0
        ),
        AppCatalogItem(
            id = "app_calculator",
            name = "Scientific Calc & Converter",
            subtitle = "Math & Unit Conversions",
            description = "High-precision scientific calculator with trigonometry, programmer bitwise math, and offline unit converter.",
            category = AppCategory.UTILITIES,
            iconName = "calculate",
            badge = "PRO",
            sizeMb = 0.9,
            rating = 4.8,
            isInstalledByDefault = true,
            accentColorHex = 0xFF009688
        ),
        AppCatalogItem(
            id = "app_focus_timer",
            name = "Focus & Pomodoro Timer",
            subtitle = "Productivity Intervals",
            description = "Customizable Pomodoro timer with customizable work/break intervals, lap tracking, and focus statistics.",
            category = AppCategory.PRODUCTIVITY,
            iconName = "timer",
            badge = "FOCUS",
            sizeMb = 1.1,
            rating = 4.9,
            isInstalledByDefault = false,
            accentColorHex = 0xFFFF5722
        ),
        AppCatalogItem(
            id = "app_prompt_lab",
            name = "Prompt & Token Lab",
            subtitle = "Dev LLM Sandbox",
            description = "Inspect token counts, experiment with system prompts, temperature tuning, and local prompt benchmarking.",
            category = AppCategory.AI_TOOLS,
            iconName = "terminal",
            badge = "DEV",
            sizeMb = 1.8,
            rating = 5.0,
            isInstalledByDefault = false,
            accentColorHex = 0xFF3F51B5
        ),
        AppCatalogItem(
            id = "app_ambient_noise",
            name = "Ambient Soundscape",
            subtitle = "White Noise & Focus Audio",
            description = "Relaxing binaural tones, gentle rain, forest ambience, and campfire white noise synthesizer for deep focus.",
            category = AppCategory.MEDIA,
            iconName = "music",
            badge = "AUDIO",
            sizeMb = 2.4,
            rating = 4.8,
            isInstalledByDefault = false,
            accentColorHex = 0xFF607D8B
        ),
        AppCatalogItem(
            id = "app_vault_explorer",
            name = "Local Vault Explorer",
            subtitle = "File & Model Browser",
            description = "Explore downloaded GGUF weights, generated studio images, cache size, and device storage partitions.",
            category = AppCategory.UTILITIES,
            iconName = "folder",
            badge = "FILES",
            sizeMb = 1.3,
            rating = 4.7,
            isInstalledByDefault = false,
            accentColorHex = 0xFF795548
        ),
        AppCatalogItem(
            id = "app_weather",
            name = "Atmosphere & Radar",
            subtitle = "Offline Weather Forecast",
            description = "Interactive meteorological dashboard with barometric pressure, wind vectors, and local forecast graphs.",
            category = AppCategory.UTILITIES,
            iconName = "cloud",
            badge = "METEO",
            sizeMb = 1.6,
            rating = 4.6,
            isInstalledByDefault = false,
            accentColorHex = 0xFF03A9F4
        )
    )
}
