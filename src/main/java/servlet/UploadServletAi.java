package servlet;

import Utility.GroqUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

@MultipartConfig
public class UploadServletAi extends HttpServlet {

    private static final String UPLOAD_DIR = System.getProperty("java.io.tmpdir");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("\n--- Nuova richiesta ricevuta su UploadServletAi ---");

        Part filePart = request.getPart("documento");
        if (filePart == null || filePart.getSize() == 0) {
            request.setAttribute("result", "Nessun file caricato!");
            request.getRequestDispatcher("indexUpload.jsp").forward(request, response);
            return;
        }

        String originalName = FilenameUtils.getName(filePart.getSubmittedFileName());
        File file = File.createTempFile("upload_", "_" + originalName, new File(UPLOAD_DIR));

        try (InputStream input = filePart.getInputStream()) {
            Files.copy(input, file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:/tesseract");
            tesseract.setLanguage("ita");

            StringBuilder textBuilder = new StringBuilder();
            boolean isPdf = originalName.toLowerCase().endsWith(".pdf");

            System.out.println("LOG: Inizio OCR...");

            if (isPdf) {
                try (PDDocument document = PDDocument.load(file)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    int maxPages = Math.min(document.getNumberOfPages(), 2);

                    for (int i = 0; i < maxPages; i++) {
                        BufferedImage image = renderer.renderImageWithDPI(i, 200);
                        textBuilder.append(tesseract.doOCR(image)).append("\n");
                    }
                }
            } else {
                textBuilder.append(tesseract.doOCR(file));
            }

            String cleanText = textBuilder.toString()
                    .replaceAll("\\s+", " ")
                    .trim();

            if (cleanText.length() > 1200) {
                cleanText = cleanText.substring(0, 1200);
            }

            System.out.println("LOG: Testo OCR estratto: " + cleanText.length() + " caratteri");

            String escapedText = cleanText
                    .replace("\"", "\\\"")
                    .replace("\n", " ");

            String prompt = """
IMPORTANTE:
Rispondi SOLO con JSON valido.
NON usare markdown.
NON aggiungere testo.

ESTRAI DAL DOCUMENTO:

- badgeName (trova qualcosa che possa andare bene come nome del badge)
- badgeDescription (descrizione riepilogativa del badge)
- dati personali utente (nome, cognome, data nascita, CF, email, telefono, indirizzo, azienda, mestiere)
- criteri

FORMATO OBBLIGATORIO:

{
  "badgeName": "",
  "badgeDescription": "",
  "user": {
    "nome": "",
    "cognome": "",
    "dataNascita": "",
    "luogoNascita": "",
    "codiceFiscale": "",
    "indirizzo": "",
    "email": [],
    "telefono": [],
    "azienda": "",
    "mestiere": ""
  },
  "criteriaPoints": [
    {
      "titolo": "",
      "valore": ""
    }
  ]
}

REGOLE:
- usa SOLO dati presenti nel testo OCR
- se un dato non esiste usa stringa vuota o array vuoto
- non inventare nulla

TESTO:
%s
""".formatted(escapedText);

            String groqRawResult;

            try {
                System.out.println("LOG: Chiamata Groq...");
                groqRawResult = GroqUtil.callGroqAPI(prompt);

                System.out.println("DEBUG RAW:");
                System.out.println(groqRawResult);

            } catch (IOException e) {
                String errorMsg = e.getMessage().toLowerCase();

                if (errorMsg.contains("413") || errorMsg.contains("limit")) {
                    throw new Exception("L'IA è temporaneamente sovraccarica. Riprova tra 60 secondi.");
                }

                throw new Exception("Errore chiamata Groq: " + e.getMessage());
            }

            JsonObject parsedJson;

            try {
                String cleanedResponse = cleanGroqJson(groqRawResult);

                if ("INVALID".equals(cleanedResponse)) {
                    throw new Exception("Groq non ha restituito JSON valido.");
                }

                parsedJson = JsonParser
                        .parseString(cleanedResponse)
                        .getAsJsonObject();

                if (!parsedJson.has("badgeName")) {
                    parsedJson.addProperty("badgeName", "Badge Generato");
                }

                if (!parsedJson.has("badgeDescription")) {
                    parsedJson.addProperty("badgeDescription", "Badge generato automaticamente.");
                }

                if (!parsedJson.has("criteriaPoints")) {
                    JsonArray defaultCriteria = new JsonArray();

                    JsonObject item = new JsonObject();
                    item.addProperty("titolo", "Documento analizzato");
                    item.addProperty("valore", "Valido");

                    defaultCriteria.add(item);

                    parsedJson.add("criteriaPoints", defaultCriteria);
                }

            } catch (Exception e) {
                throw new Exception("Errore parsing JSON Groq: " + e.getMessage());
            }

            String safeJsonPayload = new Gson().toJson(parsedJson);

            System.out.println("LOG JSON FINALE:");
            System.out.println(safeJsonPayload);

            String apiUrl
                    = "http://localhost:8080/OpenBadgeS3/webresources/openbadge/genera"
                    + "?user_id=1";

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest apiRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            safeJsonPayload,
                            StandardCharsets.UTF_8
                    ))
                    .build();

            HttpResponse<String> apiResponse
                    = client.send(apiRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("LOG API STATUS: " + apiResponse.statusCode());

            if (apiResponse.statusCode() >= 200 && apiResponse.statusCode() < 300) {

                Gson gson = new GsonBuilder()
                        .setPrettyPrinting()
                        .create();

                Type mapType = new TypeToken<Map<String, Object>>() {
                }.getType();

                Map<String, Object> resultMap
                        = gson.fromJson(apiResponse.body(), mapType);

                String prettyJson = gson.toJson(resultMap);

                request.setAttribute("apiResult", resultMap);
                request.setAttribute("prettyJson", prettyJson);

            } else {
                request.setAttribute("result",
                        "Errore API Badge (Status "
                        + apiResponse.statusCode()
                        + "): "
                        + apiResponse.body());
            }

        } catch (Exception e) {
            e.printStackTrace();

            request.setAttribute(
                    "result",
                    "❌ Errore durante la generazione badge: " + e.getMessage()
            );

        } finally {
            if (file.exists()) {
                Files.deleteIfExists(file.toPath());
                System.out.println("LOG: File temporaneo eliminato.");
            }
        }

        request.getRequestDispatcher("indexUpload.jsp").forward(request, response);
    }

    /**
     * Isola il contenuto JSON eliminando eventuale testo descrittivo dell'IA.
     */
    private String cleanGroqJson(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "INVALID";
        }

        try {
            String cleaned = response.trim()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            int start = cleaned.indexOf("{");
            int end = cleaned.lastIndexOf("}");

            if (start != -1 && end != -1 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }

            JsonParser.parseString(cleaned);

            return cleaned;

        } catch (Exception e) {
            System.err.println("JSON Groq non valido: " + response);
            return "INVALID";
        }
    }
}
