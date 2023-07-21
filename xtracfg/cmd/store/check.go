package store

import (
	"fmt"
	"os"
	"strconv"

	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/cmd/util"
	"github.com/spf13/cobra"
)

var ignoreRedundant *bool

// CheckCmd represents the check command
func CheckCmd(store client.Store, name string, verbose *bool, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "check",
		Short: "Check the store source",
		Run: func(cmd *cobra.Command, args []string) {
			if *verbose {
				fmt.Fprint(os.Stdout, "Checking store source: ", store.Label(), "\n")
			}
			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*ignoreRedundant)}, "check")

			util.PrintResults(results, err)
		},
	}

	ignoreRedundant = cmd.PersistentFlags().BoolP("ignore-redundant", "r", false, "ignore reduntant settings")

	cmd.PersistentFlags().SortFlags = false
	cmd.Flags().SortFlags = false

	return cmd
}
