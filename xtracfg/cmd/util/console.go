package util

import (
	"fmt"
	"os"

	"github.com/AlecAivazis/survey/v2"
	"github.com/fatih/color"
	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
)

var grey = color.New(color.FgHiWhite, color.Faint).SprintFunc()
var white = color.New(color.FgHiWhite).SprintFunc()
var green = color.New(color.FgGreen).SprintFunc()
var red = color.New(color.FgRed).SprintFunc()
var yellow = color.New(color.FgYellow).SprintFunc()

var check = fmt.Sprintf("%c", '\u2714')
var cross = fmt.Sprintf("%c", '\u26CC')
var danger = fmt.Sprintf("%c", '!')

// PrintResults prints the results to the console
func PrintResults(results []client.Result, err error) {
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
		case client.Info:
			fmt.Fprint(os.Stdout, *r.Message, "\n")
		case client.Confirmation:
			fmt.Fprint(os.Stdout, "\n")
			do := false
			prompt := &survey.Confirm{
				Message: *r.Message,
				Default: true,
			}
			survey.AskOne(prompt, &do)
			if !do {
				os.Exit(0)
			}
		}
	}
}
