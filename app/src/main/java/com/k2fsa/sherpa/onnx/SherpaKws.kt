// Copyright (c)  2024  Xiaomi Corporation
// Minimal keyword-spotter surface from sherpa-onnx 1.13.8 (Apache-2.0).
// Field names match the JNI library. Do not rename them.
package com.k2fsa.sherpa.onnx

import android.content.res.AssetManager

data class QnnConfig(
    var backendLib: String = "",
    var contextBinary: String = "",
    var systemLib: String = "",
)

data class FeatureConfig(
    var sampleRate: Int = 16000,
    var featureDim: Int = 80,
    var dither: Float = 0.0f,
)

data class OnlineTransducerModelConfig(
    var encoder: String = "",
    var decoder: String = "",
    var joiner: String = "",
    var qnnConfig: QnnConfig = QnnConfig(),
)

data class OnlineParaformerModelConfig(
    var encoder: String = "",
    var decoder: String = "",
)

data class OnlineZipformer2CtcModelConfig(
    var model: String = "",
)

data class OnlineNeMoCtcModelConfig(
    var model: String = "",
)

data class OnlineToneCtcModelConfig(
    var model: String = "",
)

data class OnlineModelConfig(
    var transducer: OnlineTransducerModelConfig = OnlineTransducerModelConfig(),
    var paraformer: OnlineParaformerModelConfig = OnlineParaformerModelConfig(),
    var zipformer2Ctc: OnlineZipformer2CtcModelConfig = OnlineZipformer2CtcModelConfig(),
    var neMoCtc: OnlineNeMoCtcModelConfig = OnlineNeMoCtcModelConfig(),
    var toneCtc: OnlineToneCtcModelConfig = OnlineToneCtcModelConfig(),
    var tokens: String = "",
    var numThreads: Int = 1,
    var debug: Boolean = false,
    var provider: String = "cpu",
    var modelType: String = "",
    var modelingUnit: String = "",
    var bpeVocab: String = "",
)

data class KeywordSpotterConfig(
    var featConfig: FeatureConfig = FeatureConfig(),
    var modelConfig: OnlineModelConfig = OnlineModelConfig(),
    var maxActivePaths: Int = 4,
    var keywordsFile: String = "keywords.txt",
    var keywordsScore: Float = 1.0f,
    var keywordsThreshold: Float = 0.25f,
    var numTrailingBlanks: Int = 1,
)

data class KeywordSpotterResult(
    val keyword: String,
    val tokens: Array<String>,
    val timestamps: FloatArray,
)

class OnlineStream(var ptr: Long = 0) {
    init {
        require(ptr != 0L) { "Failed to create native OnlineStream" }
    }

    fun acceptWaveform(samples: FloatArray, sampleRate: Int) = acceptWaveform(ptr, samples, sampleRate)

    fun release() {
        if (ptr != 0L) {
            delete(ptr)
            ptr = 0
        }
    }

    private external fun acceptWaveform(ptr: Long, samples: FloatArray, sampleRate: Int)
    private external fun delete(ptr: Long)

    companion object {
        init {
            Native.wake()
        }
    }
}

class KeywordSpotter(
    assetManager: AssetManager? = null,
    val config: KeywordSpotterConfig,
) {
    private var ptr: Long

    init {
        ptr = if (assetManager != null) newFromAsset(assetManager, config) else newFromFile(config)
        require(ptr != 0L) { "Invalid KeywordSpotterConfig" }
    }

    fun release() {
        if (ptr != 0L) {
            delete(ptr)
            ptr = 0
        }
    }

    fun createStream(keywords: String = ""): OnlineStream = OnlineStream(createStream(ptr, keywords))

    fun decode(stream: OnlineStream) = decode(ptr, stream.ptr)
    fun reset(stream: OnlineStream) = reset(ptr, stream.ptr)
    fun isReady(stream: OnlineStream) = isReady(ptr, stream.ptr)
    fun getResult(stream: OnlineStream) = getResult(ptr, stream.ptr)

    private external fun delete(ptr: Long)
    private external fun newFromAsset(assetManager: AssetManager, config: KeywordSpotterConfig): Long
    private external fun newFromFile(config: KeywordSpotterConfig): Long
    private external fun createStream(ptr: Long, keywords: String): Long
    private external fun isReady(ptr: Long, streamPtr: Long): Boolean
    private external fun decode(ptr: Long, streamPtr: Long)
    private external fun reset(ptr: Long, streamPtr: Long)
    private external fun getResult(ptr: Long, streamPtr: Long): KeywordSpotterResult

    companion object {
        init {
            Native.wake()
        }
    }
}

private object Native {
    fun wake() {
        try {
            System.loadLibrary("onnxruntime")
        } catch (_: UnsatisfiedLinkError) {
        }
        System.loadLibrary("sherpa-onnx-jni")
    }
}
