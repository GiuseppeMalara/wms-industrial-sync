# Industrial WMS & ERP Sync Engine

Sistema WMS (Warehouse Management System) proprietario end-to-end per la tracciabilità delle movimentazioni dei materiali su linee di produzione industriali e sincronizzazione asincrona verso l'ERP aziendale (Arca Evolution).

Il sistema sostituisce la rendicontazione manuale cartacea a fine giornata su tre postazioni produttive specializzate (Tende da Sole, Tende a Rullo, Reparto Lavorazione Alluminio/Profili), azzerando le discrepanze tra stock fisico e contabile.

---

## Architettura del Sistema

L'infrastruttura si basa su un'architettura a tre livelli con disaccoppiamento del database ERP centrale per garantire continuità operativa a bordo macchina anche in caso di picchi di carico o assenza temporanea di rete:

```text
[ Terminali Touch Industriali Android ]
  ├── Tende a Rullo (ID: Telefono_Rullo)
  ├── Sistemi per Tende (ID: Telefono_Binari)
  ├── Tende da Sole (ID: Telefono_Sole)
  └── Reparto Alluminio (ID: Telefono_Profili)
           │
           │ (HTTP REST / JSON / Cache Locale)
           ▼
[ Middleware Locale On-Premise (PHP 8.x / MySQL) ]
  ├── api_articoli.php       ─> Catalogo dinamico con mappatura lotti e stock
  ├── api_scarico.php      ─> Transazioni atomiche di prelievo con rollback ACID
  ├── importa_erp.php       ─> Batch sync mattutino con filtri blacklist
  └── Dashboard Gestionale   ─> Modifica, storno matematico e protezione CSRF
           │
           │ (Pipeline Asincrona Hot-Folder CSV)
           ▼
[ ERP Centrale (Arca Evolution) ] ──> Generazione massiva documenti fiscali SC
```

---

## Modulo Android (`/android`)

Applicazione client nativa sviluppata in **Kotlin**, progettata per l'uso su tablet industriali con interfaccia ad alto contrasto, pulsanti a bersaglio allargato e navigazione touch ottimizzata per l'ambiente di fabbrica.

### Caratteristiche Principali
* **Configuratore Guidato Profili in Alluminio:**
  * Workflow progressivo a griglia: *Categoria -> Profilo -> Colore -> Misura di Taglio*.
  * Gestione grafica delle sagome con ridimensionamento proporzionale dinamico e bypass automatico dei passaggi a variante univoca.
  * Ordinamento delle matrici allineato alle priorità operative delle schede di montaggio.
* **Gestione Lotti e Scaffalature Tessuti:**
  * Riconoscimento automatico degli articoli gestiti a pezza/lotto.
  * Visualizzazione in tempo reale della giacenza residua per singolo lotto e indicazione visiva della coordinata di stoccaggio (*es. Scaffale A-12*).
  * Controllo di capienza con blocco dei lotti esauriti e possibilità di forzatura manuale per prelievi urgenti fuori soglia.
* **Ergonomia Operativa di Reparto:**
  * Commutazione automatica della tastiera (tastiera numerica rapida per minuteria e accessori codificati).
  * Autocompilazione dei prefissi di famiglia (*RU-*, *88-*, *BN-*).
  * Ricerca rapida con filtro concorrente su codice articolo e descrizione.
* **Resilienza Offline e Storage Locale:**
  * Persistenza del carrello prelievi su `SharedPreferences` con serializzazione JSON per azzerare il rischio di perdita dati in caso di riavvio improvviso del tablet.
  * Storico transazioni locale a scorrimento (ultimi 150 movimenti memorizzati).
  * Modulo diagnostico autonomo (`GestoreLog.kt`) per il tracciamento interno di errori HTTP e timeout di rete.

> **Nota di Riservatezza sugli Asset Grafici:** I file grafici e i vettori delle sagome tecniche dei profili sono stati esclusi dal repository pubblico a tutela della proprietà intellettuale aziendale. Il layout sfrutta un meccanismo di fallback automatico (`resources.getIdentifier`) che carica icone neutre di sistema in assenza del set grafico proprietario.

---

## Modulo Backend & Middleware (`/backend`)

Middleware sviluppato in **PHP 8.x** con astrazione **PDO** e transazioni atomiche per garantire l'integrità dei dati contabili.

### Caratteristiche Principali
* **Transazioni Atomiche ACID (`api_scarico.php`):**
  Ogni operazione di scarico eseguita dai tablet esegue all'interno di una transazione SQL unica:
  1. Detrazione dalla giacenza generale dell'articolo (`articoli.giacenza_reale`).
  2. Detrazione specifica dalla pezza selezionata (`lotti_tessuti.giacenza_reale`).
  3. Scrittura del record contabile nello storico (`scarichi`) con stato `esportato = 0` e identificativo del reparto.
  In caso di errore su uno qualsiasi dei passaggi viene eseguito il rollback immediato evitando record orfani o disallineamenti di stock.
* **Algoritmo di Storno e Rettifica (`modifica_scarico.php`):**
  Permette la revisione pre-invio da parte del supervisore. La modifica o l'eliminazione di un movimento pendente calcola il delta di differenza (Delta = Quantità Nuova - Quantità Vecchia) e riallinea istantaneamente le giacenze reali di magazzino e dei relativi lotti.
* **Sicurezza:**
  * Protezione integrale contro attacchi CSRF tramite token di sessione univoci (`hash_equals`).
  * Autenticazione con hashing Bcrypt (`password_verify`).
  * Query interamente parametrizzate con prepared statements contro SQL Injection.

---

## Pipeline di Integrazione ERP

L'interscambio con Arca Evolution avviene in modalità asincrona batch senza interferire sulle tabelle transazionali del gestionale:

1. **Sincronizzazione Mattutina Catalogo (`importa_erp.php`):**
   * Elaborazione automatica dell'export anagrafico generato dall'ERP.
   * Filtro dinamico (blacklist) per escludere codici virtuali o spese accessorie (`MANODOPERA`, `SERVIZIO`, `ORD-DIR`, `TRASPORTO`).
   * Upsert massivo su database locale tramite `INSERT INTO ... ON DUPLICATE KEY UPDATE`.
2. **Esportazione Scarichi di Produzione (`export_erp.php`):**
   * Estrazione incrementale dei movimenti con flag `esportato = 0`.
   * Normalizzazione dei dati e generazione del file tracciato CSV in cartella condivisa di rete.
   * Marcatura atomica dei record elaborati (`esportato = 1`) e importazione sul gestionale con creazione del documento di scarico fiscale (**SC**).

---

## Stack Tecnologico

| Layer | Tecnologie |
| :--- | :--- |
| **Mobile Client** | Kotlin, Android SDK, ViewBinding, SharedPreferences (JSON), HttpURLConnection |
| **Backend API** | PHP 8.x, PDO, Prepared Statements, Session Security, CSRF Tokens |
| **Database** | MySQL / MariaDB (Engine InnoDB, Transazioni ACID, Indici B-Tree) |
| **Pannello Web** | Bootstrap 5, DataTables.js, jQuery |
| **Target ERP** | Wolters Kluwer Arca Evolution |
