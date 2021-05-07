package entity

import (
	"fmt"

	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/spf13/cobra"
)

var Types []string
var Entities []client.Entity

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

			Entities = entities
			Types = parseTypes(cmd)

			for _, typ := range Types {
				if !isValidType(typ, entities) {
					return fmt.Errorf("unknown entity type '%s'\n", typ)
				}
			}

			return nil
		},
	}

	subcmds := []*cobra.Command{lsCmd(api, name, debug), reloadCmd(api, name, debug)}

	for _, subcmd := range subcmds {
		addTypesFlag(subcmd)
	}

	addTypesFlag(cmd)
	cmd.AddCommand(subcmds...)

	return cmd
}

func addTypesFlag(cmd *cobra.Command) {
	cmd.Flags().StringSliceP("types", "t", []string{"*"}, "restrict entity types (either \"*\", a single type or a comma separated list)")
}

func parseTypes(cmd *cobra.Command) []string {
	types, _ := cmd.Flags().GetStringSlice("types")

	return types
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
