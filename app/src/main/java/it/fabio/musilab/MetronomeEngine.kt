package it.fabio.musilab

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Motore del metronomo: un thread scrive in continuo campioni audio (AudioTrack in streaming).
 * Ogni "tick" è un buffer di lunghezza esatta in campioni, quindi il tempo non dipende
 * dagli orologi del sistema ma dalla frequenza di campionamento.
 */
class MetronomeEngine {
    @Volatile var bpm: Int = 100
    @Volatile var beatsPerBar: Int = 4
    @Volatile var subdivisions: Int = 1
    @Volatile var accentEnabled: Boolean = true

    /** Chiamato sul thread principale: (battuta corrente, suddivisione corrente). */
    @Volatile var onTick: ((Int, Int) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private var stopFlag: AtomicBoolean? = null

    fun start() {
        if (stopFlag != null) return
        val flag = AtomicBoolean(false)
        stopFlag = flag
        val t = Thread { runLoop(flag) }
        t.priority = Thread.MAX_PRIORITY
        t.start()
    }

    fun stop() {
        stopFlag?.set(true)
        stopFlag = null
    }

    private fun makeClick(sampleRate: Int, freq: Double, gain: Double): ShortArray {
        val len = (sampleRate * 0.045).toInt()
        return ShortArray(len) { i ->
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 90.0)
            (sin(2.0 * PI * freq * t) * env * gain * 32767.0 * 0.9).toInt().toShort()
        }
    }

    private fun runLoop(flag: AtomicBoolean) {
        val sampleRate = 44100
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
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
            .setBufferSizeInBytes(maxOf(minBuf, 4096))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val accentClick = makeClick(sampleRate, 1500.0, 1.0)
        val beatClick = makeClick(sampleRate, 1000.0, 0.8)
        val subClick = makeClick(sampleRate, 700.0, 0.4)

        // Ritardo con cui il suono scritto ora arriva davvero all'altoparlante.
        val latencyMs = track.bufferSizeInFrames * 1000L / sampleRate

        var beat = 0
        var sub = 0
        var carry = 0.0

        track.play()
        try {
            while (!flag.get()) {
                if (beat >= beatsPerBar) beat = 0
                if (sub >= subdivisions) sub = 0

                val b = beat
                val s = sub
                val click = when {
                    s != 0 -> subClick
                    b == 0 && accentEnabled -> accentClick
                    else -> beatClick
                }

                val exact = sampleRate * 60.0 / bpm / subdivisions + carry
                val n = exact.toInt()
                carry = exact - n

                val buf = ShortArray(n)
                System.arraycopy(click, 0, buf, 0, min(click.size, n))

                handler.postDelayed({
                    if (!flag.get()) onTick?.invoke(b, s)
                }, latencyMs)

                val written = track.write(buf, 0, n)
                if (written < 0) break

                sub++
                if (sub >= subdivisions) {
                    sub = 0
                    beat++
                    if (beat >= beatsPerBar) beat = 0
                }
            }
        } finally {
            try {
                track.stop()
            } catch (e: IllegalStateException) {
                // già fermato
            }
            track.release()
        }
    }
}
