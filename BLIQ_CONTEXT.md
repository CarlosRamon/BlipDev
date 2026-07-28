# Bliq — Plataforma de Gestão e Automação para Franquias de Lavação Automotiva Self-Service

## Visão Geral

A **Bliq** é uma plataforma completa voltada à operação, gestão e escalabilidade de franquias de lavação automotiva de autoatendimento. O ecossistema integra know-how operacional, equipamentos, automação embarcada, meios de pagamento próprios e um sistema centralizado de gestão, garantindo padronização, controle e eficiência em toda a rede.

**Modelo de negócio:** locação de pontos de lavagem por tempo. O cliente paga em um terminal POS Android e utiliza o espaço por um período determinado, com possibilidade de aquisição de tempo adicional.

**Conceito central: o Ciclo.**
Na Bliq, toda venda é um ciclo — uma venda + um período de uso. O ciclo é a unidade atômica de negócio, métricas, billing e auditoria.

---

## Ecossistema: Três Camadas

### 1. Camada Física (Operação)
- Boxes de autoatendimento (lavagem e aspiração)
- Equipamentos controlados via **ESP32**
- Dispositivos de acionamento: válvulas, bombas, temporizadores
- Terminal **POS Android** para checkout

### 2. Camada de Automação
- Comunicação entre POS e hardware via API
- Controle em tempo real do tempo de uso
- Liberação e bloqueio remoto de equipamentos
- Telemetria operacional

### 3. Camada de Software (Plataforma Bliq)
- **Sistema da Franqueadora** — backoffice central da rede
- **Sistema do Franqueado** — operação local da unidade

---

## Sistema da Franqueadora (Backoffice Central)

### Gestão de Franqueados
- Cadastro e gestão de unidades franqueadas
- Controle de status: `Ativo` | `Suspenso` | `Bloqueado`
- Capacidade de bloquear acesso ao sistema remotamente
- Capacidade de suspender vendas nas POS remotamente
- Gestão contratual e planos de franquia

### Gestão de Pontos (Boxes)
- Visualização de todos os boxes da rede
- Status em tempo real: `Disponível` | `Em uso` | `Offline` | `Em manutenção`
- Controle remoto: ativar/desativar, reiniciar, bloquear uso

### Gestão de Produtos (Ciclos de Lavagem)
Cada venda é tratada como um ciclo de uso. Tipos de produto:
- **Lavagem por tempo fixo** (ex: 8 min, 15 min) sendo possível criar produtos por box. Cada Box tem seus produtos customizados. (preço do cliclo de x minutos) 
- **Minutagem avulsa** — sem escolha de tempo predefinido (Preço por minuto)
- **Tempo adicional** — Já no tempo adicional, precisa estar relacionado a cada produto, se escolher "Lavagem 8 minutos", poder adicionar upsell customizado.
Se tiver o produto "Lavagem 15 minutos", ja ter outra configuração e precificação. 

Configurações por produto:
- Tipos de produtos: (Tempo fixo, minutagem avulsa)
- Preço por ciclo 
- Tempo associado ao ciclo
- Regras de tempo extra (poder configurar o upSell cada produto pode ter uma configuração diferente Ex: +3min por R$3,00 e outro produto ter 4 variaçoes de upsell)
- Política de arredondamento

### Telemetria e Monitoramento
Dados coletados em tempo real:
- Número de ciclos por ponto
- Tempo total de utilização
- Taxa de ocupação
- Tempo médio por cliente
- Eventos operacionais: falhas, interrupções, uso anormal

Permite: identificação de gargalos, manutenção preditiva, auditoria de uso e faturamento.

### Gestão de Dispositivos
- Cadastro e vínculo de POS por unidade
- Vínculo obrigatório: **BOX + POS + UUID do ESP32**
- Controle de versão de software
- Atualização remota (OTA)
- Monitoramento de conectividade

---

## Sistema do Franqueado (Operação da Unidade)

### Gestão de Pontos
- Cadastro de boxes da unidade
- Monitoramento em tempo real
- Visualização de uso por ponto
- Controle de disponibilidade

### Configuração de Produtos
- Definição de planos (ex: Lavagem 8 min, Lavagem 12 min)
- Configuração de preços
- Definição de tempo extra: valor por minuto adicional, incrementos (ex: +1 min, +2 min)

