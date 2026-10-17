<?php
header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/db.php';

try {
    // 1. Estrazione dei lotti disponibili con giacenza positiva
    $stmtLotti = $pdo->query("SELECT codice_articolo, nome_lotto, posizione, giacenza_reale FROM lotti_tessuti WHERE giacenza_reale > 0 ORDER BY nome_lotto ASC");
    $mappa_lotti = [];
    while ($l = $stmtLotti->fetch()) {
        $mappa_lotti[$l['codice_articolo']][] = [
            "nome_lotto"     => $l['nome_lotto'],
            "posizione"      => $l['posizione'] ?? '',
            "giacenza_reale" => round((float)$l['giacenza_reale'], 2)
        ];
    }

    // 2. Estrazione anagrafica articoli
    $stmtArt = $pdo->query("SELECT * FROM articoli ORDER BY codice ASC");
    $articoli = [];

    while ($row = $stmtArt->fetch()) {
        $codice = $row['codice'];
        $articoli[] = [
            "codice"            => $codice,
            "descrizione"       => $row['descrizione'] ?? '',
            "um_default"        => $row['um_default'] ?? 'pz',
            "reparto_suggerito" => $row['reparto_suggerito'] ?? '',
            "categoria_profilo" => $row['categoria_profilo'] ?? '',
            "gruppo"            => $row['gruppo'] ?? '',
            "sottogruppo"       => $row['sottogruppo'] ?? '',
            "giacenza"          => round((float)($row['giacenza'] ?? 0), 2),
            "giacenza_reale"    => round((float)($row['giacenza_reale'] ?? 0), 2),
            "lotti"             => $mappa_lotti[$codice] ?? []
        ];
    }

    echo json_encode($articoli, JSON_UNESCAPED_UNICODE);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([[
        "codice"            => "ERRORE",
        "descrizione"       => "Database Offline: " . $e->getMessage(),
        "um_default"        => "pz",
        "reparto_suggerito" => "",
        "categoria_profilo" => "",
        "gruppo"            => "",
        "sottogruppo"       => "",
        "giacenza"          => 0,
        "giacenza_reale"    => 0,
        "lotti"             => []
    ]]);
}