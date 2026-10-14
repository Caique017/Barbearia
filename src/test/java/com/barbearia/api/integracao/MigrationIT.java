package com.barbearia.api.integracao;

import com.barbearia.api.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class MigrationIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(TestcontainersConfiguration.IMAGEM_POSTGRES);

    private static String bancoNovo() throws SQLException {
        String nome = "m_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection conexao = conectar(POSTGRES.getJdbcUrl()); Statement sql = conexao.createStatement()) {
            sql.execute("create database " + nome);
        }
        return POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + nome);
    }

    private static Connection conectar(String url) throws SQLException {
        return DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void migrar(String url, String ate) {
        var configuracao = Flyway.configure().dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());
        if (ate != null) {
            configuracao.target(ate);
        }
        configuracao.load().migrate();
    }

    private static void executar(String url, String... comandos) throws SQLException {
        try (Connection conexao = conectar(url); Statement sql = conexao.createStatement()) {
            for (String comando : comandos) {
                sql.execute(comando);
            }
        }
    }

    private static List<String> consultar(String url, String consulta) throws SQLException {
        List<String> linhas = new ArrayList<>();
        try (Connection conexao = conectar(url); Statement sql = conexao.createStatement();
             ResultSet resultado = sql.executeQuery(consulta)) {
            while (resultado.next()) {
                linhas.add(resultado.getString(1));
            }
        }
        return linhas;
    }

    @Test
    void v3PreencheONomeDaBarbeariaDeLinhasQueJaExistiamTruncandoEm120() throws SQLException {
        String url = bancoNovo();
        migrar(url, "2");
        String nomeLongo = "Barbearia " + "x".repeat(190);
        executar(url,
                "insert into usuario (id, nome, email, senha, role, criado_em) values "
                        + "('00000000-0000-0000-0000-000000000001', '" + nomeLongo + "', 'a@ex.com', 'h', 'BARBEIRO', now()),"
                        + "('00000000-0000-0000-0000-000000000002', 'João Silva', 'b@ex.com', 'h', 'BARBEIRO', now())",
                "insert into barbeiro (id, usuario_id, slug, criado_em) values "
                        + "(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'longo', now()),"
                        + "(gen_random_uuid(), '00000000-0000-0000-0000-000000000002', 'joao-silva', now())");

        migrar(url, null);

        assertThat(consultar(url, "select nome_barbearia from barbeiro order by slug"))
                .containsExactly("João Silva", nomeLongo.substring(0, 120));
    }

    @Test
    void v5PassaEmailParaMinusculoSemQuebrarQuandoColide() throws SQLException {
        String url = bancoNovo();
        migrar(url, "4");
        executar(url,
                "insert into usuario (id, nome, email, senha, role, criado_em) values "
                        + "(gen_random_uuid(), 'A', ' Maiusculo@Ex.COM', 'h', 'CLIENTE', now()),"
                        + "(gen_random_uuid(), 'B', 'Dup@Ex.com', 'h', 'CLIENTE', now()),"
                        + "(gen_random_uuid(), 'C', 'dup@ex.com', 'h', 'CLIENTE', now())");

        migrar(url, null);

        assertThat(consultar(url, "select email from usuario order by nome"))
                .containsExactly("maiusculo@ex.com", "Dup@Ex.com", "dup@ex.com");
    }
}
