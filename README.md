# Revisão Espaçada

Cartões de estudo com revisão baseada no histórico de respostas.

## Implementado na primeira versão

**Kotlin — núcleo executável de domínio.** Agenda a próxima revisão com uma regra inspirada em SM-2: notas de 0 a 5, repetição após erro, intervalos iniciais de 1 e 6 dias e fator de facilidade mínimo de 1,3. Também lista os cartões vencidos ou que vencem hoje a partir de uma coleção TSV.

## Como executar

Instale JDK 17+ e o compilador Kotlin 2.1.0 (ou compatível). Os comandos abaixo partem da raiz do repositório.

```sh
kotlinc Main.kt -include-runtime -d app.jar
java -jar app.jar 2026-11-03 5 0 0 2.5
java -jar app.jar --pendentes 2026-11-03 examples/cartoes.tsv
```

## Consulta de cartões pendentes

O modo `--pendentes DATA cartoes.tsv` lê um arquivo com cabeçalho `cartao<TAB>proxima_revisao` e duas colunas por linha: ID único do cartão e data `AAAA-MM-DD`. Mostra somente revisões com vencimento **até a data escolhida**, ordenadas da mais antiga para a mais recente (desempate por ID), com totais de pendentes e cadastrados. A consulta não altera o arquivo nem recalcula as notas.

O arquivo precisa ter até 1 MB e no máximo 10.000 cartões, sem IDs duplicados. Os dados do exemplo são fictícios. Este módulo não registra respostas nem sincroniza seu planejamento com a agenda.

## Testes

```sh
kotlinc Main.kt -include-runtime -d app.jar
java -jar app.jar --self-test
```

Os testes cobrem invariantes e entradas inválidas. A preparação local não dispõe deste runtime; o workflow em `.github/workflows/ci.yml` executa a validação no GitHub Actions. O resultado de cada execução está na aba Actions, associado ao commit.

## Organização e limites

Código e exemplos: `Main.kt` e `examples/cartoes.tsv`. Os dados de exemplo são fictícios. As funções de domínio são separadas da leitura de argumentos no mesmo arquivo para manter a primeira versão pequena e revisável.

É um núcleo de agendamento, sem cadastro de cartões ou banco de dados. Não há promessa de resultado de aprendizagem. Compose e Room ainda não foram integrados.

A proposta completa de evolução está em `ARQUITETURA.md`. As tecnologias propostas nessa arquitetura não devem ser interpretadas como dependências já implementadas.

## Próximas entregas

- [ ] Criar cartões e coleções — ampliar o núcleo atual até o fluxo completo.
- [ ] Agendar próxima revisão — ampliar o núcleo atual até o fluxo completo.
- [x] Consultar cartões pendentes pela data informada em TSV.
- [ ] Consultar estatísticas de acertos e histórico de respostas.

Licença: ainda não definida.
