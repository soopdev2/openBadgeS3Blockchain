<%-- 
    Document   : indexUpload
    Created on : 14 apr 2026, 09:36:43
    Author     : Aldo
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="it">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="author" content="SmartOOP">

        <title>Registro e Verifica Open Badge</title>

        <link href='https://fonts.googleapis.com/css?family=Titillium+Web' rel='stylesheet'>
        <link rel="icon" type="image/png" href="assets/logo/favicon-96x96.png" />

        <link rel="stylesheet" href="assets/css/bootstrap-italia.min.css"/>
        <link rel="stylesheet" href="assets/css/allPage.css"/>

        <link rel="stylesheet" href="https://cdn.datatables.net/1.13.7/css/dataTables.bootstrap5.min.css">
        <link rel="stylesheet" href="https://cdn.datatables.net/responsive/2.5.0/css/responsive.bootstrap5.min.css">

        <style>
            .it-card-header h2 {
                font-weight: 700;
            }
            #loaderVerifica {
                display: none;
                margin-left: 10px;
            }
            .callout-highlight {
                border-left-width: 5px;
            }
            .section-divider {
                border-top: 1px solid #e6e6e6;
                margin: 3rem 0;
            }
            .input-group-text .icon {
                fill: #5c6f82;
            }
            .table-responsive {
                background: #fdfdfd;
                padding: 15px;
                border-radius: 8px;
            }
        </style>
    </head>

    <body>

        <%@include file="include/header.jsp" %>
        <%@include file="include/menu.jsp" %>

        <main class="container py-5">

            <div class="row justify-content-center mb-5">
                <div class="col-lg-8 text-center">
                    <h3 class="text-primary fw-bold">Gestione Open Badge</h3>
                    <p class="lead">Consulta il registro storico o verifica l'integrità di un badge specifico sulla Blockchain Sepolia.</p>
                </div>
            </div>

            <div class="card shadow-sm border-0 mb-5">
                <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
                    <h5 class="mb-0 text-primary fw-bold">
                        <svg class="icon icon-sm icon-primary me-2"><use href="assets/svg/sprites.svg#it-list"></use></svg> 
                        Registro delle Transazioni Notarizzate
                    </h5>
                    <div class="d-flex align-items-center">
                        <label for="pageSize" class="me-2 small text-muted">Mostra:</label>
                        <select id="pageSize" class="form-select form-select-sm" style="width: auto;">
                            <option value="5">5</option>
                            <option value="10" selected>10</option>
                            <option value="25">25</option>
                        </select>
                    </div>
                </div>
                <div class="card-body">
                    <div id="consulta">
                        <div class="table-responsive">
                            <table class="table table-hover table-striped w-100" id="consultaTable">
                                <thead>
                                    <tr class="text-primary">
                                        <th scope="col" class="text-uppercase small fw-bold">ID</th>
                                        <th scope="col" class="text-uppercase small fw-bold">Email Utente</th>
                                        <th scope="col" class="text-uppercase small fw-bold">Hash Assertion</th>
                                        <th scope="col" class="text-uppercase small fw-bold">Hash Transazione</th>
                                        <th scope="col" class="text-uppercase small fw-bold">Azione</th>
                                    </tr>
                                </thead>
                                <tbody>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <div class="row justify-content-center">
                <div class="col-12 col-lg-7"> <div class="mb-5 text-center">
                        <h2 class="h3 text-primary fw-bold">Strumento di Validazione On-Chain</h2>
                        <p class="text-muted">Confronta i dati del badge con i record immutabili sulla Blockchain Sepolia.</p>
                    </div>

                    <div class="card shadow-lg border-0" style="border-radius: 12px; overflow: hidden;">
                        <div class="progress progress-xs" style="height: 4px;">
                            <div class="progress-bar bg-primary" role="progressbar" style="width: 100%"></div>
                        </div>

                        <div class="card-header bg-white border-0 pt-4 pb-0 text-center">
                            <div class="d-inline-flex align-items-center justify-content-center">
                                <div class="bg-light p-2 rounded-circle me-3">
                                    <svg class="icon icon-primary icon-sm"><use href="assets/svg/sprites.svg#it-check-circle"></use></svg>
                                </div>
                                <h5 class="text-primary">Inserisci i dati per verificare il badge</h5>
                            </div>
                        </div>

                        <div class="card-body p-4 pt-4">
                            <form id="formVerificaBadge">
                                <div class="row g-4">

                                    <div class="col-12">
                                        <div class="form-group mb-0">
                                            <div class="input-group">
                                                <span class="input-group-text bg-white border-end-0">
                                                    <svg class="icon icon-sm icon-muted"><use href="assets/svg/sprites.svg#it-file"></use></svg>
                                                </span>
                                                <label for="v_asHash" class="active text-primary">Hash Assertion</label>
                                                <input type="text" class="form-control border-start-0 ps-0" id="v_asHash" name="asHash" required placeholder="Inserisci l'hash dal registro">
                                            </div>
                                        </div>
                                    </div>

                                    <div class="col-12">
                                        <div class="form-group mb-0">
                                            <div class="input-group">
                                                <span class="input-group-text bg-white border-end-0">
                                                    <svg class="icon icon-sm icon-muted"><use href="assets/svg/sprites.svg#it-link"></use></svg>
                                                </span>
                                                <label for="v_txHash" class="active text-primary">Hash Transazione</label>
                                                <input type="text" class="form-control border-start-0 ps-0" id="v_txHash" name="txHash" required placeholder="0x...">
                                            </div>
                                        </div>
                                    </div>

                                    <div class="col-12">
                                        <div class="form-group mb-0">
                                            <div class="input-group">
                                                <span class="input-group-text bg-white border-end-0">
                                                    <svg class="icon icon-sm icon-muted"><use href="assets/svg/sprites.svg#it-mail"></use></svg>
                                                </span>
                                                <label for="v_email" class="active text-primary">Email Utente</label>
                                                <input type="email" class="form-control border-start-0 ps-0" id="v_email" name="email" required placeholder="esempio@email.it">
                                            </div>
                                        </div>
                                    </div>

                                    <div class="col-12 mt-5">
                                        <button type="submit" id="btnVerifica" class="btn btn-primary w-100 shadow-sm d-flex align-items-center justify-content-center" style="height: 54px; transition: all 0.3s ease;">
                                            <span id="textBtn" class="fw-bold text-uppercase">Esegui Verifica Blockchain</span>

                                            <div class="progress-spinner progress-spinner-sm progress-spinner-white" id="loaderVerifica" style="display:none; width: 24px; height: 24px;">
                                                <span class="visually-hidden">In corso...</span>
                                            </div>
                                        </button>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </div>

                    <div id="containerRisultato" class="mt-5" style="display:none;">
                        <div class="callout callout-highlight" id="calloutRisultato" style="border-radius: 8px;">
                            <div class="callout-title d-flex align-items-center">
                                <svg class="icon" id="iconRisultato"><use href="assets/svg/sprites.svg#it-info-circle"></use></svg>
                                <span id="titoloRisultato" class="h5 mb-0 fw-bold">Esito della Verifica</span>
                            </div>
                            <div class="callout-text">
                                <p id="messaggioRisultato" class="mt-2 mb-0"></p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </main>

        <%@include file="include/footer.jsp" %>

        <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
        <script src="assets/js/bootstrap-italia.bundle.min.js"></script>

        <script src="https://cdn.datatables.net/1.13.7/js/jquery.dataTables.min.js"></script>
        <script src="https://cdn.datatables.net/1.13.7/js/dataTables.bootstrap5.min.js"></script>
        <script src="https://cdn.datatables.net/responsive/2.5.0/js/dataTables.responsive.min.js"></script>
        <script src="https://cdn.datatables.net/responsive/2.5.0/js/responsive.bootstrap5.min.js"></script>

        <script src="assets/js/custom/indexConsult.js"></script>

        <script>
            $(document).ready(function () {

                $('#formVerificaBadge').on('submit', function (e) {
                    e.preventDefault();

                    const $btn = $('#btnVerifica');
                    const $loader = $('#loaderVerifica');
                    const $textBtn = $('#textBtn');
                    const $container = $('#containerRisultato');

                    $btn.prop('disabled', true);
                    $textBtn.css('opacity', '0.5');
                    $loader.fadeIn();
                    $container.hide();

                    $.ajax({
                        url: 'SearchServlet',
                        type: 'POST',
                        data: $(this).serialize() + '&isVerify=true',
                        dataType: 'json',
                        success: function (res) {
                            mostraRisultato(res);
                        },
                        error: function (xhr) {
                            let msg = "Impossibile contattare il server.";
                            if (xhr.responseJSON && xhr.responseJSON.message) {
                                msg = xhr.responseJSON.message;
                            }
                            mostraRisultato({
                                valid: false,
                                message: msg
                            });
                        },
                        complete: function () {
                            $btn.prop('disabled', false);
                            $loader.hide();
                            $textBtn.css('opacity', '1');
                        }
                    });
                });

                function mostraRisultato(res) {
                    const $container = $('#containerRisultato');
                    const $callout = $('#calloutRisultato');
                    const $icon = $('#iconRisultato use');
                    const $titolo = $('#titoloRisultato');
                    const $messaggio = $('#messaggioRisultato');

                    $callout.removeClass('success danger warning info highlight');

                    if (res.valid) {
                        $callout.addClass('success');
                        $titolo.text("Badge Autenticato");
                        $icon.attr('href', 'assets/svg/sprites.svg#it-check-circle');
                        $messaggio.html("<strong>Verifica effettuata con successo!</strong> " + res.message);
                    } else {
                        $callout.addClass('danger');
                        $titolo.text("Verifica Fallita");
                        $icon.attr('href', 'assets/svg/sprites.svg#it-close-circle');
                        $messaggio.html("<strong>Incongruenza rilevata:</strong> " + res.message);
                    }

                    $container.fadeIn();

                    $('html, body').animate({
                        scrollTop: $container.offset().top - 150
                    }, 600);
                }
            });

            function viewPdf(id) {
                if (!id) {
                    alert("Documento non disponibile");
                    return;
                }

                window.open(
                        "ViewPdfServlet?id=" + id,
                        "_blank"
                        );
            }


        </script>
    </body>
</html>
