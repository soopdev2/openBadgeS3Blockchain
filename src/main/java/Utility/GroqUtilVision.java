package Utility;

import static Utility.Utils.config;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class GroqUtilVision {

    private static final String API_KEY = config.getString("groq_key");
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";

    public static class VisionResult {

        public final String content;
        public final String rawResponseJson;
        public final long apiMillis;

        public VisionResult(String content, String rawResponseJson, long apiMillis) {
            this.content = content;
            this.rawResponseJson = rawResponseJson;
            this.apiMillis = apiMillis;
        }
    }

   
    public static VisionResult analyzeDocumentImage(byte[] imageBytes, String mimeType, String userInstruction)
            throws IOException {

        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + (mimeType != null ? mimeType : "image/jpeg") + ";base64," + base64;

        String prompt = (userInstruction == null || userInstruction.isBlank())
                ? "Estrai tutti i campi del documento e restituisci SOLO un oggetto JSON (no testo extra)."
                : userInstruction;

        String jsonInput = buildVisionJson(prompt, dataUrl);

        long tApi0 = System.nanoTime();
        String raw = postJson(API_URL, jsonInput);
        long tApi1 = System.nanoTime();
        long apiMillis = (tApi1 - tApi0) / 1_000_000L;

        String content = extractChoiceMessageContent(raw);
        return new VisionResult(content, raw, apiMillis);
    }

    private static String buildVisionJson(String prompt, String imageDataUrl) {
        return """
        {
          "model": "%s",
          "messages": [
            {
              "role": "user",
              "content": [
                { "type": "text", "text": %s },
                { "type": "image_url", "image_url": { "url": %s } }
              ]
            }
          ],
          "temperature": 0.2,
          "max_completion_tokens": 1024,
          "response_format": { "type": "json_object" }
        }
        """.formatted(
                escapeJsonString(VISION_MODEL),
                toJsonStringLiteral(prompt),
                toJsonStringLiteral(imageDataUrl)
        );
    }

    private static String postJson(String endpoint, String jsonBody) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(120_000);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);

        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");

        byte[] input = jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(input);
            os.flush();
        }

        int status = conn.getResponseCode();
        String requestId = conn.getHeaderField("x-request-id");
        InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " requestId=" + requestId + " - " + response);
        }

        return response.toString();
    }

    private static String extractChoiceMessageContent(String json) throws IOException {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject root = reader.readObject();
            JsonArray choices = root.getJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return "";
            }
            JsonObject message = choices.getJsonObject(0).getJsonObject("message");
            return message != null ? message.getString("content", "") : "";
        } catch (Exception e) {
            throw new IOException("Impossibile parsare risposta JSON: " + e.getMessage(), e);
        }
    }

    // ESCAPE JSON 
    private static String toJsonStringLiteral(String s) {
        if (s == null) {
            s = "";
        }
        // escape minimale per string literal JSON
        String escaped = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }

    private static String escapeJsonString(String s) {
        return s.replace("\"", "");
    }

    // METODO PDF -> IMAGE 
    public static byte[] pdfFirstPageToPng(byte[] pdfBytes, float dpi) throws IOException {
        // dpi consigliato: 150-220 
        try (PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(pdfBytes)) {

            if (doc.getNumberOfPages() == 0) {
                throw new IOException("PDF senza pagine.");
            }

           PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
            java.awt.image.BufferedImage img = renderer.renderImageWithDPI(
                    0,
                    dpi,
                    org.apache.pdfbox.rendering.ImageType.RGB
            );

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                javax.imageio.ImageIO.write(img, "png", baos);
                return baos.toByteArray();
            }
        }
    }

}
