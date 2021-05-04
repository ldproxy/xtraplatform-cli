package entity

import (
	"fmt"
	"strings"

	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/spf13/cobra"
)

var ids []string

// Cmd represents the reload command
func reloadCmd(api client.Client, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:     "reload ids...",
		Short:   "Reload entity configuration",
		Long:    "Reload entity configuration\n\nRereads all configuration files that are relevant for the given entities.\nIf effective changes to an entity configuration are detected, the entity is reloaded.\nBeware that erroneous configuration files will stop the affected entities.",
		Example: name + " entity reload \"*\" -t services\n" + name + " entity reload id1\n" + name + " entity reload id1,id2",
		Args: func(cmd *cobra.Command, args []string) error {
			if len(args) == 0 {
				return fmt.Errorf("no ids given, at least one entity id or \"*\" is required\n")
			}

			for _, id := range strings.Split(args[0], ",") {
				ids = append(ids, strings.TrimSpace(id))
			}

			return nil
		},
		PreRunE: func(cmd *cobra.Command, args []string) error {
			for _, id := range ids {
				if !isValidId(id, Entities) {
					return fmt.Errorf("unknown entity id: %s\n", id)
				}
			}

			return nil
		},
		Run: func(cmd *cobra.Command, args []string) {
			err := api.Reload(ids, Types)

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
