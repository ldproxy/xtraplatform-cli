## xtractl



### Synopsis

xtractl controls xtraplatform applications like [ldproxy](https://github.com/interactive-instruments/ldproxy) and XtraServer Web API.

It provides control over certain parts of the running application 
that would otherwise require a restart.

### Options

```
      --help          show help
  -h, --host string   host to connect to (default "localhost")
  -p, --port int      port to connect to (default 7081)
  -v, --verbose       verbose output
```

### SEE ALSO

* [xtractl entity](#xtractl-entity)	 - Control entities
* [xtractl log](#xtractl-log)	 - Control logging
* [xtractl tiles](#xtractl-tiles)	 - Manage tiles

## xtractl entity

Control entities

### Options

```
  -t, --types strings   restrict entity types (either "*", a single type or a comma separated list) (default ['*'])
```

### Options inherited from parent commands

```
      --help          show help
  -h, --host string   host to connect to (default "localhost")
  -p, --port int      port to connect to (default 7081)
  -v, --verbose       verbose output
```

### SEE ALSO

* [xtractl](#xtractl)
* [xtractl entity ls](#xtractl-entity-ls)	 - List entities
* [xtractl entity reload](#xtractl-entity-reload)	 - Reload entity configuration

## xtractl entity ls

List entities

```
xtractl entity ls [flags]
```

### Examples

```
xtractl entity ls
xtractl entity ls -t services
```

### Options

```
  -j, --json            enable JSON output
  -n, --no-colors       disable colored output
  -t, --types strings   restrict entity types (either "*", a single type or a comma separated list) (default ['*'])
```

### Options inherited from parent commands

```
      --help          show help
  -h, --host string   host to connect to (default "localhost")
  -p, --port int      port to connect to (default 7081)
  -v, --verbose       verbose output
```

### SEE ALSO

* [xtractl entity](#xtractl-entity)	 - Control entities

## xtractl entity reload

Reload entity configuration

### Synopsis

Reload entity configuration

Rereads all configuration files that are relevant for the given entities.
If effective changes to an entity configuration are detected, the entity is reloaded.
Beware that erroneous configuration files will stop the affected entities.

```
xtractl entity reload ids... [flags]
```

### Examples

```
xtractl entity reload "*" -t services
xtractl entity reload id1
xtractl entity reload id1,id2
```

### Options

```
  -t, --types strings   restrict entity types (either "*", a single type or a comma separated list) (default ['*'])
```

### Options inherited from parent commands

```
      --help          show help
  -h, --host string   host to connect to (default "localhost")
  -p, --port int      port to connect to (default 7081)
  -v, --verbose       verbose output
```

### SEE ALSO

* [xtractl entity](#xtractl-entity)	 - Control entities

## xtractl log

Control logging

### Options inherited from parent commands

```
      --help          show help
  -h, --host string   host to connect to (default "localhost")
  -p, --port int      port to connect to (default 7081)
  -v, --verbose       verbose output
```

### SEE ALSO

* [xtractl](#xtractl)
* [xtractl log filter](#xtractl-log-filter)	 - Switch the log filters
* [xtractl log level](#xtractl-log-level)	 - Change the log level
* [xtractl log status](#xtractl-log-status)	 - Show log status

## xtractl log filter

Switch the log filters

```
xtractl log filter filters... [flags]
```

### Examples

```
xtractl log filter sqlQueries,sqlResults
xtractl log filter "*" --disable

```

### Options

```
  -d, --disable   disable filters, the default is to enable listed filters
```

### Options inherited from parent commands

```
      --help          show help
  -h, --host string   host to connect to (default "localhost")
  -p, --port int      port to connect to (default 7081)
  -v, --verbose       verbose output
```

### SEE ALSO

* [xtractl log](#xtractl-log)	 - Control logging

## xtractl log level

Change the log level

```
xtractl log level newLevel [flags]
```

### Examples

```
xtractl log level DEBUG

```

### Options inherited from parent commands

```
      --help          show help
  -h, --host string   host to connect to (default "localhost")
  -p, --port int      port to connect to (default 7081)
  -v, --verbose       verbose output
```

### SEE ALSO

* [xtractl log](#xtractl-log)	 - Control logging

## xtractl log status

Show log status

```
xtractl log status [flags]
```

### Options

```
  -j, --json        enable JSON output
  -n, --no-colors   disable colored output
```

### Options inherited from parent commands

```
      --help          show help
  -h, --host string   host to connect to (default "localhost")
  -p, --port int      port to connect to (default 7081)
  -v, --verbose       verbose output
```

### SEE ALSO

* [xtractl log](#xtractl-log)	 - Control logging

## xtractl tiles

Manage tiles

### Options inherited from parent commands

```
      --help          show help
  -h, --host string   host to connect to (default "localhost")
  -p, --port int      port to connect to (default 7081)
  -v, --verbose       verbose output
```

### SEE ALSO

* [xtractl](#xtractl)
* [xtractl tiles purge-cache](#xtractl-tiles-purge-cache)	 - Purge tile cache

## xtractl tiles purge-cache

Purge tile cache

### Synopsis

Purge tile cache

Deletes all tiles from the cache for the given api.
Only a subset of tiles can be deleted by using the optional parameters for the collection, the tile matrix set and the WGS84 bounding box.

```
xtractl tiles purge-cache id [flags]
```

### Examples

```
xtractl tiles purge-cache api1 -c collection3 --tms WebMercatorQuad --bbox 8,49,9,50
xtractl tiles purge-cache api2 --bbox 8,49,9,50
xtractl tiles purge-cache api3
```

### Options

```
  -b, --bbox strings        WGS84 bounding box that should be purged
  -c, --collection string   id of collection that should be purged
  -t, --tms string          id of tile matrix set that should be purged
```

### Options inherited from parent commands

```
      --help          show help
  -h, --host string   host to connect to (default "localhost")
  -p, --port int      port to connect to (default 7081)
  -v, --verbose       verbose output
```

### SEE ALSO

* [xtractl tiles](#xtractl-tiles)	 - Manage tiles

