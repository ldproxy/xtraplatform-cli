package log

import (
	"fmt"
	"os"

	"github.com/fatih/color"
	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/juju/ansiterm"
	"github.com/spf13/cobra"
)

var blue = color.New(color.FgBlue).SprintFunc()
var grey = color.New(color.FgHiWhite, color.Faint).SprintFunc()
var white = color.New(color.FgHiWhite).SprintFunc()
var green = color.New(color.FgGreen).SprintFunc()
var red = color.New(color.FgHiRed).SprintFunc()
var orange = color.New(color.FgYellow).SprintFunc()

var noColors *bool
var json *bool

// statusCmd represents the status command
func statusCmd(api client.Client, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "status",
		Short: "Show log status",
		Run: func(cmd *cobra.Command, args []string) {
			if *json {
				body, err := api.LogStatusJson()

				if err != nil {
					fmt.Println(err)
				}

				fmt.Println(string(body))
				return
			}

			logs, err := api.LogStatus()

			if err != nil {
				fmt.Println(err)
			}

			if logs != nil {
				if *noColors {
					print(logs)
				} else {
					printColored(logs)
				}
			}
		},
	}

	noColors = cmd.Flags().BoolP("no-colors", "n", false, "disable colored output")
	json = cmd.Flags().BoolP("json", "j", false, "enable JSON output")

	return cmd
}

func printColored(status *client.LogStatus) {

	// initialize tabwriter
	//tw := new(tabwriter.Writer)
	tw := ansiterm.NewTabWriter(os.Stdout, 8, 8, 2, '\t', 0)

	// minwidth, tabwidth, padding, padchar, flags
	tw.Init(os.Stdout, 8, 8, 2, '\t', 0)

	var level string
	switch status.Level {
	case "OFF":
		level = grey(status.Level)
	case "ERROR":
		level = red(status.Level)
	case "WARN":
		level = orange(status.Level)
	case "INFO":
		level = blue(status.Level)
	default:
		level = white(status.Level)
	}

	fmt.Fprint(tw, white("level"), "\t", level, "\n")

	for name, state := range status.Filter {

		var state2 string
		if state {
			state2 = green("ON")
		} else {
			state2 = grey("OFF")
		}

		fmt.Fprint(tw, white(name), "\t", state2, "\n")
	}

	tw.Flush()
}

func print(status *client.LogStatus) {

	// initialize tabwriter
	tw := ansiterm.NewTabWriter(os.Stdout, 8, 8, 2, '\t', 0)

	// minwidth, tabwidth, padding, padchar, flags
	tw.Init(os.Stdout, 8, 8, 2, '\t', 0)

	fmt.Fprint(tw, "level", "\t", status.Level, "\n")

	for name, state := range status.Filter {

		var state2 string
		if state {
			state2 = "ON"
		} else {
			state2 = "OFF"
		}

		fmt.Fprint(tw, name, "\t", state2, "\n")
	}

	tw.Flush()
}
