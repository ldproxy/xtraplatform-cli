package entity

import (
	"fmt"
	"strings"

	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/spf13/cobra"
)

var startIds []string

// Cmd represents the start command
func startCmd(api client.Client, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:     "start ids...",
		Short:   "Start entity",
		Long:    "Start entity\n\nStart an entity if stopped. This will also reload the configuration files, see 'reload'.",
		Example: name + " entity start id1 -t services",
		Args: func(cmd *cobra.Command, args []string) error {
			if len(args) == 0 {
				return fmt.Errorf("no ids given, at least one entity id is required\n")
			}

			for _, id := range strings.Split(args[0], ",") {
				startIds = append(startIds, strings.TrimSpace(id))
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
			err := api.Start(startIds, Types)

			if err != nil {
				fmt.Println(err)
			}
		},
	}

	return cmd
}
