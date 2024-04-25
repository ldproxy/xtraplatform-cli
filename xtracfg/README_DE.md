# xtracfg

`xtracfg` ist ein CLI-Tool mit dem man Konfiguration auf veraltete Optionen und Fehler prüfen sowie Konfigurationen automatisch auf den aktuellen Stand bringen kann.

> [!NOTE]
> Die Hauptversion von `xtracfg` sollte der Hauptversion der `ldproxy`-Version entsprechen, mit der die Konfigurationen verwendet werden.
> Wenn Sie zum Beispiel die `ldproxy`-Version `3.6.4` verwenden, sollten Sie die neueste `xtracfg`-Version `3.x` verwenden.
> Konfigurationen, die mit der neuesten `xtracfg`-Version `3.x` aktualisiert wurden, funktionieren mit der neuesten `ldproxy`-Version `3.x` und auch mit `ldproxy`-Version `4.x`.
> Wenn Sie auf die `ldproxy`-Version `4.x` wechseln, sollten Sie auch auf die neueste `xtracfg`-Version `4.x` wechseln.

## Installation

#### Binary

Das Binary für die gewünschte Platform von der [Releases](https://github.com/interactive-instruments/xtraplatform-cli/releases) Seite herunterladen und in das gewünschte Verzeichnis kopieren. Es kann sein, dass das Executable-Flag gesetzt werden muss: `chmod +x xtracfg`

#### Docker

Das neueste Image herunterladen: `docker pull ghcr.io/ldproxy/xtracfg`

## Anwendung

### Hilfe

Alle Commands und Subcommands haben ein Hilfe-Flag, z.B.:

#### Binary

- `xtracfg --help`
- `xtracfg check --help`
- `xtracfg check entities --help`

#### Docker

- `docker run -it --rm ghcr.io/ldproxy/xtracfg --help`
- `docker run -it --rm ghcr.io/ldproxy/xtracfg check --help`
- `docker run -it --rm ghcr.io/ldproxy/xtracfg check entities --help`

### Source

Alle Commands benötigen ein Verzeichnis, auf dem sie operieren.

#### Binary

Der Default ist das aktuelle Verzeichnis, um ein anderes Verzeichnis zu verwenden:

- `xtracfg --src /ldproxy/data info`

#### Docker

Der Default ist `/src` im Container, das gewünschte Verzeichnis muss dort gemountet werden:

- `docker run -it --rm -v /ldproxy/data:/src ghcr.io/ldproxy/xtracfg info`
