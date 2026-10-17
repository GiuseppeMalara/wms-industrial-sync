<?php
header('Content-Type: text/plain; charset=utf-8');
require_once __DIR__ . '/db.php';

// Cartella di esportazione relativa
$cartella_export = __DIR__ . '/export_arca/';
if (!is_dir($cartella_export)) {
    mkdir($cartella_export, 0775, true);
}

$nome_file = 'scarichi_magazzino_' . date('Ymd_His') . '.txt';
$percorso_file = $cartella_export . $nome_file;

try {
    $stmt = $pdo->query("SELECT id, codice, qta, lotto FROM scarichi WHERE esportato = 0 ORDER BY id ASC");
    $scarichi = $stmt->fetchAll();

    if (count($scarichi) === 0) {
        echo "Nessun nuovo scarico da esportare.";
        exit();
    }

    $file = fopen($percorso_file, 'w');
    $ids_da_aggiornare = [];

    foreach ($scarichi as $riga) {
        $codice = $riga['codice'];
        $qta    = str_replace('.', ',', (string)$riga['qta']);
        $lotto  = $riga['lotto'] ?? '';

        fwrite($file, "{$codice};{$qta};{$lotto}\r\n");
        $ids_da_aggiornare[] = $riga['id'];
    }
    fclose($file);

    // Marca i record come esportati
    $placeholders = implode(',', array_fill(0, count($ids_da_aggiornare), '?'));
    $stmtUpdate = $pdo->prepare("UPDATE scarichi SET esportato = 1 WHERE id IN ($placeholders)");
    $stmtUpdate->execute($ids_da_aggiornare);

    echo "Successo! Creato il file {$nome_file} con " . count($scarichi) . " record.";

} catch (Exception $e) {
    http_response_code(500);
    echo "Errore durante la creazione del file: " . $e->getMessage();
}