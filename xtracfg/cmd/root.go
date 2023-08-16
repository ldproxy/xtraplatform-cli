package cmd

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/cmd/store"

	"github.com/spf13/cobra"
)

const version = "2.0.0-SNAPSHOT"

var name string = filepath.Base(os.Args[0])
var storeSrc client.Store

// RootCmd represents the base command when called without any subcommands
var RootCmd = &cobra.Command{
	Use:     name,
	Version: version,
	Long:    name + ` provides tools to manage configurations for xtraplatform applications like ldproxy and XtraServer Web API.`,
	/*Run: func(cmd *cobra.Command, args []string) {
		interactive()
	},*/
	PersistentPreRunE: func(cmd *cobra.Command, args []string) error {

		return storeSrc.Connect()
	},
	DisableAutoGenTag: true,
}

var subcommands = map[string]*cobra.Command{}

// Execute adds all child commands to the root command and sets flags appropriately.
// This is called by main.main(). It only needs to happen once to the RootCmd.
func Execute() {
	/*fmt.Printf("ARGS: %s\n", os.Args[1:])
	args := []string{"check", "-v"}
	fmt.Printf("TODO ARGS: %s\n", args)
	RootCmd.SetArgs(args)*/

	if err := RootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}

func init() {

	src := RootCmd.PersistentFlags().StringP("src", "s", "./", "store source")
	typ := RootCmd.PersistentFlags().StringP("driver", "d", "FS", "store source driver")
	RootCmd.PersistentFlags().Bool("help", false, "show help")
	verbose := RootCmd.PersistentFlags().BoolP("verbose", "v", false, "verbose output")
	debug := RootCmd.PersistentFlags().Bool("debug", false, "debug output")
	RootCmd.PersistentFlags().MarkHidden("debug")

	storeSrc = *client.New(src, typ, verbose, debug)

	infoCmd := store.InfoCmd(storeSrc, name, verbose, debug)

	checkCmd := store.CheckCmd(storeSrc, name, verbose, debug)

	upgradeCmd := store.UpgradeCmd(storeSrc, name, verbose, debug)

	RootCmd.AddCommand(infoCmd)
	RootCmd.AddCommand(checkCmd)
	RootCmd.AddCommand(upgradeCmd)
	RootCmd.CompletionOptions.DisableDefaultCmd = true
	RootCmd.PersistentFlags().SortFlags = false
	RootCmd.Flags().SortFlags = false

	/*for _, c := range entityCmd.Commands() {
		if !c.IsAvailableCommand() || c.IsAdditionalHelpTopicCommand() {
			continue
		}
		subcommands[c.Short] = c
	}*/
}
