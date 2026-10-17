<?php
require_once __DIR__ . '/auth.php';
require_once __DIR__ . '/db.php';

if (!isset($_GET['id']) || !is_numeric($_GET['id'])) {
    die("ID Scarico non valido.");
}
$id_scarico = (int)$_GET['id'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Validazione CSRF
    if (!isset($_POST['csrf_token']) || !hash_equals($_SESSION['csrf_token'], $_POST['csrf_token'])) {
        die("ERRORE DI SICUREZZA: Richiesta non autorizzata.");
    }

    $azione = $_POST['azione'] ?? '';

    $stmt = $pdo->prepare("SELECT * FROM scarichi WHERE id = ?");
    $stmt->execute([$id_scarico]);
    $old = $stmt->fetch();

    if (!$old || $old['esportato'] == 1) {
        die("Impossibile modificare questo scarico: inesistente o già acquisito dall'ERP.");
    }

    try {
        $pdo->beginTransaction();

        if ($azione === 'elimina') {
            // Ripristino giacenza articolo
            $pdo->prepare("UPDATE articoli SET giacenza_reale = ROUND(giacenza_reale + ?, 2) WHERE codice = ?")->execute([$old['qta'], $old['codice']]);
            // Ripristino giacenza lotto (se presente)
            if (!empty($old['lotto'])) {
                $pdo->prepare("UPDATE lotti_tessuti SET giacenza_reale = ROUND(giacenza_reale + ?, 2) WHERE nome_lotto = ? AND codice_articolo = ?")->execute([$old['qta'], $old['lotto'], $old['codice']]);
            }
            // Eliminazione record dallo storico
            $pdo->prepare("DELETE FROM scarichi WHERE id = ?")->execute([$id_scarico]);

            $pdo->commit();
            header("Location: gestione.php?msg=eliminato");
            exit();
        }

        if ($azione === 'modifica') {
            $nuovo_codice = strtoupper(trim($_POST['nuovo_codice']));
            $nuova_qta    = (float)$_POST['nuova_qta'];
            $nuovo_lotto  = trim($_POST['nuovo_lotto'] ?? '');

            // 1. Storno vecchio scarico (restituzione quantità)
            $pdo->prepare("UPDATE articoli SET giacenza_reale = ROUND(giacenza_reale + ?, 2) WHERE codice = ?")->execute([$old['qta'], $old['codice']]);
            if (!empty($old['lotto'])) {
                $pdo->prepare("UPDATE lotti_tessuti SET giacenza_reale = ROUND(giacenza_reale + ?, 2) WHERE nome_lotto = ? AND codice_articolo = ?")->execute([$old['qta'], $old['lotto'], $old['codice']]);
            }

            // 2. Applicazione nuovo scarico (detrazione quantità)
            $pdo->prepare("UPDATE articoli SET giacenza_reale = ROUND(giacenza_reale - ?, 2) WHERE codice = ?")->execute([$nuova_qta, $nuovo_codice]);
            if (!empty($nuovo_lotto) && $old['codice'] === $nuovo_codice) {
                $pdo->prepare("UPDATE lotti_tessuti SET giacenza_reale = ROUND(giacenza_reale - ?, 2) WHERE nome_lotto = ? AND codice_articolo = ?")->execute([$nuova_qta, $nuovo_lotto, $nuovo_codice]);
            } else {
                $nuovo_lotto = '';
            }

            // 3. Aggiornamento riga archivio
            $pdo->prepare("UPDATE scarichi SET codice = ?, qta = ?, lotto = ? WHERE id = ?")->execute([$nuovo_codice, $nuova_qta, $nuovo_lotto, $id_scarico]);

            $pdo->commit();
            header("Location: gestione.php?msg=modificato");
            exit();
        }

    } catch (Exception $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        die("Errore durante la rettifica: " . $e->getMessage());
    }
}

// Lettura dati record corrente
$stmt = $pdo->prepare("SELECT * FROM scarichi WHERE id = ?");
$stmt->execute([$id_scarico]);
$scarico = $stmt->fetch();
if (!$scarico) die("Scarico non trovato.");

$stmtLotti = $pdo->prepare("SELECT nome_lotto, giacenza_reale FROM lotti_tessuti WHERE codice_articolo = ? ORDER BY nome_lotto ASC");
$stmtLotti->execute([$scarico['codice']]);
$lotti_disponibili = $stmtLotti->fetchAll();
?>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta charset="UTF-8">
    <title>Modifica Scarico</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light p-4">
<div class="container bg-white p-4 p-md-5 rounded shadow-sm" style="max-width: 600px;">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h3 class="mb-0">✏️ Gestione Scarico</h3>
        <a href="gestione.php" class="btn btn-outline-secondary">Annulla</a>
    </div>

    <div class="alert alert-info small">
        Movimento registrato il <strong><?= htmlspecialchars($scarico['data_inserimento']) ?></strong>.<br>
        Le giacenze di magazzino verranno ricalcolate in automatico in modo atomico.
    </div>

    <form method="POST">
        <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($_SESSION['csrf_token']) ?>">
        
        <div class="mb-3">
            <label class="form-label fw-bold">Codice Articolo</label>
            <input type="text" name="nuovo_codice" class="form-control form-control-lg" value="<?= htmlspecialchars($scarico['codice']) ?>" required>
        </div>

        <div class="mb-3">
            <label class="form-label fw-bold">Quantità Scaricata (<?= htmlspecialchars($scarico['um']) ?>)</label>
            <input type="number" step="0.01" name="nuova_qta" class="form-control form-control-lg" value="<?= round($scarico['qta'], 2) ?>" required>
        </div>

        <div class="mb-4">
            <label class="form-label fw-bold text-muted">Lotto Associato</label>
            <?php if (count($lotti_disponibili) > 0): ?>
                <select name="nuovo_lotto" class="form-select form-select-lg">
                    <option value="">-- Nessun lotto selezionato --</option>
                    <?php foreach ($lotti_disponibili as $l): ?>
                        <option value="<?= htmlspecialchars($l['nome_lotto']) ?>" <?= ($scarico['lotto'] === $l['nome_lotto']) ? 'selected' : '' ?>>
                            Lotto: <?= htmlspecialchars($l['nome_lotto']) ?> (Disponibilità: <?= round($l['giacenza_reale'], 2) ?>)
                        </option>
                    <?php endforeach; ?>
                </select>
            <?php else: ?>
                <input type="text" class="form-control" value="Nessun lotto disponibile per questo articolo" disabled>
                <input type="hidden" name="nuovo_lotto" value="">
            <?php endif; ?>
        </div>

        <div class="d-flex justify-content-between pt-3 border-top">
            <button type="submit" name="azione" value="elimina" class="btn btn-outline-danger" onclick="return confirm('Confermi lo storno totale ed eliminazione di questo scarico?');">
                🗑️ Elimina Scarico
            </button>
            <button type="submit" name="azione" value="modifica" class="btn btn-success px-4">
                💾 Salva e Ricalcola
            </button>
        </div>
    </form>
</div>
</body>
</html>