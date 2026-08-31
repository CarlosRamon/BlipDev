# Passo a passo — Criar uma franquia no BLIQ

Acesso como **Franqueadora** (backoffice central). O menu segue a ordem: `Franqueados → Rede de Boxes → Terminais → Dispositivos → Produtos`. Essa é a sequência lógica de cadastro — cada etapa depende da anterior.

---

## 1. Criar o Franqueado

**Menu:** `Franqueados` → botão **"Novo Franqueado"**

Modal pede 4 campos:

- **Nome da empresa**
- **CNPJ** (formato `00.000.000/0001-00`)
- **E-mail do gestor** (será o login)
- **Senha do gestor** (senha inicial de acesso ao painel do franqueado)

O sistema cria o `Franqueado` já com status `ATIVO` e provisiona o `Usuario` com role `FRANQUEADO` vinculado. Depois disso, dá para trocar o status (Ativar / Suspender / Bloquear) direto na tabela.

---

## 2. Criar os Boxes (pontos físicos da unidade)

**Menu:** `Rede de Boxes` → botão **"Novo Box"**

Modal pede:

- **Tipo do box** — `Lavação` (água/sabão/enxágue) ou `Aspiração`
- **Nome do box** (ex: "Box Lavação 01")
- **Descrição** (opcional — "Ponto 1 — entrada principal")
- **Franqueado** (dropdown com os cadastrados no passo 1)

Repita para cada box físico da unidade. Cada box nasce com status `DISPONIVEL` e sem dispositivo vinculado (aparece "Sem dispositivo" no card).

---

## 3. Cadastrar os Terminais (POS Android)

**Menu:** `Terminais` → botão **"Novo Terminal"**

Modal pede:

- **Serial do dispositivo** (o serial físico da POS — único no sistema)
- **Modelo** (opcional — ex: "SmartPOS Android")
- **Franqueado** (deve ser o mesmo franqueado do box a ser vinculado)

Após criar, o terminal aparece como "Não vinculado" na coluna ESP32/Box. **Ainda não gere o código de ativação** — o botão só funciona depois do próximo passo.

---

## 4. Vincular Dispositivo (BOX + POS + ESP32)

**Menu:** `Dispositivos` → botão **"Registrar Dispositivo"**

Este é o vínculo obrigatório da plataforma. Modal pede:

- **Box** (dropdown mostra apenas boxes sem dispositivo)
- **Terminal POS** (dropdown mostra apenas terminais sem dispositivo)
- **Versão do firmware** (opcional)

O `uuidEsp32` é **gerado automaticamente** pelo backend nesse momento — não precisa digitar. Após salvar, o trio `Box + POS + ESP32` fica selado (relações 1‑1 no schema).

---

## 5. Gerar código de ativação da POS

**Menu:** `Terminais` → botão **"Ativar POS"** na linha do terminal recém-vinculado

Abre modal com um código curto em destaque (`XXXX YYYY`) e o passo-a-passo que o operador segue na POS:

1. Abrir app Bliq na SmartPOS
2. Tocar em "Configurar dispositivo"
3. Digitar o código e confirmar
4. A POS carrega automaticamente o box e os produtos

O código não expira; dá para cancelá-lo (botão `X`) e gerar outro. Depois de ativado, a coluna mostra "Ativado {data}".

---

## 6. Cadastrar Produtos por Box

**Menu:** `Produtos` → botão **"Novo Produto"**

Importante: **produtos são por Box** (`Produto.boxId`), não por franqueado. Cada box tem seu catálogo.

Modal pede:

- **Nome** (ex: "Lavagem 8 min")
- **Box** (dropdown mostra o par `Nome do box — Franqueado`)
- **Tipo**:
  - `Tempo fixo` — ciclo pré-definido (usa `tempoMinutos`)
  - `Minutagem avulsa` — preço por minuto, sem tempo pré-definido
- **Tempo (minutos)** — obrigatório para `Tempo fixo`
- **Preço (R$)** — valor do ciclo

Repita para cada produto do catálogo daquele box (ex: 8 min / 12 min / 15 min).

---

## 7. Configurar Tempo Extra (upsell) por produto

Ainda em `Produtos`, na linha de um produto do tipo `Tempo fixo`, clique em **"+ Adicionar"** na coluna **Extras**.

Modal pede, para cada opção de upsell:

- **Rótulo** (ex: "+3 min")
- **Minutos**
- **Preço (R$)**

Você pode criar N opções por produto (o modelo `ProdutoExtra` aceita múltiplas variações). Cada produto tem seu próprio conjunto de extras — "Lavagem 8 min" pode ter 3 opções e "Lavagem 15 min" outra combinação totalmente diferente.

---

## Checklist final antes de operar

Para o box conseguir vender ciclos, o encadeamento tem que estar completo:

- [ ] Franqueado `ATIVO`
- [ ] Box criado sob o franqueado, status `DISPONIVEL`
- [ ] Terminal criado sob o **mesmo** franqueado
- [ ] Dispositivo registrado (trio Box + POS + ESP32 vinculado)
- [ ] POS ativada com o código (`Ativado {data}` na coluna)
- [ ] Ao menos 1 produto ativo no box
- [ ] Extras configurados nos produtos de tempo fixo (se for oferecer upsell)

A partir daí, o app da POS busca produtos filtrando pelo `boxId` do dispositivo vinculado, e cada venda gera um **Ciclo** (a unidade central de negócio da plataforma).
