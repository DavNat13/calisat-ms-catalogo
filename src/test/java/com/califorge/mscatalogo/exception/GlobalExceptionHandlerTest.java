package com.califorge.mscatalogo.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifica el mapeo status HTTP de cada manejador del GlobalExceptionHandler.
 * Con la correccion v1.0.4, una entrada malformada o una ruta inexistente
 * responden 400/404 y ya no caen en el 500 generico.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void skuDuplicado_responde400() {
        ResponseEntity<Map<String, Object>> r = handler.skuDuplicado(new SkuDuplicadoException("AB-1"));
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }

    @Test
    void validacionBean_responde400ConCampoYDetalle() {
        BindingResult br = mock(BindingResult.class);
        when(br.getFieldErrors()).thenReturn(List.of(new FieldError("request", "sku", "sku es obligatorio")));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, br);

        ResponseEntity<Map<String, Object>> r = handler.validacion(ex);

        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().get("mensaje").toString().contains("sku"));
    }

    @Test
    void jsonIlegible_responde400() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("cuerpo ilegible", mock(HttpInputMessage.class));

        ResponseEntity<Map<String, Object>> r = handler.requestMalformado(ex);

        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().get("mensaje").toString().toLowerCase().contains("json"));
    }

    @Test
    void tipoDeArgumentoInvalido_responde400() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null);

        ResponseEntity<Map<String, Object>> r = handler.tipoArgumentoInvalido(ex);

        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().get("mensaje").toString().contains("id"));
    }

    @Test
    void parametroRequeridoFaltante_responde400() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("page", "int");

        ResponseEntity<Map<String, Object>> r = handler.parametroFaltante(ex);

        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().get("mensaje").toString().contains("page"));
    }

    @Test
    void rutaInexistente_responde404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET,
                "/api/v1/catalogo/no-existe", "GET /api/v1/catalogo/no-existe");

        ResponseEntity<Map<String, Object>> r = handler.noResourceFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
    }

    @Test
    void violacionDeIntegridad_responde400() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("viola constraint UNIQUE");

        ResponseEntity<Map<String, Object>> r = handler.integridad(ex);

        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }

    @Test
    void errorInesperado_responde500SinDetalleInterno() {
        Exception ex = new RuntimeException("causa oculta interna");

        ResponseEntity<Map<String, Object>> r = handler.generico(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
        String mensaje = r.getBody().get("mensaje").toString();
        assertTrue(mensaje.contains("Error interno"));
        assertTrue(!mensaje.contains("causa oculta"));
    }
}