package entity

import (
	"fmt"

	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/spf13/cobra"
)

var ids []string

//var types []string

// Cmd represents the reload command
func reloadCmd(api client.Client, types *[]string, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:     "reload ids...",
		Example: name + " entity reload * -t services\n" + name + " entity reload id1\n" + name + " entity reload id1,id2",
		Short:   "Reload entity configuration",
		Args: func(cmd *cobra.Command, args []string) error {
			if len(args) == 0 {
				return fmt.Errorf("no ids given, at least one entity id or * is required")
			}

			ids = args

			return nil
		},
		PreRunE: func(cmd *cobra.Command, args []string) error {
			entities, err := api.Entities()

			if err != nil {
				return err
			}

			for _, id := range ids {
				if !isValidId(id, entities) {
					return fmt.Errorf("unknown entity id: %s", id)
				}
			}

			return nil
		},
		Run: func(cmd *cobra.Command, args []string) {
			err := api.Reload(ids, *types)

			if err != nil {
				fmt.Println(err)
			}
		},
	}

	return cmd
}

func isValidId(id string, entities []client.Entity) bool {
	if id == "*" {
		return true
	}

	for _, entity := range entities {
		if id == entity.Id {
			return true
		}
	}

	return false
}
