# BliqTotem — Brief para redesign de layout Android

## 1. O que é o produto

**BliqTotem** é o app Android que roda em **totens de autoatendimento** dentro de boxes de lavação de veículos da rede **Bliq**. O cliente estaciona no box, usa o totem para escolher o serviço, paga (cartão ou PIX) e o totem libera as máquinas do box (jato, shampoo, enxágue, aspirador, ar comprimido) por tempo controlado.

**Público-alvo:** motoristas comuns (não técnicos). Uso pontual — o cliente interage com o totem por 1-3 minutos, uma vez, sob pressa. Zero tolerância para confusão.

**Modelo:** um totem por box. Vários boxes por unidade. Alguns boxes são de **LAVAÇÃO** (3 máquinas: pré-lavagem, shampoo, enxágue), outros de **ASPIRAÇÃO** (aspirador + ar comprimido). O app se adapta ao tipo do box.

## 2. Hardware e restrições

- **Dispositivos-alvo:** POS Android multi-fabricante — **Tectoy T-series, Positivo L-series, Sunmi, Gertec GPOS 700/Series 7, Ingenico**.
- **Tela:** ~5"–6", **retrato**, touch capacitivo. Resoluções variam (720x1280 típico, alguns HD).
- **Sem teclado físico.** Softkeyboard do sistema para CPF/telefone/código.
- **24/7 ligado**, sujeito a luz solar direta + poeira/água. Contraste alto é obrigatório.
- **Sem áudio.** Nada de sons/vibração (nem sempre disponível).
- **Bluetooth Low Energy** conecta o totem ao ESP32 dentro do box (é o que abre/fecha as válvulas das máquinas). Status BLE precisa ser visível em telas críticas.
- **Impressora fiscal** integrada em alguns modelos (não é responsabilidade do UI).
- **Stone SDK** processa cartão via NFC/chip/tarja e PIX via QR Code.

## 3. Identidade visual da marca (Bliq)

Extraída do brandbook oficial:

**Paleta (obrigatória, não desviar):**
- **Azul Bliq `#1679ED`** — primária, cor do logo
- **Deep Navy `#001B2D`** — texto principal, superfícies escuras
- **Off-white `#EDEDED`** — fundo institucional
- Branco puro `#FFFFFF` para cards/superfícies
- Estados: sucesso `#1FA96A`, aviso `#F5A524`, erro `#E0364B`

**Tipografia:**
- **FugazOne** (display, sans geométrica bold, forma quase italic) — títulos, preços, contadores
- **Epilogue** (sans humanista) — corpo e labels — pesos Regular / SemiBold / Bold

**DNA visual:**
- Logo BLIQ = wordmark bold italic slab + **sparkle diamante de 4 pontas** ao lado do "Q"
- Grafismo assinatura: **squircle inclinado** (quadrado super arredondado, skew leve à direita) — pastilha usada como badge/moldura
- Textura secundária: mosaico de squircles em checker
- Slogan da marca: **"Brilho em 8 minutos e ponto."**
- Tom: rápido, direto, confiante — nada de infantil ou fofo
- Alto contraste — azul chapado sobre off-white ou navy escuro
- Zero emoji: use ícones vetoriais consistentes

## 4. Premissa de UX para totem

1. **Um caminho por vez.** Nada de menus, hamburger, tabs. Cada tela tem no máximo 1 ação principal + 1 secundária.
2. **Tudo grande.** Botão CTA mínimo 60dp de altura. Corpo ≥16sp. Ícones ≥24dp. O totem é lido a 40-70cm.
3. **Reversível até pagar.** Antes do pagamento, "Voltar" existe. Depois do pagamento (Success/Session), botões destrutivos exigem confirmação.
4. **Feedback instantâneo.** Todo toque tem resposta visual (não usar só ripple padrão — o cliente precisa saber que "foi").
5. **Estado do box sempre visível.** Se o BLE cair, o cliente precisa saber. Se a máquina está ativa, o timer não pode confundir.
6. **Screen never sleeps.** O app deve segurar a tela acesa (já implementado).
7. **Recuperação de erro clara.** Sem pagamento → volta. Com pagamento e erro → suporte visível.

## 5. Mapa de fluxo

