<%-- 
    Document   : index
    Created on : 25 gen 2024, 12:33:40
    Author     : Salvatore
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>

        <header>
            <div class="it-header-slim-wrapper">
                <div class="container">
                    <div class="row">
                        <div class="col-12">
                        </div>
                    </div>
                </div>
            </div>

            <div class="it-nav-wrapper">
    <div class="it-header-center-wrapper">
        <div class="container">
            <div class="row">
                <div class="col-12">
                    <div class="it-header-center-content-wrapper">
                        <div class="logo-wrapper d-flex align-items-center">
                            <img
                                id="logo"
                                src="assets/logo/skillProof.png"
                                alt="SkillProof Logo"
                                style="width: 100px;" 
                            />
                            <div class="it-brand-wrapper">
                                <div class="it-brand-text">
                                    <div class="it-brand-title">
                                        <h3 class="text-white">
                                            <b>BADGE CHAIN - SKILL PROOF</b>
                                        </h3>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="it-right-zone">
                            </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="it-header-navbar-wrapper" id="header-nav-wrapper">
        <div class="container">
            <div class="row">
                <div class="col-12">
                    <nav class="navbar navbar-expand-lg has-megamenu" aria-label="Navigazione principale">
                        <button
                            class="custom-navbar-toggler"
                            type="button"
                            aria-controls="nav4"
                            aria-expanded="false"
                            aria-label="Mostra/Nascondi la navigazione"
                            data-bs-target="#nav4"
                            data-bs-toggle="navbarcollapsible"
                        >
                            <svg class="icon">
                                <use href=""></use>
                            </svg>
                        </button>
                        <div class="navbar-collapsable" id="nav4" style="display: none">
                            <div class="overlay" style="display: none"></div>
                            <div class="close-div">
                                <button class="btn close-menu" type="button">
                                    <span class="visually-hidden">Nascondi la navigazione</span>
                                    <svg class="icon">
                                        <use href=""></use>
                                    </svg>
                                </button>
                            </div>
                        </div>
                    </nav>
                </div>
            </div>
        </div>
    </div>
</div>
        </header>
    </body>
</html>
