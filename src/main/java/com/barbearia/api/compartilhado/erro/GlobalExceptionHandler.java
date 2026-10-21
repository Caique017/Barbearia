package com.barbearia.api.compartilhado.erro;

import com.barbearia.api.auth.CredenciaisInvalidasException;
import com.barbearia.api.auth.TentativasExcedidasException;
import com.barbearia.api.auth.TokenRedefinicaoInvalidoException;
import com.barbearia.api.auth.UsuarioExistenteException;
import com.barbearia.api.barbeiro.BarbeiroNaoEncontradoException;
import com.barbearia.api.barbeiro.SlugIndisponivelException;
import com.barbearia.api.cliente.ClienteNaoEncontradoException;
import com.barbearia.api.cliente.VinculoNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UsuarioExistenteException.class)
    public ResponseEntity<ErroResposta> tratarUsuarioExistente(UsuarioExistenteException ex,
                                                               HttpServletRequest request) {
        return montar(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(SlugIndisponivelException.class)
    public ResponseEntity<ErroResposta> tratarSlugIndisponivel(SlugIndisponivelException ex,
                                                               HttpServletRequest request) {
        return montar(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> tratarViolacaoIntegridade(DataIntegrityViolationException ex,
                                                                  HttpServletRequest request) {
        log.warn("Violação de integridade ao processar {}: {}", request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return montar(HttpStatus.CONFLICT, "Já existe um registro com esses dados.", request);
    }

    @ExceptionHandler({ClienteNaoEncontradoException.class, VinculoNaoEncontradoException.class,
            BarbeiroNaoEncontradoException.class})
    public ResponseEntity<ErroResposta> tratarNaoEncontrado(RuntimeException ex, HttpServletRequest request) {
        return montar(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResposta> tratarCredenciaisInvalidas(CredenciaisInvalidasException ex,
                                                                   HttpServletRequest request) {
        return montar(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(TentativasExcedidasException.class)
    public ResponseEntity<ErroResposta> tratarTentativasExcedidas(TentativasExcedidasException ex,
                                                                  HttpServletRequest request) {
        ResponseEntity<ErroResposta> resposta = montar(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
        return ResponseEntity.status(resposta.getStatusCode())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosParaLiberar()))
                .body(resposta.getBody());
    }

    @ExceptionHandler(TokenRedefinicaoInvalidoException.class)
    public ResponseEntity<ErroResposta> tratarTokenRedefinicaoInvalido(TokenRedefinicaoInvalidoException ex,
                                                                       HttpServletRequest request) {
        return montar(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
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
        return montar(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResposta> tratarRotaInexistente(NoResourceFoundException ex,
                                                              HttpServletRequest request) {
        return montar(HttpStatus.NOT_FOUND, "Recurso não encontrado.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResposta> tratarMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex,
                                                                 HttpServletRequest request) {
        return montar(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado nesta rota.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResposta> tratarTipoDeConteudoNaoSuportado(HttpMediaTypeNotSupportedException ex,
                                                                         HttpServletRequest request) {
        return montar(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie o corpo como application/json.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tratarTipoInvalido(MethodArgumentTypeMismatchException ex,
                                                           HttpServletRequest request) {
        return montar(HttpStatus.BAD_REQUEST,
                "O parâmetro '" + ex.getName() + "' possui formato inválido.", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResposta> tratarNaoAutenticado(AuthenticationException ex,
                                                             HttpServletRequest request) {
        return montar(HttpStatus.UNAUTHORIZED,
                "Autenticação necessária. Token ausente ou inválido.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResposta> tratarAcessoNegado(AccessDeniedException ex,
                                                           HttpServletRequest request) {
        return montar(HttpStatus.FORBIDDEN,
                "Acesso negado. Você não tem permissão para acessar este recurso.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarGenerico(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {}", request.getRequestURI(), ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.", request);
    }

    private ErroResposta.CampoErro mapearCampo(FieldError erro) {
        return new ErroResposta.CampoErro(erro.getField(), erro.getDefaultMessage());
    }

    private ResponseEntity<ErroResposta> montar(HttpStatus status, String mensagem,
                                                HttpServletRequest request) {
        return montar(status, mensagem, request, null);
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
