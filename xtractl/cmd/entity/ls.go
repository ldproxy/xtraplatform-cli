package entity

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
func lsCmd(api client.Client, types *[]string, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "ls",
		Short: "List entities",
		Run: func(cmd *cobra.Command, args []string) {
			if *json {
				body, err := api.EntitiesJson()

				if err != nil {
					fmt.Println(err)
				}

				fmt.Println(string(body))
				return
			}

			entities, err := api.Entities()

			if err != nil {
				fmt.Println(err)
			}

			if entities != nil {
				if *noColors {
					print(entities, types)
				} else {
					printColored(entities, types)
				}
			}
		},
	}

	noColors = cmd.Flags().BoolP("no-colors", "n", false, "disable colored output")
	json = cmd.Flags().BoolP("json", "j", false, "enable JSON output")

	return cmd
}

func printColored(entities []client.Entity, types *[]string) {

	// initialize tabwriter
	tw := ansiterm.NewTabWriter(os.Stdout, 8, 8, 2, '\t', 0)

	// minwidth, tabwidth, padding, padchar, flags
	tw.Init(os.Stdout, 8, 8, 2, '\t', 0)

	fmt.Fprint(tw, header("ID"), "\t", header("TYPE"), "\t", header("STATUS"), "\n")

	for _, entity := range entities {
		if !isOfType(entity.Typ, types) {
			continue
		}
		var status string
		switch entity.Status {
		case "ACTIVE":
			status = green(entity.Status)
		case "DEFECTIVE":
			status = red(entity.Status)
		case "LOADING", "RELOADING":
			status = yellow(entity.Status)
		default:
			status = grey(entity.Status)
		}
		fmt.Fprint(tw, white(entity.Id), "\t", white(entity.Typ), "\t", status, "\n")
	}

	tw.Flush()
}

func print(entities []client.Entity, types *[]string) {

	// initialize tabwriter
	tw := ansiterm.NewTabWriter(os.Stdout, 8, 8, 2, '\t', 0)

	// minwidth, tabwidth, padding, padchar, flags
	tw.Init(os.Stdout, 8, 8, 2, '\t', 0)

	fmt.Fprint(tw, "ID", "\t", "TYPE", "\t", "STATUS", "\n")

	for _, entity := range entities {
		if !isOfType(entity.Typ, types) {
			continue
		}

		fmt.Fprint(tw, entity.Id, "\t", entity.Typ, "\t", entity.Status, "\n")
	}

	tw.Flush()
}

func isOfType(typ string, types *[]string) bool {
	for _, t := range *types {
		if t == "*" || t == typ {
			return true
		}
	}

	return false
}
