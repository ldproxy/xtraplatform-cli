package store

import (
	"fmt"
	"os"
	"strconv"

	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/cmd/util"
	"github.com/spf13/cobra"
)

var backup *bool
var noConfirm *bool
var keepRedundant *bool

// Cmd represents the entity command
func UpgradeCmd(store client.Store, name string, verbose *bool, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "upgrade",
		Short: "Upgrade the store source",
		Run: func(cmd *cobra.Command, args []string) {
			if *verbose {
				fmt.Fprint(os.Stdout, "Upgrading store source: ", store.Label(), "\n")
			}
			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*keepRedundant)}, "pre_upgrade")

			if !*noConfirm {
				util.PrintResults(results, err)
			}

			if client.HasStatus(results, client.Confirmation) {
				results, err = store.Handle(map[string]string{"backup": strconv.FormatBool(*backup), "ignoreRedundant": strconv.FormatBool(*keepRedundant), "noConfirm": strconv.FormatBool(*noConfirm)}, "upgrade")

				util.PrintResults(results, err)
			}
		},
	}

	backup = cmd.PersistentFlags().BoolP("backup", "b", false, "backup files before upgrading")
	keepRedundant = cmd.PersistentFlags().BoolP("ignore-redundant", "r", false, "keep reduntant settings instead of deleting them")
	noConfirm = cmd.PersistentFlags().BoolP("yes", "y", false, "do not ask for confirmation")

	cmd.PersistentFlags().SortFlags = false
	cmd.Flags().SortFlags = false

	return cmd
}
