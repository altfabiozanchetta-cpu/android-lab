package it.fabio.musilab

import android.content.Context

/** Salvataggio delle impostazioni nelle SharedPreferences. */
class Impostazioni(context: Context) {
    private val prefs = context.getSharedPreferences("impostazioni", Context.MODE_PRIVATE)

    fun carica(m: StatoMetronomo, sc: StatoScale, cfg: StatoConfig) {
        cfg.salvaAllUscita = prefs.getBoolean("salva", true)
        cfg.suono = enumOppure(prefs.getString("suono", null), Suono.NOTE_E_CLICK)
        if (!cfg.salvaAllUscita) return
        m.bpm = prefs.getInt("bpm", m.bpm).coerceIn(30, 240)
        m.battiti = prefs.getInt("battiti", m.battiti).coerceIn(1, 12)
        m.suddivisioni = prefs.getInt("suddivisioni", m.suddivisioni).coerceIn(1, 4)
        m.accento = prefs.getBoolean("accento", m.accento)
        sc.tonica = prefs.getInt("tonica", sc.tonica).coerceIn(0, 11)
        sc.tipo = enumOppure(prefs.getString("tipo", null), sc.tipo)
        sc.strumento = enumOppure(prefs.getString("strumento", null), sc.strumento)
        val vista = prefs.getString("vista", null)
        sc.vista = Vista.entries.firstOrNull { it.name == vista }
        sc.ottave = prefs.getInt("ottave", sc.ottave).coerceIn(1, 3)
        sc.diteggiatura = prefs.getInt("diteggiatura", sc.diteggiatura)
    }

    fun salva(m: StatoMetronomo, sc: StatoScale, cfg: StatoConfig) {
        val e = prefs.edit().clear()
        e.putBoolean("salva", cfg.salvaAllUscita)
        e.putString("suono", cfg.suono.name)
        if (cfg.salvaAllUscita) {
            e.putInt("bpm", m.bpm)
            e.putInt("battiti", m.battiti)
            e.putInt("suddivisioni", m.suddivisioni)
            e.putBoolean("accento", m.accento)
            e.putInt("tonica", sc.tonica)
            e.putString("tipo", sc.tipo.name)
            e.putString("strumento", sc.strumento.name)
            sc.vista?.let { e.putString("vista", it.name) }
            e.putInt("ottave", sc.ottave)
            e.putInt("diteggiatura", sc.diteggiatura)
        }
        e.apply()
    }

    private inline fun <reified T : Enum<T>> enumOppure(nome: String?, predefinito: T): T =
        enumValues<T>().firstOrNull { it.name == nome } ?: predefinito

}
