-- Oracle PL/SQL client package for calling the MavenWildfly REST API.
--
-- Run this script as the schema that should own the package:
--   SQL> @database/oracle/comms_rest_client_pkg.sql
--
-- The executing schema also needs an Oracle network ACL that allows outbound
-- HTTP(S) connections to the WildFly host. Example:
--
-- BEGIN
--   DBMS_NETWORK_ACL_ADMIN.APPEND_HOST_ACE(
--     host       => 'localhost',
--     lower_port => 8080,
--     upper_port => 8080,
--     ace        => xs$ace_type(
--       privilege_list => xs$name_list('connect'),
--       principal_name => USER,
--       principal_type => xs_acl.ptype_db));
-- END;
-- /

CREATE OR REPLACE PACKAGE comms_rest_client AUTHID CURRENT_USER AS
  c_default_base_url CONSTANT VARCHAR2(4000) := 'http://localhost:8080/comms_processor';

  FUNCTION request(
    p_endpoint        IN VARCHAR2,
    p_base_url        IN VARCHAR2 DEFAULT c_default_base_url,
    p_method          IN VARCHAR2 DEFAULT 'GET',
    p_body            IN CLOB DEFAULT NULL,
    p_content_type    IN VARCHAR2 DEFAULT 'application/json',
    p_accept          IN VARCHAR2 DEFAULT 'application/json',
    p_wallet_path     IN VARCHAR2 DEFAULT NULL,
    p_wallet_password IN VARCHAR2 DEFAULT NULL,
    p_username        IN VARCHAR2 DEFAULT NULL,
    p_password        IN VARCHAR2 DEFAULT NULL,
    p_timeout_seconds IN NUMBER DEFAULT 30
  ) RETURN CLOB;

  FUNCTION post_json(
    p_endpoint        IN VARCHAR2,
    p_json_body       IN CLOB,
    p_base_url        IN VARCHAR2 DEFAULT c_default_base_url,
    p_wallet_path     IN VARCHAR2 DEFAULT NULL,
    p_wallet_password IN VARCHAR2 DEFAULT NULL,
    p_timeout_seconds IN NUMBER DEFAULT 30
  ) RETURN CLOB;

  FUNCTION get_status(
    p_base_url        IN VARCHAR2 DEFAULT c_default_base_url,
    p_timeout_seconds IN NUMBER DEFAULT 30
  ) RETURN CLOB;

  FUNCTION ping(
    p_base_url        IN VARCHAR2 DEFAULT c_default_base_url,
    p_timeout_seconds IN NUMBER DEFAULT 30
  ) RETURN CLOB;

  FUNCTION get_datasources(
    p_base_url        IN VARCHAR2 DEFAULT c_default_base_url,
    p_timeout_seconds IN NUMBER DEFAULT 30
  ) RETURN CLOB;

  FUNCTION send_text_message(
    p_json_body       IN CLOB,
    p_base_url        IN VARCHAR2 DEFAULT c_default_base_url,
    p_timeout_seconds IN NUMBER DEFAULT 30
  ) RETURN CLOB;
END comms_rest_client;
/

SHOW ERRORS PACKAGE comms_rest_client

