package com.barbearia.api.service;

import com.barbearia.api.domain.Assinatura;
import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Cliente;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.enums.Role;
import com.barbearia.api.exceptions.DescadastroInvalidoException;
import com.barbearia.api.exceptions.SenhaIncorretaException;
import com.barbearia.api.repositories.AgendamentoRepository;
import com.barbearia.api.repositories.AssinaturaRepository;
import com.barbearia.api.repositories.BarbeiroHorarioRepository;
import com.barbearia.api.repositories.BarbeiroRepository;
import com.barbearia.api.repositories.BloqueioHorarioRepository;
import com.barbearia.api.repositories.ClienteRepository;
import com.barbearia.api.repositories.ServicoRepository;
import com.barbearia.api.repositories.UsuarioRepository;
import com.barbearia.api.security.EventosSeguranca;
import com.barbearia.api.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-09T13:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private BarbeiroRepository barbeiroRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AssinaturaRepository assinaturaRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private BarbeiroHorarioRepository horarioRepository;
    @Mock
    private BloqueioHorarioRepository bloqueioRepository;
    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private GatewayPagamento gateway;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private EventosSeguranca eventos;
    @Mock
    private RelogioLocal relogio;

    private ContaService contaService;

    @BeforeEach
    void criar() {
        contaService = new ContaService(usuarioRepository, barbeiroRepository, clienteRepository,
                assinaturaRepository, servicoRepository, horarioRepository, bloqueioRepository,
                agendamentoRepository, gateway, passwordEncoder, tokenService, eventos, relogio,
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private Usuario usuario(Role role) {
        Usuario usuario = Usuario.builder().id(UUID.randomUUID()).email("a@ex.com").senha("hash").role(role).build();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        return usuario;
    }

    @Test
    void senhaErradaNaoApagaNada() {
        Usuario barbeiro = usuario(Role.BARBEIRO);
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> contaService.excluir(barbeiro, "errada"))
                .isInstanceOf(SenhaIncorretaException.class);
        verify(usuarioRepository, never()).apagarTodos(any());
        verify(gateway, never()).excluirCliente(any());
    }

    @Test
    void barbeiroApagaOClienteDoStripeAsContasDosClientesEAPropria() {
        Usuario dono = usuario(Role.BARBEIRO);
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        Barbeiro barbeiro = Barbeiro.builder().id(UUID.randomUUID()).usuario(dono).build();
        when(barbeiroRepository.findByUsuario(dono)).thenReturn(Optional.of(barbeiro));
        when(assinaturaRepository.findByBarbeiroId(barbeiro.getId()))
                .thenReturn(Optional.of(Assinatura.builder().stripeCustomerId("cus_1").build()));
        Usuario contaCliente = Usuario.builder().id(UUID.randomUUID()).build();
        Usuario contaPendente = Usuario.builder().id(UUID.randomUUID()).build();
        when(clienteRepository.findByBarbeiroIdOrderByNomeAsc(barbeiro.getId())).thenReturn(List.of(
                Cliente.builder().usuario(contaCliente).build(),
                Cliente.builder().usuarioPendente(contaPendente).build(),
                Cliente.builder().build()));

        contaService.excluir(dono, "senha123");

        verify(gateway).excluirCliente("cus_1");
        verify(usuarioRepository).apagarTodos(List.of(contaCliente.getId(), contaPendente.getId(), dono.getId()));
    }

    @Test
    void barbeiroQueNuncaPagouNaoChamaOStripe() {
        Usuario dono = usuario(Role.BARBEIRO);
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        Barbeiro barbeiro = Barbeiro.builder().id(UUID.randomUUID()).usuario(dono).build();
        when(barbeiroRepository.findByUsuario(dono)).thenReturn(Optional.of(barbeiro));
        when(assinaturaRepository.findByBarbeiroId(barbeiro.getId()))
                .thenReturn(Optional.of(Assinatura.builder().build()));

        contaService.excluir(dono, "senha123");

        verify(gateway, never()).excluirCliente(any());
        verify(usuarioRepository).apagarTodos(List.of(dono.getId()));
    }

    @Test
    void clienteCancelaOsFuturosEApagaSoAPropriaConta() {
        Usuario conta = usuario(Role.CLIENTE);
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        Cliente cliente = Cliente.builder().id(UUID.randomUUID()).usuario(conta).build();
        when(clienteRepository.findByUsuarioId(conta.getId())).thenReturn(Optional.of(cliente));
        LocalDateTime agoraLocal = LocalDateTime.of(2026, 11, 9, 10, 0);
        when(relogio.agora()).thenReturn(agoraLocal);

        contaService.excluir(conta, "senha123");

        verify(agendamentoRepository).cancelarFuturosDoCliente(cliente.getId(), agoraLocal);
        verify(usuarioRepository).apagarTodos(List.of(conta.getId()));
        verify(gateway, never()).excluirCliente(any());
    }

    @Test
    void descadastroComLinkInvalidoDa400() {
        when(tokenService.validarTokenDescadastro("lixo")).thenReturn(null);

        assertThatThrownBy(() -> contaService.descadastrar("lixo"))
                .isInstanceOf(DescadastroInvalidoException.class);
    }

    @Test
    void descadastroDesligaLembretesEMarketing() {
        Usuario usuario = usuario(Role.BARBEIRO);
        usuario.definirMarketing(true, AGORA);
        when(tokenService.validarTokenDescadastro("tk")).thenReturn(usuario.getId());

        contaService.descadastrar("tk");

        assertThat(usuario.recebeLembretes()).isFalse();
        assertThat(usuario.aceitaMarketing()).isFalse();
    }
}
