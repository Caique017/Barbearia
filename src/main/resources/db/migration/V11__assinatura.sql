create table assinatura
(
    id                     uuid,
    barbeiro_id            uuid         not null,
    status                 varchar(30)  not null,
    forma_pagamento        varchar(10),
    teste_ate              timestamptz  not null,
    acesso_ate             timestamptz,
    stripe_customer_id     varchar(255),
    stripe_subscription_id varchar(255),
    lembrete_enviado_em    timestamptz,
    atualizado_em          timestamptz  not null,
    primary key (id),
    constraint uk_assinatura_barbeiro unique (barbeiro_id),
    constraint uk_assinatura_customer unique (stripe_customer_id),
    constraint fk_assinatura_barbeiro
        foreign key (barbeiro_id)
            references barbeiro (id),
    constraint ck_assinatura_status
        check (status in ('TESTE', 'ATIVA', 'PAGAMENTO_PENDENTE', 'CANCELADA')),
    constraint ck_assinatura_forma
        check (forma_pagamento is null or forma_pagamento in ('CARTAO', 'PIX'))
);

insert into assinatura (id, barbeiro_id, status, teste_ate, atualizado_em)
select gen_random_uuid(), b.id, 'TESTE', now() + interval '7 days', now()
from barbeiro b;

create table evento_pagamento
(
    id            varchar(255),
    tipo          varchar(100) not null,
    processado_em timestamptz  not null,
    primary key (id)
);
