package com.barbearia.api.domain;

import com.barbearia.api.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "usuario")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "senha", nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "versao_sessao", nullable = false)
    private int versaoSessao;

    @Column(name = "termos_aceitos_em")
    private Instant termosAceitosEm;

    @Column(name = "termos_versao", length = 20)
    private String termosVersao;

    @Column(name = "marketing_aceito_em")
    private Instant marketingAceitoEm;

    @Column(name = "lembretes_desligados_em")
    private Instant lembretesDesligadosEm;

    public void aceitarTermos(String versao, Instant agora) {
        termosVersao = versao;
        termosAceitosEm = agora;
    }

    public boolean aceitaMarketing() {
        return marketingAceitoEm != null;
    }

    public boolean recebeLembretes() {
        return lembretesDesligadosEm == null;
    }

    public void definirMarketing(boolean aceita, Instant agora) {
        if (aceita && marketingAceitoEm == null) {
            marketingAceitoEm = agora;
        } else if (!aceita) {
            marketingAceitoEm = null;
        }
    }

    public void definirLembretes(boolean recebe, Instant agora) {
        if (!recebe && lembretesDesligadosEm == null) {
            lembretesDesligadosEm = agora;
        } else if (recebe) {
            lembretesDesligadosEm = null;
        }
    }

    public void descadastrar(Instant agora) {
        definirMarketing(false, agora);
        definirLembretes(false, agora);
    }

    public boolean aceitaVersaoDeSessao(int versao) {
        return versao == versaoSessao;
    }

    public void encerrarSessoes() {
        versaoSessao++;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
