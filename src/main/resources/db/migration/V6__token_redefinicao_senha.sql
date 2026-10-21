create table token_redefinicao_senha
(
    id         uuid,
    usuario_id uuid        not null,
    token_hash varchar(64) not null,
    expira_em  timestamptz not null,
    usado_em   timestamptz,
    criado_em  timestamptz not null,
    primary key (id),
    constraint uk_token_redefinicao_senha_hash unique (token_hash),
    constraint fk_token_redefinicao_senha_usuario
        foreign key (usuario_id)
            references usuario (id)
);

create index idx_token_redefinicao_senha_usuario
    on token_redefinicao_senha (usuario_id);