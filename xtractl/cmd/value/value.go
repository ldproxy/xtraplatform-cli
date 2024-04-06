package value

import (
	"fmt"

	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/spf13/cobra"
)

var Types []string
var ValueTypes []string
var Values []client.Value

// Cmd represents the entity command
func Cmd(api client.Client, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "value",
		Short: "Control values",
		PersistentPreRunE: func(cmd *cobra.Command, args []string) error {
			values, types, err := api.Values()

			if err != nil {
				return err
			}

			Values = values
			ValueTypes = types
			Types = parseTypes(cmd)

			for _, typ := range Types {
				if !isValidType(typ, types) {
					return fmt.Errorf("unknown value type '%s'\n", typ)
				}
			}

			return nil
		},
	}

	subcmds := []*cobra.Command{lsCmd(api, name, debug), reloadCmd(api, name, debug)}

	/*for _, subcmd := range subcmds {
		addTypesFlag(subcmd)
	}

	addTypesFlag(cmd)*/
	cmd.AddCommand(subcmds...)

	return cmd
}

/*func addTypesFlag(cmd *cobra.Command) {
	cmd.Flags().StringSliceP("types", "t", []string{"*"}, "restrict value types (either \"*\", a single type or a comma separated list)")
}*/

func parseTypes(cmd *cobra.Command) []string {
	types, _ := cmd.Flags().GetStringSlice("types")

	return types
}

func isValidType(typ string, types []string) bool {
	if typ == "*" {
		return true
	}

	for _, typ2 := range types {
		if typ == typ2 {
			return true
		}
	}

	return false
}
