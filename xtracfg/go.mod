module github.com/interactive-instruments/xtraplatform-cli/xtracfg

go 1.23.2

require (
	github.com/AlecAivazis/survey/v2 v2.3.7
	github.com/fatih/color v1.10.0
	github.com/gorilla/websocket v1.5.1
	github.com/interactive-instruments/xtraplatform-cli/libxtracfg/go v0.0.0-00010101000000-000000000000
	github.com/spf13/cobra v1.7.0
)

require (
	github.com/inconshreveable/mousetrap v1.1.0 // indirect
	github.com/kballard/go-shellquote v0.0.0-20180428030007-95032a82bc51 // indirect
	github.com/mattn/go-colorable v0.1.8 // indirect
	github.com/mattn/go-isatty v0.0.12 // indirect
	github.com/mgutz/ansi v0.0.0-20170206155736-9520e82c474b // indirect
	github.com/spf13/pflag v1.0.5 // indirect
	golang.org/x/net v0.17.0 // indirect
	golang.org/x/sys v0.13.0 // indirect
	golang.org/x/term v0.13.0 // indirect
	golang.org/x/text v0.13.0 // indirect
)

replace github.com/interactive-instruments/xtraplatform-cli/libxtracfg/go => ../libxtracfg/go
