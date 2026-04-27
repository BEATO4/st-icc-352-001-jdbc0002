package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase para analizar contenido HTML y extraer información estructural.
 */
public class HtmlAnalyzer {

    private final String htmlContent;

    public HtmlAnalyzer(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    /**
     * Cuenta el número de líneas del documento HTML.
     */
    public int countLines() {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return 0;
        }
        return htmlContent.split("\n", -1).length;
    }

    /**
     * Cuenta el número de párrafos
     * <p>
     * en el documento.
     */
    public int countParagraphs() {
        Pattern pattern = Pattern.compile("<p[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlContent);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Cuenta el número de imágenes <img> que están dentro de párrafos
     * <p>
     * .
     */
    public int countImagesInParagraphs() {
        Pattern paragraphPattern = Pattern.compile("<p[^>]*>(.*?)</p>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Pattern imgPattern = Pattern.compile("<img[^>]*>", Pattern.CASE_INSENSITIVE);

        Matcher paragraphMatcher = paragraphPattern.matcher(htmlContent);
        int count = 0;

        while (paragraphMatcher.find()) {
            String paragraphContent = paragraphMatcher.group(1);
            Matcher imgMatcher = imgPattern.matcher(paragraphContent);
            while (imgMatcher.find()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Representa un formulario HTML con su método y campos input.
     */
    public static class FormInfo {
        private final String method;
        private final String action;
        private final List<InputInfo> inputs;
        private final String fullContent;

        public FormInfo(String method, String action, List<InputInfo> inputs, String fullContent) {
            this.method = method != null ? method.toUpperCase() : "GET";
            this.action = action != null ? action : "";
            this.inputs = inputs;
            this.fullContent = fullContent;
        }

        public String getMethod() {
            return method;
        }

        public String getAction() {
            return action;
        }

        public List<InputInfo> getInputs() {
            return inputs;
        }

        public String getFullContent() {
            return fullContent;
        }

        public boolean isPost() {
            return "POST".equalsIgnoreCase(method);
        }

        public boolean isGet() {
            return "GET".equalsIgnoreCase(method);
        }
    }

    /**
     * Representa un campo input de un formulario.
     */
    public static class InputInfo {
        private final String name;
        private final String type;

        public InputInfo(String name, String type) {
            this.name = name != null ? name : "";
            this.type = type != null ? type : "text";
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }

    /**
     * Extrae todos los formularios del documento HTML.
     */
    public List<FormInfo> extractForms() {
        List<FormInfo> forms = new ArrayList<>();
        Pattern formPattern = Pattern.compile("<form([^>]*)>(.*?)</form>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher formMatcher = formPattern.matcher(htmlContent);

        while (formMatcher.find()) {
            String formAttributes = formMatcher.group(1);
            String formContent = formMatcher.group(2);

            String method = extractAttribute(formAttributes, "method");
            String action = extractAttribute(formAttributes, "action");
            List<InputInfo> inputs = extractInputs(formContent);

            forms.add(new FormInfo(method, action, inputs, formContent));
        }
        return forms;
    }

    /**
     * Extrae los campos input de un contenido de formulario.
     */
    private List<InputInfo> extractInputs(String formContent) {
        List<InputInfo> inputs = new ArrayList<>();
        Pattern inputPattern = Pattern.compile("<input([^>]*)>", Pattern.CASE_INSENSITIVE);
        Matcher inputMatcher = inputPattern.matcher(formContent);

        while (inputMatcher.find()) {
            String inputAttributes = inputMatcher.group(1);
            String name = extractAttribute(inputAttributes, "name");
            String type = extractAttribute(inputAttributes, "type");
            inputs.add(new InputInfo(name, type != null ? type : "text"));
        }
        return inputs;
    }

    /**
     * Extrae el valor de un atributo de una cadena de atributos HTML.
     */
    private String extractAttribute(String attributesString, String attributeName) {
        // Patrón para atributo="valor" o atributo='valor'
        Pattern pattern = Pattern.compile(attributeName + "\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(attributesString);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Patrón para atributo=valor (sin comillas)
        pattern = Pattern.compile(attributeName + "\\s*=\\s*([^\\s>]+)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(attributesString);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Cuenta formularios por método (GET o POST).
     */
    public int countFormsByMethod(String method) {
        List<FormInfo> forms = extractForms();
        int count = 0;
        for (FormInfo form : forms) {
            if (method.equalsIgnoreCase(form.getMethod())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Obtiene los formularios POST para enviar peticiones.
     */
    public List<FormInfo> getPostForms() {
        List<FormInfo> allForms = extractForms();
        List<FormInfo> postForms = new ArrayList<>();
        for (FormInfo form : allForms) {
            if (form.isPost()) {
                postForms.add(form);
            }
        }
        return postForms;
    }
}
