package value

import (
	"fmt"
	"os"

	"github.com/fatih/color"
	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/juju/ansiterm"
	"github.com/spf13/cobra"
)

var header = color.New(color.FgHiBlack, color.Underline).SprintFunc()
var grey = color.New(color.FgHiWhite, color.Faint).SprintFunc()
var white = color.New(color.FgHiWhite).SprintFunc()
var green = color.New(color.FgGreen).SprintFunc()
var red = color.New(color.FgRed).SprintFunc()
var yellow = color.New(color.FgYellow).SprintFunc()

var noColors *bool
var json *bool

// lsCmd represents the ls command
func lsCmd(api client.Client, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:     "ls",
		Short:   "List values",
		Example: name + " value ls\n" + name + " value ls -t codelists",
		Run: func(cmd *cobra.Command, args []string) {
			//TODO: filter json by type
			if *json {
				body, err := api.ValuesJson()

				if err != nil {
					fmt.Println(err)
				}

				fmt.Println(string(body))
				return
			}

			if Values != nil {
				if *noColors {
					print(Values, Types)
				} else {
					printColored(Values, Types)
				}
			}
		},
	}

	cmd.Flags().StringSliceP("types", "t", []string{"*"}, "restrict value types (either \"*\", a single type or a comma separated list)")
	noColors = cmd.Flags().BoolP("no-colors", "n", false, "disable colored output")
	json = cmd.Flags().BoolP("json", "j", false, "enable JSON output")

	return cmd
}

func printColored(entities []client.Value, types []string) {

	// initialize tabwriter
	tw := ansiterm.NewTabWriter(os.Stdout, 8, 8, 2, '\t', 0)

	// minwidth, tabwidth, padding, padchar, flags
	tw.Init(os.Stdout, 8, 8, 2, '\t', 0)

	fmt.Fprint(tw, header("ID"), "\t", header("TYPE"), "\n")

	for _, entity := range entities {
		if !isOfType(entity.Typ, types) {
			continue
		}
		fmt.Fprint(tw, white(entity.Path), "\t", white(entity.Typ), "\n")
	}

	tw.Flush()
}

func print(values []client.Value, types []string) {

	// initialize tabwriter
	tw := ansiterm.NewTabWriter(os.Stdout, 8, 8, 2, '\t', 0)

	// minwidth, tabwidth, padding, padchar, flags
	tw.Init(os.Stdout, 8, 8, 2, '\t', 0)

	fmt.Fprint(tw, "ID", "\t", "TYPE", "\n")

	for _, value := range values {
		if !isOfType(value.Typ, types) {
			continue
		}

		fmt.Fprint(tw, value.Path, "\t", value.Typ, "\n")
	}

	tw.Flush()
}

func isOfType(typ string, types []string) bool {
	for _, t := range types {
		if t == "*" || t == typ {
			return true
		}
	}

	return false
}
