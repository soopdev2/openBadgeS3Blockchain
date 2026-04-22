<%-- 
    Document   : menu
    Created on : 14 apr 2026, 09:58:58
    Author     : Aldo
--%>

<%-- 
    Document   : menuAtt
    Created on : 4 set 2025, 09:16:45
    Author     : Aldo
--%>

<%

    String upload_active = "";
    String consult_active = "";
    String home_active = "";

    String uri1 = request.getRequestURI();
    String pageName1 = uri1.substring(uri1.lastIndexOf("/") + 1);

    if (pageName1.equals("home.jsp")) {
        home_active = "active";
    } else if (pageName1.equals("indexUpload.jsp")) {
        upload_active = "active";
    } else if (pageName1.equals("indexConsult.jsp")) {
        consult_active = "active";
    }

%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>


<nav class="navbar navbar-expand-lg has-megamenu" aria-label="Menu principale">
    <button type="button" aria-label="Mostra o nascondi il menu" class="custom-navbar-toggler" aria-controls="menu" aria-expanded="false" data-bs-toggle="navbarcollapsible" data-bs-target="#navbar-E">
        <span>
            <svg role="img" class="icon"><use href=""></use></svg>
        </span>
    </button>
    <div class="navbar-collapsable" id="navbar-E">
        <div class="overlay fade"></div>
        <div class="close-div">
            <button type="button" aria-label="Chiudi il menu" class="btn close-menu">
                <span><svg role="img" class="icon"><use href=""></use></svg></span>
            </button>
        </div>
        <div class="menu-wrapper justify-content-lg-between">
            <ul class="navbar-nav">
                <!--li class="nav-item active">
                    <a class="nav-link <%=home_active%>" href="index.jsp"><span>Home</span></a>
                </li-->
                <li class="nav-item active">
                    <a class="nav-link <%=upload_active%>" href="indexUpload.jsp"><span>Upload</span></a>
                </li>
                <li class="nav-item active">
                    <a class="nav-link <%=consult_active%>" href="indexConsult.jsp"><span>Consultazione</span></a>
                </li>

            </ul>
        </div>
    </div>
</nav>

