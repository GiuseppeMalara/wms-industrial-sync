package com.example.scarico_1

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object GestoreCarrello {
    val carrello = ArrayList<ItemCarrello>()

    fun getQta(codice: String, lotto: String = ""): Double {
        return carrello
            .filter { it.codice.trim().equals(codice.trim(), ignoreCase = true) && it.lottoScelto == lotto }
            .sumOf { it.qta }
    }

    fun aggiungi(item: ItemCarrello, context: Context) {
        carrello.add(item)
        salvaInMemoria(context)
    }

    fun rimuoviAt(index: Int, context: Context) {
        carrello.removeAt(index)
        salvaInMemoria(context)
        if (carrello.isEmpty()) svuota(context)
    }

    fun aggiornaItem(index: Int, item: ItemCarrello, context: Context) {
        carrello[index] = item
        salvaInMemoria(context)
    }

    fun svuota(context: Context) {
        carrello.clear()
        context.getSharedPreferences("MemoriaMagazzino", Context.MODE_PRIVATE).edit().remove("lista_salvata").apply()
    }

    private fun salvaInMemoria(context: Context) {
        val sp = context.getSharedPreferences("MemoriaMagazzino", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in carrello) {
            val obj = JSONObject()
            obj.put("codice", item.codice); obj.put("descrizione", item.descrizione); obj.put("qta", item.qta)
            obj.put("um", item.um); obj.put("giacenza", item.giacenza); obj.put("lotto", item.lottoScelto)
            jsonArray.put(obj)
        }
        sp.edit().putString("lista_salvata", jsonArray.toString()).apply()
    }

    fun caricaDaMemoria(context: Context) {
        carrello.clear()
        val jsonStr = context.getSharedPreferences("MemoriaMagazzino", Context.MODE_PRIVATE).getString("lista_salvata", null)
        if (jsonStr != null) {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                carrello.add(ItemCarrello(obj.getString("codice"), obj.getString("descrizione"), obj.getDouble("qta"), obj.getString("um"), obj.getDouble("giacenza"), obj.optString("lotto", "")))
            }
        }
    }

    fun salvaStorico(context: Context) {
        val sp = context.getSharedPreferences("MemoriaMagazzino", Context.MODE_PRIVATE)
        val jsonArray = JSONArray(sp.getString("storico_invii", "[]") ?: "[]")
        val dataOdierna = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        for (item in carrello) {
            val obj = JSONObject()
            obj.put("data", dataOdierna); obj.put("codice", item.codice); obj.put("qta", item.qta)
            jsonArray.put(obj)
        }

        val jsonArrayFinale = JSONArray()
        val start = if (jsonArray.length() > 150) jsonArray.length() - 150 else 0
        for (i in start until jsonArray.length()) jsonArrayFinale.put(jsonArray.get(i))

        sp.edit().putString("storico_invii", jsonArrayFinale.toString()).apply()
    }
}