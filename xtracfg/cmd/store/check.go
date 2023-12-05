package store

import (
	"errors"
	"fmt"
	"os"
	"strconv"
	"strings"

	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/spf13/cobra"
)

var ignoreRedundant *bool

// CheckCmd represents the check command
func CheckCmd(store client.Store, name string, verbose *bool, debug *bool) *cobra.Command {
	check := &cobra.Command{
		Use:   "check",
		Short: "Check the store source",
		Long: `Check the store source
Executes all subcommands in order, see the subcommand help for details.`,
		Args: cobra.NoArgs,
		Run: func(cmd *cobra.Command, args []string) {
			if *debug {
				fmt.Fprint(os.Stdout, "Checking store source: ", store.Label(), "\n")
			}
			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*ignoreRedundant)}, "check")

			client.PrintResults(results, err)

			printFix(results, err, name)
		},
	}

	ignoreRedundant = check.PersistentFlags().BoolP("ignore-redundant", "r", false, "ignore redundant settings")

	check.PersistentFlags().SortFlags = false
	check.Flags().SortFlags = false

	checkCfg := &cobra.Command{
		Use:   "cfg",
		Short: "Check cfg.yml in the store source",
		Long:  `Checks cfg.yml for deprecated settings.`,
		Args:  cobra.NoArgs,
		Run: func(cmd *cobra.Command, args []string) {
			if *debug {
				fmt.Fprint(os.Stdout, "Checking cfg.yml in the store source: ", store.Label(), "\n")
			}

			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*ignoreRedundant), "subcommand": "cfg"}, "check")

			client.PrintResults(results, err)

			printFix(results, err, name)
		},
	}

	checkEntities := &cobra.Command{
		Use:   "entities [path]",
		Short: "Check entities in the store source",
		Long: `Checks entity configurations for deprecated, unknown and redundant settings.
To check only a single entity, pass the path to the file relative to the source as argument.`,
		Example: name + " check entities -v -r \n" + name + " check entities -v -r store/entities/services/api.yml",
		Args: func(cmd *cobra.Command, args []string) error {
			if len(args) > 1 {
				return errors.New("only one argument expected")
			}
			//TODO
			isValidPath := true
			if isValidPath {
				return nil
			}
			return fmt.Errorf("invalid entity path specified: %s", args[0])
		},
		Run: func(cmd *cobra.Command, args []string) {
			if *debug {
				fmt.Fprint(os.Stdout, "Checking store source: ", store.Label(), "\n")
			}
			path := ""
			if len(args) > 0 {
				path = args[0]
			}

			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*ignoreRedundant), "subcommand": "entities", "path": path}, "check")

			client.PrintResults(results, err)

			printFix(results, err, name)
		},
	}

	checkLayout := &cobra.Command{
		Use:   "layout",
		Short: "Check layout of the store source",
		Long:  `Checks for a deprecated directory layout.`,
		Args:  cobra.NoArgs,
		Run: func(cmd *cobra.Command, args []string) {
			if *debug {
				fmt.Fprint(os.Stdout, "Checking layout of store source: ", store.Label(), "\n")
			}

			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*ignoreRedundant), "subcommand": "layout"}, "check")

			client.PrintResults(results, err)

			printFix(results, err, name)
		},
	}

	check.AddCommand(checkCfg)
	check.AddCommand(checkEntities)
	check.AddCommand(checkLayout)

	return check
}

func printFix(results []client.Result, err error, name string) {
	if err == nil && client.HasStatus(results, client.Warning) {
		fmt.Fprint(os.Stdout, "\n", "Run '", name, " ", strings.Replace(strings.Join(os.Args[1:], " "), "check", "upgrade", 1), "' to fix all detected issues.", "\n")
	} else {
		fmt.Fprint(os.Stdout, "\n")
	}
}
