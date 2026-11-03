# Revisão Espaçada

Cartões de estudo com revisão baseada no histórico de respostas.

## Implementado na primeira versão

**Kotlin — núcleo executável de domínio.** Agenda a próxima revisão com uma regra inspirada em SM-2: notas de 0 a 5, repetição após erro, intervalos iniciais de 1 e 6 dias e fator de facilidade mínimo de 1,3.

## Como executar

Instale JDK 17+ e o compilador Kotlin 2.1.0 (ou compatível). Os comandos abaixo partem da raiz do repositório.

```sh
kotlinc Main.kt -include-runtime -d app.jar
java -jar app.jar 2026-11-03 5 0 0 2.5
```

## Testes

```sh
kotlinc Main.kt -include-runtime -d app.jar
java -jar app.jar --self-test
```

Os testes cobrem invariantes e entradas inválidas. A preparação local não dispõe deste runtime; o workflow em `.github/workflows/ci.yml` executa a validação no GitHub Actions. O resultado de cada execução está na aba Actions, associado ao commit.

## Organização e limites

Código e exemplos: `Main.kt`. Os dados de exemplo são fictícios. As funções de domínio são separadas da leitura de argumentos no mesmo arquivo para manter a primeira versão pequena e revisável.

É um núcleo de agendamento, sem cadastro de cartões ou banco de dados. Não há promessa de resultado de aprendizagem. Compose e Room ainda não foram integrados.

A proposta completa de evolução está em `ARQUITETURA.md`. As tecnologias propostas nessa arquitetura não devem ser interpretadas como dependências já implementadas.

## Próximas entregas

- [ ] Criar cartões e coleções — ampliar o núcleo atual até o fluxo completo.
- [ ] Agendar próxima revisão — ampliar o núcleo atual até o fluxo completo.
- [ ] Consultar acertos e cartões pendentes — ampliar o núcleo atual até o fluxo completo.

Licença: ainda não definida.