CREATE OR REPLACE PACKAGE BODY comms_rest_client AS
  c_chunk_size CONSTANT PLS_INTEGER := 32767;

  FUNCTION trim_trailing_slash(p_value IN VARCHAR2) RETURN VARCHAR2 IS
  BEGIN
    RETURN RTRIM(p_value, '/');
  END trim_trailing_slash;

  FUNCTION normalize_endpoint(p_endpoint IN VARCHAR2) RETURN VARCHAR2 IS
  BEGIN
    IF p_endpoint IS NULL THEN
      RAISE_APPLICATION_ERROR(-20000, 'Endpoint must not be null');
    END IF;

    IF SUBSTR(p_endpoint, 1, 1) = '/' THEN
      RETURN p_endpoint;
    END IF;

    RETURN '/' || p_endpoint;
  END normalize_endpoint;

  FUNCTION build_url(
    p_base_url IN VARCHAR2,
    p_endpoint IN VARCHAR2
  ) RETURN VARCHAR2 IS
  BEGIN
    IF p_base_url IS NULL THEN
      RAISE_APPLICATION_ERROR(-20001, 'Base URL must not be null');
    END IF;

    RETURN trim_trailing_slash(p_base_url) || normalize_endpoint(p_endpoint);
  END build_url;

  PROCEDURE write_body(
    p_request IN OUT NOCOPY UTL_HTTP.req,
    p_body    IN CLOB
  ) IS
    l_offset PLS_INTEGER := 1;
    l_length PLS_INTEGER;
    l_chunk  VARCHAR2(32767);
  BEGIN
    IF p_body IS NULL THEN
      RETURN;
    END IF;

    l_length := DBMS_LOB.GETLENGTH(p_body);

    WHILE l_offset <= l_length LOOP
      l_chunk := DBMS_LOB.SUBSTR(p_body, c_chunk_size, l_offset);
      UTL_HTTP.WRITE_TEXT(p_request, l_chunk);
      l_offset := l_offset + c_chunk_size;
    END LOOP;
  END write_body;

  FUNCTION request(
    p_endpoint        IN VARCHAR2,
    p_base_url        IN VARCHAR2 DEFAULT c_default_base_url,
    p_method          IN VARCHAR2 DEFAULT 'GET',
    p_body            IN CLOB DEFAULT NULL,
    p_content_type    IN VARCHAR2 DEFAULT 'application/json',
    p_accept          IN VARCHAR2 DEFAULT 'application/json',
    p_wallet_path     IN VARCHAR2 DEFAULT NULL,
    p_wallet_password IN VARCHAR2 DEFAULT NULL,
    p_username        IN VARCHAR2 DEFAULT NULL,
    p_password        IN VARCHAR2 DEFAULT NULL,
    p_timeout_seconds IN NUMBER DEFAULT 30
  ) RETURN CLOB IS
    l_url      VARCHAR2(4000);
    l_request  UTL_HTTP.req;
    l_response UTL_HTTP.resp;
    l_result   CLOB;
    l_buffer   VARCHAR2(32767);
  BEGIN
    l_url := build_url(p_base_url, p_endpoint);
    DBMS_LOB.CREATETEMPORARY(l_result, TRUE);

    IF p_wallet_path IS NOT NULL THEN
      UTL_HTTP.SET_WALLET(p_wallet_path, p_wallet_password);
    END IF;

    UTL_HTTP.SET_TRANSFER_TIMEOUT(GREATEST(1, NVL(TRUNC(p_timeout_seconds), 30)));
    UTL_HTTP.SET_RESPONSE_ERROR_CHECK(FALSE);

    l_request := UTL_HTTP.BEGIN_REQUEST(l_url, UPPER(NVL(p_method, 'GET')), 'HTTP/1.1');
    UTL_HTTP.SET_HEADER(l_request, 'User-Agent', 'Oracle UTL_HTTP comms_rest_client');
    UTL_HTTP.SET_HEADER(l_request, 'Accept', NVL(p_accept, 'application/json'));

    IF p_body IS NOT NULL THEN
      UTL_HTTP.SET_HEADER(l_request, 'Content-Type', NVL(p_content_type, 'application/json'));
      UTL_HTTP.SET_HEADER(l_request, 'Content-Length', TO_CHAR(DBMS_LOB.GETLENGTH(p_body)));
    END IF;

    IF p_username IS NOT NULL THEN
      UTL_HTTP.SET_AUTHENTICATION(l_request, p_username, p_password);
    END IF;

    write_body(l_request, p_body);

    l_response := UTL_HTTP.GET_RESPONSE(l_request);

    BEGIN
      LOOP
        UTL_HTTP.READ_TEXT(l_response, l_buffer, c_chunk_size);
        DBMS_LOB.WRITEAPPEND(l_result, LENGTH(l_buffer), l_buffer);
      END LOOP;
    EXCEPTION
      WHEN UTL_HTTP.END_OF_BODY THEN
        NULL;
    END;

    UTL_HTTP.END_RESPONSE(l_response);
    RETURN l_result;
  EXCEPTION
    WHEN OTHERS THEN
      BEGIN
        UTL_HTTP.END_RESPONSE(l_response);
      EXCEPTION
        WHEN OTHERS THEN
          NULL;
      END;

      IF l_result IS NOT NULL AND DBMS_LOB.ISTEMPORARY(l_result) = 1 THEN
        DBMS_LOB.FREETEMPORARY(l_result);
      END IF;

      RAISE;
  END request;

  FUNCTION post_json(
    p_endpoint        IN VARCHAR2,
    p_json_body       IN CLOB,
    p_base_url        IN VARCHAR2 DEFAULT c_default_base_url,
    p_wallet_path     IN VARCHAR2 DEFAULT NULL,
    p_wallet_password IN VARCHAR2 DEFAULT NULL,
    p_timeout_seconds IN NUMBER DEFAULT 30
  ) RETURN CLOB IS
  BEGIN
    RETURN request(
      p_endpoint        => p_endpoint,
      p_base_url        => p_base_url,
      p_method          => 'POST',
      p_body            => p_json_body,
      p_content_type    => 'application/json',
      p_wallet_path     => p_wallet_path,
      p_wallet_password => p_wallet_password,
      p_timeout_seconds => p_timeout_seconds);
  END post_json;

  FUNCTION get_status(
    p_base_url        IN VARCHAR2 DEFAULT c_default_base_url,
    p_timeout_seconds IN NUMBER DEFAULT 30
  ) RETURN CLOB IS
  BEGIN
    RETURN request('/api/status', p_base_url, 'GET', p_timeout_seconds => p_timeout_seconds);
  END get_status;

  FUNCTION ping(
    p_base_url        IN VARCHAR2 DEFAULT c_default_base_url,
    p_timeout_seconds IN NUMBER DEFAULT 30
  ) RETURN CLOB IS
  BEGIN
    RETURN request('/api/status/ping', p_base_url, 'GET', p_timeout_seconds => p_timeout_seconds);
  END ping;

  FUNCTION get_datasources(
    p_base_url        IN VARCHAR2 DEFAULT c_default_base_url,
    p_timeout_seconds IN NUMBER DEFAULT 30
  ) RETURN CLOB IS
  BEGIN
    RETURN request('/api/status/datasources', p_base_url, 'GET', p_timeout_seconds => p_timeout_seconds);
  END get_datasources;

  FUNCTION send_text_message(
    p_json_body       IN CLOB,
    p_base_url        IN VARCHAR2 DEFAULT c_default_base_url,
    p_timeout_seconds IN NUMBER DEFAULT 30
  ) RETURN CLOB IS
  BEGIN
    RETURN post_json('/api/smtp/sendTextMessage', p_json_body, p_base_url, p_timeout_seconds => p_timeout_seconds);
  END send_text_message;
END comms_rest_client;
/

SHOW ERRORS PACKAGE BODY comms_rest_client

-- Example calls:
--
-- SET SERVEROUTPUT ON
--
-- DECLARE
--   l_response CLOB;
-- BEGIN
--   l_response := comms_rest_client.ping('http://localhost:8080/comms_processor');
--   DBMS_OUTPUT.PUT_LINE(DBMS_LOB.SUBSTR(l_response, 4000, 1));
-- END;
-- /
--
-- DECLARE
--   l_payload  CLOB := '{"smtpHost":"smtp.example.com","smtpUser":"user@example.com","fromAddress":"sender@example.com","toAddress":"recipient@example.com","subject":"Test Email","body":"Sent from Oracle PL/SQL"}';
--   l_response CLOB;
-- BEGIN
--   l_response := comms_rest_client.send_text_message(l_payload);
--   DBMS_OUTPUT.PUT_LINE(DBMS_LOB.SUBSTR(l_response, 4000, 1));
-- END;
-- /
