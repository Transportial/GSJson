# GSJson
JSON `Getter` and `Setter` language inspired by [GJSON](https://github.com/tidwall/gjson). It provides a fast and simple way to get values from JSON documents with powerful path syntax and built-in modifiers.

Available for both the **JVM (Kotlin/Java)** via Maven Central and **JavaScript/TypeScript** via NPM.

## Installation

### JavaScript / TypeScript (NPM)

```bash
npm install gsjson
```

```js
// ESM
import GSJson from 'gsjson';
import { get, set, exists, getResult, forEachLine } from 'gsjson';

// Usage
const value = GSJson.get(data, 'name.first');
```

### Kotlin / Java (JVM)

Replace `LATEST_VERSION` with the actual latest version.

**Maven:**
```xml
<dependency>
    <groupId>com.transportial</groupId>
    <artifactId>gsjson</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

**Gradle:**
```groovy
dependencies {
    implementation 'com.transportial:gsjson:LATEST_VERSION'
}
```

## Features

- **Multiple Data Type Support**: Works with `String`, `org.json.JSONObject`, `org.json.JSONArray`, and Jackson `JsonNode` (JVM) or plain objects / JSON strings (JS)
- **Dot Notation Paths**: Select JSON values using intuitive dot notation syntax
- **Wildcard Support**: Use `*` and `?` wildcards for key matching
- **Array Operations**: Access array elements, get length, and extract child paths
- **Advanced Filtering**: Support for `==`, `!=`, `<`, `<=`, `>`, `>=`, `%` (like), `!%` (not like) operators
- **Back-references**: Use `<<` to reference parent context nodes in filters
- **Constant Selectors**: Return literal values with `"value"` (double-quoted string in path)
- **Nested Array Queries**: Complex filtering with nested conditions (`[nets.[# == "fb"]]`)
- **Built-in Modifiers**: Transform data with modifiers like `@reverse`, `@sort`, `@sum`, `@avg`, `@join`, etc.
- **Path Chaining**: Chain operations using the pipe `|` character
- **JSON Lines Support**: Process newline-delimited JSON with `..` prefix
- **Enhanced Result Type**: Rich result objects with utility methods (`.int()`, `.string()`, `.array()`, `.forEach()`)
- **Setting Values**: Write values back to JSON with `GSJson.set()`
- **Reducer Functions**: Aggregate array data with mathematical and string operations
- **Dynamic Math Arguments**: Use JSON selectors as modifier arguments (`@multiply:config.factor`)

## Path Syntax

A path is a series of keys separated by dots. GSJson supports the following path features:

| Syntax | Description |
|--------|-------------|
| `name.first` | Basic nested access |
| `fav\.movie` | Escaped dot in key name |
| `"value"` | Constant — returns literal string `value` |
| `developers.[0]` | Array index |
| `developers.[#]` | Array length |
| `developers.[#.firstName]` | All `firstName` values from array |
| `*.name` or `test?` | Wildcard key matching |
| `friends.[age > "25"]` | Filter array by condition |
| `friends.[age == <<.age]` | Filter using back-reference to parent |
| `friends.[nets.[# == "fb"]]` | Nested sub-array filter |
| `children\|@reverse` | Modifier chaining via pipe |

## Examples

For the following examples, consider this JSON:

```json
{
  "name": {"first": "Tom", "last": "Anderson"},
  "age": 37,
  "children": ["Sara", "Alex", "Jack"],
  "fav.movie": "Deer Hunter",
  "friends": [
    {"first": "Dale", "last": "Murphy", "age": 44, "nets": ["ig", "fb", "tw"]},
    {"first": "Roger", "last": "Craig", "age": 68, "nets": ["fb", "tw"]},
    {"first": "Jane", "last": "Murphy", "age": 47, "nets": ["ig", "tw"]},
    {"first": "David", "last": "Smith", "age": 25, "nets": ["fb"]}
  ]
}
```

### Basic Selection

**Kotlin:**
```kotlin
GSJson.get(json, "age")                    // Returns: 37
GSJson.get(json, "name.last")              // Returns: "Anderson"
GSJson.get(json, "children")               // Returns: ["Sara","Alex","Jack"]
GSJson.get(json, "fav\\.movie")            // Returns: "Deer Hunter"
```

**JavaScript:**
```js
GSJson.get(json, 'age')                    // Returns: 37
GSJson.get(json, 'name.last')              // Returns: 'Anderson'
GSJson.get(json, 'children')               // Returns: ['Sara','Alex','Jack']
GSJson.get(json, 'fav\\.movie')            // Returns: 'Deer Hunter'
```

### Array Operations

```kotlin
// Kotlin
GSJson.get(json, "children.[0]")           // Returns: "Sara"
GSJson.get(json, "children.[#]")           // Returns: 3
GSJson.get(json, "friends.[#.first]")      // Returns: ["Dale","Roger","Jane","David"]
GSJson.get(json, "friends.[#.age]")        // Returns: [44,68,47,25]
```

```js
// JavaScript
GSJson.get(json, 'children.[0]')           // Returns: 'Sara'
GSJson.get(json, 'children.[#]')           // Returns: 3
GSJson.get(json, 'friends.[#.first]')      // Returns: ['Dale','Roger','Jane','David']
```

### Wildcards

```kotlin
GSJson.get(json, "name.*")                 // Returns: ["Tom","Anderson"]
GSJson.get(json, "name.fir?t")             // Returns: "Tom"
```

### Filtering

```kotlin
// Basic filtering
GSJson.get(json, "friends.[last == \"Murphy\"].first")     // Returns: "Dale"
GSJson.get(json, "friends.[age > \"45\"].[0].first")       // Returns: "Roger"
GSJson.get(json, "friends.[age <= \"30\"].[0].first")      // Returns: "David"

// Pattern matching
GSJson.get(json, "friends.[first % \"D*\"].last")          // Returns: "Murphy" (Dale)
GSJson.get(json, "friends.[first !% \"D*\"].[0].first")    // Returns: "Roger"

// Nested array filtering
GSJson.get(json, "friends.[nets.[# == \"fb\"]].[#.first]") // Returns: ["Dale","Roger","David"]
```

```js
// JavaScript
GSJson.get(json, 'friends.[last == "Murphy"].first')        // Returns: 'Dale'
GSJson.get(json, 'friends.[age > "45"].[0].first')          // Returns: 'Roger'
GSJson.get(json, 'friends.[nets.[# == "fb"]].[#.first]')    // Returns: ['Dale','Roger','David']
```

### Back-references

Use `<<` to reference a previously visited node in the traversal path. Multiple `<` characters step further back.

```kotlin
// Compare each friend's age to the root-level "age" field
GSJson.get(json, "friends.[age == <<.age].[0].first")   // Returns first friend whose age matches root age
```

```js
GSJson.get(json, 'friends.[age == <<.age].[0].first')
```

### Constant Selectors

A path segment surrounded by double quotes returns the literal string:

```kotlin
GSJson.get(json, "\"hello\"")   // Returns: "hello"
```

```js
GSJson.get(json, '"hello"')     // Returns: 'hello'
```

### Built-in Modifiers

```kotlin
// Array manipulation
GSJson.get(json, "children|@reverse")      // Returns: ["Jack","Alex","Sara"]
GSJson.get(json, "children|@count")        // Returns: 3

// Object operations
GSJson.get(json, "name|@keys")             // Returns: ["first","last"]
GSJson.get(json, "name|@values")           // Returns: ["Tom","Anderson"]
GSJson.get(json, "name|@count")            // Returns: 2

// JSON formatting
GSJson.get(json, "name|@pretty")           // Returns: prettified JSON string
GSJson.get(json, "name|@ugly")             // Returns: minified JSON string
GSJson.get(json, "children|@tostr")        // Returns: JSON string of children
GSJson.get(json, ".|@fromstr")             // Parses a JSON-string field back to object
GSJson.get(json, "age|@this")              // Returns: 37 (identity modifier)

// Array flattening
GSJson.get(nestedJson, "arr|@flatten")     // Flattens nested arrays
```

### Sorting Operations

```kotlin
GSJson.get(json, "children|@sort")               // Returns: ["Alex","Jack","Sara"]
GSJson.get(json, "children|@sort:desc")           // Returns: ["Sara","Jack","Alex"]
GSJson.get(json, "friends|@sortBy:age")           // Sort by age ascending
GSJson.get(json, "friends|@sortBy:age:desc")      // Sort by age descending
GSJson.get(json, "friends|@sortBy:first")         // Sort by first name
GSJson.get(json, "friends|@sortBy:last:desc")     // Sort by last name descending
GSJson.get(json, "friends.[age > \"40\"]|@sortBy:age|[#.first]")  // Filter, sort, extract
GSJson.get(json, "friends.[#.age]|@sort:desc|[0]")                // Highest age
```

### Reducer Operations

```kotlin
GSJson.get(json, "friends.[#.age]|@sum")   // Returns: 184.0
GSJson.get(json, "friends.[#.age]|@avg")   // Returns: 46.0
GSJson.get(json, "friends.[#.age]|@min")   // Returns: 25.0
GSJson.get(json, "friends.[#.age]|@max")   // Returns: 68.0
GSJson.get(json, "friends.[#.first]|@join")        // Returns: "Dale,Roger,Jane,David"
GSJson.get(json, "friends.[#.first]|@join: | ")    // Returns: "Dale | Roger | Jane | David"
GSJson.get(json, "children|@join:-")               // Returns: "Sara-Alex-Jack"
```

### Mathematical Operations

```kotlin
val numbers = "[10, 20, 30, 40, 50]"

GSJson.get(numbers, ".|@multiply:2")    // Returns: [20.0, 40.0, 60.0, 80.0, 100.0]
GSJson.get(numbers, ".|@add:5")         // Returns: [15.0, 25.0, 35.0, 45.0, 55.0]
GSJson.get(numbers, ".|@subtract:10")   // Returns: [0.0, 10.0, 20.0, 30.0, 40.0]
GSJson.get(numbers, ".|@divide:2")      // Returns: [5.0, 10.0, 15.0, 20.0, 25.0]
GSJson.get(numbers, ".|@abs")           // Returns: absolute values
GSJson.get(numbers, ".|@round:1")       // Returns: rounded to 1 decimal place
GSJson.get(numbers, ".|@power:2")       // Returns: squared values
GSJson.get(json, "friends.[#.age]|@add:5|@sum")  // Add 5 to each age, then sum
```

### Dynamic Mathematical Operations

Mathematical modifiers support both static values and JSON selectors as arguments:

```kotlin
val dynamicMathJson = """
{
  "numbers": [10, 20, 30],
  "multiplier": 3,
  "divisor": 2,
  "config": { "factor": 4, "precision": 1 }
}
"""

GSJson.get(dynamicMathJson, "numbers|@multiply:multiplier")      // Returns: [30.0, 60.0, 90.0]
GSJson.get(dynamicMathJson, "numbers|@divide:divisor")           // Returns: [5.0, 10.0, 15.0]
GSJson.get(dynamicMathJson, "numbers|@multiply:config.factor")   // Returns: [40.0, 80.0, 120.0]
GSJson.get(dynamicMathJson, "numbers|@add:addend|@multiply:2")   // Chain dynamic + static
```

**Supported dynamic math modifiers:**
- `@multiply:selector` / `@mul:selector`
- `@divide:selector` / `@div:selector`
- `@add:selector` / `@plus:selector`
- `@subtract:selector` / `@sub:selector`
- `@power:selector` / `@pow:selector`
- `@round:selector`

### Fallback Default Values

```kotlin
GSJson.get(json, "name.middle", "J")              // Returns: "J" (field doesn't exist)
GSJson.get(json, "age", 0)                        // Returns: 37 (field exists)
GSJson.get(json, "friends.[99].name", "Unknown")  // Returns: "Unknown" (index out of bounds)
```

```js
GSJson.get(json, 'name.middle', 'J')              // Returns: 'J'
GSJson.get(json, 'friends.[99].name', 'Unknown')  // Returns: 'Unknown'
```

### Path Chaining

```kotlin
GSJson.get(json, "friends.[age > \"40\"].[#.first]|@join")  // Returns: "Dale,Roger,Jane"
GSJson.get(json, "children|@reverse|@join")                  // Returns: "Jack,Alex,Sara"
GSJson.get(json, "friends.[#.age]|@reverse|@max")           // Returns: 68.0
```

### JSON Lines

Process newline-delimited JSON:

```kotlin
val jsonLines = """
{"name": "Alice", "age": 30}
{"name": "Bob", "age": 25}
{"name": "Charlie", "age": 35}
"""

GSJson.get(jsonLines, "..#")                    // Returns: 3
GSJson.get(jsonLines, "..[1].name")             // Returns: "Bob"
GSJson.get(jsonLines, "..#.name|@join")         // Returns: "Alice,Bob,Charlie"
GSJson.get(jsonLines, "..[age > \"30\"].[0].name")  // Returns: "Charlie"
```

```js
GSJson.get(jsonLines, '..#')                    // Returns: 3
GSJson.get(jsonLines, '..[1].name')             // Returns: 'Bob'
GSJson.get(jsonLines, '..#.name|@join')         // Returns: 'Alice,Bob,Charlie'
```

### Enhanced Result Type

```kotlin
// Kotlin
val result = GSJson.getResult(json, "age")
result.int()        // Returns: 37
result.string()     // Returns: "37"
result.exists       // Returns: true
result.type         // Returns: ResultType.NUMBER

val arrayResult = GSJson.getResult(json, "friends.[#.first]")
arrayResult.array().forEach { name ->
    println(name.string())
}
```

```js
// JavaScript
const result = GSJson.getResult(json, 'age');
result.int()        // Returns: 37
result.string()     // Returns: '37'
result.exists       // Returns: true
result.type         // Returns: 'NUMBER'

const arrayResult = GSJson.getResult(json, 'friends.[#.first]');
arrayResult.forEach((r) => console.log(r.string()));
```

**ResultType values:** `STRING`, `NUMBER`, `BOOLEAN`, `NULL`, `OBJECT`, `ARRAY`

### Setting Values

```kotlin
// Kotlin
GSJson.set(json, "age", 38)
GSJson.set(json, "name.middle", "J")
GSJson.set(json, "friends.[0].age", 45)
GSJson.set(json, "hobbies.[0]", "reading")
GSJson.set(json, "scores", listOf(85, 90, 78))
```

```js
// JavaScript — always returns a JSON string
GSJson.set('', 'age', 38)                        // '{"age":38}'
GSJson.set('', 'name.first', 'Tom')              // '{"name":{"first":"Tom"}}'
GSJson.set(existing, 'friends.[0].age', 45)      // mutates and returns JSON string
```

### Utility Methods

```kotlin
// Kotlin
GSJson.exists(json, "name.first")          // Returns: true
GSJson.exists(json, "name.middle")         // Returns: false

GSJson.forEachLine(jsonLines) { line ->
    println("Name: ${line.value}")
    true // return true to continue, false to stop
}
```

```js
// JavaScript
GSJson.exists(json, 'name.first')          // Returns: true
GSJson.exists(json, 'name.middle')         // Returns: false

GSJson.forEachLine(jsonLines, (line) => {
    console.log('Name:', line.value.name);
    return true; // return false to stop iteration
});
```

## Comparison with GJSON

GSJson maintains the same powerful querying capabilities as GJSON while using slightly different syntax:

| Feature | GJSON | GSJson |
|---------|-------|--------|
| Array filtering | `friends.#(age>40)` | `friends.[age > "40"]` |
| Array access | `friends.0.name` | `friends.[0].name` |
| All elements | `friends.#.name` | `friends.[#.name]` |
| Modifiers | `children\|@reverse` | `children\|@reverse` |

## Performance

GSJson is built on Jackson for JSON parsing on the JVM, providing excellent performance for JSON operations. The JavaScript port works directly with native JS objects without any dependencies.
