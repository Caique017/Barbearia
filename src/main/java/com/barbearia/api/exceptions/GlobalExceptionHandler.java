package com.barbearia.api.exceptions;

import com.barbearia.api.dto.error.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UsuarioExistenteException.class)
    public ResponseEntity<ErroResposta> tratarUsuarioExistente(UsuarioExistenteException ex,
                                                               HttpServletRequest request) {
        return montar(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> tratarViolacaoIntegridade(DataIntegrityViolationException ex,
                                                                  HttpServletRequest request) {
        log.warn("Violação de integridade ao processar {}: {}", request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return montar(HttpStatus.CONFLICT,
                "Já existe um registro com esses dados.", request, null);
    }

    @ExceptionHandler({UsuarioNaoEncontradoException.class, ClienteNaoEncontradoException.class,
            BarbeiroNaoEncontradoException.class})
    public ResponseEntity<ErroResposta> tratarNaoEncontrado(RuntimeException ex, HttpServletRequest request) {
        return montar(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(MethodArgumentNotValidException ex,
                                                        HttpServletRequest request) {
        List<ErroResposta.CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapearCampo)
                .toList();
        return montar(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.", request, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarCorpoIlegivel(HttpMessageNotReadableException ex,
                                                            HttpServletRequest request) {
        return montar(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado.", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tratarTipoInvalido(MethodArgumentTypeMismatchException ex,
                                                           HttpServletRequest request) {
        return montar(HttpStatus.BAD_REQUEST,
                "O parâmetro '" + ex.getName() + "' possui formato inválido.", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResposta> tratarNaoAutenticado(AuthenticationException ex,
                                                             HttpServletRequest request) {
        return montar(HttpStatus.UNAUTHORIZED,
                "Autenticação necessária. Token ausente ou inválido.", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResposta> tratarAcessoNegado(AccessDeniedException ex,
                                                           HttpServletRequest request) {
        return montar(HttpStatus.FORBIDDEN,
                "Acesso negado. Você não tem permissão para acessar este recurso.", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarGenerico(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {}", request.getRequestURI(), ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.", request, null);
    }

    private ErroResposta.CampoErro mapearCampo(FieldError erro) {
        return new ErroResposta.CampoErro(erro.getField(), erro.getDefaultMessage());
    }

    private ResponseEntity<ErroResposta> montar(HttpStatus status, String mensagem,
                                                HttpServletRequest request,
                                                List<ErroResposta.CampoErro> campos) {
        ErroResposta corpo = new ErroResposta(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI(),
                campos
        );
        return ResponseEntity.status(status).body(corpo);
    }
}
