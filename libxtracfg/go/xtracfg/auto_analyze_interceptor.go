package xtracfg

import (
	"database/sql"
	"fmt"
	"sort"
	"strings"

	schema "github.com/PZahnen/sql-schema-crawler"
	_ "github.com/jackc/pgx/v5/stdlib"
	_ "modernc.org/sqlite"
)

func interceptAutoAnalyze(parameters map[string]interface{}) (*Response, bool, error) {
	featureProviderType := strings.ToUpper(getString(parameters, "featureProviderType"))

	if featureProviderType == "" {
		return errorResultResponse("featureProviderType is required for auto analyze"), true, nil
	}

	db, err := openAnalyzeDB(parameters, featureProviderType)
	if err != nil {
		return errorResultResponse(err.Error()), true, nil
	}
	defer db.Close()

	analyzeResult, skippedObjects, err := analyzeWithPermissionFallback(db)
	if err != nil {
		return errorResultResponse(err.Error()), true, nil
	}

	types := toTypesBySchema(analyzeResult)
	if len(types) == 0 {
		details := map[string]interface{}{"types": map[string][]string{}}
		if len(skippedObjects) > 0 {
			details["skippedObjects"] = skippedObjects
		}
		return successResultResponse("No types found", details), true, nil
	}

	message := "All good"
	details := map[string]interface{}{"types": types}
	if len(skippedObjects) > 0 {
		message = "All good (partial, skipped unauthorized objects)"
		details["skippedObjects"] = skippedObjects
	}

	return successResultResponse(message, details), true, nil
}

func analyzeWithPermissionFallback(db *sql.DB) (schema.AnalyzeResult, []string, error) {
	tables, tErr := schema.TableNames(db)
	views, vErr := schema.ViewNames(db)

	skipped := make([]string, 0)
	if tErr != nil {
		skipped = append(skipped, fmt.Sprintf("tables (%v)", tErr))
		tables = nil
	}
	if vErr != nil {
		skipped = append(skipped, fmt.Sprintf("views (%v)", vErr))
		views = nil
	}
	if tErr != nil && vErr != nil {
		return schema.AnalyzeResult{}, skipped, fmt.Errorf("table/view listing failed: tables=%v; views=%v", tErr, vErr)
	}

	out := schema.AnalyzeResult{}
	schemasSet := map[string]struct{}{}

	for _, n := range tables {
		out.Tables = append(out.Tables, schema.TableMeta{Schema: n[0], Name: n[1], IsView: false})
		schemasSet[n[0]] = struct{}{}
	}

	for _, n := range views {
		out.Views = append(out.Views, schema.TableMeta{Schema: n[0], Name: n[1], IsView: true})
		schemasSet[n[0]] = struct{}{}
	}

	for s := range schemasSet {
		out.Schemas = append(out.Schemas, schema.SchemaMeta{Name: s})
	}

	return out, skipped, nil
}

func openAnalyzeDB(parameters map[string]interface{}, featureProviderType string) (*sql.DB, error) {
	if dsn := getString(parameters, "dsn"); dsn != "" {
		driver := strings.ToLower(getString(parameters, "dbDriver"))
		if driver == "" {
			driver = "pgx"
		}
		return sql.Open(driver, dsn)
	}

	switch featureProviderType {
	case "PGIS":
		host := getString(parameters, "host")
		database := getString(parameters, "database")
		user := getString(parameters, "user")
		password := getString(parameters, "password")
		port := getString(parameters, "port")
		if port == "" {
			port = "5432"
		}

		if host == "" || database == "" {
			return nil, fmt.Errorf("host and database are required for featureProviderType=PGIS")
		}

		conn := fmt.Sprintf("host=%s port=%s dbname=%s", host, port, database)
		if user != "" {
			conn += fmt.Sprintf(" user=%s", user)
		}
		if password != "" {
			conn += fmt.Sprintf(" password=%s", password)
		}

		if sslmode := getString(parameters, "sslmode"); sslmode != "" {
			conn += fmt.Sprintf(" sslmode=%s", sslmode)
		} else {
			conn += " sslmode=disable"
		}

		return sql.Open("pgx", conn)

	case "GPKG":
		database := getString(parameters, "database")
		if database == "" {
			return nil, fmt.Errorf("database is required for featureProviderType=GPKG")
		}
		return sql.Open("sqlite", database)

	default:
		return nil, fmt.Errorf("featureProviderType '%s' is currently not supported by go auto analyze", featureProviderType)
	}
}

func toTypesBySchema(result schema.AnalyzeResult) map[string][]string {
	typesSet := map[string]map[string]struct{}{}

	add := func(schemaName, tableName string) {
		if schemaName == "" {
			schemaName = ""
		}
		if _, ok := typesSet[schemaName]; !ok {
			typesSet[schemaName] = map[string]struct{}{}
		}
		typesSet[schemaName][tableName] = struct{}{}
	}

	for _, t := range result.Tables {
		add(t.Schema, t.Name)
	}
	for _, v := range result.Views {
		add(v.Schema, v.Name)
	}

	types := make(map[string][]string, len(typesSet))
	for schemaName, namesSet := range typesSet {
		names := make([]string, 0, len(namesSet))
		for n := range namesSet {
			names = append(names, n)
		}
		sort.Strings(names)
		types[schemaName] = names
	}

	return types
}

func successResultResponse(message string, details map[string]interface{}) *Response {
	if details == nil {
		details = map[string]interface{}{}
	}

	results := []Result{{Status: Success, Message: &message}}
	return &Response{Results: &results, Details: details}
}

func errorResultResponse(message string) *Response {
	details := map[string]interface{}{}
	results := []Result{{Status: Error, Message: &message}}
	return &Response{Results: &results, Details: details}
}

func getString(parameters map[string]interface{}, key string) string {
	v, ok := parameters[key]
	if !ok || v == nil {
		return ""
	}
	if s, ok := v.(string); ok {
		return strings.TrimSpace(s)
	}
	return strings.TrimSpace(fmt.Sprint(v))
}
