/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Utility;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import org.apache.commons.text.StringEscapeUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 *
 * @author Salvatore
 */
public class Utils {

    public static final ResourceBundle config = ResourceBundle.getBundle("conf.config");

    public static String generateSalt(int length) {
        byte[] salt = new byte[length];
        new SecureRandom().nextBytes(salt);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(salt);
    }

    public static String calculateSha256Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String calculateSha256Hex(String text) throws Exception {
        return calculateSha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Calcola l'hash SHA-256 dei dati forniti e lo restituisce in formato
     * esadecimale.
     */
    private static String calculateSha256HashHex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Errore nell'hashing SHA-256", e);
        }
    }

    /**
     * Calcola l'hash dell'Assertion in formato JSON.
     */
    public static String calculateAssertionHash(String json) {
        return calculateSha256HashHex(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Esegue l'hashing di un'email secondo lo standard Open Badges
     * (sha256$hash).
     *
     * @param email
     * @param salt
     * @return
     */
    public static String hashRecipientEmail(String email, String salt) {
        try {
            String combined = email.trim().toLowerCase() + salt;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return "sha256$" + hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Errore durante l'hashing dell'email", e);
        }
    }

    public static LocalDateTime calcolaScadenza(String scadenza) {
        scadenza = scadenza.trim().toLowerCase();

        String valoreNumerico = scadenza.replaceAll("[^0-9]", "");
        String tipo = scadenza.replaceAll("[0-9]", "");

        int valore = Integer.parseInt(valoreNumerico);
        LocalDateTime now = LocalDateTime.now();

        switch (tipo) {
            case "m" -> {
                return now.plusMinutes(valore);
            }
            case "mo" -> {
                return now.plusMonths(valore);
            }
            case "y" -> {
                return now.plusYears(valore);
            }
            default ->
                throw new IllegalArgumentException("Formato scadenza non valido: " + scadenza);
        }
    }

    public static Integer tryParseInt(String param) {
        try {
            return Integer.valueOf(param);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Long tryParseLong(String param) {
        try {
            return Long.valueOf(param);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String sanitizeInputString(String input) {
        if (input == null) {
            return null;
        }

        // Normalize unicode
        String s = Normalizer.normalize(input, Normalizer.Form.NFKC);

        s = s.replaceAll("[\\p{Cntrl}]", "");

        s = s.replaceAll("\\p{C}", "");

        s = s.replaceAll("[<>\"'`{}\\[\\]|\\\\;$]", "");

        s = s.trim().replaceAll("\\s+", " ");

        return s;
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        input = input.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        input = input.replaceAll("[\r\n]", "");
        return StringEscapeUtils.escapeHtml4(input);
    }

    public static String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static String sanitizePath(String input) {
        input = input.replaceAll("\\.\\./", "")
                .replaceAll("~", "")
                .replaceAll("\\\\", "/");

        input = input.replaceAll("[^a-zA-Z0-9_./-]", "");
        Path sanitizedPath = Paths.get(input).normalize();

        return sanitizedPath.toString();
    }

    public static String checkAttribute(HttpSession session, String attribute) {
        try {
            if (session.getAttribute(attribute) != null) {
                return String.valueOf(session.getAttribute(attribute));
            }
        } catch (Exception e) {
        }
        return "";
    }

//    public static final ResourceBundle config = ResourceBundle.getBundle("conf.conf");
//
//    public static final String PATHLOG = config.getString("logPath");
//    private static final String APPNAME = "NewProject";
//    private static final String PAT_4 = "yyyyMMdd";
//    private static final String PAT_9 = "yyMMddHHmmssSSS";
//    private static Logger createLog() {
//        Logger logger = getLogger(APPNAME);
//        try {
//            String dataOdierna = new org.joda.time.DateTime().toString(PAT_4);
//
//            File logdir = new File(PATHLOG);
//            if (!logdir.exists()) {
//                logdir.mkdir();
//            }
//            String ora = new org.joda.time.DateTime().toString(PAT_9);
//            String pathLog = PATHLOG + dataOdierna;
//            File dirLog = new File(pathLog);
//            if (!dirLog.exists()) {
//                dirLog.mkdirs();
//            }
//            FileHandler fh = new FileHandler(pathLog + separator + APPNAME + "_" + ora + ".log", true);
//            logger.addHandler(fh);
//            fh.setFormatter(new SimpleFormatter());
//            fh.setLevel(Level.ALL);
//        } catch (IOException | SecurityException ex) {
//            logger.severe(ex.getMessage());
//        }
//        return logger;
//    }
//    public static final Logger logfile = createLog();
    public static String estraiEccezione(Exception ec1) {
        try {
            return ec1.getStackTrace()[0].getMethodName() + " - " + getStackTrace(ec1);
        } catch (Exception e) {
//            logfile.severe(estraiEccezione(e));
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, "ERRORE GENERICO", e);
        }
        return ec1.getMessage();
    }

    public static double parseDoube_v1(String valore) {
        if (valore == null || valore.trim().isEmpty()) {
            return 0.0;
        }

        try {
            // Sostituisce la virgola con il punto per garantire il formato corretto
            valore = valore.replace(",", ".");
            return Double.parseDouble(valore);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato numero non valido: " + valore, e);
        }
    }

    public static String formatDoubleSmart(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value); // es: 1.0 -> "1"
        } else {
            return new BigDecimal(value)
                    .stripTrailingZeros()
                    .toPlainString(); // es: 1.50 -> "1.5", 1.52 -> "1.52"
        }
    }

    public static int parseInt_v1(String ing) {

        try {
            return Integer.parseInt(ing);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

    }

    public static Long parseLong_v1(String valore) {
        if (valore == null || valore.trim().isEmpty()) {
            return null;
        }

        try {
            // Rimuove tutti i caratteri non numerici validi tranne eventuale '-' iniziale
            String clean = valore.trim().replaceAll("[^\\d-]", "");
            return clean.isEmpty() ? null : Long.parseLong(clean);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getRequest(HttpServletRequest req, String name) {
        try {
            return req.getParameter(name).trim();
        } catch (Exception e) {
        }
        return "";
    }

    public static String createNewRandomPassword(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        StringBuilder password = new StringBuilder();

        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            password.append(characters.charAt(randomIndex));
        }

        return password.toString();
    }

    public static double roundToTwoDecimals(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static double parseDouble(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input non può essere null o vuoto");
        }

        input = input.trim();
        input = input.replaceAll("[^0-9,\\.]", "");

        if (input.contains(",") && !input.contains(".")) {
            input = input.replace(',', '.');
        }

        return Double.parseDouble(input);
    }

    public static Long parseLongSafe(String val) {
        try {
            if (val == null || val.isBlank()) {
                return null;
            }
            return Long.valueOf(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer parseIntSafe(String val) {
        try {
            if (val == null || val.isBlank()) {
                return 0;
            }
            return Integer.valueOf(val.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Double parseDoubleSafe(String val) {
        try {
            if (val == null || val.isBlank()) {
                return 0.0;
            }
            val = val.replace(",", "."); // accetta anche "1,5"
            return Double.valueOf(val.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Metodo helper per generare JobId univoco
    public static String generateJobId() {
        return java.util.UUID.randomUUID().toString();
    }
}
