# xtractl

`xtractl` is a CLI tool that allows you to control over certain parts of the running application that would otherwise require a restart.

## Installation

### Binary

Download the binary for your platform from the [releases](https://github.com/interactive-instruments/xtraplatform-cli/releases) page and copy it to path of your liking. You may have to set the executable flag with `chmod +x xtractl`.

### Docker

Pull the latest image: `docker pull ghcr.io/ldproxy/xtractl`

## Usage

### Help

All commands and subcommands have a `--help` flag, for example:

#### Binary

- `xtractl --help` 
- `xtractl entity --help` 
- `xtractl entity ls --help` 

#### Docker

- `docker run -it --rm ghcr.io/ldproxy/xtractl --help`
- `docker run -it --rm ghcr.io/ldproxy/xtractl entity --help`
- `docker run -it --rm ghcr.io/ldproxy/xtractl entity ls --help`