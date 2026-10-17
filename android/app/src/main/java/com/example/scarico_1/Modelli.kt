package com.example.scarico_1 // <-- CONTROLLA IL NOME PACCHETTO

data class Lotto(
    val nomeLotto: String,
    val posizione: String,
    val giacenzaReale: Double
)

data class Articolo(
    val codice: String,
    val descrizione: String,
    val um: String,
    val reparto: String,
    val categoria: String,
    val gruppo: String,      // <-- AGGIUNTO
    val sottogruppo: String, // <-- AGGIUNTO
    val giacenza: Double,
    val giacenzaReale: Double,
    val lotti: List<Lotto>
)

data class ItemCarrello(
    val codice: String,
    val descrizione: String,
    var qta: Double,
    val um: String,
    val giacenza: Double,
    var lottoScelto: String = ""
)