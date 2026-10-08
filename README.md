# TubeAudio

Aplicação Android para pesquisar vídeos do YouTube e reproduzir o respetivo áudio.

## Recuperação do projeto

Esta base foi reconstruída a partir de um ZIP inicial de 2 de setembro de 2026. O código
da versão anterior com playlists, downloads e sincronização de conta não estava no
repositório. Esta entrega recupera pesquisa, reprodução em segundo plano, controlos
de pausa e posição, favoritos e histórico locais. Os favoritos e o histórico sobrevivem
ao fecho da aplicação; os URLs temporários de áudio são obtidos novamente ao tocar.

## Compilar

Requisitos: JDK 17, Android SDK 36, Gradle 9.5+ e ligação à Internet para obter dependências.

```sh
gradle :app:assembleDebug
```

O APK de debug fica em `app/build/outputs/apk/debug/app-debug.apk`. O workflow
`Android debug build` disponibiliza o APK como artefacto de cada execução bem sucedida.
Se a app anterior tiver outro identificador ou assinatura, instala-se em paralelo.

## Estado

- Pesquisa e resolução de áudio com NewPipeExtractor.
- MediaSessionService e MediaController para reprodução em segundo plano.
- Favoritos e 100 faixas recentes no armazenamento local.
- Pesquisa, favoritos, histórico, miniplayer e player com posição.
- Playlists, downloads offline e OAuth ainda por reconstruir.

A disponibilidade do áudio depende do extrator e da origem. O fluxo real de
reprodução e os controlos por Bluetooth precisam de validação num dispositivo.
