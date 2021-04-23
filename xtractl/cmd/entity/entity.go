package entity

import (
	"fmt"

	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/spf13/cobra"
)

var types *[]string

// Cmd represents the entity command
func Cmd(api client.Client, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "entity",
		Short: "Control entities",
		PersistentPreRunE: func(cmd *cobra.Command, args []string) error {
			entities, err := api.Entities()

			if err != nil {
				return err
			}

			for _, typ := range *types {
				if !isValidType(typ, entities) {
					return fmt.Errorf("unknown entity type '%s'\n", typ)
				}
			}

			return nil
		},
	}

	types = cmd.PersistentFlags().StringSliceP("types", "t", []string{"*"}, "restrict entity types (either \"*\", a single type or a comma separated list)")

	cmd.AddCommand(lsCmd(api, types, name, debug), reloadCmd(api, types, name, debug))

	return cmd
}

func isValidType(typ string, entities []client.Entity) bool {
	if typ == "*" {
		return true
	}

	for _, entity := range entities {
		if typ == entity.Typ {
			return true
		}
	}

	return false
}