### Operação de Vendas (Integração com POS)
O POS consulta a API da Bliq para:
1. Ativa a POS vinculando a um código de BOX
2. Buscar produtos disponíveis
3. Lista e Aplicar preços atualizados

Após pagamento:
1. Ciclo é gerado
2. Ponto de lavagem é liberado
3. Contagem de tempo inicia **somente após o cliente acionar o primeiro equipamento**
4. Ao chegar ao fim do tempo todos os equipamentos são desativados e o cliclo é finalizado para voltar a tela inicial para um novo cliente

### Relatórios Operacionais (V2)
- Total de ciclos por período
- Tempo total de uso por ponto
- Receita por ponto
- Horários de pico
- Taxa de ocupação

---

## Fluxo Operacional Completo

```
1. Cliente chega ao ponto de lavagem
2. Realiza pagamento na POS
3. POS consulta API da Bliq
4. Sistema valida:
   - Status do ponto
   - Permissão de venda da unidade
5. Ciclo é criado
6. Equipamento é liberado via automação (ESP32)
7. Temporizador inicia (após acionamento do primeiro equipamento)
8. Ao finalizar:
   - Ciclo é encerrado
   - Dados enviados para telemetria
   - Receita consolidada
```

---

## Modelo de Dados: Ciclo

Campos de um ciclo:

| Campo | Descrição |
|---|---|
| `id` | Identificador único |
| `box_id` | Ponto utilizado |
| `produto_id` | Produto adquirido |
| `tempo_contratado` | Tempo do ciclo comprado (segundos/minutos) |
| `tempo_utilizado` | Tempo efetivamente utilizado |
| `inicio_em` | Data/hora de início |
| `fim_em` | Data/hora de encerramento |
| `status` | `finalizado` \| `interrompido` \| `cancelado` |

---

## Vínculos de Dispositivo

Cada ponto operacional exige o vínculo de três entidades:

```
BOX (ponto físico)
 └── POS (terminal de pagamento Android)
 └── ESP32 (UUID do microcontrolador embarcado)
```

---

## Diferenciais Estratégicos

- Plataforma 100% integrada: hardware + software + pagamento
- Modelo baseado em ciclos — altamente escalável e auditável
- Telemetria avançada para gestão inteligente
- Controle centralizado da franqueadora sobre toda a rede
- Automação que reduz necessidade de operação humana
- Base de dados rica para expansão e otimização

# 🎨 Guia de Estilo — Plataforma BLIQ

Este documento define os padrões visuais, comportamentais e de experiência da plataforma BLIQ.  
Ele deve orientar o desenvolvimento de interfaces (web/mobile), produto e comunicação.

---

# 🧠 1. Fundamentos da Marca

## 🎯 Proposta
A BLIQ entrega uma experiência:
- Rápida
- Autônoma
- Simples
- Eficiente

> “Chegou, lavou, partiu.”

---

## 💡 Princípios de Produto

A plataforma deve refletir diretamente os valores da marca:

### Liberdade
- Usuário no controle
- Sem dependência de terceiros
- Fluxos sem obrigatoriedade desnecessária

### Agilidade
- Menos cliques possível
- Ações rápidas e diretas
- Tempo de resposta mínimo

### Acesso
- Interface simples
- Baixa curva de aprendizado
- Funciona para qualquer tipo de usuário

### Resultado
- Feedback claro de sucesso
- Confirmações visuais
- Sensação de conclusão (“feito”)

### Cuidado
- Segurança nas ações
- Confirmações quando necessário
- Clareza para evitar erros

---

# 🎨 2. Paleta de Cores

## 🔵 Primária — Ação

- **Pantone:** 2727 C  
- **RGB:** 48, 127, 226  
- **HEX:** #307FE2  

**Uso:**
- Botões principais (CTA)
- Links
- Elementos ativos
- Destaques

---

## 🔷 Secundária — Base

- **Pantone:** 296 C  
- **RGB:** 5, 28, 44  
- **HEX:** #051C2C  

**Uso:**
- Backgrounds
- Header / Sidebar
- Containers principais
- Estrutura

---

## ⚪ Neutras (recomendado adicionar)

Para uso em UI:

- Branco: #FFFFFF
- Cinza claro: #F5F7FA
- Cinza médio: #A0AEC0
- Cinza escuro: #2D3748

