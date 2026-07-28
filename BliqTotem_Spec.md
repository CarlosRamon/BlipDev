# BliqTotem — Especificação para App Nativo Android (Kotlin)

**Package:** `br.com.bliqbrasil.totem`  
**Backend:** `https://app.bliqbrasil.com.br`

---

## 1. Visão Geral

O **BliqTotem** é o aplicativo instalado no tablet/totem físico de um posto de lavagem veicular self-service da rede Bliq. Ele é o ponto de contato direto entre o cliente (que quer lavar o carro) e o hardware de lavagem (um ESP32 conectado via Bluetooth BLE).

O app opera sem intervenção humana durante o uso — o cliente navega sozinho pelo fluxo de compra. O operador/franqueado só interage uma única vez, para ativar o terminal com um código gerado no painel web.

---

## 2. Paleta de Cores e Identidade Visual

| Token         | Valor       | Uso                                      |
|---------------|-------------|------------------------------------------|
| Primary       | `#1A73E8`   | Botões principais, destaques, preços     |
| Background    | `#F8F9FA`   | Fundo de todas as telas                  |
| Surface       | `#FFFFFF`   | Cards e inputs                           |
| OnSurface     | `#202124`   | Textos principais                        |
| Secondary     | `#5F6368`   | Labels e textos secundários              |
| Tertiary      | `#9AA0A6`   | Hints, rodapés, placeholders             |
| Divider       | `#E8EAED`   | Linhas separadoras                       |
| Success       | `#34A853`   | Estado conectado, confirmações           |
| Warning       | `#FBBC04`   | Estados intermediários (scanning, etc.)  |
| Error         | `#EA4335`   | Erros, botão encerrar sessão             |
| ErrorSurface  | `#FDE8E8`   | Fundo de caixas de erro                  |
| Purple        | `#9C27B0`   | Equipamento Espuma                       |
| Teal          | `#00897B`   | Equipamento Enxague                      |

---

## 3. Arquitetura de Telas (Navigation Graph)

```
ActivationScreen
      │ (token salvo)
      ▼
HomeScreen ◄────────────────────────────────────────┐
      │                                             │
      │ produto sem extras          produto c/ extras│
      ▼                                    ▼        │
CheckoutScreen ◄──────── ExtrasScreen      │        │
      │                                             │
      ▼                                             │
SuccessScreen                                       │
      │ (BLE OK)                                    │
      ▼                                             │
SessionScreen ──────────────────────────────────────┘
  (ao finalizar: popToTop → HomeScreen)
```

**Regras de navegação:**
- `Activation → Home`: substituição (não empilha, sem botão voltar)
- `Home → Extras → Checkout`: pilha normal
- `Home → Checkout`: direto, sem passar por Extras (quando produto não tem extras)
- `Checkout → Success`: substituição (não permite voltar ao checkout)
- `Success → Session`: substituição
- `Session → Home`: `popToTop()` — limpa toda a pilha

---

## 4. Persistência Local

Usar `SharedPreferences` ou `EncryptedSharedPreferences`.

| Chave                  | Tipo   | Descrição                              |
|------------------------|--------|----------------------------------------|
| `@bliq:pos_token`      | String | JWT de autenticação do terminal        |

**Operações:**
- `saveToken(token: String)` — persiste o JWT
- `getToken(): String?` — lê o JWT (null = não ativado)
- `clearToken()` — remove o JWT (logout/re-ativação)

---

## 5. API REST

**Base URL:** `https://app.bliqbrasil.com.br`

Todas as requisições (exceto ativação) enviam o header:
```
Authorization: Bearer <token>
Content-Type: application/json
```

Erros retornam JSON `{ "error": "mensagem" }`. Em caso de HTTP 401 ou mensagem contendo "Token", limpar o token local e redirecionar para `ActivationScreen`.

---

### 5.1 `POST /api/pos/ativar`

Ativa o terminal pela primeira vez.

**Request:**
```json
{ "codigo": "ABCD1234" }
```

**Response 200:**
```json
{
  "token": "eyJ...",
  "terminalId": "uuid",
  "serial": "string",
  "franqueadoId": "uuid"
}
```

---

### 5.2 `GET /api/pos/config`

Retorna a configuração completa do terminal, incluindo produtos disponíveis.

