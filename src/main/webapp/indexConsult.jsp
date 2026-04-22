<%-- 
    Document   : indexUpload
    Created on : 14 apr 2026, 09:36:43
    Author     : Aldo
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="description" content="">
        <meta name="keywords" content="">
        <meta name="author" content="SmartOOP">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link href='https://fonts.googleapis.com/css?family=Titillium Web' rel='stylesheet'>
        <link rel="icon" type="image/png" href="assets/logo/favicon-96x96.png" sizes="96x96" />
        <link rel="icon" type="image/svg+xml" href="assets/logo//favicon.svg" />
        <link rel="shortcut icon" href="assets/logo/favicon.ico" />
        <link rel="apple-touch-icon" sizes="180x180" href="assets/logo/apple-touch-icon.png" />
        <link rel="manifest" href="assets/logo//site.webmanifest" />

        <link rel="stylesheet" href="assets/css/bootstrap-italia.min.css"/>
        <link rel="stylesheet" href="assets/css/allPage.css"/>

        <title>Upload document</title>
    </head>


    <body class="container-fluidn w-100" >


        <%@include file="include/header.jsp" %>
        <%@include file="include/menu.jsp" %>

        <main class="flex-grow-1 d-flex align-items-center justify-content-center bg-white">

            <div class="container">
                <div class="row justify-content-center">
                    <div class="col-lg-5 col-md-8">

                        <div class="it-card-wrapper">
                            <div class="it-card">
                                <div class="it-card-header text-center">

                                    <h2 class="mt-3 text-primary">Consulta doumenti</h2>
                                </div>

                                <div class="it-card-body">

                                </div>

                                <!--div class="it-card-footer text-center">
                                    <a href="javascript:void(0);" onclick="document.getElementById('manform').submit();" class="it-link">
                                        <span class="icon"><i class="fa fa-file-pdf text-danger"></i></span>
                                        Guida all'uso della piattaforma
                                    </a>
                                </div-->
                            </div>
                        </div>

                    </div>
                </div>
            </div>

        </main>



        <div id="footer">
            <%@include file="include/footer.jsp" %>
        </div>




        <!--SCRIPT-->


        <script src="assets/js/bootstrap-italia.bundle.min.js"></script>


        <!--SCRIPT-->





    </body>
</html>
