package tiles

import (
	"fmt"

	"strings"

	"github.com/interactive-instruments/xtraplatform-cli/xtractl/client"
	"github.com/spf13/cobra"
)

var id string
var collection *string
var tileMatrixSet *string
var bbox *[]string

// purgeCacheCmd represents the purge-cache command
func purgeCacheCmd(api client.Client, name string, debug *bool) *cobra.Command {
	cmd := &cobra.Command{
		Use:     "purge-cache id",
		Short:   "Purge tile cache",
		Long:    "Purge tile cache\n\nDeletes all tiles from the cache for the given api.\nOnly a subset of tiles can be deleted by using the optional parameters for the collection, the tile matrix set and the WGS84 bounding box.",
		Example: name + " tiles purge-cache api1 -c collection3 --tms WebMercatorQuad --bbox 8,49,9,50\n" + name + " tiles purge-cache api2 --bbox 8,49,9,50\n" + name + " tiles purge-cache api3",
		Args: func(cmd *cobra.Command, args []string) error {
			if len(args) == 0 {
				return fmt.Errorf("no api id given\n")
			}

			id = args[0]

			return nil
		},
		PreRunE: func(cmd *cobra.Command, args []string) error {
			if !isValidId(id, Entities) {
				return fmt.Errorf("unknown api id: %s\n", id)
			}

			if len(*bbox) != 0 && len(*bbox) != 4 {
				return fmt.Errorf("invalid bbox: %s\n", strings.Join(*bbox, ","))
			}

			return nil
		},
		Run: func(cmd *cobra.Command, args []string) {
			err := api.PurgeTileCache(id, *collection, *tileMatrixSet, *bbox)

			if err != nil {
				fmt.Println(err)
			}
		},
	}

	collection = cmd.Flags().StringP("collection", "c", "", "id of collection that should be purged")
	tileMatrixSet = cmd.Flags().StringP("tms", "t", "", "id of tile matrix set that should be purged")
	bbox = cmd.Flags().StringSliceP("bbox", "b", nil, "WGS84 bounding box that should be purged")

	return cmd
}

func isValidId(id string, entities []client.Entity) bool {
	for _, entity := range entities {
		if id == entity.Id && entity.Typ == "services" {
			return true
		}
	}

	return false
}
