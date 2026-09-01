package com.example

import java.io.File

/**
 * Model info descriptor parsing model metadata from local files.
 */
data class InstalledModelInfo(
    val file: File,
    val fileName: String,
    val displayName: String,
    val versionTag: String,
    val quantization: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val lastModified: Long,
    val isActive: Boolean,
    val ramRequirement: String,
    val description: String
)

object ModelInfoParser {
    fun parse(file: File, activeModelName: String?): InstalledModelInfo {
        val fileName = file.name
        val lower = fileName.lowercase()

        val displayName = when {
            lower.contains("smollm2") -> "SmolLM2 135M Instruct"
            lower.contains("qwen2.5") || lower.contains("qwen") -> "Qwen 2.5 0.5B Instruct"
            lower.contains("tinyllama") -> "TinyLlama 1.1B Chat"
            lower.contains("llama-3.2") || lower.contains("llama32") -> "Llama 3.2 1B Instruct"
            lower.contains("phi-3") || lower.contains("phi3") -> "Phi-3 Mini 3.8B 4K"
            else -> fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ").capitalizeWords()
        }

        val versionTag = when {
            lower.contains("v1.0") -> "v1.0"
            lower.contains("v2.0") -> "v2.0"
            lower.contains("v0.5") -> "v0.5"
            lower.contains("2.5") -> "v2.5"
            lower.contains("3.2") -> "v3.2"
            lower.contains("3.8b") -> "v3.8"
            lower.contains("135m") -> "v2.0-135M"
            lower.contains("1.1b") -> "v1.0-1.1B"
            lower.contains("1b") -> "v3.2-1B"
            else -> "v1.0"
        }

        val quantization = when {
            lower.contains("q4_k_m") || lower.contains("q4_k") || lower.contains("q4_0") || lower.contains("q4") -> "4-bit (Q4_K_M)"
            lower.contains("q8_0") || lower.contains("q8_k") || lower.contains("q8") -> "8-bit (Q8_0)"
            lower.contains("q5_k_m") || lower.contains("q5") -> "5-bit (Q5_K_M)"
            lower.contains("f16") -> "16-bit (FP16)"
            else -> "Standard (GGUF)"
        }

        val ramRequirement = when {
            lower.contains("smollm2") -> "< 2 GB RAM"
            lower.contains("qwen") -> if (quantization.startsWith("8")) "4-6 GB RAM" else "3-4 GB RAM"
            lower.contains("tinyllama") -> if (quantization.startsWith("8")) "6+ GB RAM" else "4 GB RAM"
            lower.contains("llama-3.2") || lower.contains("llama32") -> if (quantization.startsWith("8")) "6+ GB RAM" else "4 GB RAM"
            lower.contains("phi") -> if (quantization.startsWith("8")) "8+ GB RAM" else "6 GB RAM"
            else -> if (file.length() > 1_500_000_000L) "6+ GB RAM" else "4 GB RAM"
        }

        val description = when {
            lower.contains("smollm2") -> "Ultra-compact mobile weights. Instant on-device execution with near-zero latency."
            lower.contains("qwen") -> "Multilingual reasoning and code comprehension weights optimized for mobile devices."
            lower.contains("tinyllama") -> "Compact conversation & instruction tuned LLM weights."
            lower.contains("llama") -> "Meta's flagship edge reasoning architecture with high accuracy."
            lower.contains("phi") -> "Microsoft's high-reasoning benchmark SLM with math and logic optimization."
            else -> "Local on-device machine learning model."
        }

        val isActive = fileName.equals(activeModelName, ignoreCase = true)

        return InstalledModelInfo(
            file = file,
            fileName = fileName,
            displayName = displayName,
            versionTag = versionTag,
            quantization = quantization,
            sizeBytes = file.length(),
            formattedSize = formatSize(file.length()),
            lastModified = file.lastModified(),
            isActive = isActive,
            ramRequirement = ramRequirement,
            description = description
        )
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { 
        it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } 
    }
}
