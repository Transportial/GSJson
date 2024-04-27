# GSJSon
Json `Getter` and `Setter` language. It allows for easy `Json` structure creation or Json structure value extraction. It allows for complex mapping instructions such as filtering, parsing dates and parsing end-values


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

- Support of multiple input and output types. It supports `Strings`, `org.json` and `jackson-json`
- Select `json` values by using a dot-notation
- Select complex `json` element by filters and helpers (in-progress)
- Create `json` structures with values by using a dot-notation and as a key-value pair
- Create complex json structures with updating, filtering and helper-functions (in-progress)

## Examples

For the follow examples, keep the following json in mind: 

```json
{
  "name": "Transportial", 
  "age": 4,
  "developers": [
    {
      "firstName": "Developer 1", 
      "lastName": "Lastname 1",
      "age": 1
    },
    {
      "firstName": "Developer 2",
      "lastName": "Lastname 2",
      "age": 4
    }
  ], 
  "company": {
    "name": "Transportial BV"
  }
}
```

### Selecting
Select the age
```kotlin
GSJson.get(json, "age")
```

Select the company name
```kotlin
GSJson.get(json, "company.name")
```

Select the first developer's first name
```kotlin
GSJson.get(json, "developers.[0].firstName")
```

Select the developer's name who's age is 4
```kotlin
GSJson.get(json, "developers.[age == \"4\"].firstName")
```


### Writing

Write the age
```kotlin
GSJson.set(json, "age", 4)
```

Write the company name
```kotlin
GSJson.set(json, "company.name", "Transportial BV")
```

Write the first developer's first name
```kotlin
GSJson.set(json, "developers.[0].firstName", "Developer 1")
```

Write the developer's name who's age is 4
```kotlin
GSJson.get(json, "developers.[age == \"4\"].firstName", "Developer 2")
```