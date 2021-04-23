package log

import (
	"fmt"

	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/spf13/cobra"
)

var filters []string
var disable *bool

// Cmd represents the level command
func filterCmd(api client.Client, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:     "filter filters...",
		Example: name + " log filter sqlQueries,sqlResults\n" + name + " log filter \"*\" --disable\n",
		Short:   "Switch the log filters",
		Args: func(cmd *cobra.Command, args []string) error {
			if len(args) == 0 {
				return fmt.Errorf("no filters given, at least one filter or \"*\" is required\n")
			}

			filters = args

			return nil
		},
		Run: func(cmd *cobra.Command, args []string) {
			err := api.LogFilter(filters, *disable)

			if err != nil {
				fmt.Println(err)
			}
		},
	}

	disable = cmd.Flags().BoolP("disable", "d", false, "disable filters, the default is to enable listed filters")

	return cmd
}
