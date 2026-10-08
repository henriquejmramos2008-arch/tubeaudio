# TubeAudio

Aplicação Android para pesquisar vídeos do YouTube e reproduzir o respetivo áudio.

## Recuperação do projeto

Esta base foi reconstruída a partir de um ZIP inicial de 2 de setembro de 2026. O código
da versão anterior com downloads e sincronização de conta não estava no
repositório. Esta entrega recupera pesquisa, reprodução em segundo plano, controlos
de pausa e posição, favoritos, histórico, playlists e downloads locais. A biblioteca sobrevive
ao fecho da aplicação; os URLs temporários de áudio são obtidos novamente ao tocar.

## Compilar

Requisitos: JDK 17, Android SDK 36, Gradle 9.5+ e ligação à Internet para obter dependências.

```sh
gradle :app:assembleDebug
```

O APK de debug fica em `app/build/outputs/apk/debug/app-debug.apk`. O workflow
`Android debug build` disponibiliza o APK como artefacto de cada execução bem sucedida.
Se a app anterior tiver outro identificador, instala-se em paralelo. Se tiver o
mesmo identificador e uma assinatura diferente, é preciso desinstalá-la antes
(o que apaga os dados locais dessa instalação).

## Estado

- Pesquisa e resolução de áudio com NewPipeExtractor.
- MediaSessionService e MediaController para reprodução em segundo plano.
- Favoritos, playlists e 100 faixas recentes no armazenamento local.
- Início com acesso rápido, pesquisa, biblioteca, miniplayer e player com posição.
- Fila a partir dos resultados e das playlists, próximo/anterior, aleatório e repetir uma faixa.
- Download para armazenamento privado, notificação de progresso e reprodução do ficheiro sem rede.
- Erros de pesquisa com repetição e detalhes técnicos para diagnóstico.
- OAuth e sincronização de conta ainda por reconstruir.

A disponibilidade do áudio depende do extrator e da origem. O fluxo real de
reprodução e os controlos por Bluetooth precisam de validação num dispositivo.
Os downloads precisam de validação num dispositivo e dependem de o extrator obter
um fluxo direto disponível. Desinstalar a app apaga os downloads privados.
