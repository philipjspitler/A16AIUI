package com.example

import java.util.UUID

enum class DownloadTaskStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val fileName: String,
    val status: DownloadTaskStatus = DownloadTaskStatus.QUEUED,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val speedBytesPerSec: Long = 0L,
    val errorMessage: String? = null,
    val addedTime: Long = System.currentTimeMillis()
)

enum class QuantizationLevel(
    val label: String,
    val shortName: String,
    val ramRequirement: String,
    val description: String,
    val bitDepth: Int
) {
    Q4_BIT(
        label = "4-bit Quantization (Q4_K_M)",
        shortName = "4-bit (Low RAM)",
        ramRequirement = "2GB - 4GB RAM",
        description = "Optimized memory footprint, ultra-fast inference, ~50% smaller file size. Recommended for low-memory devices.",
        bitDepth = 4
    ),
    Q8_BIT(
        label = "8-bit Quantization (Q8_0)",
        shortName = "8-bit (High Precision)",
        ramRequirement = "6GB+ RAM",
        description = "Near-lossless precision, enhanced reasoning depth & perplexity. Recommended for high-end devices.",
        bitDepth = 8
    )
}

data class QuantizedVariant(
    val level: QuantizationLevel,
    val fileName: String,
    val url: String,
    val estimatedSize: String,
    val ramRecommendation: String
)

data class ModelPreset(
    val id: String,
    val title: String,
    val description: String,
    val tag: String,
    val variants: Map<QuantizationLevel, QuantizedVariant>
) {
    fun getVariant(level: QuantizationLevel): QuantizedVariant {
        return variants[level] ?: variants[QuantizationLevel.Q4_BIT] ?: variants.values.first()
    }

    val fileName: String get() = getVariant(QuantizationLevel.Q4_BIT).fileName
    val url: String get() = getVariant(QuantizationLevel.Q4_BIT).url
    val estimatedSize: String get() = getVariant(QuantizationLevel.Q4_BIT).estimatedSize
}

val POPULAR_MODEL_PRESETS = listOf(
    ModelPreset(
        id = "smollm2_135m",
        title = "SmolLM2 135M Instruct",
        description = "Ultra-fast, lightweight model ideal for quick testing and low memory.",
        tag = "Lightweight",
        variants = mapOf(
            QuantizationLevel.Q4_BIT to QuantizedVariant(
                level = QuantizationLevel.Q4_BIT,
                fileName = "smollm2-135m-instruct-q4_k_m.gguf",
                url = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q4_k_m.gguf",
                estimatedSize = "~98 MB",
                ramRecommendation = "< 2GB RAM"
            ),
            QuantizationLevel.Q8_BIT to QuantizedVariant(
                level = QuantizationLevel.Q8_BIT,
                fileName = "smollm2-135m-instruct-q8_0.gguf",
                url = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q8_0.gguf",
                estimatedSize = "~152 MB",
                ramRecommendation = "< 3GB RAM"
            )
        )
    ),
    ModelPreset(
        id = "qwen25_05b",
        title = "Qwen 2.5 0.5B Instruct",
        description = "Fast, high-capability compact instruction and coding model.",
        tag = "Reasoning",
        variants = mapOf(
            QuantizationLevel.Q4_BIT to QuantizedVariant(
                level = QuantizationLevel.Q4_BIT,
                fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
                url = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
                estimatedSize = "~398 MB",
                ramRecommendation = "3GB - 4GB RAM"
            ),
            QuantizationLevel.Q8_BIT to QuantizedVariant(
                level = QuantizationLevel.Q8_BIT,
                fileName = "qwen2.5-0.5b-instruct-q8_0.gguf",
                url = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q8_0.gguf",
                estimatedSize = "~652 MB",
                ramRecommendation = "4GB - 6GB RAM"
            )
        )
    ),
    ModelPreset(
        id = "tinyllama_11b",
        title = "TinyLlama 1.1B Chat",
        description = "Standard compact conversational assistant model.",
        tag = "Chat",
        variants = mapOf(
            QuantizationLevel.Q4_BIT to QuantizedVariant(
                level = QuantizationLevel.Q4_BIT,
                fileName = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
                url = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
                estimatedSize = "~669 MB",
                ramRecommendation = "4GB RAM"
            ),
            QuantizationLevel.Q8_BIT to QuantizedVariant(
                level = QuantizationLevel.Q8_BIT,
                fileName = "tinyllama-1.1b-chat-v1.0.Q8_0.gguf",
                url = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q8_0.gguf",
                estimatedSize = "~1.17 GB",
                ramRecommendation = "6GB+ RAM"
            )
        )
    ),
    ModelPreset(
        id = "llama32_1b",
        title = "Llama 3.2 1B Instruct",
        description = "Meta's highly capable edge reasoning and instruction model.",
        tag = "Smart Edge",
        variants = mapOf(
            QuantizationLevel.Q4_BIT to QuantizedVariant(
                level = QuantizationLevel.Q4_BIT,
                fileName = "llama-3.2-1b-instruct-q4_k_m.gguf",
                url = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                estimatedSize = "~770 MB",
                ramRecommendation = "4GB RAM"
            ),
            QuantizationLevel.Q8_BIT to QuantizedVariant(
                level = QuantizationLevel.Q8_BIT,
                fileName = "llama-3.2-1b-instruct-q8_0.gguf",
                url = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q8_0.gguf",
                estimatedSize = "~1.32 GB",
                ramRecommendation = "6GB+ RAM"
            )
        )
    ),
    ModelPreset(
        id = "phi3_mini",
        title = "Phi-3 Mini 3.8B 4K",
        description = "Full-capability research local reasoning and math model.",
        tag = "High Quality",
        variants = mapOf(
            QuantizationLevel.Q4_BIT to QuantizedVariant(
                level = QuantizationLevel.Q4_BIT,
                fileName = "phi-3-mini-4k-instruct-q4.gguf",
                url = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf",
                estimatedSize = "~2.39 GB",
                ramRecommendation = "6GB RAM"
            ),
            QuantizationLevel.Q8_BIT to QuantizedVariant(
                level = QuantizationLevel.Q8_BIT,
                fileName = "phi-3-mini-4k-instruct-q8_0.gguf",
                url = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q8_0.gguf",
                estimatedSize = "~4.06 GB",
                ramRecommendation = "8GB+ RAM"
            )
        )
    )
)
