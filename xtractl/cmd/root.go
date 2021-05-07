package cmd

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/interactive-instruments/xtraplatform-cli/xtractl/cmd/entity"
	"github.com/interactive-instruments/xtraplatform-cli/xtractl/cmd/log"

	//	"github.com/AlecAivazis/survey/v2"
	"github.com/spf13/cobra"
)

const version = "1.0.2"

var name string = filepath.Base(os.Args[0])
var api client.Client

// RootCmd represents the base command when called without any subcommands
var RootCmd = &cobra.Command{
	Use:     name,
	Version: version,
	Long: name + ` controls xtraplatform applications like ldproxy and XtraServer Web API.

It provides control over certain parts of the running application 
that would otherwise require a restart.`,
	/*Run: func(cmd *cobra.Command, args []string) {
		interactive()
	},*/
	PersistentPreRunE: func(cmd *cobra.Command, args []string) error {

		return api.Login()
	},
	DisableAutoGenTag: true,
}

var subcommands = map[string]*cobra.Command{}

// Execute adds all child commands to the root command and sets flags appropriately.
// This is called by main.main(). It only needs to happen once to the RootCmd.
func Execute() {
	if err := RootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}

func init() {

	RootCmd.PersistentFlags().Bool("help", false, "show help")
	host := RootCmd.PersistentFlags().StringP("host", "h", "localhost", "host to connect to")
	port := RootCmd.PersistentFlags().IntP("port", "p", 7081, "port to connect to")
	password := "" // RootCmd.PersistentFlags().StringP("password", "w", "", "password for connection")
	debug := RootCmd.PersistentFlags().BoolP("verbose", "v", false, "verbose output")

	api = *client.New(host, port, &password, debug)

	entityCmd := entity.Cmd(api, name, debug)
	logCmd := log.Cmd(api, name, debug)

	RootCmd.AddCommand(entityCmd, logCmd)

	for _, c := range entityCmd.Commands() {
		if !c.IsAvailableCommand() || c.IsAdditionalHelpTopicCommand() {
			continue
		}
		subcommands[c.Short] = c
	}
}

/*func interactive() {
	commands := []string{}
	for c := range subcommands {
		commands = append(commands, c)
	}

	questions := []*survey.Question{
		{
			Name: "command",
			Prompt: &survey.Select{
				Message: "What do you want to do?",
				Options: commands,
			},
			Validate: survey.Required,
		},
	}

	answers := struct {
		Command string
	}{}

	err := survey.Ask(questions, &answers)

	if err != nil {
		fmt.Println(err.Error())
		return
	}

	fmt.Println("running ", answers.Command)
}*/
