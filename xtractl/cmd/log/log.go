package log

import (
	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/spf13/cobra"
)

// Cmd represents the log command
func Cmd(api client.Client, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "log",
		Short: "Control logging",
	}

	cmd.AddCommand(statusCmd(api, name, debug), levelCmd(api, name, debug), filterCmd(api, name, debug))

	return cmd
}
