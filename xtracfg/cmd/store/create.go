package store

import (
	"encoding/json"
	"errors"
	"fmt"
	"os"

	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/spf13/cobra"
)

var valueType *string
var api *string
var fileName *string
var collectionColors *string

var validValueTypes = []string{"maplibre-styles"}

func isValidValueType(value string) bool {
	for _, v := range validValueTypes {
		if v == value {
			return true
		}
	}
	return false
}

func CreateCmd(store client.Store, name string, verbose *bool, debug *bool) *cobra.Command {
	create := &cobra.Command{
		Use:   "create",
		Short: "Create values for an API",
		Args:  cobra.NoArgs,
		Run: func(cmd *cobra.Command, args []string) {
			results, err := store.Handle(map[string]interface{}{}, "create")
			client.PrintResults(results, err)
		},
	}

	create.PersistentFlags().SortFlags = false
	create.Flags().SortFlags = false

	createValue := &cobra.Command{
		Use:     "value [path]",
		Short:   "Create value for an API",
		Example: name + " create value -v -r \n" + name + " create value -v -r --valueType maplibre-styles --api strassen --name strassenStyles --src /cfg default",
		Args: func(cmd *cobra.Command, args []string) error {
			if len(args) > 1 {
				return errors.New("only one argument expected")
			}
			return nil
		},
		Run: func(cmd *cobra.Command, args []string) {
			if *debug {
				fmt.Fprint(os.Stdout, "Creating value for the api: ", store.Label(), "\n")
			}

			if !isValidValueType(*valueType) {
				fmt.Fprintln(os.Stderr, "Invalid valueType:", *valueType)
				os.Exit(1)
			}

			// Call analyze
			resultAnalyze, err := store.Handle(map[string]interface{}{"type": *valueType, "apiId": *api, "name": *fileName}, "autoValue", "analyze")
			if err != nil {
				client.PrintResults(resultAnalyze, err)
				os.Exit(1)
			} else {

				if len(resultAnalyze) > 0 {
					firstResult := resultAnalyze[0]
					detailsMap := firstResult.Details

					if len(detailsMap) == 0 {
						fmt.Fprintln(os.Stderr, "Details map is empty")
						os.Exit(1)
					}

					collectionColorsInterface, ok := detailsMap["Collection Colors"]
					if !ok {
						fmt.Fprintln(os.Stderr, "Collection Colors key not found in Details map")
						os.Exit(1)
					}
					collectionColorsMap, ok := collectionColorsInterface.(map[string]interface{})
					if !ok {
						fmt.Fprintln(os.Stderr, "Collection Colors is not of type map[string]interface{}")
						os.Exit(1)
					}

					collectionColorsJSON, _ := json.Marshal(collectionColorsMap)
					collectionColorsStr := string(collectionColorsJSON)

					collectionColors = &collectionColorsStr
				} else {
					fmt.Fprintln(os.Stderr, "resultAnalyze is empty")
					os.Exit(1)
				}

				results, err := store.Handle(map[string]interface{}{
					"type":             *valueType,
					"apiId":            *api,
					"name":             *fileName,
					"collectionColors": *collectionColors,
				}, "autoValue", "generate")

				if err != nil {
					fmt.Fprintln(os.Stderr, "Generate failed:", err)
					os.Exit(1)
				}

				client.PrintResults(results, err)
			}
		},
	}

	valueType = createValue.PersistentFlags().StringP("valueType", "t", "maplibre-styles", "type of the value to create")
	api = createValue.PersistentFlags().StringP("api", "a", "", "id of the api to create the value for")
	fileName = createValue.PersistentFlags().StringP("name", "n", "", "name of the value file to create")

	createValue.PersistentFlags().SortFlags = false
	createValue.Flags().SortFlags = false

	create.AddCommand(createValue)

	return create
}
