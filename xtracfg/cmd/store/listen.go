package store

import (
	"fmt"

	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/spf13/cobra"
)

// InfoCmd represents the check command
func ListenCmd(store client.Store, name string, version string, verbose *bool, debug *bool) *cobra.Command {
	listen := &cobra.Command{
		Use:    "listen [port]",
		Short:  "Listen for command on websocket",
		Hidden: true,
		Args:   cobra.MaximumNArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			port := ":8080"
			if len(args) > 0 {
				port = ":" + args[0]
			}
			fmt.Printf("%s (%s) listening on port %s\n", name, version, port)
			client.OpenWebsocket(store, port)
		},
	}

	listen.PersistentFlags().SortFlags = false
	listen.Flags().SortFlags = false

	return listen
}
