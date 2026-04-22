/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Api;

import ENUM.TipoAccessoEnum;
import Entity.InfoTrack;
import Entity.Transazione;
import Entity.Utente;
import Utility.JpaUtil;
import static Utility.JpaUtil.trovaUtenteById;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static Utility.Utils.config;
import static Utility.Utils.generateSalt;
import static Utility.Utils.hashRecipientEmail;
import static Utility.Utils.tryParseLong;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 *
 * @author Salvatore
 */
@Path("/openbadge")
public class OpenBadgeApi {

    private static final String ACCESS_KEY = config.getString("access_key");
    private static final String SECRET_KEY = config.getString("secret_key");
    private static final String AWS_REGION = config.getString("aws_region");
    private static final String BUCKET_NAME = config.getString("bucket_name");
    private static final String BASE_URL = "https://" + BUCKET_NAME + ".s3." + AWS_REGION + ".amazonaws.com/";
    private static final String NODE_URL
            = "https://ethereum-sepolia-rpc.publicnode.com";

    private static final Web3j web3j
            = Web3j.build(new HttpService(NODE_URL));

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private S3Client createS3Client() {
        return S3Client.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)
                ))
                .build();
    }

    @POST
    @Path("/genera")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generaBadge(
            @QueryParam("user_id") String user_id_param,
            @QueryParam("selected_user_id") String selected_user_id_param,
            Map<String, Object> input) {

        InfoTrack infoTrack = new InfoTrack();
        infoTrack.setDataEvento(LocalDateTime.now());
        infoTrack.setAzione("OPENBADGE GENERATION");

        try (S3Client s3 = createS3Client()) {

            Long userId = tryParseLong(user_id_param);
            Utente mittente = trovaUtenteById(userId);

            if (mittente == null) {
                return error("Utente mittente non trovato", 404);
            }

            if (!mittente.getRuolo().getTipo().equals(TipoAccessoEnum.ADMIN)) {
                return error("Non autorizzato: richiesto profilo ADMIN", 401);
            }

            Map<String, Object> user = (Map<String, Object>) input.get("user");
            if (user == null) {
                return error("Dati utente mancanti", 400);
            }

            String email = extractFirst(user.get("email"));
            if (email == null || email.isBlank()) {
                return error("Email mancante", 400);
            }

            String badgeName = String.valueOf(input.getOrDefault("badgeName", "Reader"));
            String badgeDescription = String.valueOf(input.getOrDefault("badgeDescription", "Badge generato"));
            String imageUrl = config.getString("imageUrl");
            System.out.println("ImageUrl" + imageUrl);

            String suffix = uuid(10);
            String issuerFile = "issuer-" + suffix + ".json";
            String badgeFile = "badge-" + suffix + ".json";
            String criteriaFile = "criteria-" + suffix + ".html";
            String assertionFile = "assertion-" + suffix + ".json";

            Map<String, Object> issuer = Map.of(
                    "@context", "https://w3id.org/openbadges/v2",
                    "id", BASE_URL + issuerFile,
                    "type", "Issuer",
                    "name", mittente.getName(),
                    "url", mittente.getUrl()
            );

            List<Map<String, String>> criteriaData = parseCriteria(input);
            if (criteriaData.isEmpty()) {
                criteriaData.add(Map.of("titolo", "Documento analizzato", "valore", "OK"));
            }
            String criteriaHtml = createCriteriaHtml(badgeName, criteriaData, user);

            Map<String, Object> badgeClass = new HashMap<>();
            badgeClass.put("@context", "https://w3id.org/openbadges/v2");
            badgeClass.put("id", BASE_URL + badgeFile);
            badgeClass.put("type", "BadgeClass");
            badgeClass.put("name", badgeName);
            badgeClass.put("description", badgeDescription);
            badgeClass.put("image", imageUrl);
            badgeClass.put("issuer", BASE_URL + issuerFile);
            badgeClass.put("criteria", Map.of("type", "Criteria", "url", BASE_URL + criteriaFile));

            String salt = generateSalt(16);
            String hashedEmail = hashRecipientEmail(email, salt);

            Map<String, Object> assertion = new HashMap<>();
            assertion.put("@context", "https://w3id.org/openbadges/v2");
            assertion.put("id", BASE_URL + assertionFile);
            assertion.put("type", "Assertion");
            assertion.put("badge", BASE_URL + badgeFile);
            assertion.put("recipient", Map.of(
                    "type", "email",
                    "hashed", true,
                    "identity", hashedEmail,
                    "salt", salt
            ));
            assertion.put("issuedOn", Instant.now().toString());

            String assertionJson = gson.toJson(assertion);

            String hashHex = sha256(assertionJson);
            System.out.println("Notarizzazione Hash: 0x" + hashHex);

            String txHash = null;
            try {
                txHash = inviaHashSuBlockchain("0x" + hashHex, infoTrack);
            } catch (Exception e) {
                System.err.println("Errore Blockchain: " + e.getMessage());
                // L'errore viene loggato in infoTrack dal metodo stesso
            }

            uploadToS3(s3, issuerFile, gson.toJson(issuer), infoTrack);
            uploadToS3(s3, badgeFile, gson.toJson(badgeClass), infoTrack);
            uploadToS3(s3, assertionFile, assertionJson, infoTrack);
            uploadToS3(s3, criteriaFile, criteriaHtml, infoTrack);

            Map<String, Object> result = new HashMap<>();
            result.put("issuerUrl", BASE_URL + issuerFile);
            result.put("badgeUrl", BASE_URL + badgeFile);
            result.put("assertionUrl", BASE_URL + assertionFile);
            result.put("criteriaUrl", BASE_URL + criteriaFile);
            result.put("imageUrl", imageUrl);
            result.put("txHash", txHash);
            result.put("hashNotarizzato", "0x" + hashHex);
            Transazione transazione = new Transazione();
            if (txHash != null) {
                transazione.setTxHash(txHash);
            }
            if (hashHex != null) {
                transazione.setHashHex("0x" + hashHex);
            }
            if (email != null) {
                transazione.setEmail(email);
            }

            JpaUtil.saveTxHashAndHashHexOnDb(transazione);

            result.put("message", txHash != null
                    ? "Badge generato e notarizzato su Blockchain"
                    : "Badge generato correttamente ma notarizzazione fallita");

            return Response.ok(result).build();

        } catch (Exception e) {
            e.printStackTrace();
            return error("Errore interno: " + e.getMessage(), 500);
        }
    }

    private String createCriteriaHtml(String badgeName,
            List<Map<String, String>> criteria,
            Map<String, Object> user) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><head><style>")
                .append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #333; line-height: 1.6; padding: 20px; background-color: #f9f9f9; }")
                .append("h1 { color: #0066CC; border-bottom: 2px solid #3498db; padding-bottom: 10px; text-transform: uppercase; font-size: 24px; }")
                .append("h2 { color: #0066CC; margin-top: 30px; font-size: 18px; }")
                .append("table { border-collapse: separate; border-spacing: 0; width: 100%; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); margin-bottom: 20px; }")
                .append("th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #edf2f7; }")
                .append("th { background-color: #0066CC; color: white; font-weight: 600; text-transform: uppercase; font-size: 13px; }")
                .append("tr:last-child td { border-bottom: none; }")
                .append("tr:nth-child(even) { background-color: #f8fbff; }")
                .append(".label { font-weight: bold; color: #7f8c8d; width: 30%; }")
                .append("</style></head><body>");

        sb.append("<h1>").append(badgeName).append("</h1>");

        sb.append("<h2>Dettagli Utente</h2><table>");
        sb.append("<thead><tr><th>Proprietà</th><th>Valore</th></tr></thead><tbody>");
        user.forEach((k, v)
                -> sb.append("<tr><td class='label'>").append(k)
                        .append("</td><td>").append(v != null ? v : "-")
                        .append("</td></tr>")
        );
        sb.append("</tbody></table>");

        sb.append("<h2>Criteri di Valutazione</h2><table>");
        sb.append("<thead><tr><th>Criterio</th><th>Stato / Valore</th></tr></thead><tbody>");
        for (Map<String, String> c : criteria) {
            sb.append("<tr><td class='label'>")
                    .append(c.getOrDefault("titolo", "N/D"))
                    .append("</td><td>")
                    .append(c.getOrDefault("valore", "-"))
                    .append("</td></tr>");
        }
        sb.append("</tbody></table></body></html>");

        return sb.toString();
    }

    private List<Map<String, String>> parseCriteria(Map<String, Object> input) {
        List<Map<String, String>> list = new ArrayList<>();
        Object raw = input.get("criteriaPoints");

        if (raw instanceof List<?> rawList) {
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?> map) {

                    Object rawTitolo = map.get("titolo");
                    String titolo = (rawTitolo != null) ? rawTitolo.toString() : "Criterio";

                    Object rawValore = map.get("valore");
                    String valore = (rawValore != null) ? rawValore.toString() : "N/A";

                    list.add(Map.of(
                            "titolo", titolo,
                            "valore", valore
                    ));
                }
            }
        }

        if (list.isEmpty()) {
            list.add(Map.of(
                    "titolo", "Documento analizzato",
                    "valore", "OK"
            ));
        }

        return list;
    }

    private String sha256(String data) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        return Numeric.toHexStringNoPrefix(d.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String uuid(int len) {
        return UUID.randomUUID().toString().substring(0, len);
    }

    private String safe(Map<String, Object> map, String key) {
        return map == null ? "" : String.valueOf(map.getOrDefault(key, ""));
    }

    private String extractFirst(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0));
        }

        return value.toString();
    }

    private Response error(String msg, int code) {
        return Response.status(code)
                .entity(Map.of("error", msg))
                .build();
    }

    private String inviaHashSuBlockchain(String hashHex, InfoTrack infoTrack) {
        String nodeUrl = "https://ethereum-sepolia-rpc.publicnode.com";
        String privateKey = config.getString("sepolia_private_key");
        String recipient = config.getString("tx_recipient_address");

        if (recipient == null || recipient.isBlank()) {
            recipient = "0x9879B435E9c1c5984CdEa39c9393aE3D6289b050";
        }

        Web3j web3j = Web3j.build(new HttpService(nodeUrl));
        System.out.println("\n--- DEBUG BLOCKCHAIN START ---");

        try {
            if (privateKey == null || privateKey.isBlank()) {
                System.err.println("ERRORE: Private Key mancante nel config!");
                infoTrack.setDescrizione("ERRORE - 500 - Private Key mancante nel config");
                JpaUtil.salvaInfoTrack(infoTrack);
                return null;
            }

            Credentials credentials = Credentials.create(privateKey);
            String myAddress = credentials.getAddress();
            String data = hashHex.startsWith("0x") ? hashHex : "0x" + hashHex;

            EthGetBalance balanceRes = web3j.ethGetBalance(myAddress, DefaultBlockParameterName.LATEST).send();
            BigInteger balanceWei = balanceRes.getBalance();
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

            org.web3j.protocol.core.methods.request.Transaction transactionSim
                    = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                            myAddress,
                            recipient,
                            data
                    );

            EthEstimateGas estimateGasRes = web3j.ethEstimateGas(transactionSim).send();

            if (estimateGasRes.hasError()) {
                String errorMsg = estimateGasRes.getError().getMessage();
                System.err.println("ERRORE STIMA GAS: " + errorMsg);
                infoTrack.setDescrizione("ERRORE - 502 - Stima Gas fallita: " + errorMsg);
                JpaUtil.salvaInfoTrack(infoTrack);
                return "ERROR: " + errorMsg;
            }

            BigInteger gasUnitsEstimated = estimateGasRes.getAmountUsed();
            BigInteger totalCostWei = gasUnitsEstimated.multiply(gasPrice);

            java.math.BigDecimal totalCostEth = new java.math.BigDecimal(totalCostWei)
                    .divide(new java.math.BigDecimal(BigInteger.TEN.pow(18)), 18, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal balanceEth = new java.math.BigDecimal(balanceWei)
                    .divide(new java.math.BigDecimal(BigInteger.TEN.pow(18)), 18, java.math.RoundingMode.HALF_UP);

            System.out.println("Mittente (tu):        " + myAddress);
            System.out.println("Destinatario:         " + recipient);
            System.out.println("Bilancio Disponibile: " + balanceEth.toPlainString() + " ETH");
            System.out.println("---------------------------------------------------------");
            System.out.println("Dati da notarizzare:  " + data);
            System.out.println("Gas Unit stimate:     " + gasUnitsEstimated);
            System.out.println("Gas Price attuale:    " + gasPrice + " Wei");
            System.out.println("SPESA PREVISTA:        " + totalCostEth.toPlainString() + " ETH");
            System.out.println("---------------------------------------------------------");

            if (balanceWei.compareTo(totalCostWei) < 0) {
                System.err.println("ESITO: Fondi INSUFFICIENTI per completare l'operazione.");
                infoTrack.setDescrizione("ERRORE - 402 - Fondi insufficienti. Richiesti: " + totalCostEth.toPlainString() + " ETH");
                JpaUtil.salvaInfoTrack(infoTrack);
                return null;
            } else {
                System.out.println("ESITO: Fondi sufficienti. Procedo all'invio.");
            }

            BigInteger nonce = web3j.ethGetTransactionCount(myAddress, DefaultBlockParameterName.PENDING)
                    .send().getTransactionCount();
            BigInteger chainId = web3j.ethChainId().send().getChainId();

            RawTransaction rawTx = RawTransaction.createTransaction(
                    nonce, gasPrice, gasUnitsEstimated.add(BigInteger.valueOf(2000)),
                    recipient, BigInteger.ZERO, data);

            byte[] signedMessage = TransactionEncoder.signMessage(rawTx, chainId.longValue(), credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            System.out.println("Inviando transazione reale...");
            EthSendTransaction resp = web3j.ethSendRawTransaction(hexValue).send();

            if (resp.hasError()) {
                String msg = resp.getError().getMessage();
                System.err.println("Errore durante l'invio: " + msg);
                infoTrack.setDescrizione("ERRORE - 502 - Errore invio transazione: " + msg);
                JpaUtil.salvaInfoTrack(infoTrack);
                return null;
            }

            String txHash = resp.getTransactionHash();
            System.out.println("Transazione completata con Hash: " + txHash);

            // Log di successo
            infoTrack.setDescrizione("SUCCESSO - 200 - Hash simulato e inviato. TxHash: " + txHash);
            JpaUtil.salvaInfoTrack(infoTrack);

            return txHash;

        } catch (Exception e) {
            System.err.println("Eccezione durante la simulazione: " + e.getMessage());
            infoTrack.setDescrizione("ERRORE - 500 - Eccezione blockchain: " + e.getMessage());
            JpaUtil.salvaInfoTrack(infoTrack);
            return null;
        } finally {
            System.out.println("--- DEBUG BLOCKCHAIN END ---");
            web3j.shutdown();
        }
    }

    private void uploadToS3(S3Client s3, String fileName, String content, InfoTrack infoTrack) throws Exception {
        try {
            String contentType;
            if (fileName.endsWith(".html")) {
                contentType = "text/html";
            } else if (fileName.endsWith(".json")) {
                contentType = "application/json";
            } else {
                contentType = "application/octet-stream";
            }

            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3.putObject(putOb, RequestBody.fromString(content));

            infoTrack.setDescrizione("SUCCESSO - File caricato su S3: " + fileName);
            JpaUtil.salvaInfoTrack(infoTrack);

        } catch (S3Exception e) {
            infoTrack.setDescrizione("ERRORE - 500 - Errore S3 durante l'upload di " + fileName + ": " + e.awsErrorDetails().errorMessage());
            JpaUtil.salvaInfoTrack(infoTrack);
            throw new Exception("Errore durante l'upload su S3 (" + fileName + "): " + e.awsErrorDetails().errorMessage());

        } catch (SdkClientException e) {
            infoTrack.setDescrizione("ERRORE - 500 - Errore di connessione S3 durante l'upload di " + fileName + ": " + e.getMessage());
            JpaUtil.salvaInfoTrack(infoTrack);
            throw new Exception("Errore di connessione al servizio S3 (" + fileName + "): " + e.getMessage());

        } catch (Exception e) {
            infoTrack.setDescrizione("ERRORE - 500 - Errore generico durante l'upload di " + fileName + ": " + e.getMessage());
            JpaUtil.salvaInfoTrack(infoTrack);
        }
    }

    private void uploadToS3Base64(S3Client s3, String fileName, Object content, String contentType, InfoTrack infoTrack) throws Exception {
        try {
            if (contentType == null || contentType.isBlank()) {
                if (fileName.endsWith(".html")) {
                    contentType = "text/html";
                } else if (fileName.endsWith(".json")) {
                    contentType = "application/json";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".gif")) {
                    contentType = "image/gif";
                } else {
                    contentType = "application/octet-stream";
                }
            }

            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            if (content instanceof String textContent) {
                s3.putObject(putOb, RequestBody.fromString(textContent));
            } else if (content instanceof byte[] byteContent) {
                s3.putObject(putOb, RequestBody.fromBytes(byteContent));
            } else {
                throw new IllegalArgumentException("Tipo di contenuto non supportato per upload S3: " + content.getClass());
            }

            infoTrack.setDescrizione("SUCCESSO - File caricato su S3: " + fileName);
            JpaUtil.salvaInfoTrack(infoTrack);

        } catch (S3Exception e) {
            infoTrack.setDescrizione("ERRORE - 500 - Errore S3 durante l'upload di " + fileName + ": " + e.awsErrorDetails().errorMessage());
            JpaUtil.salvaInfoTrack(infoTrack);
            throw new Exception("Errore durante l'upload su S3 (" + fileName + "): " + e.awsErrorDetails().errorMessage());

        } catch (SdkClientException e) {
            infoTrack.setDescrizione("ERRORE - 500 - Errore di connessione S3 durante l'upload di " + fileName + ": " + e.getMessage());
            JpaUtil.salvaInfoTrack(infoTrack);
            throw new Exception("Errore di connessione al servizio S3 (" + fileName + "): " + e.getMessage());

        } catch (Exception e) {
            infoTrack.setDescrizione("ERRORE - 500 - Errore generico durante l'upload di " + fileName + ": " + e.getMessage());
            JpaUtil.salvaInfoTrack(infoTrack);
            throw e;
        }
    }

}
