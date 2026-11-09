package com.barbearia.api.service;

import com.barbearia.api.domain.Agendamento;
import com.barbearia.api.domain.Assinatura;
import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Cliente;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.conta.DadosContaResponse;
import com.barbearia.api.dto.conta.PreferenciasRequest;
import com.barbearia.api.dto.conta.PreferenciasResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContaService {

    static final String SENHA_INCORRETA = "Senha incorreta.";
    static final String LINK_INVALIDO = "Link de descadastro inválido.";

    private final UsuarioRepository usuarioRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final ServicoRepository servicoRepository;
    private final BarbeiroHorarioRepository horarioRepository;
    private final BloqueioHorarioRepository bloqueioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final GatewayPagamento gateway;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EventosSeguranca eventos;
    private final RelogioLocal relogio;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DadosContaResponse exportar(Usuario logado) {
        Usuario usuario = recarregar(logado);

        DadosContaResponse.Conta conta = new DadosContaResponse.Conta(usuario.getNome(), usuario.getEmail(),
                usuario.getRole(), usuario.getCriadoEm(), usuario.getTermosVersao(), usuario.getTermosAceitosEm(),
                usuario.getMarketingAceitoEm(), usuario.recebeLembretes());

        DadosContaResponse.Barbearia barbearia = usuario.getRole() == Role.BARBEIRO
                ? barbeiroRepository.findByUsuario(usuario).map(this::barbearia).orElse(null)
                : null;

        DadosContaResponse.CadastroCliente cadastro = usuario.getRole() == Role.CLIENTE
                ? clienteRepository.findByUsuarioId(usuario.getId()).map(this::cadastroCliente).orElse(null)
                : null;

        eventos.dadosExportados(usuario.getEmail());
        return new DadosContaResponse(clock.instant(), conta, barbearia, cadastro);
    }

    @Transactional
    public void excluir(Usuario logado, String senha) {
        Usuario usuario = recarregar(logado);
        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new SenhaIncorretaException(SENHA_INCORRETA);
        }

        List<UUID> contas = new ArrayList<>();
        if (usuario.getRole() == Role.BARBEIRO) {
            barbeiroRepository.findByUsuario(usuario).ifPresent(barbeiro -> {
                assinaturaRepository.findByBarbeiroId(barbeiro.getId())
                        .map(Assinatura::getStripeCustomerId)
                        .ifPresent(gateway::excluirCliente);
                contas.addAll(contasDosClientes(barbeiro));
            });
        } else {
            clienteRepository.findByUsuarioId(usuario.getId())
                    .ifPresent(cliente -> agendamentoRepository.cancelarFuturosDoCliente(cliente.getId(),
                            relogio.agora()));
        }

        contas.add(usuario.getId());
        usuarioRepository.apagarTodos(contas);
        eventos.contaExcluida(usuario.getEmail(), usuario.getRole());
    }

    @Transactional(readOnly = true)
    public PreferenciasResponse preferencias(Usuario logado) {
        return preferenciasDe(recarregar(logado));
    }

    @Transactional
    public PreferenciasResponse atualizarPreferencias(Usuario logado, PreferenciasRequest data) {
        Usuario usuario = recarregar(logado);
        if (data.marketing() != null) {
            usuario.definirMarketing(data.marketing(), clock.instant());
        }
        if (data.lembretes() != null) {
            usuario.definirLembretes(data.lembretes(), clock.instant());
        }
        return preferenciasDe(usuario);
    }

    @Transactional
    public void descadastrar(String token) {
        UUID usuarioId = tokenService.validarTokenDescadastro(token);
        if (usuarioId == null) {
            throw new DescadastroInvalidoException(LINK_INVALIDO);
        }
        usuarioRepository.findById(usuarioId).ifPresent(usuario -> usuario.descadastrar(clock.instant()));
    }

    private Usuario recarregar(Usuario logado) {
        return usuarioRepository.findById(logado.getId()).orElseThrow();
    }

    private static PreferenciasResponse preferenciasDe(Usuario usuario) {
        return new PreferenciasResponse(usuario.aceitaMarketing(), usuario.recebeLembretes());
    }

    private List<UUID> contasDosClientes(Barbeiro barbeiro) {
        List<UUID> contas = new ArrayList<>();
        for (Cliente cliente : clienteRepository.findByBarbeiroIdOrderByNomeAsc(barbeiro.getId())) {
            if (cliente.getUsuario() != null) {
                contas.add(cliente.getUsuario().getId());
            }
            if (cliente.getUsuarioPendente() != null) {
                contas.add(cliente.getUsuarioPendente().getId());
            }
        }
        return contas;
    }

    private DadosContaResponse.Barbearia barbearia(Barbeiro barbeiro) {
        UUID id = barbeiro.getId();

        DadosContaResponse.Assinatura assinatura = assinaturaRepository.findByBarbeiroId(id)
                .map(a -> new DadosContaResponse.Assinatura(a.getStatus(), a.getFormaPagamento(), a.getTesteAte(),
                        a.getAcessoAte()))
                .orElse(null);

        List<DadosContaResponse.Servico> servicos = servicoRepository.findByBarbeiroIdOrderByNomeAsc(id).stream()
                .map(s -> new DadosContaResponse.Servico(s.getNome(), s.getPreco(), s.getDuracaoMinutos(),
                        s.getCustoInsumo(), s.isAtivo()))
                .toList();

        List<DadosContaResponse.Faixa> horarios = horarioRepository.doBarbeiro(id).stream()
                .map(h -> new DadosContaResponse.Faixa(h.getDiaSemana(), h.getAbre(), h.getFecha()))
                .toList();

        List<DadosContaResponse.Bloqueio> bloqueios = bloqueioRepository
                .findByBarbeiroIdOrderByInicio(id).stream()
                .map(b -> new DadosContaResponse.Bloqueio(b.getInicio(), b.getFim(), b.getMotivo()))
                .toList();

        List<DadosContaResponse.Cliente> clientes = clienteRepository.findByBarbeiroIdOrderByNomeAsc(id).stream()
                .map(c -> new DadosContaResponse.Cliente(c.getNome(), c.getTelefone(), c.getUsuario() != null))
                .toList();

        List<DadosContaResponse.Atendimento> agendamentos = agendamentoRepository.todosDoBarbeiro(id).stream()
                .map(a -> atendimento(a, a.getCliente().getNome()))
                .toList();

        return new DadosContaResponse.Barbearia(barbeiro.getNomeBarbearia(), barbeiro.getSlug(),
                barbeiro.getHorarioFuncionamento(), assinatura, servicos, horarios, bloqueios, clientes, agendamentos);
    }

    private DadosContaResponse.CadastroCliente cadastroCliente(Cliente cliente) {
        List<DadosContaResponse.Atendimento> agendamentos = agendamentoRepository.doCliente(cliente.getId()).stream()
                .map(a -> atendimento(a, cliente.getNome()))
                .toList();

        return new DadosContaResponse.CadastroCliente(cliente.getBarbeiro().getNomeBarbearia(), cliente.getNome(),
                cliente.getTelefone(), agendamentos);
    }

    private static DadosContaResponse.Atendimento atendimento(Agendamento agendamento, String cliente) {
        return new DadosContaResponse.Atendimento(agendamento.getDataHora(), agendamento.getStatus(),
                agendamento.getServico().getNome(), cliente, agendamento.getPreco());
    }
}
