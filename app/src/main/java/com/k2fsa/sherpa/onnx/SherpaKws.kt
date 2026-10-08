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
    private val gate = Any()

    init {
        require(ptr != 0L) { "Failed to create native OnlineStream" }
    }

    /** A released stream has a null native pointer. Calling into it crashes the process. */
    fun acceptWaveform(samples: FloatArray, sampleRate: Int) {
        if (samples.isEmpty()) return
        val native = synchronized(gate) { ptr }
        if (native == 0L) return
        acceptWaveform(native, samples, sampleRate)
    }

    fun release() {
        val native = synchronized(gate) {
            val current = ptr
            ptr = 0
            current
        }
        if (native != 0L) delete(native)
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

    fun decode(stream: OnlineStream) {
        val native = stream.ptr
        if (ptr == 0L || native == 0L) return
        decode(ptr, native)
    }

    fun reset(stream: OnlineStream) {
        val native = stream.ptr
        if (ptr == 0L || native == 0L) return
        reset(ptr, native)
    }

    fun isReady(stream: OnlineStream): Boolean {
        val native = stream.ptr
        if (ptr == 0L || native == 0L) return false
        return isReady(ptr, native)
    }

    fun getResult(stream: OnlineStream): KeywordSpotterResult {
        val native = stream.ptr
        if (ptr == 0L || native == 0L) return KeywordSpotterResult("", emptyArray(), FloatArray(0))
        return getResult(ptr, native)
    }

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