```
[Activation] ──(sucesso)──▶ [Home] ─┬──▶ [Extras]         ──▶ [CpfInput] ──▶ [Checkout] ──▶ [Payment] ──▶ [Success] ──▶ [Session] ──▶ [Home]
                                    ├──▶ [MinutesPicker]  ──▶ [CpfInput] ──▶ [Checkout] ──▶ [Payment] ──▶ [Success] ──▶ [Session] ──▶ [Home]
                                    └──▶ [CpfInput] direto ──▶ [Checkout] ──▶ [Payment] ──▶ [Success] ──▶ [Session] ──▶ [Home]

[Home] ──(Ajuda)──▶ [Support]
[Home] ──(long-tap secreto 7x no logo)──▶ reset → [Activation]
[Session] ──(sessão retomada por heartbeat)──▶ vai direto da [Home] pra [Session]
```

**Ponto de entrada:** se há token salvo → `Home`; senão → `Activation`.

## 6. Especificação tela a tela

### 6.1 Activation — Vinculação do terminal
- **Quando aparece:** primeira vez que o app roda, ou após reset.
- **O que faz:** pede um código de 8 caracteres gerado no painel web da franqueadora. Vincula esse totem específico a um box.
- **Elementos:** logo Bliq, texto explicativo curto, campo grande de 8 caracteres (letras/números, uppercase, spacing 6sp entre chars), botão "Ativar terminal", link textual "peça ao gestor". Erro em card vermelho.
- **Estados:** vazio / preenchendo (botão desabilitado até 8 chars) / loading / erro.
- **Observação de design:** essa tela é vista **uma vez na vida do terminal**. Pode ser mais institucional e explicativa que as demais.

### 6.2 Home — Seleção de serviço (**tela mais vista**)
- **Quando aparece:** todo início de fluxo de venda.
- **O que faz:** exibe os produtos configurados para aquele box (`WashOption`) e permite escolher.
- **Elementos:**
  - Logo Bliq no topo
  - **Nome do box** (ex: "Box 3") em destaque
  - **Nome do franqueado** (ex: "Bliq Kobrasol") pequeno abaixo
  - Instrução: "Selecione a lavagem" (LAVACAO) ou "Selecione a aspiração" (ASPIRACAO)
  - Botão "Ajuda" no canto superior direito
  - **Chip de status BLE** (equipamento conectado / conectando / erro) — sempre visível
  - **Lista de produtos** (`WashOption`): cada um tem `label`, `minutes` (ou "tempo à escolha"), `price`, opcionalmente `extras`
  - Tipos de produto:
    - Tempo fixo (ex: "Lavagem completa — 15 min — R$ 25")
    - Minutagem avulsa (ex: "R$ 1,50 / min — tempo à escolha")
    - Com extras (ex: "Lavagem premium + shampoo + cera opcional")
- **Overlay Welcome (attract screen):** antes da primeira interação, tela cheia azul com logo, nome do box, CTA "Toque para iniciar" pulsante. Mostra também estado da conexão com o equipamento. Bom candidato a virar **attract mode com loop** de mensagens/cenas.
- **Long-tap secreto:** 7 taps no logo abrem confirmação de reset do terminal (função de manutenção).
- **Estados:** loading / erro (com retry) / lista carregada / precisa reativar.

### 6.3 MinutesPicker — Escolha de tempo (produto de minutagem avulsa)
- **Quando aparece:** quando o cliente escolhe um produto do tipo `MINUTAGEM_AVULSA`.
- **O que faz:** cliente define quantos minutos quer comprar (1-120).
- **Elementos:** nome do serviço, preço/minuto, controle **−  [64sp número gigante]  +**, card de "Total" com valor calculado, botão "Continuar · R$ XX,XX".

### 6.4 Extras — Add-ons do produto
- **Quando aparece:** quando o produto escolhido tem `extras` (ex: shampoo turbinado, cera, aromatizador).
- **O que faz:** cliente marca add-ons opcionais. Cada extra soma minutos e preço.
- **Elementos:** card com resumo (produto base + tempo + preço base), lista de cards de extras com checkbox visual, cada um mostra "+ X min" e preço, card grande de **Total** (tempo + valor) na cor primária.

