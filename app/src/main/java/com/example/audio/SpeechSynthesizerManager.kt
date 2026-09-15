package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.models.VoiceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

class SpeechSynthesizerManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    private val _currentUtteranceId = MutableStateFlow<String?>(null)
    val currentUtteranceId = _currentUtteranceId.asStateFlow()

    // For karaoke word highlighting (character index / range)
    private val _speechProgressRatio = MutableStateFlow(0f)
    val speechProgressRatio = _speechProgressRatio.asStateFlow()

    private var onSpeechDoneCallback: (() -> Unit)? = null

    // Ambient audio synth
    private var ambientTrack: AudioTrack? = null
    private var ambientJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.Default)

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    _currentUtteranceId.value = utteranceId
                    _speechProgressRatio.value = 0.05f
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _speechProgressRatio.value = 1f
                    onSpeechDoneCallback?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    super.onRangeStart(utteranceId, start, end, frame)
                    // Relative progress can be updated if text length known
                }
            })
        } else {
            Log.e("SpeechSynthesizer", "TTS Initialization failed")
        }
    }

    fun speakScene(
        text: String,
        profile: VoiceProfile,
        utteranceId: String = "scene_speech",
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) {
            onDone?.invoke()
            return
        }

        onSpeechDoneCallback = onDone

        // Detect Hindi or English
        val hasHindi = text.any { it in '\u0900'..'\u097F' }
        if (hasHindi) {
            val hindiLocale = Locale("hi", "IN")
            val check = tts?.isLanguageAvailable(hindiLocale)
            if (check != TextToSpeech.LANG_MISSING_DATA && check != TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = hindiLocale
            } else {
                tts?.language = Locale.getDefault()
            }
        } else {
            tts?.language = Locale.US
        }

        tts?.setPitch(profile.pitch)
        tts?.setSpeechRate(profile.speechRate)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _speechProgressRatio.value = 0f
    }

    fun startAmbientSound(enabled: Boolean = true) {
        if (!enabled) {
            stopAmbientSound()
            return
        }
        if (ambientJob != null && ambientJob?.isActive == true) return

        ambientJob = audioScope.launch {
            try {
                val sampleRate = 44100
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                ambientTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                ambientTrack?.play()

                // Generate gentle warm harmonic cinematic pad (110Hz + 220Hz + 330Hz)
                val buffer = ShortArray(bufferSize)
                var angle1 = 0.0
                var angle2 = 0.0
                var angle3 = 0.0

                val freq1 = 110.0 // A2 drone
                val freq2 = 164.81 // E3 fifth
                val freq3 = 220.0 // A3 octave

                while (isActive) {
                    for (i in buffer.indices) {
                        val sample1 = sin(angle1) * 0.12
                        val sample2 = sin(angle2) * 0.08
                        val sample3 = sin(angle3) * 0.05
                        val combined = (sample1 + sample2 + sample3) * Short.MAX_VALUE * 0.35
                        buffer[i] = combined.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                        angle1 += 2.0 * Math.PI * freq1 / sampleRate
                        angle2 += 2.0 * Math.PI * freq2 / sampleRate
                        angle3 += 2.0 * Math.PI * freq3 / sampleRate
                    }
                    ambientTrack?.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                Log.d("SpeechSynthesizer", "Ambient track stopped or not supported: ${e.message}")
            }
        }
    }

    fun stopAmbientSound() {
        ambientJob?.cancel()
        ambientJob = null
        try {
            ambientTrack?.stop()
            ambientTrack?.release()
        } catch (e: Exception) {
            // ignore
        }
        ambientTrack = null
    }

    fun release() {
        stop()
        stopAmbientSound()
        tts?.shutdown()
        tts = null
    }
}
