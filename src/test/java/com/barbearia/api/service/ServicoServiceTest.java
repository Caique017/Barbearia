package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Servico;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.servico.ServicoAtualizacaoRequest;
import com.barbearia.api.dto.servico.ServicoRequest;
import com.barbearia.api.dto.servico.ServicoResponse;
import com.barbearia.api.exceptions.ServicoDuplicadoException;
import com.barbearia.api.exceptions.ServicoNaoEncontradoException;
import com.barbearia.api.repositories.BarbeiroRepository;
import com.barbearia.api.repositories.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private BarbeiroRepository barbeiroRepository;
    @Mock
    private BarbeiroLogado barbeiroLogado;

    @InjectMocks
    private ServicoService servicoService;

    private final Usuario logado = new Usuario();
    private Barbeiro barbeiro;
    private Servico corte;

    @BeforeEach
    void barbeiroComUmServico() {
        barbeiro = Barbeiro.builder().id(UUID.randomUUID()).build();
        corte = Servico.builder().id(UUID.randomUUID()).barbeiro(barbeiro).nome("Corte")
                .preco(new BigDecimal("40.00")).duracaoMinutos(30).build();
    }

    private void logadoComBarbeiro() {
        when(barbeiroLogado.de(logado)).thenReturn(barbeiro);
    }

    private void corteEncontrado() {
        logadoComBarbeiro();
        when(servicoRepository.findByIdAndBarbeiroId(corte.getId(), barbeiro.getId())).thenReturn(Optional.of(corte));
    }

    @Test
    void criarSalvaNoBarbeiroDoTokenComCustoZeroPorPadrao() {
        logadoComBarbeiro();
        ArgumentCaptor<Servico> salvo = ArgumentCaptor.forClass(Servico.class);

        ServicoResponse resposta = servicoService.criar(logado,
                new ServicoRequest("Barba", new BigDecimal("30.00"), 20, null));

        verify(servicoRepository).save(salvo.capture());
        assertThat(salvo.getValue().getBarbeiro()).isSameAs(barbeiro);
        assertThat(resposta.custoInsumo()).isEqualByComparingTo("0");
        assertThat(resposta.ativo()).isTrue();
    }

    @Test
    void criarComNomeRepetidoDa409SemSalvar() {
        logadoComBarbeiro();
        when(servicoRepository.existsByBarbeiroIdAndNomeIgnoreCase(barbeiro.getId(), "corte")).thenReturn(true);

        assertThatThrownBy(() -> servicoService.criar(logado,
                new ServicoRequest("corte", new BigDecimal("40.00"), 30, null)))
                .isInstanceOf(ServicoDuplicadoException.class);
        verify(servicoRepository, never()).save(any());
    }

    @Test
    void servicoDeOutroBarbeiroDa404() {
        logadoComBarbeiro();
        UUID deOutro = UUID.randomUUID();

        assertThatThrownBy(() -> servicoService.buscar(logado, deOutro))
                .isInstanceOf(ServicoNaoEncontradoException.class);
    }

    @Test
    void atualizarSoOPrecoNaoMexeNoResto() {
        corteEncontrado();

        ServicoResponse resposta = servicoService.atualizar(logado, corte.getId(),
                new ServicoAtualizacaoRequest(null, new BigDecimal("45.00"), null, null, null));

        assertThat(resposta.preco()).isEqualByComparingTo("45.00");
        assertThat(resposta.nome()).isEqualTo("Corte");
        assertThat(resposta.duracaoMinutos()).isEqualTo(30);
    }

    @Test
    void desativarMantemOServico() {
        corteEncontrado();

        ServicoResponse resposta = servicoService.atualizar(logado, corte.getId(),
                new ServicoAtualizacaoRequest(null, null, null, null, false));

        assertThat(resposta.ativo()).isFalse();
        verify(servicoRepository, never()).delete(any());
    }

    @Test
    void renomearParaNomeDeOutroServicoDa409() {
        corteEncontrado();
        when(servicoRepository.existsByBarbeiroIdAndNomeIgnoreCase(barbeiro.getId(), "Barba")).thenReturn(true);

        assertThatThrownBy(() -> servicoService.atualizar(logado, corte.getId(),
                new ServicoAtualizacaoRequest("Barba", null, null, null, null)))
                .isInstanceOf(ServicoDuplicadoException.class);
        assertThat(corte.getNome()).isEqualTo("Corte");
    }

    @Test
    void renomearMudandoSoAsMaiusculasNaoConsultaDuplicidade() {
        corteEncontrado();

        servicoService.atualizar(logado, corte.getId(), new ServicoAtualizacaoRequest("CORTE", null, null, null, null));

        verify(servicoRepository, never()).existsByBarbeiroIdAndNomeIgnoreCase(any(), any());
        assertThat(corte.getNome()).isEqualTo("CORTE");
    }

    @Test
    void rotaPublicaSoMostraOsAtivos() {
        when(barbeiroRepository.findBySlug("barbearia-x")).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findByBarbeiroIdAndAtivoTrueOrderByNomeAsc(barbeiro.getId())).thenReturn(List.of(corte));

        assertThat(servicoService.listarPublicos("barbearia-x"))
                .singleElement()
                .satisfies(publico -> assertThat(publico.nome()).isEqualTo("Corte"));
    }
}
