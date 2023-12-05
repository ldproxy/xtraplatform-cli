package store

import (
	"errors"
	"fmt"
	"os"
	"strconv"

	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/spf13/cobra"
)

var backup *bool
var force *bool
var noConfirm *bool
var keepRedundant *bool

// Cmd represents the entity command
func UpgradeCmd(store client.Store, name string, verbose *bool, debug *bool) *cobra.Command {
	upgrade := &cobra.Command{
		Use:   "upgrade",
		Short: "Upgrade the store source",
		Long: `Upgrade the store source
Executes all subcommands in order, see the subcommand help for details.
No changes are made without confirmation (unless --yes is set).`,
		Args: cobra.NoArgs,
		Run: func(cmd *cobra.Command, args []string) {
			if *debug {
				fmt.Fprint(os.Stdout, "Upgrading store source: ", store.Label(), "\n")
			}

			fmt.Fprint(os.Stdout, "\n", "Upgrading cfg.yml", "\n")

			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*keepRedundant), "subcommand": "cfg"}, "pre_upgrade")

			if !*noConfirm {
				client.PrintResults(results, err)
			}

			if client.HasStatus(results, client.Confirmation) {
				results, err = store.Handle(map[string]string{"backup": strconv.FormatBool(*backup), "ignoreRedundant": strconv.FormatBool(*keepRedundant), "noConfirm": strconv.FormatBool(*noConfirm), "subcommand": "cfg"}, "upgrade")

				client.PrintResults(results, err)
			}

			fmt.Fprint(os.Stdout, "\n", "Upgrading entities", "\n")

			results, err = store.Handle(map[string]string{"force": strconv.FormatBool(*force), "ignoreRedundant": strconv.FormatBool(*keepRedundant), "subcommand": "entities"}, "pre_upgrade")

			if !*noConfirm {
				client.PrintResults(results, err)
			}

			if client.HasStatus(results, client.Confirmation) {
				results, err = store.Handle(map[string]string{"backup": strconv.FormatBool(*backup), "force": strconv.FormatBool(*force), "ignoreRedundant": strconv.FormatBool(*keepRedundant), "noConfirm": strconv.FormatBool(*noConfirm), "subcommand": "entities"}, "upgrade")

				client.PrintResults(results, err)
			}

			fmt.Fprint(os.Stdout, "\n", "Upgrading layout", "\n")

			results, err = store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*keepRedundant), "subcommand": "layout"}, "pre_upgrade")

			if !*noConfirm {
				client.PrintResults(results, err)
			}

			if client.HasStatus(results, client.Confirmation) {
				results, err = store.Handle(map[string]string{"backup": strconv.FormatBool(*backup), "ignoreRedundant": strconv.FormatBool(*keepRedundant), "noConfirm": strconv.FormatBool(*noConfirm), "subcommand": "layout"}, "upgrade")

				client.PrintResults(results, err)
			}

			fmt.Fprint(os.Stdout, "\n")
		},
	}

	backup = upgrade.PersistentFlags().BoolP("backup", "b", false, "backup files before upgrading")
	force = upgrade.PersistentFlags().BoolP("force", "f", false, "upgrade files even if there are no detected issues; useful to harmonize yaml details like quoting and property order")
	keepRedundant = upgrade.PersistentFlags().BoolP("ignore-redundant", "r", false, "keep reduntant settings instead of deleting them")
	noConfirm = upgrade.PersistentFlags().BoolP("yes", "y", false, "do not ask for confirmation")

	upgrade.PersistentFlags().SortFlags = false
	upgrade.Flags().SortFlags = false

	upgradeCfg := &cobra.Command{
		Use:   "cfg",
		Short: "Upgrade cfg.yml in the store source",
		Long: `Upgrades cfg.yml with deprecated settings.
No changes are made without confirmation (unless --yes is set).`,
		Args: cobra.NoArgs,
		Run: func(cmd *cobra.Command, args []string) {
			if *debug {
				fmt.Fprint(os.Stdout, "Upgrading cfg.yml in the store source: ", store.Label(), "\n")
			}

			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*keepRedundant), "subcommand": "cfg"}, "pre_upgrade")

			if !*noConfirm {
				client.PrintResults(results, err)
			}

			if client.HasStatus(results, client.Confirmation) {
				results, err = store.Handle(map[string]string{"backup": strconv.FormatBool(*backup), "ignoreRedundant": strconv.FormatBool(*keepRedundant), "noConfirm": strconv.FormatBool(*noConfirm), "subcommand": "cfg"}, "upgrade")

				client.PrintResults(results, err)
			}

			fmt.Fprint(os.Stdout, "\n")
		},
	}

	upgradeEntities := &cobra.Command{
		Use:   "entities [path]",
		Short: "Upgrade entities in the store source",
		Long: `Upgrades entity configurations with deprecated, unknown and redundant settings.
To upgrade only a single entity, pass the path to the file relative to the source as argument.
No changes are made without confirmation (unless --yes is set).`,
		Example: name + " upgrade entities -v -r \n" + name + " upgrade entities -v -r store/entities/services/api.yml",
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
				fmt.Fprint(os.Stdout, "Upgrading entities for store source: ", store.Label(), "\n")
			}
			path := ""
			if len(args) > 0 {
				path = args[0]
			}

			results, err := store.Handle(map[string]string{"force": strconv.FormatBool(*force), "ignoreRedundant": strconv.FormatBool(*keepRedundant), "subcommand": "entities", "path": path}, "pre_upgrade")

			if !*noConfirm {
				client.PrintResults(results, err)
			}

			if client.HasStatus(results, client.Confirmation) {
				results, err = store.Handle(map[string]string{"backup": strconv.FormatBool(*backup), "force": strconv.FormatBool(*force), "ignoreRedundant": strconv.FormatBool(*keepRedundant), "noConfirm": strconv.FormatBool(*noConfirm), "subcommand": "entities", "path": path}, "upgrade")

				client.PrintResults(results, err)
			}

			fmt.Fprint(os.Stdout, "\n")
		},
	}

	upgradeLayout := &cobra.Command{
		Use:   "layout",
		Short: "Upgrade layout of the store source",
		Long: `Upgrades a deprecated directory layout.
No changes are made without confirmation (unless --yes is set).`,
		Args: cobra.NoArgs,
		Run: func(cmd *cobra.Command, args []string) {
			if *debug {
				fmt.Fprint(os.Stdout, "Upgrading layout of the store source: ", store.Label(), "\n")
			}

			results, err := store.Handle(map[string]string{"ignoreRedundant": strconv.FormatBool(*keepRedundant), "subcommand": "layout"}, "pre_upgrade")

			if !*noConfirm {
				client.PrintResults(results, err)
			}

			if client.HasStatus(results, client.Confirmation) {
				results, err = store.Handle(map[string]string{"backup": strconv.FormatBool(*backup), "ignoreRedundant": strconv.FormatBool(*keepRedundant), "noConfirm": strconv.FormatBool(*noConfirm), "subcommand": "layout"}, "upgrade")

				client.PrintResults(results, err)
			}

			fmt.Fprint(os.Stdout, "\n")
		},
	}

	upgrade.AddCommand(upgradeCfg)
	upgrade.AddCommand(upgradeEntities)
	upgrade.AddCommand(upgradeLayout)

	return upgrade
}