**Response 200:**
```json
{
  "terminal": { "id": "uuid", "serial": "string", "modelo": "string|null" },
  "franqueado": { "id": "uuid", "nome": "string", "status": "string" },
  "box": { "id": "uuid", "nome": "string", "status": "string" },
  "esp32": {
    "uuid": "string",
    "versao": "string|null",
    "online": true,
    "ultimaVezEm": "ISO8601|null"
  },
  "produtos": [
    {
      "id": "uuid",
      "nome": "string",
      "tipo": "TEMPO_FIXO | MINUTAGEM_AVULSA",
      "tempoMinutos": 10,
      "preco": "29.90",
      "boxId": "uuid",
      "extras": [
        {
          "id": "uuid",
          "produtoId": "uuid",
          "rotulo": "string",
          "minutos": 5,
          "preco": "9.90",
          "ativo": true
        }
      ]
    }
  ]
}
```

---

### 5.3 `GET /api/pos/ciclos/ativo`

Verifica se há ciclo em andamento (usado ao inicializar o app para retomada de sessão).

**Response 200:** objeto ou `null`
```json
{
  "id": "uuid",
  "tempoContratado": 15,
  "status": "AGUARDANDO | EM_ANDAMENTO",
  "segundosRestantes": 743
}
```

---

### 5.4 `POST /api/pos/ciclos`

Cria um novo ciclo de lavagem após pagamento confirmado.

**Request:**
```json
{
  "produtoId": "uuid",
  "tempoContratado": 15,
  "metodoPagamento": "CARTAO_CREDITO | CARTAO_DEBITO | PIX",
  "valor": 29.90,
  "transacaoId": "string (opcional)"
}
```

**Response 200:** objeto `ApiCiclo`
```json
{
  "id": "uuid",
  "boxId": "uuid",
  "terminalId": "uuid",
  "produtoId": "uuid",
  "tempoContratado": 15,
  "status": "AGUARDANDO",
  "createdAt": "ISO8601"
}
```

---

### 5.5 `PATCH /api/pos/ciclos/{cicloId}/extra`

Adiciona um extra ao ciclo. Chamado uma vez por extra selecionado.

**Request:**
```json
{ "produtoExtraId": "uuid" }
```

**Response 200:** objeto `ApiCiclo` atualizado.

---

### 5.6 `PATCH /api/pos/ciclos/{cicloId}/iniciar`

Marca o ciclo como `EM_ANDAMENTO`. Chamado logo após enviar `START` ao ESP32.

**Response 200:** objeto `ApiCiclo`.

---

### 5.7 `PATCH /api/pos/ciclos/{cicloId}/finalizar`

Marca o ciclo como finalizado. Deve ser chamado com **retry** (até 4 tentativas, backoff 1.5s × tentativa).

**Response 200:** objeto `ApiCiclo`.

---

### 5.8 `POST /api/pos/telemetria`

Registra eventos para rastreabilidade. Fire-and-forget (ignora falhas).

**Request:**
```json
{
  "evento": "CICLO_INICIADO | CICLO_FINALIZADO",
  "dados": { "cicloId": "uuid", "totalMinutes": 15, "automatic": true, "espInitiated": false }
}
```

---

## 6. Bluetooth BLE (ESP32)

**Dispositivo alvo:** nome `ESP32_LAVAGEM`  
**Service UUID:** `12345678-1234-1234-1234-1234567890ab`  
**Characteristic UUID:** `abcd1234-5678-90ab-cdef-1234567890ab`  
**Encoding:** todos os dados trafegam como Base64 (JSON → UTF-8 bytes → Base64)

### 6.1 Permissões Android

| API Level | Permissões necessárias |
|-----------|------------------------|
| ≥ 31 (Android 12+) | `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` |
| < 31 | `ACCESS_FINE_LOCATION` |

### 6.2 Comandos enviados pelo app → ESP32

| Ação         | JSON enviado                                      | Quando enviar                                          |
|--------------|---------------------------------------------------|--------------------------------------------------------|
| Iniciar sessão | `{"action":"START","duration":<minutos>}`       | Na `SuccessScreen`, após pagamento confirmado           |
| Selecionar equipamento | `{"action":"SELECT","machine":"PRE_LAVAGEM\|ESPUMA\|ENXAGUE"}` | Na `SessionScreen`, ao tocar em um equipamento |
| Encerrar sessão | `{"action":"STOP"}`                            | Na `SessionScreen`, ao encerrar (manual ou por tempo) |

