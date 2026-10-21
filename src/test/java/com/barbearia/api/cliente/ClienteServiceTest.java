package com.barbearia.api.cliente;

import com.barbearia.api.auth.Usuario;
import com.barbearia.api.auth.UsuarioExistenteException;
import com.barbearia.api.auth.UsuarioRepository;
import com.barbearia.api.barbeiro.Barbeiro;
import com.barbearia.api.barbeiro.BarbeiroLogado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private BarbeiroLogado barbeiroLogado;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ClienteService clienteService;

    private final Usuario logado = new Usuario();
    private Barbeiro barbeiro;
    private Cliente cliente;

    @BeforeEach
    void carteiraComUmCliente() {
        barbeiro = Barbeiro.builder().id(UUID.randomUUID()).build();
        cliente = Cliente.builder().id(UUID.randomUUID()).barbeiro(barbeiro).nome("Carlos").telefone("11999998888").build();
        when(barbeiroLogado.de(logado)).thenReturn(barbeiro);
    }

    private void clienteEncontrado() {
        when(clienteRepository.findByIdAndBarbeiroId(cliente.getId(), barbeiro.getId())).thenReturn(Optional.of(cliente));
    }

    @Test
    void cadastrarComTelefoneJaNaCarteiraDa409SemSalvar() {
        when(clienteRepository.existsByTelefoneAndBarbeiroId("11999998888", barbeiro.getId())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.cadastrarPeloBarbeiro(logado,
                new ClienteRapidoRequest("Outro Carlos", "11999998888")))
                .isInstanceOf(UsuarioExistenteException.class);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void clienteDeOutroBarbeiroDa404() {
        UUID outro = UUID.randomUUID();

        assertThatThrownBy(() -> clienteService.atualizarCliente(logado, outro, new ClienteAtualizacaoRequest("X", null)))
                .isInstanceOf(ClienteNaoEncontradoException.class);
    }

    @Test
    void atualizarSoONomeNaoMexeNoTelefone() {
        clienteEncontrado();

        ClienteResumoResponse resumo = clienteService.atualizarCliente(logado, cliente.getId(),
                new ClienteAtualizacaoRequest("Carlos Silva", null));

        assertThat(resumo.nome()).isEqualTo("Carlos Silva");
        assertThat(resumo.telefone()).isEqualTo("11999998888");
    }

    @Test
    void trocarParaTelefoneDeOutroClienteDa409() {
        clienteEncontrado();
        when(clienteRepository.existsByTelefoneAndBarbeiroId("21987654321", barbeiro.getId())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.atualizarCliente(logado, cliente.getId(),
                new ClienteAtualizacaoRequest(null, "21987654321")))
                .isInstanceOf(UsuarioExistenteException.class);
        assertThat(cliente.getTelefone()).isEqualTo("11999998888");
    }

    @Test
    void mandarOMesmoTelefoneNaoColideConsigoMesmo() {
        clienteEncontrado();

        clienteService.atualizarCliente(logado, cliente.getId(), new ClienteAtualizacaoRequest(null, "11999998888"));

        verify(clienteRepository, never()).existsByTelefoneAndBarbeiroId(any(), any());
    }

    @Test
    void aprovarVinculoPassaOPendenteParaUsuario() {
        Usuario pendente = Usuario.builder().nome("Carlos").email("carlos@ex.com").build();
        cliente.setUsuarioPendente(pendente);
        clienteEncontrado();

        ClienteResumoResponse resumo = clienteService.aprovarVinculo(logado, cliente.getId());

        assertThat(cliente.getUsuario()).isSameAs(pendente);
        assertThat(cliente.getUsuarioPendente()).isNull();
        assertThat(resumo.temConta()).isTrue();
        assertThat(resumo.pedidoVinculo()).isNull();
    }

    @Test
    void recusarVinculoApagaAContaPendente() {
        Usuario pendente = Usuario.builder().nome("Invasor").email("invasor@ex.com").build();
        cliente.setUsuarioPendente(pendente);
        clienteEncontrado();

        clienteService.recusarVinculo(logado, cliente.getId());

        assertThat(cliente.getUsuarioPendente()).isNull();
        assertThat(cliente.getUsuario()).isNull();
        verify(usuarioRepository).delete(pendente);
    }

    @Test
    void aprovarOuRecusarSemPedidoDa404() {
        clienteEncontrado();

        assertThatThrownBy(() -> clienteService.aprovarVinculo(logado, cliente.getId()))
                .isInstanceOf(VinculoNaoEncontradoException.class);
        assertThatThrownBy(() -> clienteService.recusarVinculo(logado, cliente.getId()))
                .isInstanceOf(VinculoNaoEncontradoException.class);
    }

    @Test
    void resumoMostraQuemPediuOVinculo() {
        cliente.setUsuarioPendente(Usuario.builder().nome("Invasor").email("invasor@ex.com").build());
        clienteEncontrado();

        ClienteResumoResponse resumo = clienteService.buscarMeuCliente(logado, cliente.getId());

        assertThat(resumo.temConta()).isFalse();
        assertThat(resumo.pedidoVinculo().nome()).isEqualTo("Invasor");
        assertThat(resumo.pedidoVinculo().email()).isEqualTo("invasor@ex.com");
    }
}
