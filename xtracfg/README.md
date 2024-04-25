# xtracfg

`xtracfg` is a CLI tool that allows you to check your configurations for deprecated options and errors and can also automatically upgrade your configurations to the recent version.

> [!NOTE]
> The major version of `xtracfg` should match the major version of the `ldproxy` version that you are using the configurations with. 
> So if you are for example using `ldproxy` version `3.6.4`, you should use the most recent `xtracfg` version `3.x`. 
> Configurations that are upgraded with the most recent `xtracfg` version `3.x` will work with the most recent `ldproxy` version `3.x` and also with `ldproxy` version `4.x`.
> When you switch to `ldproxy` version `4.x`, you should also switch to the most recent `xtracfg` version `4.x`.

## Installation

#### Binary

Download the binary for your platform from the [releases](https://github.com/interactive-instruments/xtraplatform-cli/releases) page and copy it to path of your liking. You may have to set the executable flag with `chmod +x xtracfg`.

#### Docker

Pull the latest image: `docker pull ghcr.io/ldproxy/xtracfg`

## Usage

### Help

All commands and subcommands have a `--help` flag, for example:

#### Binary

- `xtracfg --help`
- `xtracfg check --help`
- `xtracfg check entities --help`

#### Docker

- `docker run -it --rm ghcr.io/ldproxy/xtracfg --help`
- `docker run -it --rm ghcr.io/ldproxy/xtracfg check --help`
- `docker run -it --rm ghcr.io/ldproxy/xtracfg check entities --help`

### Source

All Commands commands and subcommands need a directory to operate on.

#### Binary

The default is the current directory, to use another directory:

- `xtracfg --src /ldproxy/data info`

#### Docker

The default is `/src` inside of the container, so you have to mount the desired directory:

- `docker run -it --rm -v /ldproxy/data:/src ghcr.io/ldproxy/xtracfg info`
