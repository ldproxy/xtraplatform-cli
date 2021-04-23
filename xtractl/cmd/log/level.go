package log

import (
	"fmt"

	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/spf13/cobra"
)

var level string

// Cmd represents the level command
func levelCmd(api client.Client, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:     "level newLevel",
		Example: name + " log level DEBUG\n",
		Short:   "Change the log level",
		Args: func(cmd *cobra.Command, args []string) error {
			if len(args) == 0 {
				return fmt.Errorf("no level given")
			}

			level = args[0]

			return nil
		},
		Run: func(cmd *cobra.Command, args []string) {
			err := api.LogLevel(level)

			if err != nil {
				fmt.Println(err)
			}
		},
	}

	return cmd
}
