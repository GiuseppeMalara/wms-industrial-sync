<?php
if (session_status() === PHP_SESSION_NONE) {
    ini_set('session.cookie_httponly', '1');
    ini_set('session.use_only_cookies', '1');
    ini_set('session.cookie_samesite', 'Lax');
    session_start();
}

if (empty($_SESSION['csrf_token'])) {
    $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
}

// Hash generato per la password (admin123 di default)
$HASH_ACCESSO = getenv('APP_ADMIN_HASH') ?: '$2y$10$rmHHtlqE7bvwlYQa4v7ZsuddDhWHr66tYYZznNqraVzPTszLLiNu6';

// Logout
if (isset($_GET['logout'])) {
    $_SESSION = [];
    if (ini_get("session.use_cookies")) {
        $p = session_get_cookie_params();
        setcookie(session_name(), '', time() - 42000, $p["path"], $p["domain"], $p["secure"], $p["httponly"]);
    }
    session_destroy();
    header("Location: gestione.php");
    exit();
}

// Verifica Login
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['password'])) {
    if (password_verify($_POST['password'], $HASH_ACCESSO)) {
        session_regenerate_id(true);
        $_SESSION['loggato'] = true;
        header("Location: " . $_SERVER['REQUEST_URI']);
        exit();
    } else {
        $errore_auth = "Password errata o non autorizzata.";
    }
}

// Schermata di blocco per utenti non autenticati
if (empty($_SESSION['loggato']) || $_SESSION['loggato'] !== true) {
    ?>
    <!DOCTYPE html>
    <html lang="it">
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta charset="UTF-8">
        <title>Accesso Gestionale Magazzino</title>
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    </head>
    <body class="bg-dark d-flex align-items-center justify-content-center" style="min-height: 100vh;">
        <div class="card p-4 shadow-lg w-100" style="max-width: 380px;">
            <h4 class="text-center mb-3">Area Riservata</h4>
            <p class="text-muted small text-center mb-3">Pannello Gestione Scarichi</p>
            <?php if (!empty($errore_auth)): ?>
                <div class="alert alert-danger py-2 small"><?= htmlspecialchars($errore_auth) ?></div>
            <?php endif; ?>
            <form method="POST">
                <div class="mb-3">
                    <label class="form-label text-muted small fw-bold">PASSWORD</label>
                    <input type="password" name="password" class="form-control" required autofocus>
                </div>
                <button type="submit" class="btn btn-primary w-100 fw-bold">ACCEDI</button>
            </form>
        </div>
    </body>
    </html>
    <?php
    exit();
}