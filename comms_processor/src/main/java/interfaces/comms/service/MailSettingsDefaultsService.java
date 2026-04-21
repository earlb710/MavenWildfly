package interfaces.comms.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads default IMAP and SMTP settings from JSON resources and applies them as fallbacks.
 */
@ApplicationScoped
public class MailSettingsDefaultsService {

    private static final Logger logger = Logger.getLogger(MailSettingsDefaultsService.class.getName());
    private static final String IMAP_DEFAULTS_RESOURCE = "/imap-default-settings.json";
    private static final String SMTP_DEFAULTS_RESOURCE = "/smtp-default-settings.json";

    private volatile Map<String, Object> imapDefaults;
    private volatile Map<String, Object> smtpDefaults;

    public Map<String, String> applyImapDefaults(Map<String, String> request) {
        Map<String, String> resolved = new HashMap<>();
        if (request != null) {
            resolved.putAll(request);
        }

        Map<String, Object> defaults = getImapDefaults();
        applyStringDefault(resolved, "host", defaults, "host");
        applyStringDefault(resolved, "mailboxHost", defaults, "host");
        applyStringDefault(resolved, "mailboxFolder", defaults, "mailboxFolder");
        return resolved;
    }

    public Map<String, Object> applySmtpDefaults(Map<String, Object> request) {
        Map<String, Object> resolved = new HashMap<>();
        if (request != null) {
            resolved.putAll(request);
        }

        Map<String, Object> defaults = getSmtpDefaults();
        applyObjectStringDefault(resolved, "host", defaults, "host");
        applyObjectStringDefault(resolved, "smtpHost", defaults, "host");
        applyObjectIntegerDefault(resolved, "port", defaults, "port");
        return resolved;
    }

    public Map<String, String> applySmtpDefaultsToStringMap(Map<String, String> request) {
        Map<String, String> resolved = new HashMap<>();
        if (request != null) {
            resolved.putAll(request);
        }

        Map<String, Object> defaults = getSmtpDefaults();
        applyStringDefault(resolved, "host", defaults, "host");
        applyStringDefault(resolved, "smtpHost", defaults, "host");
        return resolved;
    }

    private Map<String, Object> getImapDefaults() {
        if (imapDefaults == null) {
            synchronized (this) {
                if (imapDefaults == null) {
                    imapDefaults = loadDefaults(IMAP_DEFAULTS_RESOURCE);
                }
            }
        }
        return imapDefaults;
    }

    private Map<String, Object> getSmtpDefaults() {
        if (smtpDefaults == null) {
            synchronized (this) {
                if (smtpDefaults == null) {
                    smtpDefaults = loadDefaults(SMTP_DEFAULTS_RESOURCE);
                }
            }
        }
        return smtpDefaults;
    }

    private Map<String, Object> loadDefaults(String resourcePath) {
        try (InputStream input = getClass().getResourceAsStream(resourcePath)) {
            if (input == null) {
                logger.warning("Default settings resource not found: " + resourcePath);
                return new HashMap<>();
            }

            try (JsonReader reader = Json.createReader(input)) {
                JsonObject jsonObject = reader.readObject();
                return toMap(jsonObject);
            }
        } catch (Exception e) {
            logger.warning("Could not load default settings from " + resourcePath + ": " + e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, Object> toMap(JsonObject jsonObject) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            map.put(entry.getKey(), toJavaValue(entry.getValue()));
        }
        return map;
    }

    private Object toJavaValue(JsonValue value) {
        switch (value.getValueType()) {
            case STRING:
                return ((JsonString) value).getString();
            case NUMBER:
                JsonNumber number = (JsonNumber) value;
                return number.isIntegral() ? number.intValue() : number.doubleValue();
            case TRUE:
                return true;
            case FALSE:
                return false;
            case ARRAY:
                return toList(value.asJsonArray());
            case OBJECT:
                return toMap(value.asJsonObject());
            case NULL:
            default:
                return null;
        }
    }

    private List<Object> toList(JsonArray jsonArray) {
        List<Object> values = new ArrayList<>();
        for (JsonValue item : jsonArray) {
            values.add(toJavaValue(item));
        }
        return values;
    }

    private void applyStringDefault(Map<String, String> request, String requestKey,
            Map<String, Object> defaults, String defaultKey) {
        if (isBlank(request.get(requestKey))) {
            String defaultValue = getString(defaults.get(defaultKey));
            if (!isBlank(defaultValue)) {
                request.put(requestKey, defaultValue);
            }
        }
    }

    private void applyObjectStringDefault(Map<String, Object> request, String requestKey,
            Map<String, Object> defaults, String defaultKey) {
        Object currentValue = request.get(requestKey);
        if (!(currentValue instanceof String) || isBlank((String) currentValue)) {
            String defaultValue = getString(defaults.get(defaultKey));
            if (!isBlank(defaultValue)) {
                request.put(requestKey, defaultValue);
            }
        }
    }

    private void applyObjectIntegerDefault(Map<String, Object> request, String requestKey,
            Map<String, Object> defaults, String defaultKey) {
        if (request.get(requestKey) == null) {
            Object defaultValue = defaults.get(defaultKey);
            if (defaultValue instanceof Number) {
                request.put(requestKey, ((Number) defaultValue).intValue());
            }
        }
    }

    private String getString(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
