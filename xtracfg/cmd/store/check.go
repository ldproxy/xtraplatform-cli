package store

import (
	"errors"
	"fmt"
	"os"
	"strconv"
	"strings"

	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/cmd/util"
	"github.com/spf13/cobra"
)

var ignoreRedundant *bool

// CheckCmd represents the check command
func CheckCmd(store client.Store, name string, verbose *bool, debug *bool) *cobra.Command {
	check := &cobra.Command{
		Use:   "check",
		Short: "Check the store source",
		Run: func(cmd *cobra.Command, args []string) {
			if *verbose {
				fmt.Fprint(os.Stdout, "Checking store source: ", store.Label(), "\n")
			}
			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*ignoreRedundant)}, "check")

			util.PrintResults(results, err)

			if err != nil {
				fmt.Fprint(os.Stdout, "\n", "To fix all detected issues run: ", name, " upgrade ", strings.Join(args, " "), "\n")
			}
		},
	}

	ignoreRedundant = check.PersistentFlags().BoolP("ignore-redundant", "r", false, "ignore redundant settings")

	check.PersistentFlags().SortFlags = false
	check.Flags().SortFlags = false

	checkEntities := &cobra.Command{
		Use:   "entities",
		Short: "Check entities in the store source",
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
			if *verbose {
				fmt.Fprint(os.Stdout, "Checking store source: ", store.Label(), "\n")
			}
			path := ""
			if len(args) > 0 {
				path = args[0]
			}

			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*ignoreRedundant), "onlyEntities": "true", "path": path}, "check")

			util.PrintResults(results, err)
		},
	}

	checkLayout := &cobra.Command{
		Use:   "layout",
		Short: "Check layout of the store source",
		Run: func(cmd *cobra.Command, args []string) {
			if *verbose {
				fmt.Fprint(os.Stdout, "Checking layout of store source: ", store.Label(), "\n")
			}

			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*ignoreRedundant), "onlyLayout": "true"}, "check")

			util.PrintResults(results, err)
		},
	}

	check.AddCommand(checkEntities)
	check.AddCommand(checkLayout)

	return check
}