### 6.3 Notificações recebidas do ESP32 → app

| JSON recebido         | Significado                                     |
|-----------------------|-------------------------------------------------|
| `{"status":"DONE"}`   | ESP32 sinalizou fim da sessão — app encerra     |

### 6.4 Estados de conexão BLE

```
DISCONNECTED → SCANNING → CONNECTING → CONNECTED
                                          │
                                    (queda de sinal)
                                          ▼
                                    RECONNECTING ──► (backoff) ──► CONNECTING
DISCONNECTED ◄─────── (desconexão manual do usuário)
ERROR ◄──────────────── (timeout de scan ou falha)
```

**Backoff de reconexão automática:** 3s, 5s, 8s, 12s, 15s, 15s…

**Estratégia de reconexão:**
- Tentativas pares: conectar direto pelo device ID (mais rápido)
- Tentativas ímpares: fazer scan completo (cobre reboot do ESP32)

### 6.5 BLE Status Bar (componente permanente na HomeScreen)

Componente fixo que mostra o estado BLE com um indicador visual (ponto colorido ou spinner) e botões contextuais:

| Estado       | Cor do indicador | Texto                        | Botão visível   |
|--------------|------------------|------------------------------|-----------------|
| disconnected | `#BDC1C6` (cinza)  | "Desconectado"               | "Conectar"      |
| scanning     | `#FBBC04` (amarelo) + spinner | "Procurando ESP32..."  | —          |
| connecting   | `#FBBC04` + spinner | "Conectando..."             | —               |
| connected    | `#34A853` (verde) | "ESP32 conectado"            | "Desconectar"   |
| reconnecting | `#FBBC04` + spinner | "Reconectando ao ESP32..."  | —               |
| error        | `#EA4335` (vermelho) | "Falha na conexão" + msg  | "Conectar"      |

---

## 7. Fluxo Detalhado por Tela

---

### 7.1 Inicialização do App

1. Ler token do `SharedPreferences`
2. Se token presente → ir para `HomeScreen`
3. Se token ausente → ir para `ActivationScreen`
4. Durante leitura: exibir tela de splash com `CircularProgressIndicator` centralizado (cor `#1A73E8`)

---

### 7.2 ActivationScreen

**Sem header/AppBar.**

**Elementos:**
- Ícone de carro (grande, centralizado) //TODO: Substituir por logo da Bliq
- Título: "Bliq Totem" (tamanho 30, bold)
- Subtítulo: "Digite o código de ativação\ngerado no painel da franqueadora"
- Card branco com:
  - Label "CÓDIGO DE ATIVAÇÃO" (uppercase, tamanho 13)
  - Campo de texto: máx 8 chars, `allCaps`, fonte tamanho 28, bold, centralizado, `letterSpacing` amplo, placeholder "XXXX XXXX"
  - Hint: "8 caracteres — válido por 2 horas"
  - Caixa de erro (fundo `#FDE8E8`) quando há erro
- Botão "Ativar terminal" (desabilitado se `input.length != 8`)
- Rodapé: "Peça o código ao gestor da franqueadora"

**Lógica:**
1. Ao pressionar o botão, chamar `POST /api/pos/ativar` com o código em uppercase
2. Em caso de sucesso: salvar o token e navegar para `HomeScreen` (sem voltar)
3. Em caso de erro: exibir mensagem na caixa de erro

---

### 7.3 HomeScreen

**AppBar:** "Bliq Totem" | sem botão voltar | sem gesto de swipe

**Ao entrar na tela:**
1. Ler token; se inválido (null ou 401) → `clearToken()` + redirecionar para `ActivationScreen`
2. Chamar `GET /api/pos/config`
3. Chamar `GET /api/pos/ciclos/ativo`:
   - Se houver ciclo ativo com `segundosRestantes > 0` → navegar direto para `SessionScreen` passando `cicloId`, `totalMinutes = tempoContratado`, `resumeFromSeconds = segundosRestantes`
4. Exibir produtos como lista de cards

**Estados de carregamento:**
- Loading: spinner centralizado + texto "Carregando..."
- Erro de API: mensagem de erro + botão "Tentar novamente"
- Sucesso: lista de cards de produtos

