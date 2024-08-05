package store

import (
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

var validValueTypes = []string{"maplibre-style"}

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
		Short: "Create Styles for the api",
		Args:  cobra.NoArgs,
		Run: func(cmd *cobra.Command, args []string) {
			results, err := store.Handle(map[string]interface{}{}, "create")
			client.PrintResults(results, err)
		},
	}

	valueType = create.PersistentFlags().StringP("valueType", "t", "maplibre-style", "type of the value to create")
	api = create.PersistentFlags().StringP("api", "a", "xtra", "name of the entity")
	fileName = create.PersistentFlags().StringP("name", "n", "", "name of the value file to create")

	create.PersistentFlags().SortFlags = false
	create.Flags().SortFlags = false

	createValue := &cobra.Command{
		Use:   "value [path]",
		Short: "Create value for the api",
		Example: name + " create value -v -r \n" + name + " create value -v -r --valueType maplibre-styles --api strassen --name strassenStyles default",
		Args: func(cmd *cobra.Command, args []string) error {
			if len(args) > 1 {
				return errors.New("only one argument expected")
			}
			return nil
		},
		Run: func(cmd *cobra.Command, args []string) {
			if *debug {
				fmt.Fprint(os.Stdout, "Creating Value for the api: ", store.Label(), "\n")
			}

			if !isValidValueType(*valueType) {
				fmt.Fprintln(os.Stderr, "Invalid valueType:", *valueType)
				os.Exit(1)
			}

			// Call analyze
			resultAnalyze, err := store.Handle(map[string]interface{}{"type": *valueType, "apiId": *api}, "autoValue", "analyze")
			if err != nil {
				fmt.Fprintln(os.Stderr, "Analyze failed:", err)
				os.Exit(1)
			} else {
				fmt.Printf("Message1: %+v\n", resultAnalyze)

				if len(resultAnalyze) > 0 {
					firstResult := resultAnalyze[0]
					detailsMap := firstResult.Details
					fmt.Printf("Message2: %+v\n", detailsMap)

					if len(detailsMap) == 0 {
						fmt.Fprintln(os.Stderr, "Details map is empty")
						os.Exit(1)
					}

					collectionColorsInterface, ok := detailsMap["Collection Colors"]
					if !ok {
						fmt.Fprintln(os.Stderr, "Collection Colors key not found in Details map")
						os.Exit(1)
					}
					fmt.Printf("Before: %+v\n", collectionColorsInterface)
					collectionColorsMap, ok := collectionColorsInterface.(map[string]interface{})
					if !ok {
						fmt.Fprintln(os.Stderr, "Collection Colors is not of type map[string]interface{}")
						os.Exit(1)
					}

					var collectionColorsStr string
					for key, value := range collectionColorsMap {
						strValue, ok := value.(string)
						if !ok {
							fmt.Fprintf(os.Stderr, "Value for key %s is not of type string\n", key)
							os.Exit(1)
						}
						collectionColorsStr += fmt.Sprintf("%s: %s\n", key, strValue)
					}

					fmt.Printf("Message4: %s\n", collectionColorsStr)

					// Zuweisung der Adresse des Strings an collectionColors
					collectionColors = &collectionColorsStr
					fmt.Printf("MessageFinal: %s\n", *collectionColors)
					

				} else {
					fmt.Fprintln(os.Stderr, "resultAnalyze is empty")
				}

				// Wenn analyze erfolgreich war, rufe generate auf
				results, err := store.Handle(map[string]interface{}{
					"type":            *valueType,
					"apiId":           *api,
					"name":            *fileName,
					"collectionColors": *collectionColors, 
				}, "autoValue", "generate")
				if err != nil {
					fmt.Fprintln(os.Stderr, "Generate failed:", err)
					os.Exit(1)
				}

				// Ergebnisse von generate ausgeben
				client.PrintResults(results, err)
			}
		},
	}

	create.AddCommand(createValue)

	return create
}