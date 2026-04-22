/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package servlet;

import Entity.Transazione;
import Utility.JpaUtil;
import Utility.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.http.HttpService;

/**
 *
 * @author Salvatore
 */
public class SearchServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    private static final AtomicInteger sEchoCounter = new AtomicInteger(0);

    public static final String ITOTALRECORDS = "iTotalRecords";
    public static final String ITOTALDISPLAY = "iTotalDisplayRecords";
    public static final String SECHO = "sEcho";
    public static final String SCOLUMS = "sColumns";
    public static final String APPJSON = "application/json";
    public static final String CONTENTTYPE = "Content-Type";
    public static final String AADATA = "aaData";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String searchParam = request.getParameter("isSearch");
        String verifyParam = request.getParameter("isVerify");

        boolean isSearch = searchParam != null && searchParam.equalsIgnoreCase("true");
        boolean isVerify = verifyParam != null && verifyParam.equalsIgnoreCase("true");
        if (isSearch) {
            consultaTransazioni(request, response);
        } else if (isVerify) {
            verificaTransazioni(request, response);
        }
    }

    protected void consultaTransazioni(HttpServletRequest request, HttpServletResponse response) {
        try {

            int draw = Utils.tryParseInt(request.getParameter("draw"));
            int start = Utils.tryParseInt(request.getParameter("start"));
            int length = Utils.tryParseInt(request.getParameter("length"));

            if (length <= 0) {
                length = 10;
            }

            JpaUtil jpaUtil = new JpaUtil();

            long totalRecords = jpaUtil.countTransazioni();
            List<Transazione> transazioni = jpaUtil.ricercaTransazioni(start, length);

            JsonObject jsonResponse = new JsonObject();
            JsonArray jsonData = new JsonArray();

            for (Transazione t : transazioni) {
                JsonObject row = new JsonObject();
                row.addProperty("id", t.getId());
                row.addProperty("email", t.getEmail() != null ? t.getEmail() : "N/D");
                row.addProperty("hashhex", t.getHashHex() != null ? t.getHashHex() : "N/D");
                row.addProperty("txhash", t.getTxHash() != null ? t.getTxHash() : "N/D");

                jsonData.add(row);
            }

            jsonResponse.addProperty("draw", draw);
            jsonResponse.addProperty("recordsTotal", totalRecords);
            jsonResponse.addProperty("recordsFiltered", totalRecords);
            jsonResponse.add("aaData", jsonData);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.print(jsonResponse.toString());
            }

        } catch (Exception e) {
            Utils.estraiEccezione(e);
        }
    }

    protected void verificaTransazioni(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        JSONObject jsonResponse = new JSONObject();

        try {
            // 1. Recupero parametri dal form JSP
            String inputHashAssertion = request.getParameter("asHash");
            String txHash = request.getParameter("txHash");
            String inputEmail = request.getParameter("email");

            if (inputHashAssertion == null || txHash == null || inputEmail == null) {
                throw new IllegalArgumentException("Tutti i campi sono obbligatori per la verifica.");
            }

            if (!inputHashAssertion.startsWith("0x")) {
                inputHashAssertion = "0x" + inputHashAssertion;
            }

            Transazione transazioneDb = JpaUtil.trovaTransazioneByHash(txHash);
            if (transazioneDb == null) {
                throw new Exception("Transazione non registrata nel database di sistema.");
            }

            String onChainHash;
            try (Web3j web3j = Web3j.build(new HttpService("https://ethereum-sepolia-rpc.publicnode.com"))) {
                EthTransaction ethTransaction = web3j.ethGetTransactionByHash(txHash).send();
                Transaction tx = ethTransaction.getTransaction()
                        .orElseThrow(() -> new Exception("Transazione non trovata sulla blockchain Sepolia."));
                onChainHash = tx.getInput();
            }

            boolean emailCorrisponde = inputEmail.trim().equalsIgnoreCase(transazioneDb.getEmail());

            boolean matchInputDb = inputHashAssertion.equalsIgnoreCase(transazioneDb.getHashHex());
            boolean matchDbBlockchain = transazioneDb.getHashHex().equalsIgnoreCase(onChainHash);

            boolean esitoFinale = emailCorrisponde && matchInputDb && matchDbBlockchain;

            jsonResponse.put("valid", esitoFinale);

            if (esitoFinale) {
                jsonResponse.put("message", "✅ Badge Autentico: L'hash e l'email corrispondono alla notarizzazione Blockchain.");
            } else {
                String errore = "";
                if (!emailCorrisponde) {
                    errore = "L'email inserita non è associata a questo badge. ";
                }
                if (!matchInputDb) {
                    errore += "L'hash dell'assertion non corrisponde ai record di sistema. ";
                }
                if (!matchDbBlockchain) {
                    errore += "Incongruenza rilevata con i dati on-chain.";
                }
                jsonResponse.put("message", "⚠️ Verifica Fallita: " + errore);
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("valid", false);
            jsonResponse.put("message", "❌ Errore: " + e.getMessage());
        }

        out.print(jsonResponse.toString());
        out.flush();
    }

// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
