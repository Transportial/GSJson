# GSJson
JSON `Getter` and `Setter` language inspired by [GJSON](https://github.com/tidwall/gjson). It provides a fast and simple way to get values from JSON documents with powerful path syntax and built-in modifiers.

## Installation
Install using Maven or Gradle. Replace `LATEST_VERSION` with the actual latest version. 

**Maven:**
``` xml
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

- **Multiple Data Type Support**: Works with `String`, `org.json.JSONObject`, `org.json.JSONArray`, and Jackson `JsonNode`
- **Dot Notation Paths**: Select JSON values using intuitive dot notation syntax
- **Wildcard Support**: Use `*` and `?` wildcards for key matching
- **Array Operations**: Access array elements, get length, and extract child paths
- **Advanced Filtering**: Support for `==`, `!=`, `<`, `<=`, `>`, `>=`, `%` (like), `!%` (not like) operators
- **Nested Array Queries**: Complex filtering with nested conditions
- **Built-in Modifiers**: Transform data with modifiers like `@reverse`, `@sum`, `@avg`, `@join`, etc.
- **Path Chaining**: Chain operations using the pipe `|` character
- **JSON Lines Support**: Process newline-delimited JSON with `..` prefix
- **Enhanced Result Type**: Rich result objects with utility methods
- **Reducer Functions**: Aggregate array data with mathematical and string operations

## Path Syntax

A path is a series of keys separated by dots. GSJSON supports the following path features:

- **Basic Access**: `name.first` - Access nested properties
- **Array Index**: `developers.[0]` - Access array elements by index
- **Array Length**: `developers.[#]` - Get array length
- **All Elements**: `developers.[#.firstName]` - Get all firstName values from array
- **Wildcards**: `*.name` or `test?` - Match keys with wildcards
- **Filtering**: `developers.[age > "25"]` - Filter arrays by conditions
- **Escaping**: `fav\.movie` - Escape special characters

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

```kotlin
// Basic property access
GSJson.get(json, "age")                    // Returns: 37
GSJson.get(json, "name.last")              // Returns: "Anderson"
GSJson.get(json, "children")               // Returns: ["Sara","Alex","Jack"]

// Escaped property names
GSJson.get(json, "fav\\.movie")            // Returns: "Deer Hunter"
```

### Array Operations

```kotlin
// Array indexing
GSJson.get(json, "children.[0]")           // Returns: "Sara"
GSJson.get(json, "children.[1]")           // Returns: "Alex"

// Array length
GSJson.get(json, "children.[#]")           // Returns: 3
GSJson.get(json, "friends.[#]")            // Returns: 4

// All child elements
GSJson.get(json, "friends.[#.first]")      // Returns: ["Dale","Roger","Jane","David"]
GSJson.get(json, "friends.[#.age]")        // Returns: [44,68,47,25]
```

### Wildcards

```kotlin
// Wildcard matching
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

### Built-in Modifiers

```kotlin
// Array manipulation
GSJson.get(json, "children|@reverse")      // Returns: ["Jack","Alex","Sara"]
GSJson.get(json, "children|@count")        // Returns: 3

// Object operations  
GSJson.get(json, "name|@keys")             // Returns: ["first","last"]
GSJson.get(json, "name|@values")           // Returns: ["Tom","Anderson"]

// JSON formatting
GSJson.get(json, "name|@pretty")           // Returns: prettified JSON
GSJson.get(json, "name|@ugly")             // Returns: minified JSON
```

### Reducer Operations

```kotlin
// Mathematical operations
GSJson.get(json, "friends.[#.age]|@sum")   // Returns: 184.0
GSJson.get(json, "friends.[#.age]|@avg")   // Returns: 46.0
GSJson.get(json, "friends.[#.age]|@min")   // Returns: 25.0  
GSJson.get(json, "friends.[#.age]|@max")   // Returns: 68.0

// String operations
GSJson.get(json, "friends.[#.first]|@join")        // Returns: "Dale,Roger,Jane,David"
GSJson.get(json, "friends.[#.first]|@join: | ")    // Returns: "Dale | Roger | Jane | David"
GSJson.get(json, "children|@join:-")               // Returns: "Sara-Alex-Jack"
```

### Path Chaining

```kotlin
// Chain multiple operations
GSJson.get(json, "friends.[age > \"40\"].[#.first]|@join")  // Returns: "Dale,Roger,Jane"
GSJson.get(json, "children|@reverse|@join")                 // Returns: "Jack,Alex,Sara"
GSJson.get(json, "friends.[#.age]|@reverse|@max")          // Returns: 68.0
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
GSJson.get(jsonLines, "..[#.name]|@join")       // Returns: "Alice,Bob,Charlie"
GSJson.get(jsonLines, "..[age > \"30\"].[0].name")  // Returns: "Charlie"
```

### Enhanced Result Type

```kotlin
val result = GSJson.getResult(json, "age")
result.int()        // Returns: 37
result.string()     // Returns: "37"
result.exists       // Returns: true
result.type         // Returns: ResultType.NUMBER

// Array results
val arrayResult = GSJson.getResult(json, "friends.[#.first]")
arrayResult.array().forEach { name -> 
    println(name.string())
}
```

### Setting Values

```kotlin
// Basic setting
GSJson.set(json, "age", 38)
GSJson.set(json, "name.middle", "J")
GSJson.set(json, "friends.[0].age", 45)

// Array operations
GSJson.set(json, "hobbies.[0]", "reading")
GSJson.set(json, "scores", listOf(85, 90, 78))
```

### Utility Methods

```kotlin
// Check existence
GSJson.exists(json, "name.first")          // Returns: true
GSJson.exists(json, "name.middle")         // Returns: false

// Process JSON Lines
GSJson.forEachLine(jsonLines) { line ->
    println("Name: ${line.get("name").string()}")
    true // continue iteration
}
```

## Comparison with GJSON

GSJSON maintains the same powerful querying capabilities as GJSON while using slightly different syntax:

| Feature | GJSON | GSJSON |
|---------|-------|--------|
| Array filtering | `friends.#(age>40)` | `friends.[age > "40"]` |
| Array access | `friends.0.name` | `friends.[0].name` |
| All elements | `friends.#.name` | `friends.[#.name]` |
| Modifiers | `children\|@reverse` | `children\|@reverse` |

## Performance

GSJSON is built on Jackson for JSON parsing, providing excellent performance for JSON operations while maintaining the intuitive path syntax inspired by GJSON.