### 6.5 CpfInput — Identificação (LGPD, opcional)
- **Quando aparece:** entre a seleção e o checkout — em **todo** fluxo, mesmo sem extras.
- **O que faz:** identifica o cliente para fidelização (opcional). Fluxo em 4 sub-estados:
  1. **CONSENT** — pergunta se quer se identificar. Emoji cadeado, texto sobre LGPD, "Autorizar" ou "Continuar sem identificação".
  2. **ENTERING_CPF** — campo grande de CPF com máscara `000.000.000-00` e validação. Consulta backend em tempo real.
  3. **FOUND** — mostra "Bem-vindo de volta, [nome]" + CPF mascarado + botões "Continuar" / "Não sou eu".
  4. **NOT_FOUND** — formulário de cadastro (CPF pré-preenchido, nome, telefone) + 3 checkboxes de consentimento: **Termos** (obrigatório), Marketing (opcional), Uso de imagem (opcional). Link "Ler termos completos" abre dialog scrollável.
- **Estados extras:** LOADING (busca), REGISTERING (envio), erro genérico em card vermelho.

### 6.6 Checkout — Resumo e escolha de pagamento
- **Quando aparece:** depois do CPF.
- **O que faz:** confirma o pedido e escolhe forma de pagamento.
- **Elementos:**
  - Título "Resumo do Pedido"
  - **Card "Detalhes da lavagem":** tipo do produto, tempo base, cada extra em linha, tempo total destacado
  - **Card grande "Total a pagar"** em cor primária, fonte de destaque
  - **3 cards horizontais de forma de pagamento:** Crédito / Débito / Pix — cada um com ícone vetorial em pastilha, seleção com borda + halo azul + dot indicador
  - Botão CTA "Confirmar Pagamento" (desabilitado até escolher método)

### 6.7 Payment — Processamento Stone
- **Quando aparece:** depois de "Confirmar Pagamento".
- **O que faz:** interage com o SDK da Stone. É tela reativa a estados do SDK.
- **Estados (cada um tem visual próprio):**
  - **WaitingCard** — ícone contactless em bolha pulsante, "Aproxime ou insira o cartão", "Aguardando pagamento...", botão Cancelar
  - **WaitingQrCode (Pix)** — título "Escaneie o QR Code PIX", bitmap do QR Code (300dp+ com moldura), instrução, botão Cancelar
  - **WaitingPassword** — halo azul, ícone cadeado, "Digite sua senha", "Use o teclado da maquininha"
  - **Sending / WaitingRemoveCard** — halo azul, ícone velocímetro, "Processando..."
  - **Success** — halo verde, ícone check circle, "Aprovado!", spinner "Registrando ciclo..."
  - **Cancelled** — halo âmbar, ícone cancel, "Cancelado"
  - **Failure** — halo vermelho, ícone X, "Não autorizada", mensagem detalhada, botões "Tentar novamente" + "Voltar ao checkout"
- **Banner do método:** chip pequeno no topo mostra o método escolhido (com ícone).

### 6.8 Success — Confirmação de pagamento
- **Quando aparece:** logo após aprovação da Stone.
- **O que faz:** confirma o pagamento visualmente **enquanto** o backend registra o ciclo E o BLE conecta ao ESP32 do box.
- **Elementos:** bolha verde com check, "Pagamento realizado com sucesso!", card com resumo (forma, tempo, valor), **card de progresso BLE** com sub-estados:
  - IDLE / CONNECTING / REGISTERING / SUCCESS / ERROR (com retry + "Voltar ao início")
- **Comportamento:** navega automaticamente para Session assim que `sessionReady=true`.

### 6.9 Session — Sessão em andamento (**tela crítica, cliente fica olhando**)
- **Quando aparece:** após o Success, ou retomada de sessão ativa detectada por heartbeat na Home.
- **O que faz:** cliente escolhe qual máquina ativar; timer conta regressivo enquanto usa; pode trocar de máquina a qualquer momento dentro do tempo comprado.
- **Elementos:**
  - Topbar azul com título "Ciclo em andamento — [nome do box]"
  - **Card do timer:** label "TEMPO RESTANTE" (ou "Aguardando início" / "Sessão pausada" / "Encerrando..."), cronômetro `MM:SS` em fonte enorme (88sp+), cor muda por urgência (>3min primary, ≤3min warning, ≤1min error)
  - **Chip da máquina ativa** (ícone + label na cor da máquina) ou "Selecione um equipamento"
  - **Barra de status BLE** (só quando NÃO conectado)
  - Label "SELECIONAR EQUIPAMENTO" (ou "SELECIONAR SERVIÇO" para aspiração)
  - **Cards de máquina** — um por máquina disponível (LAVACAO: Pré-lavagem/Shampoo/Enxágue; ASPIRACAO: Aspirador/Ar comprimido); cada um com ícone, label, cor própria; ativa tem borda + halo + dot
  - Botão vermelho "Encerrar sessão" com dialog de confirmação (mostra tempo restante que será perdido)
