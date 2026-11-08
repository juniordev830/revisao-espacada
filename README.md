# Revisão Espaçada — Kotlin

Ferramenta de estudo baseada em repetição espaçada com algoritmo de revisão em Kotlin e um **baralho local persistente**. O algoritmo original e seus comandos permanecem disponíveis.

## Novo nesta branch

- Criação, edição, consulta e exclusão de cartões de pergunta e resposta.
- Revisão por nota de 0 a 5, reaproveitando o algoritmo original de intervalo, sequência e facilidade.
- Listagem de cartões vencidos ou previstos para o dia, ordenados pela data da próxima revisão.
- Busca textual por identificador, pergunta ou resposta.
- Estatísticas de cartões pendentes, em aprendizagem, dominados e revisões acumuladas.
- Armazenamento local em arquivo TSV versionado, com codificação Base64 URL-safe para textos multilinha.
- Gravação por arquivo temporário e substituição, reduzindo o risco de arquivos incompletos.
- Testes automatizados com casos de validação, evolução dos cartões, persistência e CLI.

## Requisitos

Kotlin **2.1** e Java **17 ou superior**. O GitHub Actions baixa e instala o compilador Kotlin para compilar e executar os testes.

```bash
kotlinc Main.kt Deck.kt DeckCli.kt DeckTests.kt -include-runtime -d app.jar
java -jar app.jar --self-test
```

## Criar e revisar cartões

Com conteúdo fictício:

```bash
java -jar app.jar --deck baralho.local.tsv add cartao-1 "O que é uma API?" "Interface de programação" 2026-11-08
java -jar app.jar --deck baralho.local.tsv add cartao-2 "O que faz git status?" "Mostra alterações no repositório" 2026-11-08
java -jar app.jar --deck baralho.local.tsv due 2026-11-08
java -jar app.jar --deck baralho.local.tsv review cartao-1 5 2026-11-08
java -jar app.jar --deck baralho.local.tsv stats 2026-11-09
java -jar app.jar --deck baralho.local.tsv search API
java -jar app.jar --deck baralho.local.tsv edit cartao-2 "Como verificar o status do Git?" "Usar git status"
java -jar app.jar --deck baralho.local.tsv list
java -jar app.jar --deck baralho.local.tsv remove cartao-2
```

Cada nota (0–5) atualiza a próxima revisão. O programa valida datas em ordem cronológica e recusa notas fora do limite.

## Compatibilidade com o programa original

O cálculo individual ainda funciona:

```bash
java -jar app.jar 2026-11-08 5 0 0 2.5
```

Ou, para rodar os testes do algoritmo original e do novo baralho:

```bash
java -jar app.jar --self-test
```

## Privacidade e limites

O arquivo `baralho.local.tsv` contém suas perguntas, respostas e histórico de estudo. **Não é criptografado**, não deve ser publicado num repositório público e não é sincronizado com serviços externos. O limite do protótipo é **2.500 cartões**, com no máximo **1 MB** por baralho.

A nova ferramenta de terminal não inclui interface gráfica, notificações, contas de usuário ou sincronização entre dispositivos.

## Licença

Ainda não definida.
