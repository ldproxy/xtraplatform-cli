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

// Cmd represents the entity command
func UpgradeCmd(store client.Store, name string, verbose *bool, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "upgrade",
		Short: "Upgrade the store source",
		Run: func(cmd *cobra.Command, args []string) {
			if *verbose {
				fmt.Fprint(os.Stdout, "Upgrading store source: ", store.Label(), "\n")
			}
			results, err := store.Handle(map[string]string{}, "pre_upgrade")

			util.PrintResults(results, err)

			if client.HasStatus(results, client.Confirmation) {
				results, err = store.Handle(map[string]string{"backup": strconv.FormatBool(*backup)}, "upgrade")

				util.PrintResults(results, err)
			}
		},
	}

	cmd.PersistentFlags().SortFlags = false
	backup = cmd.PersistentFlags().BoolP("backup", "b", false, "backup files before upgrading")

	return cmd
}
