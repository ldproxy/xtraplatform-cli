package store

import (
	"errors"
	"fmt"
	"os"
	"strconv"

	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/cmd/util"
	"github.com/spf13/cobra"
)

var backup *bool
var noConfirm *bool
var keepRedundant *bool

// Cmd represents the entity command
func UpgradeCmd(store client.Store, name string, verbose *bool, debug *bool) *cobra.Command {
	upgrade := &cobra.Command{
		Use:   "upgrade",
		Short: "Upgrade the store source",
		Run: func(cmd *cobra.Command, args []string) {
			if *verbose {
				fmt.Fprint(os.Stdout, "Upgrading store source: ", store.Label(), "\n")
			}
			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*keepRedundant)}, "pre_upgrade")

			if !*noConfirm {
				util.PrintResults(results, err)
			}

			if client.HasStatus(results, client.Confirmation) {
				results, err = store.Handle(map[string]string{"backup": strconv.FormatBool(*backup), "ignoreRedundant": strconv.FormatBool(*keepRedundant), "noConfirm": strconv.FormatBool(*noConfirm)}, "upgrade")

				util.PrintResults(results, err)
			}
		},
	}

	backup = upgrade.PersistentFlags().BoolP("backup", "b", false, "backup files before upgrading")
	keepRedundant = upgrade.PersistentFlags().BoolP("ignore-redundant", "r", false, "keep reduntant settings instead of deleting them")
	noConfirm = upgrade.PersistentFlags().BoolP("yes", "y", false, "do not ask for confirmation")

	upgrade.PersistentFlags().SortFlags = false
	upgrade.Flags().SortFlags = false

	upgradeEntities := &cobra.Command{
		Use:   "entities",
		Short: "Upgrade entities in the store source",
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
				fmt.Fprint(os.Stdout, "Upgrading entities for store source: ", store.Label(), "\n")
			}
			path := ""
			if len(args) > 0 {
				path = args[0]
			}

			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*keepRedundant), "onlyEntities": "true", "path": path}, "pre_upgrade")

			if !*noConfirm {
				util.PrintResults(results, err)
			}

			if client.HasStatus(results, client.Confirmation) {
				results, err = store.Handle(map[string]string{"backup": strconv.FormatBool(*backup), "ignoreRedundant": strconv.FormatBool(*keepRedundant), "noConfirm": strconv.FormatBool(*noConfirm), "onlyEntities": "true", "path": path}, "upgrade")

				util.PrintResults(results, err)
			}
		},
	}

	upgradeLayout := &cobra.Command{
		Use:   "layout",
		Short: "Upgrade layout of the store source",
		Run: func(cmd *cobra.Command, args []string) {
			if *verbose {
				fmt.Fprint(os.Stdout, "Upgrading layout of the store source: ", store.Label(), "\n")
			}

			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*keepRedundant), "onlyLayout": "true"}, "pre_upgrade")

			if !*noConfirm {
				util.PrintResults(results, err)
			}

			if client.HasStatus(results, client.Confirmation) {
				results, err = store.Handle(map[string]string{"backup": strconv.FormatBool(*backup), "ignoreRedundant": strconv.FormatBool(*keepRedundant), "noConfirm": strconv.FormatBool(*noConfirm), "onlyLayout": "true"}, "upgrade")

				util.PrintResults(results, err)
			}
		},
	}

	upgrade.AddCommand(upgradeEntities)
	upgrade.AddCommand(upgradeLayout)

	return upgrade
}
