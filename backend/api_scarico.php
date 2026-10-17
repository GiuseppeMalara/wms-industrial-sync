<?php
header('Content-Type: text/plain; charset=utf-8');
require_once __DIR__ . '/db.php';

$codice      = trim($_POST['codice'] ?? '');
$descrizione = trim($_POST['descrizione'] ?? '');
$qta         = (float)($_POST['qta'] ?? 0);
$um          = trim($_POST['um'] ?? 'pz');
$lotto       = trim($_POST['lotto'] ?? '');
$reparto     = trim($_POST['reparto'] ?? 'Terminale_Generico');

if ($codice === '' || $qta <= 0) {
    http_response_code(400);
    echo "ERRORE_PARAMETRI";
    exit();
}

try {
    $pdo->beginTransaction();

    // 1. Aggiorna giacenza reale dell'articolo
    $stmt1 = $pdo->prepare("UPDATE articoli SET giacenza_reale = ROUND(giacenza_reale - ?, 2) WHERE codice = ?");
    $stmt1->execute([$qta, $codice]);

    // 2. Se è specificato un lotto, aggiorna la disponibilità specifica del tessuto
    if ($lotto !== '') {
        $stmt2 = $pdo->prepare("UPDATE lotti_tessuti SET giacenza_reale = ROUND(giacenza_reale - ?, 2) WHERE codice_articolo = ? AND nome_lotto = ?");
        $stmt2->execute([$qta, $codice, $lotto]);
    }

    // 3. Registra il movimento contabile nello storico scarichi
    $stmt3 = $pdo->prepare("
        INSERT INTO scarichi (codice, descrizione, qta, um, lotto, reparto_lotto, data_inserimento, esportato)
        VALUES (?, ?, ?, ?, ?, ?, NOW(), 0)
    ");
    $stmt3->execute([$codice, $descrizione, $qta, $um, $lotto, $reparto]);

    $pdo->commit();
    echo "OK";

} catch (Exception $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    http_response_code(500);
    echo "ERRORE";
}