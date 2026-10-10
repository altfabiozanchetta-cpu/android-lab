package it.fabio.musilab

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

object Sintesi {
    const val FREQUENZA = 44100

    /**
     * Corda pizzicata con l'algoritmo di Karplus-Strong: rumore in una linea di ritardo lunga un
     * periodo, filtrata a ogni giro. È ricca di armonici, così anche le note gravi del basso
     * si sentono dagli altoparlanti del telefono. Il suono viene sommato a [out] da [inizio].
     */
    fun pizzico(midi: Int, n: Int, out: IntArray, inizio: Int = 0, volume: Double = 0.55) {
        val freq = 440.0 * 2.0.pow((midi - 69) / 12.0)
        val periodo = (FREQUENZA / freq - 0.5).roundToInt().coerceAtLeast(2)
        val corda = DoubleArray(periodo) { Random.nextDouble(-1.0, 1.0) }
        // Un primo passaggio di filtro ammorbidisce l'attacco.
        for (i in 1 until periodo) corda[i] = 0.5 * (corda[i] + corda[i - 1])
        val smorzamento = 0.996
        val dissolvenza = (FREQUENZA * 0.004).toInt()
        for (i in 0 until n) {
            if (inizio + i >= out.size) break
            val j = i % periodo
            val v = corda[j]
            corda[j] = smorzamento * 0.5 * (v + corda[(j + 1) % periodo])
            val inviluppo = if (i >= n - dissolvenza) (n - i).toDouble() / dissolvenza else 1.0
            out[inizio + i] += (v * inviluppo * 32767.0 * volume).toInt()
        }
    }

    /** Suona un accordo leggermente arpeggiato (come una pennata), senza bloccare l'interfaccia. */
    fun suonaAccordo(note: List<Int>) {
        Thread {
            val durata = (FREQUENZA * 1.6).toInt()
            val pennata = (FREQUENZA * 0.035).toInt()
            val mix = IntArray(durata + pennata * note.size)
            note.forEachIndexed { i, m -> pizzico(m, durata, mix, i * pennata, 0.35) }
            val buf = ShortArray(mix.size) { mix[it].coerceIn(-32768, 32767).toShort() }
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
                        .setSampleRate(FREQUENZA)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buf.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            try {
                track.write(buf, 0, buf.size)
                track.play()
                Thread.sleep(buf.size * 1000L / FREQUENZA + 100)
            } finally {
                track.release()
            }
        }.start()
    }
}