---

## 🚦 Estados (UI)

- Sucesso: Verde (#22C55E)
- Erro: Vermelho (#EF4444)
- Atenção: Amarelo (#F59E0B)

---

# 🔤 3. Tipografia

## Principal — Interface
**Cornero**
- Uso: títulos, headers, branding
- Sensação: velocidade, precisão, força

## Display / Destaque
**Fugaz One**
- Uso: chamadas, CTAs, números grandes
- Sensação: impacto, urgência

## Fallback Web
- `Inter`, `Roboto`, `System UI`

---

# 🧩 4. Diretrizes de Interface (UI)

## 📱 Layout

- Grid simples (8px base)
- Espaçamento consistente
- Interface limpa (low cognitive load)

---

## 🔘 Botões

### Primário
- Cor: Azul (#307FE2)
- Ação principal da tela

### Secundário
- Outline ou neutro

### Regra
> Cada tela deve ter **1 ação principal clara**

---

## 📊 Feedbacks

Sempre mostrar retorno:

- Loading → imediato
- Sucesso → visual claro
- Erro → mensagem objetiva

Exemplo:
- ❌ “Erro ao processar”
- ✅ “Pagamento aprovado”

---

## ⚡ Performance percebida

- Skeleton loading
- Transições rápidas
- Evitar travamentos

---

# 🧭 5. UX — Fluxos

## Princípio central:
> “Não fazer o usuário pensar”

### Diretrizes:
- Máximo de 3 passos por ação crítica
- Reduzir campos ao mínimo
- Autopreenchimento sempre que possível

---

## 📲 Exemplo (fluxo ideal)

**Abrir → Escolher → Confirmar → Executar**

Sem telas desnecessárias.

---

# 🗣️ 6. Tom de Voz (UI Copy)

## Características

- Curto
- Direto
- Conversacional
- Sem termos técnicos

---

## Exemplos

❌ Evitar:
> “Sistema pressurizado ativado com sucesso”

✅ Usar:
> “Ligado. Pode usar.”

---

## Padrões

- “Pronto.”
- “Feito.”
- “Tudo certo.”
- “Agora é com você.”

---

# 🎯 7. Microcopy (UX Writing)

### Botões
- “Começar”
- “Pagar”
- “Finalizar”
- “Abrir”

### Estados
- “Carregando…”
- “Quase lá…”
- “Deu certo”

---

# 🔐 8. Segurança e Confiança

A interface deve transmitir:

- Controle do usuário
- Clareza de ação
- Previsibilidade

### Boas práticas:
- Mostrar valores antes de cobrar
- Confirmação antes de ações críticas
- Histórico acessível

---

# 🌆 9. Identidade Visual na Interface

## Conceitos a aplicar

- Movimento
- Brilho
- Rapidez
- Cidade / urbano

---

## Elementos visuais

- Ícones simples
- Linhas limpas
- Animações rápidas (não decorativas)

---

# ✨ 10. Experiência Sensorial

A marca é visual.

A interface deve comunicar:

- “Limpo”
- “Pronto”
- “Brilhando”

### Aplicação:
- Feedbacks visuais claros
- Antes/depois (quando possível)
- Estados de conclusão fortes

---

# 📈 11. Funcionalidades recomendadas (produto)

Com base nos insights do material:

## 💳 Recorrência
- Planos mensais
- Créditos pré-pagos

## 🕒 Horários inteligentes
- Descontos em horários ociosos

## 👥 Perfis
- Motoristas de app
- Frotas
- Usuários comuns

## 🎟️ Promoções
- Cupons
- Cortesias
- Campanhas

---

# 🧠 12. Personalidade da Plataforma

A interface deve parecer:

- Rápida
- Inteligente
- Independente
- Sem burocracia

---

# 🚀 Regra de ouro

Se precisar explicar muito, está errado.

A experiência deve ser:

> Intuitiva na primeira vez.

---

# 📌 Checklist de implementação

Antes de publicar qualquer tela:

- [ ] Existe uma ação principal clara?
- [ ] Dá para usar sem explicação?
- [ ] Está rápido?
- [ ] Está limpo visualmente?
- [ ] O usuário sente controle?
- [ ] O resultado é evidente?

---
