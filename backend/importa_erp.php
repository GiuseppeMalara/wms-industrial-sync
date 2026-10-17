<?php
header('Content-Type: text/html; charset=utf-8');
require_once __DIR__ . '/db.php';

// Percorso relativo alla cartella di importazione
$cartella_import = __DIR__ . '/import/';
$percorso_file   =$cartella_import . 'mega_export_arca.csv';

$prefissi_da_ignorare = ["ORD-DIR", "SERVIZIO", "TRASPORTO", "MANODOPERA"];

if (!file_exists($percorso_file)) {
    die("<h2>⚠️ Errore: File ERP non trovato in: " . htmlspecialchars($percorso_file) . "</h2>");
}

try {
    $pdo->beginTransaction();

    // 1. Pulizia mattutina: azzera giacenze e svuota la tabella lotti temporanea
    $pdo->exec("UPDATE articoli SET giacenza = 0, giacenza_reale = 0");
    $pdo->exec("TRUNCATE TABLE lotti_tessuti");

    // Query preparate: corretto il nome campo su `nome_lotto`
    $stmt_articolo =$pdo->prepare("
        INSERT INTO articoli (codice, descrizione, giacenza, giacenza_reale, um_default)
        VALUES (?, ?, ?, ?, 'pz')
        ON DUPLICATE KEY UPDATE descrizione = VALUES(descrizione), giacenza = VALUES(giacenza), giacenza_reale = VALUES(giacenza_reale)
    ");
    $stmt_lotto =$pdo->prepare("INSERT INTO lotti_tessuti (codice_articolo, nome_lotto, giacenza_reale) VALUES (?, ?, ?)");

    $file = fopen($percorso_file, "r");
    $righe_importate = 0;
    $righe_saltate = 0;

    while (($riga = fgetcsv($file, 1000, ";")) !== false) {
        $codice = trim($riga[0] ?? '');
        if ($codice === '') continue;

        // Filtro Blacklist
        $salta = false;
        foreach ($prefissi_da_ignorare as$prefisso) {
            if (str_starts_with(strtoupper($codice), $prefisso)) {$salta = true;
                break;
            }
        }
        if ($salta) {$righe_saltate++;
            continue;
        }

        $descrizione  = trim($riga[1] ?? '');
        $lotto        = trim($riga[2] ?? '');
        $giacenza_str = str_replace(',', '.', trim($riga[3] ?? '0'));
        $giacenza     = (float)$giacenza_str;

        // Upsert Articolo
        $stmt_articolo->execute([$codice,$descrizione, $giacenza,$giacenza]);

        // Inserimento Lotto (se presente nella riga)
        if ($lotto !== '') {
            $stmt_lotto->execute([$codice, $lotto,$giacenza]);
        }

        $righe_importate++;
    }

    fclose($file);$pdo->commit();

    echo "<h2>✅ Importazione Completata!</h2>";
    echo "<p>Articoli e Lotti elaborati: <b>$righe_importate</b></p>";
    if ($righe_saltate > 0) {
        echo "<p style='color: gray;'>Articoli ignorati (Blacklist): <b>$righe_saltate</b></p>";
    }

} catch (Exception $e) {
    if ($pdo->inTransaction()) {$pdo->rollBack();
    }
    die("Errore critico durante l'importazione: " . $e->getMessage());
}