**Card de produto:**
- Borda esquerda azul (`#1A73E8`, espessura 4dp)
- Coluna esquerda: nome do produto (bold 17), "X minutos" (13, cinza), "N opção(ões) de extra" (11, azul) — só se tiver extras
- Coluna direita: preço formatado em BRL (bold 20, azul), seta "›"

**Ao selecionar produto:**
- Se `produto.extras.length > 0` → navegar para `ExtrasScreen`
- Se não → navegar para `CheckoutScreen` com `selectedExtras=[]`, `totalMinutes=produto.minutes`, `totalPrice=produto.price`

**BLE Status Bar** exibida abaixo do header, antes da lista de produtos.

**Header interno da lista:**
- Ícone de carro grande
- Nome do box (`config.box.nome`)
- Texto "Selecione a lavagem"
- Nome do franqueado (`config.franqueado.nome`, tamanho 12, `#9AA0A6`)

---

### 7.4 ExtrasScreen

**AppBar:** "Extras"

**Elementos:**
- Card de resumo (branco): tipo da lavagem, tempo base, valor base
- Seção "OPÇÕES DISPONÍVEIS": lista de cards de extras
- Card de total (fundo azul `#1A73E8`): tempo total e valor total
- Botão "Continuar para pagamento"

**Card de extra (toggle):**
- Estado normal: borda transparente, fundo branco
- Estado selecionado: borda azul 2dp, fundo `#EAF1FB`
- Conteúdo: rótulo do extra (bold), "+ X minutos", preço
- Checkbox no lado direito: normal = borda cinza; selecionado = fundo azul com "✓" branco

**Cálculo dinâmico do total:**
- `totalMinutes = washOption.minutes + Σ(extras selecionados).minutos`
- `totalPrice = washOption.price + Σ(extras selecionados).preco`

**Ao continuar:** navegar para `CheckoutScreen` com todos os parâmetros calculados.

---

### 7.5 CheckoutScreen

**AppBar:** "Checkout"

**Elementos:**
- Título "Resumo do Pedido"
- Card de resumo (branco):
  - Tipo da lavagem
  - Tempo base
  - Linha por extra selecionado: rótulo + "+ X min  R$ Y,YY" (azul)
  - Divider
  - Tempo total (azul, tamanho 16)
- Card de total (fundo azul): label "Total a pagar" + valor em branco (bold 26)
- Seção de forma de pagamento (3 opções lado a lado):
  - **Crédito** 💳
  - **Débito** 🏦
  - **Pix** ⚡
  - Card selecionado: borda azul 2dp, fundo `#EAF1FB`
  - Ponto azul pequeno no card selecionado (indicador visual adicional)
- Caixa de erro (se houver)
- Botão "Confirmar Pagamento" (desabilitado até selecionar forma de pagamento)

**Ao confirmar:**
1. Simular processamento de pagamento (2s de delay — stub para integração futura com SDK de pagamento)
2. Gerar `transacaoId` aleatório (8 chars alfanuméricos uppercase)
3. Chamar `POST /api/pos/ciclos` com os dados do pedido
4. Para cada extra selecionado: chamar `PATCH /api/pos/ciclos/{id}/extra`
5. Navegar para `SuccessScreen` (substituição — sem voltar)

---

### 7.6 SuccessScreen

**AppBar:** "Pagamento Confirmado" | sem botão voltar | sem gesto de swipe

**Elementos:**
- Círculo verde (`#E6F4EA`) com ícone ✅ grande centralizado
- Título "Pagamento realizado\ncom sucesso!"
- Card branco com: forma de pagamento, tempo liberado, valor cobrado
- Card de status BLE (fundo `#F1F3F4`, ou `#FDE8E8` em erro):
  - Spinner ou ícone + texto de estado
  - Botão "Tentar novamente" (só em caso de erro)
- Botão "Voltar ao início" (só em caso de erro BLE persistente)

**Sequência BLE automática ao entrar na tela:**
1. Se BLE não conectado → `setBleStep("connecting")` → `scanAndConnect()`
2. `setBleStep("sending")` → enviar `{"action":"START","duration":<totalMinutes>}`
3. `setBleStep("registering")` → chamar `PATCH /api/pos/ciclos/{id}/iniciar`
4. `setBleStep("success")` → registrar telemetria `CICLO_INICIADO`
5. Aguardar 600ms → navegar para `SessionScreen` (substituição)

**Textos dos estados BLE:**

