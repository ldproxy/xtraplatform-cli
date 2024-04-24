package cmd

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/cmd/store"

	"github.com/spf13/cobra"
)

var gitTag string
var gitSha string
var gitBranch = "unknown"

func version() string {
	if len(gitTag) > 0 {
		return gitTag
	} else if len(gitSha) > 0 {
		return gitBranch + "-" + gitSha
	}

	return "DEV"
}

var name string = filepath.Base(os.Args[0])
var storeSrc client.Store

// RootCmd represents the base command when called without any subcommands
var RootCmd = &cobra.Command{
	Use:     name,
	Version: version(),
	Long:    name + ` provides tools to manage configurations for xtraplatform applications like ldproxy and XtraServer Web API.`,
	Run: func(cmd *cobra.Command, args []string) {
		cmd.Help()
	},
	PersistentPreRunE: func(cmd *cobra.Command, args []string) error {
		if cmd.Name() == "help" || cmd.Name() == "listen" || cmd.Name() == name {
			return nil
		}
		return storeSrc.Connect()
	},
	DisableAutoGenTag: true,
}

// Execute a call
func Execute() {
	if err := RootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}

func init() {

	src := RootCmd.PersistentFlags().StringP("src", "s", "./", "store source")
	typ := RootCmd.PersistentFlags().StringP("driver", "d", "FS", "store source driver; currently the only option is FS")
	verbose := RootCmd.PersistentFlags().BoolP("verbose", "v", false, "verbose output")
	debug := RootCmd.PersistentFlags().Bool("debug", false, "debug output")
	RootCmd.PersistentFlags().MarkHidden("debug")
	RootCmd.PersistentFlags().Bool("help", false, "show help")

	storeSrc = *client.New(src, typ, verbose, debug)

	infoCmd := store.InfoCmd(storeSrc, name, verbose, debug)

	checkCmd := store.CheckCmd(storeSrc, name, verbose, debug)

	upgradeCmd := store.UpgradeCmd(storeSrc, name, verbose, debug)

	listenCmd := store.ListenCmd(storeSrc, name, version(), verbose, debug)

	RootCmd.AddCommand(infoCmd)
	RootCmd.AddCommand(checkCmd)
	RootCmd.AddCommand(upgradeCmd)
	RootCmd.AddCommand(listenCmd)

	RootCmd.SetHelpCommand(&cobra.Command{Use: "help2", Hidden: true})

	RootCmd.CompletionOptions.DisableDefaultCmd = true
	RootCmd.PersistentFlags().SortFlags = false
	RootCmd.Flags().SortFlags = false
}
