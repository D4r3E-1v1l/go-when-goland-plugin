# go-when-goland-plugin

GoLand plugin for [`go-when`](https://github.com/D4r3E-1v1l/go-when).

This plugin checks `go-when` matcher chains and reports structural and semantic issues that Go's type system cannot express.

## Features

- Detects matcher roots without terminal calls
- Detects incomplete matcher branches
- Detects invalid conditions for matcher type
- Detects invalid actions and terminals for normal / fallible matchers
- Supports `.WithErr()` root modifier
- Detects duplicate terminals
- Detects terminal calls that are not last
- Detects numeric condition overlap
- Detects unreachable numeric conditions

## Numeric semantic rules

The plugin uses strict numeric condition semantics.

`Case(3)` is treated as the closed point interval `[3, 3]`.

Range helpers are interpreted as:

- `when.Range(a, b)` -> `[a, b)`
- `when.Closed(a, b)` -> `[a, b]`
- `when.From(a)` -> `[a, +∞)`
- `when.Until(b)` -> `(-∞, b)`

If numeric `Case` and `Range` conditions overlap, the plugin reports `NUMERIC_OVERLAP` or `UNREACHABLE_NUMERIC_CONDITION`.

## Current limitations

- Numeric semantic checks only support numeric literals
- Variables and expressions are skipped
- No gap check
- No enum exhaustive check yet
- No quickfix for numeric semantic issues yet

## License

MIT