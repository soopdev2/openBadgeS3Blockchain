<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map" %>

<!DOCTYPE html>
<html lang="it">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link href='https://fonts.googleapis.com/css?family=Titillium Web' rel='stylesheet'>
        <link rel="icon" type="image/png" href="assets/logo/favicon-96x96.png" sizes="96x96" />
        <link rel="icon" type="image/svg+xml" href="assets/logo//favicon.svg" />
        <link rel="shortcut icon" href="assets/logo/favicon.ico" />
        <link rel="apple-touch-icon" sizes="180x180" href="assets/logo/apple-touch-icon.png" />
        <link rel="manifest" href="assets/logo//site.webmanifest" />

        <title>Upload documento - OpenBadge AI</title>

        <!-- Bootstrap Italia -->
        <link rel="stylesheet" href="assets/css/bootstrap-italia.min.css"/>
        <link rel="stylesheet" href="assets/css/allPage.css"/>
    </head>

    <body class="d-flex flex-column min-vh-100">

        <%@include file="include/header.jsp" %>
        <%@include file="include/menu.jsp" %>

        <main class="container my-5">

            <br>

            <div class="row justify-content-center">
                <div class="col-lg-8">

                    <h2 class="mb-4 text-primary">
                        Carica documento (PDF, JPG, PNG)
                    </h2>

                    <!-- FORM UPLOAD -->
                    <div class="card shadow-sm mb-4">
                        <div class="card-body">

                            <form action="UploadServletAi" method="post" enctype="multipart/form-data">

                                <div class="form-group">
                                    <label for="documento" class="active">Seleziona file</label>
                                    <input type="file"
                                           class="form-control"
                                           name="documento"
                                           id="documento"
                                           accept=".jpg,.jpeg,.png,.pdf"
                                           required>
                                </div>

                                <button type="submit" class="btn btn-primary mt-3">
                                    Carica e analizza
                                </button>

                            </form>

                        </div>
                    </div>

                    <!-- RISULTATO API -->
                    <%                Map<String, Object> apiResult
                                = (Map<String, Object>) request.getAttribute("apiResult");

                        String prettyJson
                                = (String) request.getAttribute("prettyJson");

                        String error
                                = (String) request.getAttribute("result");
                    %>

                    <% if (error != null) {%>
                    <div class="alert alert-danger">
                        <%= error%>
                    </div>
                    <% } %>

                    <% if (apiResult != null) {%>

                    <!-- CARD RISULTATO -->
                    <div class="card border-success shadow-sm mb-4">
                        <div class="card-header bg-success text-white">
                            Badge generato con successo
                        </div>

                        <div class="card-body">

                            <p>
                                <strong>Messaggio:</strong>
                                <%= apiResult.get("message")%>
                            </p>

                            <p>
                                <strong>Issuer URL:</strong><br>
                                <a href="<%= apiResult.get("issuerUrl")%>" target="_blank">
                                    <%= apiResult.get("issuerUrl")%>
                                </a>
                            </p>

                            <p>
                                <strong>Badge URL:</strong><br>
                                <a href="<%= apiResult.get("badgeUrl")%>" target="_blank">
                                    <%= apiResult.get("badgeUrl")%>
                                </a>
                            </p>

                            <p>
                                <strong>Criteria URL:</strong><br>
                                <a href="<%= apiResult.get("criteriaUrl")%>" target="_blank">
                                    <%= apiResult.get("criteriaUrl")%>
                                </a>
                            </p>

                            <p>
                                <strong>Assertion URL:</strong><br>
                                <a href="<%= apiResult.get("assertionUrl")%>" target="_blank">
                                    <%= apiResult.get("assertionUrl")%>
                                </a>
                            </p>

                            <p>
                                <strong>Transaction Hash:</strong><br>
                                <code><%= apiResult.get("txHash")%></code>
                            </p>

                            <p>
                                <strong>Hash assertion:</strong><br>
                                <code><%= apiResult.get("hashNotarizzato")%></code>
                            </p>

                            <p>
                                <strong>Immagine Badge:</strong><br>
                                <img src="<%=apiResult.get("imageUrl")%>"
                                     style="max-width:180px;"
                                     class="img-fluid mt-2 rounded">
                            </p>

                        </div>
                    </div>

                    <!-- JSON PRETTIFICATO COMPLETO -->
                    <% if (prettyJson != null) {%>
                    <div class="card border-primary shadow-sm">
                        <div class="card-header bg-primary text-white">
                            JSON completo risposta API (debug)
                        </div>

                        <div class="card-body">

                            <pre class="p-3 rounded"
                                 style="
                                 background:#f8f9fa;
                                 border:1px solid #ddd;
                                 font-size:13px;
                                 white-space: pre-wrap;
                                 word-break: break-word;
                                 "><%= prettyJson%></pre>

                        </div>
                    </div>
                    <% } %>

                    <% }%>

                </div>
            </div>

        </main>

              
        <%@include file="include/footer.jsp" %>

        <!-- Bootstrap Italia JS -->
        <script src="assets/js/bootstrap-italia.bundle.min.js"></script>

    </body>
</html>