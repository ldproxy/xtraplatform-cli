package util

import (
	"fmt"
	"os"

	"github.com/AlecAivazis/survey/v2"
	"github.com/fatih/color"
	"github.com/interactive-instruments/xtraplatform-cli/libxtracfg/go/xtracfg"
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
func PrintResults(results []xtracfg.Result, err error) {
	fmt.Fprint(os.Stdout, "\n")

	if err != nil {
		fmt.Fprint(os.Stdout, err, "\n")
		return
	}

	for _, r := range results {
		switch r.Status {
		case xtracfg.Error:
			fmt.Fprint(os.Stdout, red(cross), " ", *r.Message, "\n")
		case xtracfg.Warning:
			fmt.Fprint(os.Stdout, yellow(danger), " ", *r.Message, "\n")
		case xtracfg.Success:
			fmt.Fprint(os.Stdout, green(check), " ", *r.Message, "\n")
		case xtracfg.Info:
			fmt.Fprint(os.Stdout, *r.Message, "\n")
		case xtracfg.Confirmation:
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
