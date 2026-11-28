# Engenharia reversa e estudo — Revisão Espaçada — Kotlin

## Objetivo e fluxo

Revisão espaçada com baralho persistente e agenda. Main.kt calcula a próxima revisão. StudyDeck em Deck.kt aplica o resultado às operações do baralho, e DeckCli.kt organiza os comandos e a consulta de revisões.

**Tecnologias identificadas** · Kotlin.

**Revisão de referência** · `d1871ff8cc72`.

## Mapa do código

| Caminho | Papel de leitura | Símbolos selecionados |
| --- | --- | --- |
| [Deck.kt](../Deck.kt) | Implementação | `StudyDeck`, `encode`, `decodeUtf8`, `decode` |
| [DeckCli.kt](../DeckCli.kt) | Implementação | — |
| [DeckTests.kt](../DeckTests.kt) | Implementação | `failure` |
| [Main.kt](../Main.kt) | Implementação | — |

## Configuração e referências

Referências existentes: [ARQUITETURA.md](../ARQUITETURA.md), [docs/GUIA_DE_VALIDACAO.md](GUIA_DE_VALIDACAO.md).

## Roteiro de estudo e desenvolvimento

1. Comece pelo fluxo descrito acima e localize seus pontos de entrada no mapa. Acompanhe uma entrada válida até a transformação ou a resposta.
2. Abra um teste relacionado ao fluxo e descreva qual regra ele protege. Compare o tratamento esperado para entradas válidas, inválidas e nos limites.
3. Ao adicionar uma regra, determine qual módulo controla o dado e cubra o caso nesse nível. Em seguida, confira o efeito no comando, na API ou na interface de entrada.

**Pergunta de entrevista**

Como manter uma revisão com nota baixa consistente com o reinício da sequência e o cálculo da próxima data?

## Testes versionados

Arquivos para começar a leitura dos cenários de teste: [DeckTests.kt](../DeckTests.kt).

Os comandos e versões de runtime usados no CI ficam em [.github/workflows/ci.yml](../.github/workflows/ci.yml).

[Voltar ao README](../README.md).
