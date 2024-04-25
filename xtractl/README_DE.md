# xtractl

`xtractl` ist ein CLI-Tool mit dem man Teile der laufenden Applikation kontrollieren kann, die ansonsten einen Neustart benötigen würden.

> [!NOTE]
> Die Hauptversion von `xtractl` sollte der Hauptversion der verwendeten `ldproxy`-Version entsprechen.
> Wenn Sie zum Beispiel die `ldproxy`-Version `3.6.4` verwenden, sollten Sie die neueste `xtractl`-Version `3.x` verwenden.

## Installation

#### Binary

Das Binary für die gewünschte Platform von der [Releases](https://github.com/interactive-instruments/xtraplatform-cli/releases) Seite herunterladen und in das gewünschte Verzeichnis kopieren. Es kann sein, dass das Executable-Flag gesetzt werden muss: `chmod +x xtractl`

#### Docker

Das neueste Image herunterladen: `docker pull ghcr.io/ldproxy/xtractl`

## Anwendung

### Hilfe

Alle Commands und Subcommands haben ein Hilfe-Flag, z.B.:

#### Binary

- `xtractl --help`
- `xtractl entity --help`
- `xtractl entity ls --help`

#### Docker

- `docker run -it --rm ghcr.io/ldproxy/xtractl --help`
- `docker run -it --rm ghcr.io/ldproxy/xtractl entity --help`
- `docker run -it --rm ghcr.io/ldproxy/xtractl entity ls --help`
