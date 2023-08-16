package client

import (
	"fmt"
	"os"

	"github.com/AlecAivazis/survey/v2"
	"github.com/fatih/color"
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
func PrintResults(results []Result, err error) {
	fmt.Fprint(os.Stdout, "\n")

	if err != nil {
		fmt.Fprint(os.Stdout, err, "\n")
		return
	}

	for _, r := range results {
		switch r.Status {
		case Error:
			fmt.Fprint(os.Stdout, red(cross), " ", *r.Message, "\n")
		case Warning:
			fmt.Fprint(os.Stdout, yellow(danger), " ", *r.Message, "\n")
		case Success:
			fmt.Fprint(os.Stdout, green(check), " ", *r.Message, "\n")
		case Info:
			fmt.Fprint(os.Stdout, *r.Message, "\n")
		case Confirmation:
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