- **Snackbar de reconexão:** quando o box reconecta, aparece toast verde ("Sessão retomada automaticamente" ou "Equipamento reconectado. Selecione para continuar").
- **Comportamento crítico:** cliente pode pausar trocando de máquina; back button abre confirmação; ao acabar o tempo (ou ao encerrar), volta para Home.

### 6.10 Support — Fale conosco
- **Quando aparece:** botão "Ajuda" na Home.
- **O que faz:** exibe canais de suporte (e-mail, telefone/WhatsApp, horário).
- **Elementos:** topbar branca, texto explicativo, contatos em rows (label + valor), horário.

### 6.11 Welcome (overlay dentro da Home)
- **Quando aparece:** ao entrar na Home, antes de qualquer interação.
- **O que faz:** attract mode. Fundo azul cheio, logo tinted branco, nome do box, CTA "Toque para iniciar" pulsante.
- **Estados do ping:** IDLE (mostra CTA), CONNECTING (spinner + "Conectando equipamento..."), CONNECTED (check verde + "Equipamento conectado!").
- **Rodapé:** status BLE em texto discreto branco.
- **Oportunidade de redesign:** hoje é uma tela estática. Pode virar loop com múltiplas cenas ("brilho em 8 minutos", "pague com PIX", benefícios) alternando com fade.

## 7. Componentes globais recorrentes

- **TopAppBar** — atualmente azul chapado com título FugazOne. Botão de voltar em branco quando existe.
- **PrimaryButton / OutlineButton** — 60dp altura, radius 16, CTA em gradiente azul→azul-escuro
- **BleStatusBar** — card com dot colorido (verde/âmbar/vermelho) + label + botão de retry quando erro
- **Snackbar** — verde escuro, radius 10, canto inferior
- **AlertDialog** — Material padrão, título FugazOne, corpo Epilogue, ação destrutiva em vermelho
- **BliqCard** — superfície branca, radius 20, sombra 3dp
- **BliqIconChip** — pastilha squircle com ícone vetorial dentro (usada para métodos de pagamento e status)
- **Sparkle** — diamante de 4 pontas do logo, desenhado em Canvas, disponível como composable
- **Máquina** tem `icon`, `label`, `colorHex` próprios (ex: Pré-lavagem azul, Shampoo roxo, Enxágue teal, Aspirador índigo, Ar comprimido ciano)

## 8. Estados globais persistentes que o design precisa acomodar

- **BLE** — DISCONNECTED / SCANNING / CONNECTING / CONNECTED / RECONNECTING / ERROR. Presente em Home, Session e Success. Em WelcomeContent aparece só como texto discreto.
- **Ativação/token** — sem token → Activation. Token inválido no meio do uso → volta pra Activation.
- **Sessão ativa** — heartbeat pode detectar que uma sessão foi iniciada antes; se detectar, pula direto pra Session.
- **Loading / Error genéricos** em cada tela com fetch (Home, CpfInput, Success/BLE, Payment/Stone).

## 9. O que o redesign precisa entregar

**Objetivos:**
- Aumentar percepção de marca (hoje é meio "Material genérico" mesmo com paleta correta).
- Passar sensação de "produto premium" — o cliente está pagando R$20-40 e precisa sentir que a experiência vale.
- Manter/melhorar clareza para autoatendimento.
- Aplicar a paleta e o DNA visual do brandbook (squircle, sparkle, gradientes).

**Ideias já cogitadas (jogar pra IA como referência, não como decisão):**
- Home em **grid de 2 colunas** com hero visual por serviço + badge "mais vendida"
- **Timer circular animado** na Session (arco de progresso decrescente)
- **QR PIX 320dp+** com moldura squircle e logo Bliq no centro
- **Attract mode** com loop de mensagens no Welcome
- **Ilustração animada de cartão** no Payment em vez de ícone estático
- **Microinterações:** scale-on-press, transições slide/fade entre rotas, skeleton loader no lugar de spinner

**Que NÃO fazer:**
- Nada de darkmode (o totem está sempre em ambiente iluminado — modo claro é definitivo)
- Nada de tabs, hamburger, navegação lateral (é fluxo linear)
- Nada de emoji nos ícones
- Nada de tipografia diferente de FugazOne / Epilogue
- Nada de cores fora da paleta Bliq
