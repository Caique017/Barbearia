alter table servico
    add constraint ck_servico_valores
        check (preco >= 0 and custo_insumo >= 0 and duracao_minutos between 5 and 480);

create index idx_servico_barbeiro
    on servico (barbeiro_id);
