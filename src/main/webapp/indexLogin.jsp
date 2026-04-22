<%-- 
    Document   : indexLogin
    Created on : 14 apr 2026, 10:15:48
    Author     : Aldo
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="description" content="Free Web tutorials">
        <meta name="keywords" content="HTML, CSS, JavaScript">
        <meta name="author" content="SmartOOP">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">

        <link rel="stylesheet" href="assets/css/bootstrap-italia.min.css"/>

        <title>Login</title>
    </head>
    <body class="d-flex flex-column min-vh-100">
        <%@include file="include/header.jsp" %>

        <main class="flex-grow-1 d-flex align-items-center justify-content-center bg-white">
            <div class="container">
                <div class="row justify-content-center">
                    <div class="col-lg-5 col-md-8">

                        <div class="it-card-wrapper">
                            <div class="it-card">
                                <div class="it-card-header text-center">

                                    <h2 class="mt-3 text-primary">Accedi</h2>
                                </div>

                                <div class="it-card-body">
                                    <form action="Login?type=login" method="post" onsubmit="return ctrlForm();">
                                        <input type="hidden" name="_csrf" value="..."/>

                                        <div class="form-group mb-3">
                                            <label for="user">Username</label>
                                            <input type="text" class="form-control" id="user" name="username" autocomplete="off">
                                        </div>
                                        <br>
                                        <div class="form-group mb-3">
                                            <label for="password">Password</label>
                                            <input type="password" class="form-control" id="password" name="password" autocomplete="off">
                                        </div>

                                        <div class="d-flex justify-content-between mb-3">
                                            
                                            <a href="javascript:;" id="kt_login_forgot" class="it-link">Password dimenticata?</a>
                                        </div>

                                        <button type="submit" class="btn btn-primary it-btn w-100">Login</button>
                                    </form>
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

        <%@ include file="include/footer.jsp"%>


        <!--SCRIPT-->


        <script src="assets/js/bootstrap-italia.bundle.min.js"></script>


        <!--SCRIPT-->



    </body>
</html>
