<?php
require_once __DIR__ . '/auth.php';
require_once __DIR__ . '/db.php';

$data_inizio = $_GET['data_inizio'] ?? date('Y-m-d', strtotime('-30 days'));
$data_fine   = $_GET['data_fine'] ?? date('Y-m-d');
$solo_pendenti = isset($_GET['solo_da_inviare']) && $_GET['solo_da_inviare'] == '1';

try {
    if ($solo_pendenti) {
        $stmt = $pdo->prepare("SELECT * FROM scarichi WHERE esportato = 0 ORDER BY data_inserimento DESC");
        $stmt->execute();
    } else {
        $stmt = $pdo->prepare("SELECT * FROM scarichi WHERE data_inserimento >= ? AND data_inserimento <= ? ORDER BY data_inserimento DESC");
        $stmt->execute([$data_inizio . " 00:00:00", $data_fine . " 23:59:59"]);
    }
    $scarichi = $stmt->fetchAll();
} catch (PDOException $e) {
    die("Errore: " . $e->getMessage());
}
?>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta charset="UTF-8">
    <title>Registro Scarichi</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.datatables.net/1.13.6/css/dataTables.bootstrap5.min.css" rel="stylesheet">
</head>
<body class="bg-light p-4">
<div class="container-fluid bg-white p-4 rounded shadow-sm">
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 gap-3">
        <div>
            <h2 class="mb-0">📦 Registro Scarichi Produzione</h2>
            <span class="text-muted small">Terminali reparti produttivi</span>
        </div>
        <div>
            <a href="gestione_articoli.php" class="btn btn-primary me-2">🔍 Monitor Giacenze</a>
            <a href="?logout=1" class="btn btn-outline-danger">Esci</a>
        </div>
    </div>

    <?php if (isset($_GET['msg']) && $_GET['msg'] === 'modificato'): ?>
        <div class="alert alert-success alert-dismissible fade show">Rettifica applicata con ricalcolo giacenze.</div>
    <?php elseif (isset($_GET['msg']) && $_GET['msg'] === 'eliminato'): ?>
        <div class="alert alert-warning alert-dismissible fade show">Scarico stornato ed eliminato dallo storico.</div>
    <?php endif; ?>

    <form class="row g-3 mb-4 bg-light p-3 rounded border" method="GET">
        <div class="col-auto">
            <label class="form-label fw-bold small">Data Inizio</label>
            <input type="date" name="data_inizio" class="form-control" value="<?= htmlspecialchars($data_inizio) ?>">
        </div>
        <div class="col-auto">
            <label class="form-label fw-bold small">Data Fine</label>
            <input type="date" name="data_fine" class="form-control" value="<?= htmlspecialchars($data_fine) ?>">
        </div>
        <div class="col-auto align-self-end">
            <button type="submit" class="btn btn-dark">🔍 Filtra Date</button>
        </div>
        <div class="col-auto align-self-end ms-auto">
            <a href="gestione.php?solo_da_inviare=1" class="btn btn-warning fw-bold">🚨 Revisione Pre-Invio</a>
            <a href="gestione.php" class="btn btn-outline-secondary">Tutti</a>
        </div>
    </form>

    <div class="table-responsive">
        <table id="tabellaScarichi" class="table table-hover table-striped align-middle">
            <thead class="table-dark">
                <tr>
                    <th>Data/Ora</th>
                    <th>Codice</th>
                    <th>Lotto</th>
                    <th>Qta</th>
                    <th>Reparto</th>
                    <th>Stato Export</th>
                    <th>Azioni</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($scarichi as $s): ?>
                <tr>
                    <td><?= htmlspecialchars($s['data_inserimento']) ?></td>
                    <td><strong><?= htmlspecialchars($s['codice']) ?></strong></td>
                    <td><?= !empty($s['lotto']) ? htmlspecialchars($s['lotto']) : '-' ?></td>
                    <td><span class="fs-5 fw-bold text-danger">-<?= round($s['qta'], 2) ?></span> <?= htmlspecialchars($s['um']) ?></td>
                    <td><span class="badge bg-secondary"><?= htmlspecialchars($s['reparto_lotto']) ?></span></td>
                    <td>
                        <?= $s['esportato'] == 1 
                            ? '<span class="badge bg-success">Inviato ad Arca</span>' 
                            : '<span class="badge bg-warning text-dark">Da Inviare</span>' ?>
                    </td>
                    <td>
                        <?php if ($s['esportato'] == 0): ?>
                            <a href="modifica_scarico.php?id=<?= $s['id'] ?>" class="btn btn-sm btn-primary">✏️ Modifica</a>
                        <?php else: ?>
                            <button class="btn btn-sm btn-secondary" disabled>🔒 Bloccato</button>
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
        $('#tabellaScarichi').DataTable({
            "language": { "url": "//cdn.datatables.net/plug-ins/1.13.6/i18n/it-IT.json" },
            "order": [[ 0, "desc" ]]
        });
    });
</script>
</body>
</html>