| Step       | Ícone/Spinner | Texto                         | Cor       |
|------------|--------------|-------------------------------|-----------|
| idle       | ⏳           | "Aguardando..."                | `#9AA0A6` |
| connecting | spinner      | "Conectando ao dispositivo..." | `#FBBC04` |
| sending    | spinner      | "Iniciando sessão..."          | `#1A73E8` |
| registering| spinner      | "Registrando ciclo..."         | `#1A73E8` |
| success    | spinner      | "Tudo certo! Abrindo painel..." | `#34A853` |
| error      | ❌           | (mensagem do erro)             | `#EA4335` |

---

### 7.7 SessionScreen

**AppBar:** "Sessão de Lavagem" | sem botão voltar | sem gesto de swipe

**Elementos:**

#### Timer Card (topo, card branco com shadow)
- Label superior: "Aguardando início" / "Tempo restante" / "Encerrando..."
- Timer em fonte grande (72sp, bold): formato `MM:SS`
- Chip de equipamento ativo (ou "Selecione um equipamento" em cinza)

Cores do timer conforme tempo restante:
- `remaining > 180s`: `#1A73E8` (azul)
- `60s < remaining ≤ 180s`: `#FBBC04` (amarelo)
- `remaining ≤ 60s`: `#EA4335` (vermelho)
- Sessão não iniciada: `#9AA0A6` (cinza)

#### Seleção de Equipamento

3 cards clicáveis, dispostos em coluna:

| ID           | Label       | Ícone | Cor base  |
|--------------|-------------|-------|-----------|
| PRE_LAVAGEM  | Pré-lavagem | 💦    | `#1A73E8` |
| ESPUMA       | Espuma      | 🫧    | `#9C27B0` |
| ENXAGUE      | Enxague     | 🚿    | `#00897B` |

Card selecionado: borda 2dp na cor do equipamento, fundo `<cor>12` (12% opacidade), ponto colorido no canto superior direito.

**Ao tocar em equipamento:**
1. Se sessão não iniciada → inicia o timer
2. Enviar `{"action":"SELECT","machine":"<ID>"}` via BLE
3. Se BLE desconectado → tentar reconectar antes de enviar
4. Se falhar → exibir mensagem de erro BLE (caixa vermelha)

#### Caixa de erro BLE
Fundo `#FDE8E8`, texto vermelho — exibida temporariamente quando há falha ao enviar comando.

#### Botão "Encerrar sessão"
- Borda vermelha, texto vermelho
- Ao tocar: `AlertDialog` de confirmação
  - Se sessão em andamento: "Ainda restam MM:SS de lavagem. Deseja encerrar mesmo assim?"
  - Se não iniciada: "Deseja cancelar a sessão?"
  - Ações: "Cancelar" | "Encerrar" (destructive)

**Lógica de encerramento da sessão:**
1. Marcar `ended = true` (idempotente — impede dupla finalização)
2. Parar timer
3. Se não foi o ESP32 que iniciou: enviar `{"action":"STOP"}` via BLE
4. Chamar `PATCH /api/pos/ciclos/{id}/finalizar` com **retry** (4 tentativas, backoff 1.5s × N)
5. Registrar telemetria `CICLO_FINALIZADO`
6. Navegar para `HomeScreen` (`popToTop`)

**Gatilhos de encerramento automático:**
- Timer chega a zero (`remaining == 0`)
- ESP32 envia notificação BLE `{"status":"DONE"}`

**Retomada de sessão (`resumeFromSeconds` presente):**
- Timer inicia imediatamente com `resumeFromSeconds`
- Chamar `PATCH /api/pos/ciclos/{id}/iniciar` (fire-and-forget)
- Quando BLE conectar pela primeira vez nesta sessão: enviar `START` com `ceil(segundosRestantes / 60)` minutos

---

## 8. Modelos de Dados (Data Classes Kotlin)

