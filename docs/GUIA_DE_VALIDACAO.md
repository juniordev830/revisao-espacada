# Guia técnico — Revisão Espaçada

## Estado atual

Lógica Kotlin de agendamento de revisão; ainda não oferece cadastro de cartões e interface móvel.

## Referências de código

`Main.kt` e `ARQUITETURA.md`.

## Validar mudanças

Compilar `kotlinc Main.kt -include-runtime -d app.jar` e executar o modo de demonstração/teste documentado no README.

Usar somente dados fictícios e registrar comandos executados com resultados no PR. A mera presença de um teste no repositório não comprova que ele passou.

## Cenários de regressão

Conferir datas de repetição, limite mínimo de intervalo, respostas inválidas, transição de desempenho e resultados com relógio de teste fixo.

## Próximo incremento

Adicionar persistência de cartões e coleções só depois de definir um formato de dados versionado; não prometer eficácia de aprendizagem.

## Critérios de revisão

- [ ] Código e documentação refletem o mesmo comportamento.
- [ ] Casos de fronteira estão protegidos por teste ou roteiro de validação.
- [ ] Arquivos e saídas geradas ficam fora do versionamento, quando contêm dados privados.
- [ ] Mudanças passam por branch e PR, sem integração automática.
