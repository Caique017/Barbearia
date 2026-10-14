package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Cliente;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.cliente.ClienteCadastroRequest;
import com.barbearia.api.dto.cliente.ClienteCadastroResponse;
import com.barbearia.api.enums.Role;
import com.barbearia.api.exceptions.CredenciaisInvalidasException;
import com.barbearia.api.exceptions.TentativasExcedidasException;
import com.barbearia.api.exceptions.UsuarioExistenteException;
import com.barbearia.api.repositories.BarbeiroRepository;
import com.barbearia.api.repositories.ClienteRepository;
import com.barbearia.api.repositories.UsuarioRepository;
import com.barbearia.api.security.LimiteTentativasLogin;
import com.barbearia.api.security.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "pessoa@ex.com";
    private static final String TELEFONE = "11999998888";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private BarbeiroRepository barbeiroRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private LimiteTentativasLogin limiteTentativas;

    @InjectMocks
    private AuthService authService;

    private static Usuario usuario(Role role) {
        return Usuario.builder().nome("Pessoa").email(EMAIL).senha("hash").role(role).build();
    }

    @Test
    void loginCorretoLimpaAsFalhas() {
        Usuario barbeiro = usuario(Role.BARBEIRO);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(barbeiro));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);

        assertThat(authService.login(EMAIL, "senha123", Role.BARBEIRO)).isSameAs(barbeiro);
        verify(limiteTentativas).limpar(EMAIL);
    }

    @Test
    void osTresCasosDeFalhaDevolvemAMesmaMensagem() {
        when(usuarioRepository.findByEmail("inexistente@ex.com")).thenReturn(Optional.empty());
        Throwable emailInexistente = catchThrowable(() -> authService.login("inexistente@ex.com", "x", Role.BARBEIRO));

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(Role.BARBEIRO)));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);
        Throwable senhaErrada = catchThrowable(() -> authService.login(EMAIL, "errada", Role.BARBEIRO));

        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(Role.CLIENTE)));
        Throwable roleErrada = catchThrowable(() -> authService.login(EMAIL, "senha123", Role.BARBEIRO));

        assertThat(emailInexistente).isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(senhaErrada).isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(roleErrada).isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(senhaErrada.getMessage()).isEqualTo(emailInexistente.getMessage());
        assertThat(roleErrada.getMessage()).isEqualTo(emailInexistente.getMessage());
    }

    @Test
    void emailInexistenteAindaRodaOBcryptParaNaoEntregarPeloTempo() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        catchThrowable(() -> authService.login(EMAIL, "senha123", Role.BARBEIRO));

        verify(passwordEncoder).matches(eq("senha123"), anyString());
    }

    @Test
    void roleErradaComSenhaCertaNaoAutentica() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(Role.CLIENTE)));

        assertThatThrownBy(() -> authService.login(EMAIL, "senha123", Role.BARBEIRO))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(passwordEncoder, never()).matches("senha123", "hash");
    }

    @Test
    void falhaRegistraTentativa() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        catchThrowable(() -> authService.login(EMAIL, "x", Role.BARBEIRO));

        verify(limiteTentativas).registrarFalha(EMAIL);
        verify(limiteTentativas, never()).limpar(any());
    }

    @Test
    void emailBloqueadoNemConsultaOBanco() {
        doThrow(new TentativasExcedidasException("bloqueado", 60)).when(limiteTentativas).verificar(EMAIL);

        assertThatThrownBy(() -> authService.login(EMAIL, "senha123", Role.BARBEIRO))
                .isInstanceOf(TentativasExcedidasException.class);
        verify(usuarioRepository, never()).findByEmail(any());
    }

    private Barbeiro barbeiroExistente() {
        Barbeiro barbeiro = Barbeiro.builder().id(UUID.randomUUID()).build();
        when(barbeiroRepository.findById(barbeiro.getId())).thenReturn(Optional.of(barbeiro));
        return barbeiro;
    }

    private static ClienteCadastroRequest cadastro(Barbeiro barbeiro) {
        return new ClienteCadastroRequest("Invasor", TELEFONE, EMAIL, "senha123", barbeiro.getId());
    }

    @Test
    void telefoneNovoCriaClienteJaVinculado() {
        Barbeiro barbeiro = barbeiroExistente();

        ClienteCadastroResponse response = authService.cadastroCliente(cadastro(barbeiro));

        ArgumentCaptor<Cliente> salvo = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(salvo.capture());
        assertThat(salvo.getValue().getUsuario()).isNotNull();
        assertThat(salvo.getValue().getUsuarioPendente()).isNull();
        assertThat(response.vinculoPendente()).isFalse();
    }

    @Test
    void telefoneDeClienteSemContaViraPedidoDeVinculoSemTrocarONome() {
        Barbeiro barbeiro = barbeiroExistente();
        Cliente carlos = Cliente.builder().barbeiro(barbeiro).nome("Carlos").telefone(TELEFONE).build();
        when(clienteRepository.findByTelefoneAndBarbeiroId(TELEFONE, barbeiro.getId())).thenReturn(Optional.of(carlos));

        ClienteCadastroResponse response = authService.cadastroCliente(cadastro(barbeiro));

        assertThat(response.vinculoPendente()).isTrue();
        assertThat(carlos.getUsuario()).isNull();
        assertThat(carlos.getUsuarioPendente()).isNotNull();
        assertThat(carlos.getNome()).isEqualTo("Carlos");
    }

    @Test
    void telefoneDeClienteComContaDa409SemCriarUsuario() {
        Barbeiro barbeiro = barbeiroExistente();
        Cliente comConta = Cliente.builder().barbeiro(barbeiro).telefone(TELEFONE).usuario(new Usuario()).build();
        when(clienteRepository.findByTelefoneAndBarbeiroId(TELEFONE, barbeiro.getId())).thenReturn(Optional.of(comConta));

        assertThatThrownBy(() -> authService.cadastroCliente(cadastro(barbeiro)))
                .isInstanceOf(UsuarioExistenteException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void telefoneComPedidoPendenteDa409SemCriarUsuario() {
        Barbeiro barbeiro = barbeiroExistente();
        Cliente pendente = Cliente.builder().barbeiro(barbeiro).telefone(TELEFONE).usuarioPendente(new Usuario()).build();
        when(clienteRepository.findByTelefoneAndBarbeiroId(TELEFONE, barbeiro.getId())).thenReturn(Optional.of(pendente));

        assertThatThrownBy(() -> authService.cadastroCliente(cadastro(barbeiro)))
                .isInstanceOf(UsuarioExistenteException.class)
                .hasMessageContaining("aguardando");
        verify(usuarioRepository, never()).save(any());
    }
}