```kotlin
// Persistência
data class PosToken(val value: String)

// API
data class PosConfig(
    val terminal: Terminal,
    val franqueado: Franqueado,
    val box: Box,
    val esp32: Esp32Info,
    val produtos: List<Produto>
)

data class Terminal(val id: String, val serial: String, val modelo: String?)
data class Franqueado(val id: String, val nome: String, val status: String)
data class Box(val id: String, val nome: String, val status: String)
data class Esp32Info(val uuid: String, val versao: String?, val online: Boolean, val ultimaVezEm: String?)

data class Produto(
    val id: String,
    val nome: String,
    val tipo: String,           // "TEMPO_FIXO" | "MINUTAGEM_AVULSA"
    val tempoMinutos: Int?,
    val preco: String,          // string decimal, ex: "29.90"
    val boxId: String,
    val extras: List<ProdutoExtra>
)

data class ProdutoExtra(
    val id: String,
    val produtoId: String,
    val rotulo: String,
    val minutos: Int,
    val preco: String,          // string decimal
    val ativo: Boolean
)

data class Ciclo(
    val id: String,
    val boxId: String,
    val terminalId: String,
    val produtoId: String,
    val tempoContratado: Int,
    val status: String,
    val createdAt: String
)

data class CicloAtivo(
    val id: String,
    val tempoContratado: Int,
    val status: String,         // "AGUARDANDO" | "EM_ANDAMENTO"
    val segundosRestantes: Int
)

// Navegação interna
data class WashOption(
    val id: String,
    val label: String,
    val minutes: Int,
    val price: Double,
    val extras: List<WashOptionExtra>
)

data class WashOptionExtra(
    val id: String,
    val rotulo: String,
    val minutos: Int,
    val preco: Double
)

enum class PaymentMethod { CREDIT, DEBIT, PIX }
enum class MetodoPagamento { CARTAO_CREDITO, CARTAO_DEBITO, PIX }
enum class Machine { PRE_LAVAGEM, ESPUMA, ENXAGUE }

// BLE
enum class ConnectionStatus { DISCONNECTED, SCANNING, CONNECTING, CONNECTED, RECONNECTING, ERROR }
```

---

## 9. Formatação de Moeda

Usar `NumberFormat` com `Locale("pt", "BR")` e `Currency.getInstance("BRL")`.

```kotlin
fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
```

---

## 10. Stub de Pagamento

O processamento de pagamento atual é um **stub** (simulação). A integração real com SDK de maquininha será feita futuramente. Por ora:

1. Aguardar 2 segundos (simular processamento)
2. Gerar `transacaoId` aleatório (8 chars alfanuméricos uppercase)
3. Retornar sucesso sempre

```kotlin
suspend fun processPayment(method: PaymentMethod, amount: Double): PaymentResult {
    delay(2_000)
    return PaymentResult(
        success = true,
        transactionId = (1..8).map { ('A'..'Z').random() }.joinToString(""),
        method = method,
        amount = amount,
        timestamp = System.currentTimeMillis()
    )
}
```

---

## 11. Dependências Sugeridas (Gradle)

```kotlin
// UI
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose")
implementation("androidx.activity:activity-compose")

// Rede
implementation("com.squareup.retrofit2:retrofit")
implementation("com.squareup.retrofit2:converter-gson")
implementation("com.squareup.okhttp3:logging-interceptor")

// Bluetooth BLE
implementation("no.nordicsemi.android:ble:2.7.5")
// ou usar a API nativa Android BLE (android.bluetooth.le)

// Persistência
implementation("androidx.security:security-crypto")   // EncryptedSharedPreferences

// Coroutines / ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")
```

---

## 12. Notas de Implementação

- **Singleton BLE Manager:** instanciar o `BluetoothAdapter` / scanner uma única vez no ciclo de vida do app (via `Application` class ou hilt singleton).
- **Reconexão automática:** implementar via `CoroutineScope` com `delay()` e backoff — não usar handlers manuais.
- **Idempotência no encerramento:** usar `AtomicBoolean` para garantir que `finalizarCiclo` só é chamado uma vez, mesmo com múltiplos gatilhos simultâneos (timer + ESP32 DONE).
- **Retry com backoff:** ao finalizar ciclo, tentar até 4 vezes com delay crescente (`1500ms * tentativa`).
- **Fire-and-forget:** telemetria nunca deve bloquear o fluxo — usar `launch { }.invokeOnCompletion {}` sem tratar erros.
- **Token inválido (401):** interceptar globalmente no cliente HTTP (OkHttp interceptor), limpar token e postar evento para a UI navegar para `ActivationScreen`.
- **Retomada de sessão:** ao abrir o app com token válido, sempre checar `GET /api/pos/ciclos/ativo` antes de exibir a Home. Se houver ciclo ativo, redirecionar para `SessionScreen` automaticamente.
x