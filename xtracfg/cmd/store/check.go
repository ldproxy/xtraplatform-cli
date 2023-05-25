package store

import (
	"fmt"
	"os"

	"github.com/fatih/color"
	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/spf13/cobra"
)

var grey = color.New(color.FgHiWhite, color.Faint).SprintFunc()
var white = color.New(color.FgHiWhite).SprintFunc()
var green = color.New(color.FgGreen).SprintFunc()
var red = color.New(color.FgRed).SprintFunc()
var yellow = color.New(color.FgYellow).SprintFunc()

var check = fmt.Sprintf("%c", '\u2714')
var cross = fmt.Sprintf("%c", '\u26CC')
var danger = fmt.Sprintf("%c", '!')

// Cmd represents the entity command
func CheckCmd(store client.Store, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "check",
		Short: "Check the store source",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Fprint(os.Stdout, "Checking store source: ", store.Label(), "\n")

			results, err := store.Check()

			fmt.Fprint(os.Stdout, "\n")

			if err != nil {
				fmt.Fprint(os.Stdout, err, "\n")
				return
			}

			for _, r := range results {
				switch r.Status {
				case client.Error:
					fmt.Fprint(os.Stdout, red(cross), " ", *r.Message, "\n")
				case client.Warning:
					fmt.Fprint(os.Stdout, yellow(danger), " ", *r.Message, "\n")
				case client.Success:
					fmt.Fprint(os.Stdout, green(check), " ", *r.Message, "\n")
				}
			}

		},
	}

	return cmd
}
