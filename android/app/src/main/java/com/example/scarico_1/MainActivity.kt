package com.example.scarico_1

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private var idTerminaleDinamico = "Telefono_Generale"

    private val listaArticoliCompleta = ArrayList<Articolo>()
    private var listaFiltrataCategoria = ArrayList<Articolo>()

    private lateinit var autoSearch: AutoCompleteTextView
    private lateinit var etQuantita: EditText
    private lateinit var btnAggiungi: Button
    private lateinit var btnVediCarrello: Button
    private lateinit var spinNomeFile: Spinner
    private lateinit var bloccoRicercaStandard: LinearLayout
    private lateinit var bloccoProfili: LinearLayout
    private lateinit var layoutPulsantiSubCategoria: LinearLayout
    private lateinit var btnTagliaSlash: Button
    private lateinit var txtGiacenzaA: TextView
    private lateinit var contenitoreLottiMain: LinearLayout

    private var lottoSelezionatoAttualmente: Lotto? = null
    private lateinit var searchAdapter: RicercaAdapter

    private var categoriaAttualeProfili = ""
    private var profColoreSel = ""
    private var profMisuraSel = ""
    private var prefissoAttuale = ""

    // BLACKLIST PROFILI OBSOLETI / KIT
    private val listaNeraProfili = listOf(
        "BAC",
        "BASLKIT",
        "GCEKIT",
        "GCIKIT",
        "GVK",
        "GVKKIT",
        "STEP",
        "STYLE"
    )

    // SCALA DELLE SAGOME PROPORZIONATA
    private val scalaProfiliMap = mutableMapOf(
        "CAP" to 18,
        "GBG" to 18,
        "MGL" to 18,
        "GDA55F" to 14,
        "GDA45F" to 24,
        "GDA55L" to 14,
        "GDA45L" to 24,
        "R608" to 10,
        "R70L" to 10,
        "R708" to 6,
        "R78L" to 6,
        "RUL32" to 12,
        "CAS64" to 12,
        "FRG40" to 14,
        "CAS90" to 0,
        "R85L" to 0,
        "R7010" to 0,
        "RUL43" to 0,
        "FRG60" to 0,
        "FR3G" to 0
    )

    // ORDINAMENTO DA FOGLIO: TENDE DA SOLE (SINISTRA)
    private val ordineTendeDaSole = mapOf(
        "R608" to 1,
        "R708" to 2,
        "R7010" to 3,
        "FRBA" to 4,
        "BACI" to 5,
        "BACS" to 6,
        "BASL" to 7,
        "BRQ" to 8,
        "FR3G" to 9,
        "FRBQ" to 10,
        "FRG60" to 11,
        "GCE" to 12,
        "GCI" to 13,
        "CAP" to 14,
        "CART" to 15,
        "FRG40" to 16,
        "FRSTR" to 17,
        "GBG" to 18,
        "R70L" to 19,
        "R78L" to 20,
        "R85L" to 21,
        "MGL" to 22,
        "PAT1" to 23,
        "PAT2" to 24,
        "BRAC" to 25
    )

    private val ordineTendeARullo = mapOf(
        "RUL43" to 1,
        "RUL32" to 2,
        "RUL56" to 3,
        "FR20" to 4,
        "LEDA" to 5,
        "FROVL" to 6,
        "PRTRL2" to 7,
        "CAS90" to 8,
        "CAS64" to 9,
        "RUL19" to 10,
        "TOND" to 11,
        "FR15" to 12,
        "FRMCR" to 13,
        "PRTMCR" to 14,
        "GDA45F" to 15,
        "GDA45L" to 16,
        "GDA55F" to 17,
        "GDA55L" to 18,
        "PRTRL" to 19
    )

    private val arrayFamiglie = arrayOf(
        "TUTTE LE CATEGORIE",
        "01 MAGAZZINO",
        "02 SISTEMI PER TENDE",
        "03 TENDE TECNICHE",
        "06 TENDE DA ESTERNO",
        "07 MOTORI E ACCESSORI",
        "09 TENDE A RULLO",
        "PROFILI (Modalità A)",
        "PROFILI (Modalità B)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sp = getSharedPreferences("ImpostazioniApp", MODE_PRIVATE)
        val repartoSalvato = sp.getString("categoria_default", "TUTTE LE CATEGORIE") ?: "TUTTE LE CATEGORIE"
        aggiornaNomeTerminale(repartoSalvato)

        inizializzaUI(repartoSalvato)
        impostaEventi()

        GestoreCarrello.caricaDaMemoria(this)
        aggiornaContatoreCarrello()
        GestoreLog.scrivi(this, "INFO", "Avvio app completato. Terminale: $idTerminaleDinamico")
        scaricaDatiRete()
    }

    private fun aggiornaNomeTerminale(scelta: String) {
        idTerminaleDinamico = when(scelta) {
            "09 TENDE A RULLO" -> "Telefono_Rullo"
            "02 SISTEMI PER TENDE" -> "Telefono_Binari"
            "06 TENDE DA ESTERNO" -> "Telefono_Sole"
            "PROFILI (Modalità A)", "PROFILI (Modalità B)" -> "Telefono_Profili"
            else -> "Telefono_Generale"
        }
    }

    private fun inizializzaUI(repartoIniziale: String) {
        spinNomeFile = findViewById(R.id.spinNomeFile)
        autoSearch = findViewById(R.id.autoSearch)
        etQuantita = findViewById(R.id.etQuantita)
        btnAggiungi = findViewById(R.id.btnAggiungi)
        btnVediCarrello = findViewById(R.id.btnVediCarrello)
        bloccoRicercaStandard = findViewById(R.id.bloccoRicercaStandard)
        bloccoProfili = findViewById(R.id.bloccoProfili)
        layoutPulsantiSubCategoria = findViewById(R.id.layoutPulsantiSubCategoria)
        btnTagliaSlash = findViewById(R.id.btnTagliaSlash)
        txtGiacenzaA = findViewById(R.id.txtGiacenzaA)
        contenitoreLottiMain = findViewById(R.id.contenitoreLottiMain)

        searchAdapter = RicercaAdapter(this, ArrayList())
        autoSearch.setAdapter(searchAdapter)

        val spinnerAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayFamiglie) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = (convertView as? TextView) ?: TextView(this@MainActivity).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(28, 0, 24, 0)
                }
                tv.text = getItem(position)
                tv.setTextColor(Color.BLACK)
                tv.textSize = 24f
                tv.typeface = Typeface.DEFAULT_BOLD
                return tv
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = (convertView as? TextView) ?: TextView(this@MainActivity).apply {
                    val density = resources.displayMetrics.density
                    val padH = (24 * density).toInt()
                    val padV = (16 * density).toInt()
                    setPadding(padH, padV, padH, padV)
                    gravity = Gravity.CENTER_VERTICAL
                    setBackgroundColor(Color.WHITE)
                }
                tv.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                tv.text = getItem(position)
                tv.setTextColor(Color.BLACK)
                tv.textSize = 22f
                tv.typeface = Typeface.DEFAULT_BOLD
                return tv
            }
        }
        spinNomeFile.adapter = spinnerAdapter

        val pos = arrayFamiglie.indexOf(repartoIniziale)
        if (pos >= 0) spinNomeFile.setSelection(pos)
    }

    private fun impostaTipoTastiera(isNumerica: Boolean) {
        if (isNumerica) {
            autoSearch.inputType = InputType.TYPE_CLASS_PHONE
        } else {
            autoSearch.inputType = InputType.TYPE_CLASS_TEXT
        }
        autoSearch.typeface = Typeface.DEFAULT_BOLD
    }

    private fun mostraMessaggioTablet(titolo: String, messaggio: String, isErrore: Boolean = false) {
        val density = resources.displayMetrics.density

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (24 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val tvTitolo = TextView(this).apply {
            text = titolo
            textSize = 25f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (isErrore) Color.parseColor("#D32F2F") else Color.parseColor("#2E7D32"))
            setPadding(0, 0, 0, (14 * density).toInt())
        }

        val tvMessaggio = TextView(this).apply {
            text = messaggio
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            // Errore risolto: lineHeight rimosso
        }

        layout.addView(tvTitolo)
        layout.addView(tvMessaggio)

        val dialog = AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("HO CAPITO") { d, _ -> d.dismiss() }
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1976D2"))
            val padBtn = (16 * density).toInt()
            setPadding(padBtn, padBtn, padBtn, padBtn)
        }
    }

    private fun mostraDialogTablet(builder: AlertDialog.Builder): AlertDialog {
        val dialog = builder.create()
        dialog.show()

        dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.apply {
            textSize = 25f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }
        dialog.findViewById<TextView>(android.R.id.message)?.apply {
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            val pad = (14 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            val pad = (14 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            val pad = (14 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        return dialog
    }

    private fun impostaEventi() {
        btnVediCarrello.setOnClickListener { apriPaginaRiepilogo() }

        findViewById<ImageButton>(R.id.btnSvuotaRicerca).setOnClickListener { azzeraRicerca() }

        btnTagliaSlash.setOnClickListener {
            val testo = autoSearch.text.toString()
            if (testo.contains("/")) {
                val tagliato = testo.substring(0, testo.lastIndexOf("/") + 1)
                autoSearch.setText(tagliato)
                autoSearch.setSelection(tagliato.length)
                autoSearch.showDropDown()
            }
        }

        findViewById<Button>(R.id.btnChiudiRiepilogo).setOnClickListener { chiudiPaginaRiepilogo() }

        findViewById<Button>(R.id.btnInviaScarico).setOnClickListener {
            if (GestoreCarrello.carrello.isEmpty()) return@setOnClickListener
            val b = AlertDialog.Builder(this)
                .setTitle("Conferma finale")
                .setMessage("Inviare questi scarichi al Server?")
                .setPositiveButton("SÌ") { _, _ -> inviaScaricoRete() }
                .setNegativeButton("NO", null)
            mostraDialogTablet(b)
        }

        spinNomeFile.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val scelta = spinNomeFile.selectedItem.toString()
                if (scelta == "PROFILI (Modalità B)") {
                    setModalitaProfili()
                } else {
                    applicaFiltroCategoria(scelta)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        autoSearch.setOnItemClickListener { parent, _, position, _ ->
            selezionaDaRicerca(parent.getItemAtPosition(position) as Articolo)
        }

        btnAggiungi.setOnClickListener { aggiungiAlCarrelloLogica() }
    }

    private fun isProfiloValido(codice: String): Boolean {
        val radice = codice.split("|")[0].trim().uppercase()
        return !listaNeraProfili.contains(radice)
    }

    private fun getArticoliValidiGlobali(): List<Articolo> {
        return listaArticoliCompleta.filter { art ->
            val f = art.gruppo.trim().padStart(2, '0')
            val g = art.sottogruppo.trim().padStart(2, '0')

            if (!art.categoria.isNullOrEmpty()) return@filter false

            if (f == "06" && g == "04") return@filter false
            if (f == "06" && g == "01") return@filter false
            if (f == "09" && g == "01") return@filter false
            if (f == "09" && g == "05") return@filter false
            if (f == "03" && g != "04") return@filter false
            if (f == "04" || f == "05") return@filter false

            true
        }
    }

    private fun applicaFiltroCategoria(famigliaScelta: String) {
        bloccoRicercaStandard.visibility = View.VISIBLE
        bloccoProfili.visibility = View.GONE
        layoutPulsantiSubCategoria.visibility = View.GONE
        layoutPulsantiSubCategoria.removeAllViews()
        btnTagliaSlash.visibility = View.GONE
        prefissoAttuale = ""
        impostaTipoTastiera(false)

        val sp = getSharedPreferences("ImpostazioniApp", MODE_PRIVATE)
        val usaPrefissi = sp.getBoolean("pref_autocompila_prefissi", true)
        val tastieraNumerica = sp.getBoolean("pref_tastiera_numerica_accessori", false)

        val validi = getArticoliValidiGlobali()

        when (famigliaScelta) {
            "TUTTE LE CATEGORIE" -> {
                listaFiltrataCategoria = ArrayList(validi)
            }
            "01 MAGAZZINO" -> {
                listaFiltrataCategoria = ArrayList(validi.filter {
                    val f = it.gruppo.trim().padStart(2, '0')
                    f == "01" || f == "08"
                })
            }
            "02 SISTEMI PER TENDE" -> {
                prefissoAttuale = if (usaPrefissi) "BN-" else ""
                listaFiltrataCategoria = ArrayList(validi.filter { it.gruppo.trim().padStart(2, '0') == "02" })
            }
            "03 TENDE TECNICHE" -> {
                prefissoAttuale = if (usaPrefissi) "VERTES|" else ""
                listaFiltrataCategoria = ArrayList(validi.filter { it.gruppo.trim().padStart(2, '0') == "03" })
            }
            "07 MOTORI E ACCESSORI" -> {
                listaFiltrataCategoria = ArrayList(validi.filter { it.gruppo.trim().padStart(2, '0') == "07" })
            }
            "06 TENDE DA ESTERNO" -> {
                layoutPulsantiSubCategoria.visibility = View.VISIBLE
                creaBottoniSubCategoria(listOf("TESSUTI SOLE", "ACCESSORI VARI")) { sceltaBot ->
                    if (sceltaBot == "TESSUTI SOLE") {
                        prefissoAttuale = ""
                        impostaTipoTastiera(false)
                        listaFiltrataCategoria = ArrayList(validi.filter { art ->
                            art.gruppo.trim().padStart(2, '0') == "06" && art.sottogruppo.trim().padStart(2, '0') == "02"
                        })
                    } else {
                        prefissoAttuale = if (usaPrefissi) "88-" else ""
                        impostaTipoTastiera(tastieraNumerica)
                        listaFiltrataCategoria = ArrayList(validi.filter { art ->
                            art.gruppo.trim().padStart(2, '0') == "06" && art.sottogruppo.trim().padStart(2, '0') != "02"
                        })
                    }
                    searchAdapter.updateList(listaFiltrataCategoria)
                    azzeraRicerca()
                }
                layoutPulsantiSubCategoria.getChildAt(0)?.performClick()
                return
            }
            "09 TENDE A RULLO" -> {
                layoutPulsantiSubCategoria.visibility = View.VISIBLE
                creaBottoniSubCategoria(listOf("TESSUTI RULLO", "ACCESSORI RULLO")) { sceltaBot ->
                    if (sceltaBot == "TESSUTI RULLO") {
                        prefissoAttuale = ""
                        btnTagliaSlash.visibility = View.GONE
                        impostaTipoTastiera(false)
                        listaFiltrataCategoria = ArrayList(validi.filter { art ->
                            val f = art.gruppo.trim().padStart(2, '0')
                            val sg = art.sottogruppo.trim().padStart(2, '0')
                            f == "09" && sg == "04"
                        })
                    } else {
                        prefissoAttuale = if (usaPrefissi) "RU-" else ""
                        btnTagliaSlash.visibility = View.VISIBLE
                        impostaTipoTastiera(tastieraNumerica)
                        listaFiltrataCategoria = ArrayList(validi.filter { art ->
                            val f = art.gruppo.trim().padStart(2, '0')
                            val sg = art.sottogruppo.trim().padStart(2, '0')
                            f == "09" && sg != "04"
                        })
                    }
                    searchAdapter.updateList(listaFiltrataCategoria)
                    azzeraRicerca()
                }
                layoutPulsantiSubCategoria.getChildAt(0)?.performClick()
                return
            }
            "PROFILI (Modalità A)" -> {
                listaFiltrataCategoria = ArrayList(listaArticoliCompleta.filter {
                    !it.categoria.isNullOrEmpty() && isProfiloValido(it.codice)
                })
            }
        }

        searchAdapter.updateList(listaFiltrataCategoria)
        autoSearch.showDropDown()
        azzeraRicerca()
    }

    private fun creaBottoniSubCategoria(nomi: List<String>, onClick: (String) -> Unit) {
        layoutPulsantiSubCategoria.removeAllViews()
        val bottoni = mutableListOf<Button>()
        val density = resources.displayMetrics.density
        val hPx = (75 * density).toInt()

        for (nome in nomi) {
            val btn = Button(this).apply {
                text = nome
                layoutParams = LinearLayout.LayoutParams(0, hPx, 1f).apply {
                    setMargins((8 * density).toInt(), 0, (8 * density).toInt(), 0)
                }
                textSize = 21f
                setTypeface(null, Typeface.BOLD)
                setBackgroundColor(Color.parseColor("#E0E0E0"))
                setTextColor(Color.BLACK)
            }

            btn.setOnClickListener {
                bottoni.forEach { b ->
                    b.setBackgroundColor(Color.parseColor("#E0E0E0"))
                    b.setTextColor(Color.BLACK)
                }
                btn.setBackgroundColor(Color.parseColor("#1976D2"))
                btn.setTextColor(Color.WHITE)
                onClick(nome)
            }

            bottoni.add(btn)
            layoutPulsantiSubCategoria.addView(btn)
        }
    }

    private fun azzeraRicerca(apriTastiera: Boolean = false) {
        autoSearch.setText(prefissoAttuale)
        if (prefissoAttuale.isNotEmpty()) {
            autoSearch.setSelection(prefissoAttuale.length)
            autoSearch.showDropDown()
        }
        etQuantita.setText("")
        txtGiacenzaA.text = ""
        txtGiacenzaA.visibility = View.GONE
        contenitoreLottiMain.visibility = View.GONE
        lottoSelezionatoAttualmente = null

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        if (apriTastiera) {
            autoSearch.requestFocus()
            autoSearch.post {
                imm.showSoftInput(autoSearch, InputMethodManager.SHOW_IMPLICIT)
            }
        } else {
            imm.hideSoftInputFromWindow(autoSearch.windowToken, 0)
        }
    }

    private fun aggiornaContatoreCarrello() {
        val quanti = GestoreCarrello.carrello.size
        if (quanti == 0) {
            btnVediCarrello.text = "LISTA VUOTA"
            btnVediCarrello.setBackgroundColor(Color.parseColor("#9E9E9E"))
        } else {
            btnVediCarrello.text = "LISTA SCARICHI ($quanti)"
            btnVediCarrello.setBackgroundColor(Color.parseColor("#1976D2"))
        }
    }

    private fun scaricaDatiRete() {
        GestoreRete.scaricaArticoli(
            this,
            onSuccess = { lista ->
                runOnUiThread {
                    listaArticoliCompleta.clear()
                    listaArticoliCompleta.addAll(lista)
                    val scelta = spinNomeFile.selectedItem.toString()
                    if (scelta != "PROFILI (Modalità B)") applicaFiltroCategoria(scelta)
                }
            },
            onError = { dettaglioErrore ->
                runOnUiThread {
                    mostraMessaggioTablet(
                        "⚠️ ERRORE DI CONNESSIONE",
                        "Impossibile scaricare gli articoli dal server.\n\nDettaglio: $dettaglioErrore\n\nVerifica il Wi-Fi o controlla 'Registro Log' nei 3 puntini.",
                        isErrore = true
                    )
                }
            }
        )
    }

    private fun selezionaDaRicerca(artSel: Articolo) {
        autoSearch.setText(artSel.codice)
        autoSearch.setSelection(autoSearch.text.length)

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(autoSearch.windowToken, 0)

        lottoSelezionatoAttualmente = null
        etQuantita.setText("")
        aggiornaGiacenzaVisiva(artSel.codice)
        disegnaLottiInPagina(artSel)

        if (artSel.lotti.isEmpty()) etQuantita.requestFocus()
    }

    private fun aggiungiAlCarrelloLogica() {
        val testoCodice = autoSearch.text.toString().trim()
        val testoQta = etQuantita.text.toString().toDoubleOrNull() ?: 0.0

        if (testoCodice.isEmpty() || testoQta == 0.0) {
            mostraMessaggioTablet(
                "⚠️ DATI MANCANTI",
                "Seleziona prima un articolo valido e inserisci una quantità diversa da zero!",
                isErrore = true
            )
            return
        }

        val artTrovato = listaArticoliCompleta.find { it.codice.trim().equals(testoCodice, true) } ?: return

        if (artTrovato.lotti.isNotEmpty() && lottoSelezionatoAttualmente == null) {
            mostraMessaggioTablet(
                "⚠️ LOTTO MANCANTE",
                "Questo articolo ha gestione a lotti!\n\nSeleziona uno dei riquadri grigi con il lotto di prelievo prima di premere AGGIUNGI.",
                isErrore = true
            )
            return
        }

        val inCarrello = GestoreCarrello.getQta(testoCodice, lottoSelezionatoAttualmente?.nomeLotto ?: "")
        val dispReale = if (lottoSelezionatoAttualmente != null) lottoSelezionatoAttualmente!!.giacenzaReale else artTrovato.giacenzaReale

        val calcolo = dispReale - inCarrello
        val dispDaControllare = Math.round(calcolo * 1000.0) / 1000.0

        if (testoQta > 0.0 && testoQta > dispDaControllare) {
            val b = AlertDialog.Builder(this)
                .setTitle("⚠️ Disponibilità insufficiente")
                .setMessage("Giacenza attuale: $dispDaControllare ${artTrovato.um}.\nVuoi forzare lo scarico di $testoQta?")
                .setPositiveButton("SÌ, FORZA SCARICO") { _, _ ->
                    val nomeLotto = lottoSelezionatoAttualmente?.nomeLotto ?: ""
                    GestoreCarrello.aggiungi(ItemCarrello(testoCodice.uppercase(), artTrovato.descrizione, testoQta, artTrovato.um, dispDaControllare, nomeLotto), this)
                    GestoreLog.scrivi(this, "INFO", "Forzato scarico oltre giacenza: $testoCodice (Qta: $testoQta)")
                    aggiornaContatoreCarrello()
                    azzeraRicerca(apriTastiera = true)
                }
                .setNegativeButton("NO, ANNULLA", null)
            mostraDialogTablet(b)
        } else {
            val nomeLotto = lottoSelezionatoAttualmente?.nomeLotto ?: ""
            GestoreCarrello.aggiungi(ItemCarrello(testoCodice.uppercase(), artTrovato.descrizione, testoQta, artTrovato.um, dispDaControllare, nomeLotto), this)
            aggiornaContatoreCarrello()
            azzeraRicerca(apriTastiera = true)
        }
    }

    private fun inviaScaricoRete() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarInvio)
        progressBar.visibility = View.VISIBLE
        findViewById<Button>(R.id.btnInviaScarico).isEnabled = false

        GestoreRete.inviaScarichi(this, idTerminaleDinamico, GestoreCarrello.carrello,
            onSuccess = {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    findViewById<Button>(R.id.btnInviaScarico).isEnabled = true
                    GestoreCarrello.salvaStorico(this)
                    GestoreCarrello.svuota(this)
                    aggiornaContatoreCarrello()
                    azzeraRicerca()
                    scaricaDatiRete()
                    chiudiPaginaRiepilogo()
                    mostraMessaggioTablet(
                        "✅ INVIO COMPLETATO!",
                        "Tutti gli scarichi sono stati registrati con successo nel Database del Server.",
                        isErrore = false
                    )
                }
            },
            onError = { dettaglioErrore ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    findViewById<Button>(R.id.btnInviaScarico).isEnabled = true
                    mostraMessaggioTablet(
                        "⚠️ ERRORE INVIO AL DB",
                        "Si è verificato un errore durante la trasmissione.\n\nDettaglio: $dettaglioErrore\n\nI dati rimangono nel carrello: puoi riprovare appena la rete è attiva.",
                        isErrore = true
                    )
                }
            }
        )
    }

    private fun disegnaLottiInPagina(articolo: Articolo) {
        contenitoreLottiMain.removeAllViews()
        if (articolo.lotti.isEmpty()) {
            contenitoreLottiMain.visibility = View.GONE
            return
        }

        contenitoreLottiMain.visibility = View.VISIBLE
        val density = resources.displayMetrics.density

        contenitoreLottiMain.addView(TextView(this).apply {
            text = "👇 SELEZIONA IL LOTTO DI PRELIEVO:"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#D32F2F"))
            setPadding(0, (6 * density).toInt(), 0, (12 * density).toInt())
        })

        val listaBottoniLotto = mutableListOf<Button>()
        for (lotto in articolo.lotti) {
            val calcolo = lotto.giacenzaReale - GestoreCarrello.getQta(articolo.codice, lotto.nomeLotto)
            val dispEffettiva = Math.round(calcolo * 1000.0) / 1000.0

            if (dispEffettiva > 0) {
                val dispStr = if (dispEffettiva % 1.0 == 0.0) dispEffettiva.toInt().toString() else dispEffettiva.toString()
                val scaffaleStr = if (lotto.posizione.isNotEmpty()) lotto.posizione else "N/D"

                val btnLotto = Button(this).apply {
                    text = "🏷️ LOTTO: ${lotto.nomeLotto}\n📍 SCAFFALE: $scaffaleStr   |   🟢 DISP: $dispStr ${articolo.um}"
                    textSize = 21f
                    isAllCaps = false
                    setTypeface(null, Typeface.BOLD)
                    minHeight = (85 * density).toInt()
                    val padH = (16 * density).toInt()
                    val padV = (14 * density).toInt()
                    setPadding(padH, padV, padH, padV)
                    elevation = 4 * density

                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                        setMargins(0, 0, 0, (14 * density).toInt())
                    }

                    val isSelezionato = (lottoSelezionatoAttualmente?.nomeLotto == lotto.nomeLotto)
                    setBackgroundColor(if (isSelezionato) Color.parseColor("#2E7D32") else Color.parseColor("#CFD8DC"))
                    setTextColor(if (isSelezionato) Color.WHITE else Color.BLACK)
                }

                btnLotto.setOnClickListener {
                    lottoSelezionatoAttualmente = lotto
                    listaBottoniLotto.forEach { b ->
                        b.setBackgroundColor(Color.parseColor("#CFD8DC"))
                        b.setTextColor(Color.BLACK)
                    }
                    btnLotto.setBackgroundColor(Color.parseColor("#2E7D32"))
                    btnLotto.setTextColor(Color.WHITE)

                    etQuantita.requestFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(etQuantita, InputMethodManager.SHOW_IMPLICIT)
                }
                listaBottoniLotto.add(btnLotto)
                contenitoreLottiMain.addView(btnLotto)
            }
        }

        if (contenitoreLottiMain.childCount == 1) {
            contenitoreLottiMain.addView(TextView(this).apply {
                text = "⚠️ TUTTI I LOTTI RISULTANO ESAURITI LOCALMENTE."
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.RED)
            })
        }
    }

    private fun aggiornaGiacenzaVisiva(codiceSel: String) {
        val art = listaArticoliCompleta.find { it.codice.trim().equals(codiceSel.trim(), ignoreCase = true) }
        if (art == null) { txtGiacenzaA.text = ""; txtGiacenzaA.visibility = View.GONE; return }
        val calcolo = art.giacenzaReale - GestoreCarrello.getQta(codiceSel)
        val dispReale = Math.round(calcolo * 1000.0) / 1000.0
        val dispRealeStr = if (dispReale % 1.0 == 0.0) dispReale.toInt().toString() else dispReale.toString()
        txtGiacenzaA.text = "📝 ${art.descrizione}\n📦 Disp Globale: $dispRealeStr ${art.um}"
        txtGiacenzaA.visibility = View.VISIBLE
    }

    private fun apriPaginaRiepilogo() {
        if (GestoreCarrello.carrello.isEmpty()) return
        findViewById<View>(R.id.barraNavigazioneSingola).visibility = View.GONE
        findViewById<View>(R.id.layoutPrincipale).visibility = View.GONE
        findViewById<View>(R.id.layoutRiepilogo).visibility = View.VISIBLE
        aggiornaListaRiepilogo()
    }

    private fun chiudiPaginaRiepilogo() {
        findViewById<View>(R.id.barraNavigazioneSingola).visibility = View.VISIBLE
        findViewById<View>(R.id.layoutPrincipale).visibility = View.VISIBLE
        findViewById<View>(R.id.layoutRiepilogo).visibility = View.GONE
    }

    private fun aggiornaListaRiepilogo() {
        val contenitore = findViewById<LinearLayout>(R.id.contenitoreListaRiepilogo)
        contenitore.removeAllViews()

        for ((indice, item) in GestoreCarrello.carrello.withIndex()) {
            val qtaStr = if (item.qta % 1.0 == 0.0) item.qta.toInt().toString() else item.qta.toString()
            val testoLotto = if (item.lottoScelto.isNotEmpty()) " [Lotto: ${item.lottoScelto}]" else ""

            contenitore.addView(TextView(this).apply {
                text = "✏️ ${item.codice}$testoLotto\n   Qta: $qtaStr ${item.um}"
                textSize = 22f; setPadding(0, 30, 0, 30); setTextColor(Color.BLACK)
                setTypeface(null, Typeface.BOLD)
                setOnClickListener { mostraDialogModificaQuantita(indice, item) }
                setOnLongClickListener {
                    val b = AlertDialog.Builder(this@MainActivity)
                        .setTitle("🗑️ Eliminare?")
                        .setMessage("Rimuovere ${item.codice}?")
                        .setPositiveButton("SI") { _, _ ->
                            GestoreCarrello.rimuoviAt(indice, this@MainActivity)
                            aggiornaContatoreCarrello()
                            if (GestoreCarrello.carrello.isEmpty()) chiudiPaginaRiepilogo() else aggiornaListaRiepilogo()
                        }.setNegativeButton("NO", null)
                    mostraDialogTablet(b)
                    true
                }
            })
            contenitore.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 2); setBackgroundColor(Color.LTGRAY) })
        }
    }

    private fun mostraDialogModificaQuantita(indice: Int, item: ItemCarrello) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(40, 20, 40, 20) }
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            gravity = Gravity.CENTER; textSize = 30f; layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            setText(if (item.qta % 1.0 == 0.0) item.qta.toInt().toString() else item.qta.toString())
        }
        layout.addView(input)

        val maxModificabile = item.giacenza - (GestoreCarrello.getQta(item.codice, item.lottoScelto) - item.qta)

        val b = AlertDialog.Builder(this)
            .setTitle("Modifica Quantità")
            .setMessage("Giacenza max consentita: $maxModificabile")
            .setView(layout)
            .setPositiveButton("AGGIORNA") { _, _ ->
                val nuovaQta = input.text.toString().toDoubleOrNull() ?: 0.0
                if (nuovaQta != 0.0) {
                    if (nuovaQta > 0.0 && nuovaQta > maxModificabile) {
                        val subB = AlertDialog.Builder(this)
                            .setTitle("⚠️ Disponibilità insufficiente")
                            .setMessage("Giacenza max consentita: $maxModificabile.\nVuoi forzare a $nuovaQta?")
                            .setPositiveButton("SÌ, FORZA") { _, _ ->
                                GestoreCarrello.aggiornaItem(indice, ItemCarrello(item.codice, item.descrizione, nuovaQta, item.um, item.giacenza, item.lottoScelto), this)
                                aggiornaListaRiepilogo()
                            }.setNegativeButton("NO", null)
                        mostraDialogTablet(subB)
                    } else {
                        GestoreCarrello.aggiornaItem(indice, ItemCarrello(item.codice, item.descrizione, nuovaQta, item.um, item.giacenza, item.lottoScelto), this)
                        aggiornaListaRiepilogo()
                    }
                } else {
                    mostraMessaggioTablet("⚠️ QUANTITÀ NON VALIDA", "Inserisci un numero valido diverso da zero!", isErrore = true)
                }
            }.setNeutralButton("ELIMINA RIGA") { _, _ ->
                GestoreCarrello.rimuoviAt(indice, this)
                aggiornaContatoreCarrello()
                if (GestoreCarrello.carrello.isEmpty()) chiudiPaginaRiepilogo() else aggiornaListaRiepilogo()
            }
        mostraDialogTablet(b)
    }

    // ======================================================
    // LOGICA PROFILI: 4 COLONNE AD ALTA DENSITÀ VERTICALE
    // ======================================================
    private fun setModalitaProfili() {
        bloccoRicercaStandard.visibility = View.GONE
        bloccoProfili.visibility = View.VISIBLE
        contenitoreLottiMain.visibility = View.GONE
        popolaStep0Categorie()
    }

    private fun popolaStep0Categorie() {
        categoriaAttualeProfili = ""
        val contenitore = findViewById<LinearLayout>(R.id.contenitoreStepProfili)
        contenitore.removeAllViews()
        findViewById<LinearLayout>(R.id.barraPulsantiProfilo).visibility = View.GONE
        contenitore.addView(TextView(this).apply {
            text = "📂 SELEZIONA CATEGORIA"; textSize = 26f; setTypeface(null, Typeface.BOLD); setTextColor(Color.BLACK); setPadding(0, 20, 0, 30); gravity = Gravity.CENTER
        })

        val righeCategorie = listOf("Tende da Sole", "Screeny", "Tende a Rullo", "Binari", "Mantovane").chunked(2)
        val hPx = (150 * resources.displayMetrics.density).toInt()

        for (riga in righeCategorie) {
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 8, 0, 8) }; weightSum = 2f }
            for (cat in riga) {
                rowLayout.addView(Button(this).apply {
                    text = cat; textSize = 21f; setTypeface(null, Typeface.BOLD); setBackgroundColor(Color.parseColor("#263238")); setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(0, hPx, 1f).apply { setMargins(8, 0, 8, 0) }
                    setOnClickListener { popolaStep1Profili(cat) }
                })
            }
            for (i in 0 until (2 - riga.size)) rowLayout.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, hPx, 1f).apply { setMargins(8, 0, 8, 0) } })
            contenitore.addView(rowLayout)
        }
    }

    // STEP 1: ORDINAMENTO DA FOGLIO + 4 COLONNE COMPATTE + LEDA DOPPIO
    private fun popolaStep1Profili(categoriaScelta: String) {
        categoriaAttualeProfili = categoriaScelta
        val contenitore = findViewById<LinearLayout>(R.id.contenitoreStepProfili)
        contenitore.removeAllViews()
        findViewById<LinearLayout>(R.id.barraPulsantiProfilo).visibility = View.VISIBLE
        findViewById<Button>(R.id.btnIndietroProfilo).setOnClickListener { popolaStep0Categorie() }
        findViewById<Button>(R.id.btnHomeProfilo).setOnClickListener { popolaStep0Categorie() }

        val mappaOrdine = when (categoriaScelta.lowercase().trim()) {
            "tende da sole", "tende da esterno" -> ordineTendeDaSole
            "tende a rullo" -> ordineTendeARullo
            else -> emptyMap()
        }

        val profiliBase = listaArticoliCompleta
            .filter { art ->
                val radice = art.codice.split("|")[0].trim().uppercase()
                val matchCategoria = art.categoria.equals(categoriaScelta, ignoreCase = true) ||
                        (radice == "LEDA" && (categoriaScelta.equals("Binari", ignoreCase = true) || categoriaScelta.equals("Tende a Rullo", ignoreCase = true)))
                matchCategoria && isProfiloValido(art.codice)
            }
            .map { it.codice.split("|")[0].trim() }
            .distinct()
            .sortedWith(compareBy({ mappaOrdine[it.uppercase().trim()] ?: 999 }, { it }))

        val density = resources.displayMetrics.density

        // GRIGLIA A 4 COLONNE COMPATTA
        for (riga in profiliBase.chunked(4)) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                    setMargins(0, (2 * density).toInt(), 0, (2 * density).toInt())
                }
                weightSum = 4f
            }
            for (p in riga) {
                val viewCard = layoutInflater.inflate(R.layout.layout_card_profilo, rowLayout, false)
                viewCard.findViewById<TextView>(R.id.txtCodiceProfilo).apply {
                    text = p
                    textSize = 20f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.BLACK)
                }
                val img = viewCard.findViewById<ImageView>(R.id.imgProfilo)

                val idImg = resources.getIdentifier(p.lowercase().replace("-", "_"), "drawable", packageName)
                if (idImg != 0) img.setImageResource(idImg) else img.setImageResource(android.R.drawable.ic_menu_gallery)

                val padDp = scalaProfiliMap[p.uppercase().trim()] ?: 0
                val padPx = (padDp * density).toInt()
                img.setPadding(padPx, padPx, padPx, padPx)

                viewCard.layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                    setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                }
                viewCard.setOnClickListener { popolaStep2Colori(p, if (idImg != 0) idImg else 0, padDp) }
                rowLayout.addView(viewCard)
            }
            for (i in 0 until (4 - riga.size)) {
                rowLayout.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                        setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                    }
                })
            }
            contenitore.addView(rowLayout)
        }
    }

    private fun popolaStep2Colori(profiloScelto: String, idImmaginePadre: Int, padDpPadre: Int) {
        val colori = listaArticoliCompleta
            .filter { it.codice.startsWith("$profiloScelto|") && isProfiloValido(it.codice) }
            .map { val p = it.codice.split("|"); if (p.size > 1) p[1] else "" }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

        if (colori.size == 1) {
            val unicoColore = colori[0]
            profColoreSel = unicoColore
            popolaStep3Misure(profiloScelto, unicoColore, idImmaginePadre, padDpPadre, coloreSaltato = true)
            return
        }

        val contenitore = findViewById<LinearLayout>(R.id.contenitoreStepProfili)
        contenitore.removeAllViews()
        findViewById<Button>(R.id.btnIndietroProfilo).setOnClickListener {
            popolaStep1Profili(if (categoriaAttualeProfili.isNotEmpty()) categoriaAttualeProfili else "Tende da Sole")
        }

        val density = resources.displayMetrics.density

        if (idImmaginePadre != 0) {
            contenitore.addView(ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(-1, (160 * density).toInt()).apply { setMargins(0, 10, 0, 10) }
                scaleType = ImageView.ScaleType.FIT_CENTER
                val padPx = (padDpPadre * density).toInt()
                setPadding(padPx, padPx, padPx, padPx)
                setImageResource(idImmaginePadre)
            })
        }
        contenitore.addView(TextView(this).apply {
            text = "PROFILO: $profiloScelto"; textSize = 26f; setTypeface(null, Typeface.BOLD); setTextColor(Color.BLACK); gravity = Gravity.CENTER; setPadding(0, 6, 0, 20)
        })

        val hPx = (95 * density).toInt()

        for (riga in colori.chunked(3)) {
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 8, 0, 8) }; weightSum = 3f }
            for (colore in riga) {
                rowLayout.addView(Button(this).apply {
                    text = colore; textSize = 21f; setTypeface(null, Typeface.BOLD); setBackgroundColor(Color.parseColor("#263238")); setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(0, hPx, 1f).apply { setMargins(8, 0, 8, 0) }
                    setOnClickListener {
                        profColoreSel = colore
                        popolaStep3Misure(profiloScelto, colore, idImmaginePadre, padDpPadre, coloreSaltato = false)
                    }
                })
            }
            for (i in 0 until (3 - riga.size)) rowLayout.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, hPx, 1f).apply { setMargins(8, 0, 8, 0) } })
            contenitore.addView(rowLayout)
        }
    }

    private fun popolaStep3Misure(
        profilo: String,
        colore: String,
        idImmaginePadre: Int,
        padDpPadre: Int,
        coloreSaltato: Boolean = false
    ) {
        val misure = listaArticoliCompleta
            .filter { it.codice.startsWith("$profilo|$colore|") && isProfiloValido(it.codice) }
            .map { val p = it.codice.split("|"); if (p.size > 2) p[2] else "" }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

        if (misure.size == 1) {
            val unicaMisura = misure[0]
            profMisuraSel = unicaMisura
            mostraDialogInserimentoQuantita(profilo, colore, unicaMisura, onAnnulla = {
                if (coloreSaltato) {
                    popolaStep1Profili(if (categoriaAttualeProfili.isNotEmpty()) categoriaAttualeProfili else "Tende da Sole")
                } else {
                    popolaStep2Colori(profilo, idImmaginePadre, padDpPadre)
                }
            })
            return
        }

        val contenitore = findViewById<LinearLayout>(R.id.contenitoreStepProfili)
        contenitore.removeAllViews()
        findViewById<Button>(R.id.btnIndietroProfilo).setOnClickListener {
            if (coloreSaltato) {
                popolaStep1Profili(if (categoriaAttualeProfili.isNotEmpty()) categoriaAttualeProfili else "Tende da Sole")
            } else {
                popolaStep2Colori(profilo, idImmaginePadre, padDpPadre)
            }
        }
        contenitore.addView(TextView(this).apply {
            text = "Configurazione: $profilo | $colore"; textSize = 24f; setTypeface(null, Typeface.BOLD); setTextColor(Color.BLACK); setPadding(0, 10, 0, 20); gravity = Gravity.CENTER
        })

        val hPx = (95 * resources.displayMetrics.density).toInt()

        for (riga in misure.chunked(3)) {
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 8, 0, 8) }; weightSum = 3f }
            for (misura in riga) {
                rowLayout.addView(Button(this).apply {
                    val art = listaArticoliCompleta.find { it.codice.trim().equals("$profilo|$colore|$misura", true) }
                    val calcolo = (art?.giacenzaReale ?: 0.0) - GestoreCarrello.getQta("$profilo|$colore|$misura")
                    val giacVisualizzata = Math.round(calcolo * 1000.0) / 1000.0
                    text = "$misura\nGIAC: ${if (giacVisualizzata % 1.0 == 0.0) giacVisualizzata.toInt() else giacVisualizzata}"
                    textSize = 21f; setTypeface(null, Typeface.BOLD); setBackgroundColor(Color.parseColor("#263238")); setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(0, hPx, 1f).apply { setMargins(8, 0, 8, 0) }
                    setOnClickListener {
                        profMisuraSel = misura
                        mostraDialogInserimentoQuantita(profilo, colore, misura)
                    }
                })
            }
            for (i in 0 until (3 - riga.size)) rowLayout.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, hPx, 1f).apply { setMargins(8, 0, 8, 0) } })
            contenitore.addView(rowLayout)
        }
    }

    private fun mostraDialogInserimentoQuantita(
        profilo: String,
        colore: String,
        misura: String,
        onAnnulla: (() -> Unit)? = null
    ) {
        val cod = "$profilo|$colore|$misura"
        val artTrovato = listaArticoliCompleta.find { it.codice.trim().equals(cod, true) }
        val giacDb = artTrovato?.giacenzaReale ?: 0.0
        val calcolo = giacDb - GestoreCarrello.getQta(cod)
        val giacenzaDisp = Math.round(calcolo * 1000.0) / 1000.0

        val density = resources.displayMetrics.density
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            padding(24, 20, 24, 20)
        }

        val tvCodice = TextView(this).apply {
            text = "📦 SCARICO:\n$cod"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            // Errore risolto: lineHeight rimosso
            setPadding(0, 0, 0, (12 * density).toInt())
        }
        layout.addView(tvCodice)

        val txtGiacenza = TextView(this).apply {
            text = "Giacenza residua: $giacenzaDisp ${artTrovato?.um ?: "pz"}"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#455A64"))
            setPadding(0, 0, 0, (18 * density).toInt())
        }
        layout.addView(txtGiacenza)

        val stepperLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btnMeno = Button(this).apply {
            text = "−"
            textSize = 34f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams((84 * density).toInt(), (68 * density).toInt())
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            setText("1")
            gravity = Gravity.CENTER
            textSize = 32f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(16, 0, 16, 0)
            }
        }

        val btnPiu = Button(this).apply {
            text = "+"
            textSize = 34f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams((84 * density).toInt(), (68 * density).toInt())
        }

        btnMeno.setOnClickListener {
            val curr = input.text.toString().toDoubleOrNull() ?: 1.0
            val nuovo = if (curr > 1.0) curr - 1.0 else 0.0
            input.setText(if (nuovo % 1.0 == 0.0) nuovo.toInt().toString() else nuovo.toString())
        }

        btnPiu.setOnClickListener {
            val curr = input.text.toString().toDoubleOrNull() ?: 0.0
            val nuovo = curr + 1.0
            input.setText(if (nuovo % 1.0 == 0.0) nuovo.toInt().toString() else nuovo.toString())
        }

        stepperLayout.addView(btnMeno)
        stepperLayout.addView(input)
        stepperLayout.addView(btnPiu)
        layout.addView(stepperLayout)

        val b = AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("AGGIUNGI") { _, _ ->
                val qta = input.text.toString().toDoubleOrNull() ?: 0.0
                if (qta != 0.0) {
                    if (qta > 0.0 && qta > giacenzaDisp) {
                        val subB = AlertDialog.Builder(this)
                            .setTitle("⚠️ Disponibilità insufficiente")
                            .setMessage("Giacenza residua: $giacenzaDisp.\nVuoi forzare lo scarico di $qta?")
                            .setPositiveButton("SÌ, FORZA") { _, _ ->
                                GestoreCarrello.aggiungi(ItemCarrello(cod, artTrovato?.descrizione ?: "", qta, artTrovato?.um ?: "pz", giacDb, ""), this)
                                GestoreLog.scrivi(this, "INFO", "Forzato scarico profilo: $cod (Qta: $qta)")
                                aggiornaContatoreCarrello()
                                popolaStep1Profili(if (categoriaAttualeProfili.isNotEmpty()) categoriaAttualeProfili else "Tende da Sole")
                            }.setNegativeButton("NO", null)
                        mostraDialogTablet(subB)
                    } else {
                        GestoreCarrello.aggiungi(ItemCarrello(cod, artTrovato?.descrizione ?: "", qta, artTrovato?.um ?: "pz", giacDb, ""), this)
                        aggiornaContatoreCarrello()
                        popolaStep1Profili(if (categoriaAttualeProfili.isNotEmpty()) categoriaAttualeProfili else "Tende da Sole")
                    }
                }
            }
            .setNegativeButton("ANNULLA") { _, _ ->
                onAnnulla?.invoke()
            }
            .setOnCancelListener {
                onAnnulla?.invoke()
            }

        mostraDialogTablet(b)
    }

    private fun LinearLayout.padding(l: Int, t: Int, r: Int, b: Int) {
        val d = resources.displayMetrics.density
        setPadding((l * d).toInt(), (t * d).toInt(), (r * d).toInt(), (b * d).toInt())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val s1 = SpannableString("📜  Storico Scarichi")
        s1.setSpan(AbsoluteSizeSpan(22, true), 0, s1.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        s1.setSpan(StyleSpan(Typeface.BOLD), 0, s1.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        menu?.add(0, 1, 0, s1)

        val s2 = SpannableString("⚙️  Impostazioni Reparto")
        s2.setSpan(AbsoluteSizeSpan(22, true), 0, s2.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        s2.setSpan(StyleSpan(Typeface.BOLD), 0, s2.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        menu?.add(0, 2, 0, s2)

        val s3 = SpannableString("🛠️  Personalizzazione Ricerca")
        s3.setSpan(AbsoluteSizeSpan(22, true), 0, s3.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        s3.setSpan(StyleSpan(Typeface.BOLD), 0, s3.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        menu?.add(0, 3, 0, s3)

        val s4 = SpannableString("📋  Registro Log & Errori")
        s4.setSpan(AbsoluteSizeSpan(22, true), 0, s4.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        s4.setSpan(StyleSpan(Typeface.BOLD), 0, s4.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        menu?.add(0, 4, 0, s4)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                val jsonArray = JSONArray(getSharedPreferences("MemoriaMagazzino", MODE_PRIVATE).getString("storico_invii", "[]") ?: "[]")
                val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40) }
                if (jsonArray.length() == 0) {
                    layout.addView(TextView(this).apply { text = "Nessun invio recente."; textSize = 22f; gravity = Gravity.CENTER; setTextColor(Color.BLACK); setTypeface(null, Typeface.BOLD) })
                } else {
                    for (i in jsonArray.length() - 1 downTo 0) {
                        val obj = jsonArray.getJSONObject(i)
                        layout.addView(TextView(this).apply { text = "${obj.getString("data")} | ${obj.getString("codice")}\nQta: ${obj.getDouble("qta")}"; textSize = 21f; setPadding(0, 0, 0, 24); setTextColor(Color.BLACK); setTypeface(null, Typeface.BOLD) })
                        layout.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 2).apply { setMargins(0, 0, 0, 24) }; setBackgroundColor(Color.LTGRAY) })
                    }
                }
                val b = AlertDialog.Builder(this)
                    .setTitle("📜 ULTIMI INVII LOCALI")
                    .setView(ScrollView(this).apply { addView(layout) })
                    .setPositiveButton("CHIUDI", null)
                mostraDialogTablet(b)
                return true
            }
            2 -> { mostraDialogImpostazioni(); return true }
            3 -> { mostraDialogPersonalizzazione(); return true }
            4 -> { mostraDialogRegistroLog(); return true }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun mostraDialogRegistroLog() {
        val logs = GestoreLog.leggi(this)
        val density = resources.displayMetrics.density

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        if (logs.isEmpty()) {
            layout.addView(TextView(this).apply {
                text = "Nessun errore o log registrato."
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setPadding(0, (30 * density).toInt(), 0, (30 * density).toInt())
            })
        } else {
            for (log in logs) {
                val data = log.optString("data", "")
                val livello = log.optString("livello", "INFO")
                val msg = log.optString("msg", "")

                val tvLivello = TextView(this).apply {
                    text = "[$data] $livello"
                    textSize = 17f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(
                        when (livello) {
                            "ERRORE" -> Color.parseColor("#D32F2F")
                            "RETE" -> Color.parseColor("#E65100")
                            else -> Color.parseColor("#1976D2")
                        }
                    )
                }

                val tvMsg = TextView(this).apply {
                    text = msg
                    textSize = 20f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.BLACK)
                    setPadding(0, (4 * density).toInt(), 0, (14 * density).toInt())
                }

                val separatore = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(-1, 2).apply {
                        setMargins(0, 0, 0, (14 * density).toInt())
                    }
                    setBackgroundColor(Color.LTGRAY)
                }

                layout.addView(tvLivello)
                layout.addView(tvMsg)
                layout.addView(separatore)
            }
        }

        val b = AlertDialog.Builder(this)
            .setTitle("📋 REGISTRO EVENTI & ERRORI")
            .setView(ScrollView(this).apply { addView(layout) })
            .setPositiveButton("CHIUDI", null)
            .setNeutralButton("CANCELLA LOG") { _, _ ->
                GestoreLog.svuota(this)
                Toast.makeText(this, "Log cancellati!", Toast.LENGTH_SHORT).show()
            }

        mostraDialogTablet(b)
    }

    private fun mostraDialogPersonalizzazione() {
        val sp = getSharedPreferences("ImpostazioniApp", MODE_PRIVATE)
        val density = resources.displayMetrics.density

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            padding(24, 20, 24, 20)
        }

        val cbPrefissi = CheckBox(this).apply {
            text = "Autocompila prefissi (RU-, 88-, ecc.)"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            isChecked = sp.getBoolean("pref_autocompila_prefissi", true)
            setPadding((12 * density).toInt(), (14 * density).toInt(), 0, (14 * density).toInt())
        }

        val cbTastiera = CheckBox(this).apply {
            text = "Tastiera solo numerica in Accessori\n(Tende a Rullo e Sole)"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            isChecked = sp.getBoolean("pref_tastiera_numerica_accessori", false)
            setPadding((12 * density).toInt(), (14 * density).toInt(), 0, (14 * density).toInt())
        }

        layout.addView(cbPrefissi)
        layout.addView(cbTastiera)

        val b = AlertDialog.Builder(this)
            .setTitle("🛠️ Personalizzazione")
            .setView(layout)
            .setPositiveButton("SALVA") { _, _ ->
                sp.edit()
                    .putBoolean("pref_autocompila_prefissi", cbPrefissi.isChecked)
                    .putBoolean("pref_tastiera_numerica_accessori", cbTastiera.isChecked)
                    .apply()

                val scelta = spinNomeFile.selectedItem.toString()
                if (scelta != "PROFILI (Modalità B)") {
                    applicaFiltroCategoria(scelta)
                }
            }
            .setNegativeButton("ANNULLA", null)

        mostraDialogTablet(b)
    }

    private fun mostraDialogImpostazioni() {
        val sp = getSharedPreferences("ImpostazioniApp", MODE_PRIVATE)
        val attuale = sp.getString("categoria_default", "TUTTE LE CATEGORIE")
        val index = arrayFamiglie.indexOf(attuale).takeIf { it >= 0 } ?: 0
        var sceltaTemporanea = attuale ?: "TUTTE LE CATEGORIE"

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, arrayFamiglie) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as CheckedTextView
                v.textSize = 22f
                v.setTypeface(null, Typeface.BOLD)
                val pad = (18 * resources.displayMetrics.density).toInt()
                v.setPadding(pad, pad, pad, pad)
                v.setTextColor(Color.BLACK)
                return v
            }
        }

        val b = AlertDialog.Builder(this)
            .setTitle("⚙️ Configura Tablet / Telefono")
            .setSingleChoiceItems(adapter, index) { _, which -> sceltaTemporanea = arrayFamiglie[which] }
            .setPositiveButton("SALVA") { _, _ ->
                sp.edit().putString("categoria_default", sceltaTemporanea).apply()
                aggiornaNomeTerminale(sceltaTemporanea)
                val pos = arrayFamiglie.indexOf(sceltaTemporanea)
                if (pos >= 0) spinNomeFile.setSelection(pos)
                mostraMessaggioTablet("✅ CONFIGURAZIONE SALVATA", "Terminale impostato su:\n$idTerminaleDinamico")
            }
            .setNegativeButton("ANNULLA", null)
        mostraDialogTablet(b)
    }
}