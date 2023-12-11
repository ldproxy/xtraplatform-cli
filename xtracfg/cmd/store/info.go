package store

import (
	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/spf13/cobra"
)

// InfoCmd represents the check command
func InfoCmd(store client.Store, name string, verbose *bool, debug *bool) *cobra.Command {
	info := &cobra.Command{
		Use:   "info",
		Short: "Print info about the store source",
		Args:  cobra.NoArgs,
		Run: func(cmd *cobra.Command, args []string) {
			results, err := store.Handle(map[string]interface{}{}, "info")

			client.PrintResults(results, err)
		},
	}

	info.PersistentFlags().SortFlags = false
	info.Flags().SortFlags = false

	return info
}
