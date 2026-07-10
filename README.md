# go-when-goland-plugin

[中文文档](./README.zh-CN.md)

GoLand companion plugin for [`go-when`](https://github.com/D4r3E-1v1l/go-when).

`go-when-goland-plugin` adds inspections for `go-when` matcher chains and reports structural and semantic issues that Go's type system cannot express.

The `go-when` library works without this plugin.
This plugin is an optional development-time helper.

## Features

### Chain structure inspections

* Detects matcher roots without terminal calls
* Detects incomplete matcher branches
* Detects duplicate terminal calls
* Detects terminal calls that are not last

### Matcher compatibility inspections

* Detects invalid conditions for matcher type
* Detects invalid actions for normal / fallible matchers
* Detects invalid terminals for normal / fallible matchers
* Supports `.WithErr()` root modifier

### Numeric semantic inspections

* Detects numeric condition overlap
* Detects unreachable numeric conditions
* Supports mixed numeric `Case(...)` and `Range(...)` conditions

### Enum exhaustive inspection

* Detects missing enum cases when `.Exhaustive()` is used
* Reports one warning with all missing enum cases

### Quick fixes

* Add `.Exhaustive()`
* Add `.Else(...)`
* Add `.ElseDo(...)`

## Numeric semantic rules

The plugin uses strict numeric condition semantics.

`Case(3)` is treated as the closed point interval `[3, 3]`.

Range helpers are interpreted as:

* `when.Range(a, b)` -> `[a, b)`
* `when.Closed(a, b)` -> `[a, b]`
* `when.Open(a, b)` -> `(a, b)`
* `when.From(a)` -> `[a, +∞)`
* `when.After(a)` -> `(a, +∞)`
* `when.To(b)` -> `(-∞, b]`
* `when.Until(b)` -> `(-∞, b)`

If numeric `Case` and `Range` conditions overlap, the plugin reports `NUMERIC_OVERLAP` or `UNREACHABLE_NUMERIC_CONDITION`.

Example:

```go
level := when.MatchAs[string](score).
	Case(100).Then("perfect").
	Range(when.Range(90, 100)).Then("excellent").
	Range(when.Range(60, 90)).Then("passed").
	Else("failed")
```

## Enum exhaustive rules

Enum exhaustive checking is only applied when `.Exhaustive()` is used.

Example:

```go
type OrderState int

const (
	OrderCreated OrderState = iota
	OrderPaid
	OrderPacked
	OrderShipped
	OrderCancelled
)

action := when.MatchAs[Action](state).
	Case(OrderCreated).Then(requestPayment).
	Case(OrderPaid).Then(packOrder).
	Case(OrderPacked).Then(shipOrder).
	Exhaustive()
```

If `OrderShipped` and `OrderCancelled` are not covered, the plugin reports one warning listing the missing cases.

Use `.Else(...)` when fallback behavior is expected.

Use `.Exhaustive()` when all valid enum cases are expected to be covered.

## Current limitations

* Numeric semantic checks currently support numeric literals only
* Numeric variables, constants, and expressions are skipped
* No numeric gap check
* No quick fix for numeric overlap / unreachable numeric conditions
* Enum exhaustive checks currently support simple enum declarations in the current file
* Enum exhaustive checks currently focus on `type Name int` and `type Name string` style enums
* Cross-package enum exhaustive checking is not supported yet
* Deep structural pattern matching is not supported

## Relationship with go-when

This plugin is designed specifically for [`go-when`](https://github.com/D4r3E-1v1l/go-when).

`go-when` is a typed decision-table helper for Go.
It focuses on flat, result-oriented decision mappings such as:

* value -> value
* value -> handler
* value -> `(value, error)`
* error -> value or handler
* numeric range -> category
* mixed `Case` / `Range` / `When` / `Pattern` conditions

This plugin helps keep those matcher chains structurally complete and easier to maintain.

## Installation

Build the plugin zip:

```bash
./gradlew clean buildPlugin
```

The plugin zip will be generated under:

```text
build/distributions/
```

Install it in GoLand:

```text
Settings / Preferences
-> Plugins
-> Gear icon
-> Install Plugin from Disk...
```

## License

MIT
