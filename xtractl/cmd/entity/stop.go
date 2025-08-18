package entity

import (
	"fmt"
	"strings"

	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/spf13/cobra"
)

var stopIds []string

// Cmd represents the stop command
func stopCmd(api client.Client, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:     "stop ids...",
		Short:   "Stop entity",
		Long:    "Stop entity\n\nStop an entity if running.",
		Example: name + " entity stop id1 -t services",
		Args: func(cmd *cobra.Command, args []string) error {
			if len(args) == 0 {
				return fmt.Errorf("no ids given, at least one entity id is required\n")
			}

			for _, id := range strings.Split(args[0], ",") {
				stopIds = append(stopIds, strings.TrimSpace(id))
			}

			return nil
		},
		PreRunE: func(cmd *cobra.Command, args []string) error {
			if len(Types) == 0 {
				return fmt.Errorf("no types given, at least one entity type is required\n")
			}
			for _, id := range startIds {
				if !isValidId(id, Entities) {
					return fmt.Errorf("unknown entity id: %s\n", id)
				}
			}

			return nil
		},
		Run: func(cmd *cobra.Command, args []string) {
			err := api.Stop(stopIds, Types)

			if err != nil {
				fmt.Println(err)
			}
		},
	}

	return cmd
}
