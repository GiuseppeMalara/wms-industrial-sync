package com.example.scarico_1

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GestoreLog {
    private const val PREFS_NAME = "MemoriaMagazzino"
    private const val KEY_LOGS = "registro_log"
    private const val MAX_LOGS = 150

    fun scrivi(context: Context, livello: String, messaggio: String) {
        try {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonArray = JSONArray(sp.getString(KEY_LOGS, "[]") ?: "[]")

            val dataOra = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date())
            val entry = JSONObject().apply {
                put("data", dataOra)
                put("livello", livello) // "ERRORE", "INFO", "RETE"
                put("msg", messaggio)
            }

            jsonArray.put(entry)

            // Mantiene solo gli ultimi 150 log per non appesantire la memoria
            val arrayFinale = JSONArray()
            val start = if (jsonArray.length() > MAX_LOGS) jsonArray.length() - MAX_LOGS else 0
            for (i in start until jsonArray.length()) {
                arrayFinale.put(jsonArray.get(i))
            }

            sp.edit().putString(KEY_LOGS, arrayFinale.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun leggi(context: Context): List<JSONObject> {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray(sp.getString(KEY_LOGS, "[]") ?: "[]")
        val lista = mutableListOf<JSONObject>()
        // Mostra dal più recente al più vecchio
        for (i in jsonArray.length() - 1 downTo 0) {
            lista.add(jsonArray.getJSONObject(i))
        }
        return lista
    }

    fun svuota(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_LOGS).apply()
    }
}