package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;

/**
 * Programa principal que analiza recursos web utilizando Java HTTP Client.
 * Solicita una URL por consola y muestra información del recurso.
 */
public class Main {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Ingrese una URL válida: ");
        String urlString = scanner.nextLine().trim();

        if (urlString.isEmpty()) {
            System.out.println("Error: No se proporcionó una URL.");
            return;
        }

        // Agregar protocolo si no está presente
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            urlString = "https://" + urlString;
        }

        System.out.print("Ingrese su número de matrícula: ");
        String matriculaId = scanner.nextLine().trim();

        if (matriculaId.isEmpty()) {
            System.out.println("Advertencia: No se proporcionó matrícula. Se usará 'sin-matricula'.");
            matriculaId = "sin-matricula";
        }

        try {
            analyzeUrl(urlString, matriculaId);
        } catch (Exception e) {
            System.out.println("Error al procesar la URL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Analiza una URL y muestra información del recurso.
     */
    private static void analyzeUrl(String urlString, String matriculaId) throws IOException, InterruptedException {
        URI uri = URI.create(urlString);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .header("User-Agent", "Mozilla/5.0 JavaHttpClient/11")
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("\n" + "=".repeat(60));
        System.out.println("ANÁLISIS DE RECURSO WEB");
        System.out.println("=".repeat(60));
        System.out.println("URL: " + urlString);
        System.out.println("Código de respuesta: " + response.statusCode());

        // Obtener Content-Type
        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("desconocido");

        System.out.println("\n--- a) TIPO DE RECURSO ---");
        System.out.println("Content-Type: " + contentType);
        System.out.println("Tipo: " + getResourceType(contentType));

        // Verificar si es HTML
        if (isHtmlContent(contentType)) {
            analyzeHtmlContent(response.body(), uri, matriculaId);
        } else {
            System.out.println("\nEl recurso no es un documento HTML. No se realiza análisis detallado.");
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("FIN DEL ANÁLISIS");
        System.out.println("=".repeat(60));
    }

    /**
     * Determina el tipo de recurso basándose en el Content-Type.
     */
    private static String getResourceType(String contentType) {
        String ct = contentType.toLowerCase();
        if (ct.contains("text/html")) {
            return "Documento HTML";
        } else if (ct.contains("application/pdf")) {
            return "Documento PDF";
        } else if (ct.contains("image/")) {
            if (ct.contains("png"))
                return "Imagen PNG";
            if (ct.contains("jpeg") || ct.contains("jpg"))
                return "Imagen JPEG";
            if (ct.contains("gif"))
                return "Imagen GIF";
            if (ct.contains("webp"))
                return "Imagen WebP";
            if (ct.contains("svg"))
                return "Imagen SVG";
            return "Imagen";
        } else if (ct.contains("application/json")) {
            return "Documento JSON";
        } else if (ct.contains("application/xml") || ct.contains("text/xml")) {
            return "Documento XML";
        } else if (ct.contains("text/plain")) {
            return "Texto plano";
        } else if (ct.contains("text/css")) {
            return "Hoja de estilos CSS";
        } else if (ct.contains("application/javascript") || ct.contains("text/javascript")) {
            return "JavaScript";
        } else {
            return "Otro tipo: " + contentType;
        }
    }

    /**
     * Verifica si el Content-Type corresponde a HTML.
     */
    private static boolean isHtmlContent(String contentType) {
        return contentType.toLowerCase().contains("text/html");
    }

    /**
     * Analiza el contenido HTML y muestra estadísticas.
     */
    private static void analyzeHtmlContent(String htmlContent, URI baseUri, String matriculaId)
            throws IOException, InterruptedException {

        HtmlAnalyzer analyzer = new HtmlAnalyzer(htmlContent);

        System.out.println("\n--- b) ANÁLISIS DE DOCUMENTO HTML ---");

        // 1. Cantidad de líneas
        int lineCount = analyzer.countLines();
        System.out.println("\n  1. Cantidad de líneas: " + lineCount);

        // 2. Cantidad de párrafos
        int paragraphCount = analyzer.countParagraphs();
        System.out.println("  2. Cantidad de párrafos <p>: " + paragraphCount);

        // 3. Cantidad de imágenes en párrafos
        int imagesInParagraphs = analyzer.countImagesInParagraphs();
        System.out.println("  3. Cantidad de imágenes <img> dentro de párrafos: " + imagesInParagraphs);

        // 4. Cantidad de formularios por método
        List<HtmlAnalyzer.FormInfo> forms = analyzer.extractForms();
        int postForms = analyzer.countFormsByMethod("POST");
        int getForms = analyzer.countFormsByMethod("GET");

        System.out.println("\n  4. Cantidad de formularios:");
        System.out.println("     - Total: " + forms.size());
        System.out.println("     - Método POST: " + postForms);
        System.out.println("     - Método GET: " + getForms);

        // 5. Mostrar inputs de cada formulario
        System.out.println("\n  5. Campos input por formulario:");
        if (forms.isEmpty()) {
            System.out.println("     (No se encontraron formularios)");
        } else {
            int formIndex = 1;
            for (HtmlAnalyzer.FormInfo form : forms) {
                System.out.println("\n     Formulario #" + formIndex + " [" + form.getMethod() + "]");
                System.out.println("     Action: " + (form.getAction().isEmpty() ? "(vacío)" : form.getAction()));

                List<HtmlAnalyzer.InputInfo> inputs = form.getInputs();
                if (inputs.isEmpty()) {
                    System.out.println("       (Sin campos input)");
                } else {
                    for (HtmlAnalyzer.InputInfo input : inputs) {
                        System.out
                                .println("       - name=\"" + input.getName() + "\" type=\"" + input.getType() + "\"");
                    }
                }
                formIndex++;
            }
        }

        // 6. Enviar peticiones POST a formularios
        System.out.println("\n  6. Peticiones POST a formularios:");
        List<HtmlAnalyzer.FormInfo> postFormsList = analyzer.getPostForms();

        if (postFormsList.isEmpty()) {
            System.out.println("     (No hay formularios con método POST)");
        } else {
            int formIndex = 1;
            for (HtmlAnalyzer.FormInfo form : postFormsList) {
                System.out.println("\n     --- Enviando petición a Formulario POST #" + formIndex + " ---");
                sendPostRequest(form, baseUri, matriculaId);
                formIndex++;
            }
        }
    }

    /**
     * Envía una petición POST a un formulario con los parámetros requeridos.
     */
    private static void sendPostRequest(HtmlAnalyzer.FormInfo form, URI baseUri, String matriculaId)
            throws IOException, InterruptedException {

        // Construir la URL del action
        String actionUrl = form.getAction();
        URI targetUri;

        if (actionUrl.isEmpty() || actionUrl.equals("#")) {
            targetUri = baseUri;
        } else if (actionUrl.startsWith("http://") || actionUrl.startsWith("https://")) {
            targetUri = URI.create(actionUrl);
        } else if (actionUrl.startsWith("/")) {
            targetUri = URI.create(baseUri.getScheme() + "://" + baseUri.getHost() +
                    (baseUri.getPort() != -1 ? ":" + baseUri.getPort() : "") + actionUrl);
        } else {
            String basePath = baseUri.getPath();
            String parentPath = basePath.substring(0, basePath.lastIndexOf('/') + 1);
            targetUri = URI.create(baseUri.getScheme() + "://" + baseUri.getHost() +
                    (baseUri.getPort() != -1 ? ":" + baseUri.getPort() : "") + parentPath + actionUrl);
        }

        // Construir el body con el parámetro requerido
        String requestBody = "asignatura=" + URLEncoder.encode("practica1", StandardCharsets.UTF_8);

        System.out.println("     URL destino: " + targetUri);
        System.out.println("     Parámetro: asignatura=practica1");
        System.out.println("     Header: matricula-id=" + matriculaId);

        try {
            HttpRequest postRequest = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "Mozilla/5.0 JavaHttpClient/11")
                    .header("matricula-id", matriculaId)
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("\n     Respuesta del servidor:");
            System.out.println("     Código de estado: " + response.statusCode());

            String responseContentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("desconocido");
            System.out.println("     Content-Type: " + responseContentType);

            // Mostrar primeras líneas del body de respuesta
            String responseBody = response.body();
            if (responseBody != null && !responseBody.isEmpty()) {
                String[] lines = responseBody.split("\n");
                int linesToShow = Math.min(10, lines.length);
                System.out.println("     Primeras " + linesToShow + " líneas de la respuesta:");
                for (int i = 0; i < linesToShow; i++) {
                    System.out.println("       " + lines[i]);
                }
                if (lines.length > 10) {
                    System.out.println("       ... (" + (lines.length - 10) + " líneas más)");
                }
            }
        } catch (Exception e) {
            System.out.println("     Error al enviar petición: " + e.getMessage());
        }
    }
}