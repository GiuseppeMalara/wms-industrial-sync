<?php
require_once __DIR__ . '/auth.php';
require_once __DIR__ . '/db.php';

try {
    $ricerca = trim($_GET['ricerca'] ?? '');
    
    if ($ricerca !== '') {
        $stmt = $pdo->prepare("SELECT * FROM articoli WHERE codice LIKE ? OR descrizione LIKE ? OR gruppo LIKE ? ORDER BY codice ASC");
        $param = "%$ricerca%";
        $stmt->execute([$param, $param, $param]);
    } else {
        $stmt = $pdo->query("SELECT * FROM articoli ORDER BY codice ASC");
    }
    $articoli = $stmt->fetchAll();
    
    $stmtLotti = $pdo->prepare("SELECT nome_lotto, posizione, giacenza_reale FROM lotti_tessuti WHERE codice_articolo = ? ORDER BY nome_lotto ASC");
} catch (PDOException $e) {
    die("Errore DB: " . $e->getMessage());
}
?>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta charset="UTF-8">
    <title>Monitor Giacenze</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.datatables.net/1.13.6/css/dataTables.bootstrap5.min.css" rel="stylesheet">
</head>
<body class="bg-light p-4">
<div class="container-fluid bg-white p-4 rounded shadow-sm">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 gap-3">
        <h2>📊 Monitor Giacenze in Tempo Reale</h2>
        <div>
            <a href="gestione.php" class="btn btn-secondary me-2">⬅️ Storico Scarichi</a>
            <a href="?logout=1" class="btn btn-outline-danger">Esci</a>
        </div>
    </div>

    <form class="row g-2 mb-4 bg-light p-2 rounded border align-items-center" method="GET" style="max-width: 650px;">
        <div class="col-sm-8">
            <input type="text" name="ricerca" class="form-control" placeholder="Cerca codice, descrizione o gruppo..." value="<?= htmlspecialchars($ricerca) ?>" autofocus>
        </div>
        <div class="col-sm-4 d-flex gap-2">
            <button type="submit" class="btn btn-primary flex-grow-1">🔍 Cerca</button>
            <?php if ($ricerca !== ''): ?>
                <a href="gestione_articoli.php" class="btn btn-outline-secondary">✖️ Azzera</a>
            <?php endif; ?>
        </div>
    </form>

    <div class="table-responsive">
        <table id="tabellaArticoli" class="table table-hover table-striped align-middle">
            <thead class="table-dark">
                <tr>
                    <th>Codice</th>
                    <th class="d-none d-md-table-cell">Descrizione</th>
                    <th class="d-none d-md-table-cell">Gruppo</th>
                    <th class="d-none d-md-table-cell">Giac. ERP</th>
                    <th>Giacenza Reale</th>
                    <th>Dettaglio Lotti</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($articoli as $a):
                    $stmtLotti->execute([$a['codice']]);
                    $lotti = $stmtLotti->fetchAll();

                    if (count($lotti) > 0) {
                        $giacenza_mostrata = 0;
                        foreach ($lotti as $l) {
                            $giacenza_mostrata += (float)$l['giacenza_reale'];
                        }
                    } else {
                        $giacenza_mostrata = (float)$a['giacenza_reale'];
                    }
                    $giacenza_mostrata = round($giacenza_mostrata, 2);
                    $coloreReale = ($giacenza_mostrata <= 0) ? 'text-danger fw-bold' : 'text-success fw-bold';
                ?>
                <tr>
                    <td><strong><?= htmlspecialchars($a['codice']) ?></strong></td>
                    <td class="d-none d-md-table-cell"><?= htmlspecialchars($a['descrizione']) ?></td>
                    <td class="d-none d-md-table-cell"><span class="badge bg-info text-dark"><?= htmlspecialchars($a['gruppo'] ?? '') ?></span></td>
                    <td class="d-none d-md-table-cell"><?= round($a['giacenza'], 2) ?> <?= htmlspecialchars($a['um_default']) ?></td>
                    <td class="<?= $coloreReale ?>"><?= number_format($giacenza_mostrata, 2, ',', '.') ?> <?= htmlspecialchars($a['um_default']) ?></td>
                    <td>
                        <?php if (count($lotti) > 0): ?>
                            <ul class="list-unstyled mb-0 small">
                                <?php foreach ($lotti as $l): ?>
                                    <li>📦 <b><?= htmlspecialchars($l['nome_lotto']) ?></b>: <?= number_format(round($l['giacenza_reale'], 2), 2, ',', '.') ?> <?= !empty($l['posizione']) ? '(' . htmlspecialchars($l['posizione']) . ')' : '' ?></li>
                                <?php endforeach; ?>
                            </ul>
                        <?php else: ?>
                            <span class="text-muted small">-</span>
                        <?php endif; ?>
                    </td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
<script src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>
<script src="https://cdn.datatables.net/1.13.6/js/dataTables.bootstrap5.min.js"></script>
<script>
    $(document).ready(function() {
        $('#tabellaArticoli').DataTable({
            "language": { "url": "//cdn.datatables.net/plug-ins/1.13.6/i18n/it-IT.json" },
            "pageLength": 50,
            "searching": false
        });
    });
</script>
</body>
</html>