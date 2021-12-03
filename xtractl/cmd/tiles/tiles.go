package tiles

import (
	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/spf13/cobra"
)

var Entities []client.Entity

// Cmd represents the log command
func Cmd(api client.Client, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "tiles",
		Short: "Manage tiles",
		PersistentPreRunE: func(cmd *cobra.Command, args []string) error {
			entities, err := api.Entities()

			if err != nil {
				return err
			}

			Entities = entities

			return nil
		},
	}

	cmd.AddCommand(purgeCacheCmd(api, name, debug))

	return cmd
}
