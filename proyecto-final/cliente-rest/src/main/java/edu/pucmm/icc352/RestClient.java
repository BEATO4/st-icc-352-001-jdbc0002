package edu.pucmm.icc352;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RestClient {
    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);
    private final String baseUrl;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String authToken;

    public RestClient(String host, int port) {
        this(buildBaseUrl(host, port));
    }

    public RestClient(String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
        logger.info("Cliente REST conectando a: {}", baseUrl);
    }

    private static String buildBaseUrl(String host, int port) {
        String normalizedHost = host == null ? "" : host.trim();
        if (normalizedHost.startsWith("http://") || normalizedHost.startsWith("https://")) {
            return normalizedHost;
        }
        String protocol = (port == 443) ? "https://" : "http://";
        return protocol + normalizedHost + ":" + port;
    }

    private static String normalizeBaseUrl(String rawUrl) {
        if (rawUrl == null) {
            return "";
        }
        String trimmed = rawUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public LoginResponse login(String username, String password) {
        try {
            logger.info("Intentando login para: {}", username);

            Map<String, String> body = new HashMap<>();
            body.put("username", username);
            body.put("password", password);

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpPost request = new HttpPost(baseUrl + "/api/auth/login");
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

            ClassicHttpResponse response = httpClient.executeOpen(null, request, null);
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            if (statusCode == 200) {
                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                String token = jsonResponse.path("token").asText();
                if (token.isEmpty()) token = jsonResponse.path("data").path("token").asText();

                JsonNode userNode = jsonResponse.has("user") ? jsonResponse.path("user") : jsonResponse.path("data").path("user");
                String responseUsername = userNode.path("username").asText();
                String userId = userNode.path("id").asText();
                if (userId.isEmpty()) userId = userNode.path("_id").asText();

                if (token.isEmpty() || responseUsername.isEmpty() || userId.isEmpty()) {
                    logger.warn("Error: Campos vacios en respuesta del login");
                    return new LoginResponse(false, null, null, null);
                }

                this.authToken = token;
                logger.info("Login exitoso para: {}", responseUsername);

                return new LoginResponse(true, token, responseUsername, userId);
            } else {
                logger.warn("Error en login: codigo {}", statusCode);
                return new LoginResponse(false, null, null, null);
            }

        } catch (Exception e) {
            logger.error("Error al intentar login", e);
            return new LoginResponse(false, null, null, null);
        }
    }

    public List<FormularioDTO> listarFormularios(String userId) {
        try {
            logger.info("Listando formularios del usuario: {}", userId);

            HttpGet request = new HttpGet(baseUrl + "/api/surveys/user/" + userId);
            addAuthHeader(request);

            ClassicHttpResponse response = httpClient.executeOpen(null, request, null);
            String responseBody = EntityUtils.toString(response.getEntity());

            if (response.getCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                JsonNode dataArray = jsonResponse.has("forms")
                        ? jsonResponse.path("forms")
                        : jsonResponse.path("data").path("forms");

                List<FormularioDTO> formularios = new ArrayList<>();
                if (dataArray.isArray()) {
                    for (JsonNode node : dataArray) {
                        formularios.add(parseFormulario(node));
                    }
                }

                logger.info("Se obtuvieron {} formularios", formularios.size());
                return formularios;
            } else if (response.getCode() == 401) {
                logger.warn("Sesion expirada (401)");
                throw new RuntimeException("SESION_EXPIRADA");
            } else {
                logger.warn("Error al listar formularios: codigo {}", response.getCode());
                return new ArrayList<>();
            }

        } catch (Exception e) {
            logger.error("Error al listar formularios", e);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    public List<FormularioDTO> listarTodos() {
        try {
            logger.info("Listando TODOS los formularios");

            HttpGet request = new HttpGet(baseUrl + "/api/surveys");
            addAuthHeader(request);

            ClassicHttpResponse response = httpClient.executeOpen(null, request, null);
            String responseBody = EntityUtils.toString(response.getEntity());

            if (response.getCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                JsonNode dataArray = jsonResponse.has("forms")
                        ? jsonResponse.path("forms")
                        : jsonResponse.path("data").path("forms");

                List<FormularioDTO> formularios = new ArrayList<>();
                if (dataArray.isArray()) {
                    for (JsonNode node : dataArray) {
                        formularios.add(parseFormulario(node));
                    }
                }

                logger.info("Se obtuvieron {} formularios en total", formularios.size());
                return formularios;
            } else if (response.getCode() == 401) {
                logger.warn("Sesion expirada (401)");
                throw new RuntimeException("SESION_EXPIRADA");
            } else {
                logger.warn("Error al listar todos: codigo {}", response.getCode());
                return new ArrayList<>();
            }

        } catch (Exception e) {
            logger.error("Error al listar todos los formularios", e);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    public CrearFormularioResponse crearFormulario(String name, String sector, String educationalLevel,
                                                    double latitude, double longitude, String photoBase64) {
        try {
            logger.info("Creando formulario: {}", name);

            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("sector", sector);
            body.put("educationalLevel", educationalLevel);
            body.put("latitude", latitude);
            body.put("longitude", longitude);
            if (photoBase64 != null && !photoBase64.isEmpty()) {
                body.put("photoBase64", photoBase64);
            }

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpPost request = new HttpPost(baseUrl + "/api/surveys");
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            addAuthHeader(request);

            ClassicHttpResponse response = httpClient.executeOpen(null, request, null);
            String responseBody = EntityUtils.toString(response.getEntity());

            if (response.getCode() == 201 || response.getCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                String id = jsonResponse.path("form").path("id").asText();
                if (id.isEmpty()) {
                    id = jsonResponse.path("data").path("form").path("_id").asText();
                }
                logger.info("Formulario creado exitosamente con ID: {}", id);
                return new CrearFormularioResponse(true, id, "OK");
            } else if (response.getCode() == 401) {
                throw new RuntimeException("SESION_EXPIRADA");
            } else {
                logger.warn("Error al crear formulario: codigo {}", response.getCode());
                return new CrearFormularioResponse(false, null, "Error");
            }

        } catch (Exception e) {
            logger.error("Error al crear formulario", e);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    public CrearFormularioResponse actualizarFormulario(String id, String name, String sector, String educationalLevel,
                                                        double latitude, double longitude, String photoBase64) {
        try {
            logger.info("Actualizando formulario: {}", id);

            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("sector", sector);
            body.put("educationalLevel", educationalLevel);
            body.put("latitude", latitude);
            body.put("longitude", longitude);
            if (photoBase64 != null && !photoBase64.isEmpty()) {
                body.put("photoBase64", photoBase64);
            }

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpPut request = new HttpPut(baseUrl + "/api/surveys/" + id);
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            addAuthHeader(request);

            ClassicHttpResponse response = httpClient.executeOpen(null, request, null);
            String responseBody = EntityUtils.toString(response.getEntity());

            if (response.getCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                logger.info("Formulario actualizado exitosamente");
                return new CrearFormularioResponse(true, id, "OK");
            } else if (response.getCode() == 401) {
                throw new RuntimeException("SESION_EXPIRADA");
            } else {
                logger.warn("Error al actualizar formulario: codigo {}", response.getCode());
                return new CrearFormularioResponse(false, null, "Error");
            }

        } catch (Exception e) {
            logger.error("Error al actualizar formulario", e);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    public boolean eliminarFormulario(String id) {
        try {
            logger.info("Eliminando formulario: {}", id);

            HttpDelete request = new HttpDelete(baseUrl + "/api/surveys/" + id);
            addAuthHeader(request);

            ClassicHttpResponse response = httpClient.executeOpen(null, request, null);

            if (response.getCode() == 200) {
                logger.info("Formulario eliminado exitosamente");
                return true;
            } else if (response.getCode() == 401) {
                throw new RuntimeException("SESION_EXPIRADA");
            } else {
                logger.warn("Error al eliminar formulario: codigo {}", response.getCode());
                return false;
            }

        } catch (Exception e) {
            logger.error("Error al eliminar formulario", e);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    private FormularioDTO parseFormulario(JsonNode node) {
        String id = node.has("_id") ? node.path("_id").asText() : node.path("id").asText();
        String photo = node.has("photoBase64") && !node.get("photoBase64").isNull()
                ? node.get("photoBase64").asText()
                : (node.path("hasPhoto").asBoolean(false) ? "HAS_PHOTO" : null);

        return new FormularioDTO(
                id,
                node.path("name").asText(),
                node.path("sector").asText(),
                node.path("educationalLevel").asText(),
                node.path("latitude").asDouble(),
                node.path("longitude").asDouble(),
                photo,
                node.path("createdAt").asText(),
                node.path("userId").asText(),
                node.path("username").asText()
        );
    }

    private void addAuthHeader(HttpGet request) {
        if (authToken != null && !authToken.isEmpty()) {
            request.setHeader("Authorization", "Bearer " + authToken);
        }
    }

    private void addAuthHeader(HttpPost request) {
        if (authToken != null && !authToken.isEmpty()) {
            request.setHeader("Authorization", "Bearer " + authToken);
        }
    }

    private void addAuthHeader(HttpPut request) {
        if (authToken != null && !authToken.isEmpty()) {
            request.setHeader("Authorization", "Bearer " + authToken);
        }
    }

    private void addAuthHeader(HttpDelete request) {
        if (authToken != null && !authToken.isEmpty()) {
            request.setHeader("Authorization", "Bearer " + authToken);
        }
    }

    public static class LoginResponse {
        public boolean success;
        public String token;
        public String username;
        public String userId;

        public LoginResponse(boolean success, String token, String username, String userId) {
            this.success = success;
            this.token = token;
            this.username = username;
            this.userId = userId;
        }
    }

    public static class CrearFormularioResponse {
        public boolean success;
        public String id;
        public String message;

        public CrearFormularioResponse(boolean success, String id, String message) {
            this.success = success;
            this.id = id;
            this.message = message;
        }
    }
}
