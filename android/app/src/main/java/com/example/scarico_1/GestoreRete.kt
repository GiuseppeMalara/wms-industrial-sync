package com.example.scarico_1

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object GestoreRete {
    private const val URL_SERVER = "http://192.168.1.200/salva_scarico.php"
    private const val URL_LEGGI_ARTICOLI = "http://192.168.1.200/get_articoli.php"

    fun scaricaArticoli(context: Context, onSuccess: (List<Articolo>) -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                GestoreLog.scrivi(context, "RETE", "Tentativo connessione a $URL_LEGGI_ARTICOLI...")
                val conn = URL(URL_LEGGI_ARTICOLI).openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    val errMsg = "HTTP $responseCode: Risposta errata dal server"
                    GestoreLog.scrivi(context, "ERRORE", "Download articoli fallito: $errMsg")
                    conn.disconnect()
                    onError(errMsg)
                    return@Thread
                }

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var linea: String?
                while (reader.readLine().also { linea = it } != null) sb.append(linea)
                reader.close()
                conn.disconnect()

                val jsonArray = JSONArray(sb.toString())
                val lista = ArrayList<Articolo>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val listLotti = ArrayList<Lotto>()

                    if (obj.has("lotti")) {
                        val arrLotti = obj.getJSONArray("lotti")
                        for (j in 0 until arrLotti.length()) {
                            val objL = arrLotti.getJSONObject(j)
                            listLotti.add(Lotto(objL.getString("nome_lotto"), objL.optString("posizione", ""), objL.getDouble("giacenza_reale")))
                        }
                    }

                    val gGrezzo = obj.optString("gruppo", "")
                    val sgGrezzo = obj.optString("sottogruppo", "")
                    val gPad = if (gGrezzo.length == 1) "0$gGrezzo" else gGrezzo
                    val sgPad = if (sgGrezzo.length == 1) "0$sgGrezzo" else sgGrezzo

                    lista.add(
                        Articolo(
                            codice = obj.getString("codice"),
                            descrizione = obj.getString("descrizione"),
                            um = obj.getString("um_default"),
                            reparto = obj.optString("reparto_suggerito", ""),
                            categoria = obj.optString("categoria_profilo", ""),
                            gruppo = gPad,
                            sottogruppo = sgPad,
                            giacenza = obj.getDouble("giacenza"),
                            giacenzaReale = obj.getDouble("giacenza_reale"),
                            lotti = listLotti
                        )
                    )
                }

                GestoreLog.scrivi(context, "INFO", "Scaricati con successo ${lista.size} articoli dal server")
                onSuccess(lista)
            } catch (e: Exception) {
                val dettaglio = "${e.javaClass.simpleName}: ${e.localizedMessage ?: e.message}"
                GestoreLog.scrivi(context, "ERRORE", "Download articoli fallito: $dettaglio")
                onError(dettaglio)
            }
        }.start()
    }

    fun inviaScarichi(context: Context, nomeTelefono: String, carrello: List<ItemCarrello>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        Thread {
            var errore = false
            var messaggioErrore = ""

            for (r in carrello) {
                try {
                    val conn = URL(URL_SERVER).openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.requestMethod = "POST"
                    conn.doOutput = true

                    val postData = "codice=${URLEncoder.encode(r.codice, "UTF-8")}&descrizione=${URLEncoder.encode(r.descrizione, "UTF-8")}&qta=${r.qta}&um=${r.um}&reparto=${URLEncoder.encode(nomeTelefono, "UTF-8")}&lotto=${URLEncoder.encode(r.lottoScelto, "UTF-8")}"
                    conn.outputStream.use { it.write(postData.toByteArray()) }

                    val resCode = conn.responseCode
                    val resBody = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                    conn.disconnect()

                    if (resCode != 200 || resBody != "OK") {
                        errore = true
                        messaggioErrore = "Articolo ${r.codice}: HTTP $resCode - Risposta server: '$resBody'"
                        GestoreLog.scrivi(context, "ERRORE", "Invio fallito: $messaggioErrore")
                        break
                    } else {
                        GestoreLog.scrivi(context, "INFO", "Scaricato: ${r.codice} (Qta: ${r.qta}) da $nomeTelefono")
                    }
                } catch (e: Exception) {
                    errore = true
                    messaggioErrore = "${e.javaClass.simpleName}: ${e.localizedMessage ?: e.message}"
                    GestoreLog.scrivi(context, "ERRORE", "Errore connessione invio (${r.codice}): $messaggioErrore")
                    break
                }
            }

            if (!errore) {
                GestoreLog.scrivi(context, "INFO", "Invio completato: tutti i ${carrello.size} scarichi registrati su DB")
                onSuccess()
            } else {
                onError(messaggioErrore)
            }
        }.start()
    }
}