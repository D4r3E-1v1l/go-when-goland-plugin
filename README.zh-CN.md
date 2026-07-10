# go-when-goland-plugin

[English README](./README.md)

[`go-when`](https://github.com/D4r3E-1v1l/go-when) 的 GoLand 配套插件。

`go-when-goland-plugin` 为 `go-when` matcher chain 提供检查能力，用于发现 Go 类型系统本身无法表达的结构问题和部分语义问题。

`go-when` 库本身不依赖这个插件。
这个插件只是一个可选的开发期辅助工具。

## 功能

### Chain 结构检查

* 检查缺少 terminal call 的 matcher root
* 检查不完整的 matcher branch
* 检查重复的 terminal call
* 检查 terminal call 不在 chain 最后的位置

### Matcher 兼容性检查

* 检查当前 matcher type 不支持的 condition
* 检查 normal / fallible matcher 中不合法的 action
* 检查 normal / fallible matcher 中不合法的 terminal
* 支持 `.WithErr()` root modifier

### Numeric 语义检查

* 检查 numeric condition overlap
* 检查 unreachable numeric condition
* 支持混合 numeric `Case(...)` 和 `Range(...)` 条件

### Enum exhaustive 检查

* 在使用 `.Exhaustive()` 时检查缺失的 enum case
* 将所有缺失的 enum case 聚合成一个 warning

### Quick fixes

* 添加 `.Exhaustive()`
* 添加 `.Else(...)`
* 添加 `.ElseDo(...)`

## Numeric 语义规则

插件使用严格的 numeric condition 语义。

`Case(3)` 会被视为闭合点区间 `[3, 3]`。

Range helper 的解释规则如下：

* `when.Range(a, b)` -> `[a, b)`
* `when.Closed(a, b)` -> `[a, b]`
* `when.Open(a, b)` -> `(a, b)`
* `when.From(a)` -> `[a, +∞)`
* `when.After(a)` -> `(a, +∞)`
* `when.To(b)` -> `(-∞, b]`
* `when.Until(b)` -> `(-∞, b)`

如果 numeric `Case` 和 `Range` 条件发生重叠，插件会报告 `NUMERIC_OVERLAP` 或 `UNREACHABLE_NUMERIC_CONDITION`。

示例：

```go
level := when.MatchAs[string](score).
	Case(100).Then("perfect").
	Range(when.Range(90, 100)).Then("excellent").
	Range(when.Range(60, 90)).Then("passed").
	Else("failed")
```

## Enum exhaustive 规则

Enum exhaustive 检查只会在使用 `.Exhaustive()` 时触发。

示例：

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

如果 `OrderShipped` 和 `OrderCancelled` 没有被覆盖，插件会报告一个 warning，并列出所有缺失的 case。

如果你希望提供 fallback 行为，使用 `.Else(...)`。

如果你认为所有合法 enum case 都应该被覆盖，使用 `.Exhaustive()`。

## 当前限制

* Numeric 语义检查目前只支持 numeric literal
* Numeric 变量、常量和表达式目前会被跳过
* 暂不支持 numeric gap check
* 暂不支持 numeric overlap / unreachable numeric condition 的 quick fix
* Enum exhaustive 检查目前只支持当前文件中的简单 enum 声明
* Enum exhaustive 检查目前主要支持 `type Name int` 和 `type Name string` 风格的 enum
* 暂不支持跨 package enum exhaustive 检查
* 不支持 deep structural pattern matching

## 和 go-when 的关系

这个插件专门服务于 [`go-when`](https://github.com/D4r3E-1v1l/go-when)。

`go-when` 是一个面向 Go 的 typed decision-table helper。
它主要用于扁平、结果导向的决策映射，例如：

* value -> value
* value -> handler
* value -> `(value, error)`
* error -> value 或 handler
* numeric range -> category
* 混合 `Case` / `Range` / `When` / `Pattern` 条件

这个插件的目标是让这些 matcher chain 更完整、更清晰、更容易维护。

## 安装

构建插件 zip：

```bash
./gradlew clean buildPlugin
```

插件 zip 会生成在：

```text
build/distributions/
```

在 GoLand 中安装：

```text
Settings / Preferences
-> Plugins
-> Gear icon
-> Install Plugin from Disk...
```

## License

MIT
