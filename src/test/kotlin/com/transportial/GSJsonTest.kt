package com.transportial

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test


internal object GSJsonTest {

    private val selectJson = """
        {
          "age":37,
          "children": ["Sara","Alex","Jack"],
          "fav.movie": "Deer Hunter",
          "friends": [
            {"age": 44, "first": "Dale", "last": "Murphy"},
            {"age": 68, "first": "Roger", "last": "Craig"},
            {"age": 47, "first": "Jane", "last": "Murphy"},
            {"age": 37, "first": "Sjenkie", "last": "Name"}
          ],
          "name": {"first": "Tom", "last": "Anderson"}
        }
    """.trimIndent()


    @Test
    fun testObjectSelectors() {
        val age = GSJson.get(selectJson, "age")
        assertEquals(37, age, "Age should be 37")
        assertNotEquals("37", age, "Age should not be a string of \"37\"")

        val firstName = GSJson.get(selectJson, "name.first")
        assertEquals("Tom", firstName, "First name should be \"Tom\"")

        val favMovie = GSJson.get(selectJson, "fav\\.movie")
        assertEquals("Deer Hunter", favMovie, "fav.movie should be \"Deer Hunter\"")
    }

    @Test
    fun testArraySelectors() {
        val children = GSJson.get(selectJson, "children")
        assertEquals("""["Sara","Alex","Jack"]""", children, "The children array as a String")

        val childOne = GSJson.get(selectJson, "children.[0]")
        assertEquals("Sara", childOne, "Child one should be \"Sara\"")

        val childTwo = GSJson.get(selectJson, "children.[1]")
        assertEquals("Alex", childTwo, "Child two should be \"Alex\"")

        val friendsOne = GSJson.get(selectJson, "friends.[0].first")
        assertEquals("Dale", friendsOne, "Friend one, first name, should be \"Dale\"")

        val friendAge47 = GSJson.get(selectJson, "friends.[age == \"47\"].[0].first")
        assertEquals("Jane", friendAge47, "Friend of age 47, first name, should be \"Jane\"")

        val friendAtDynamicAge = GSJson.get(selectJson, "friends.[age == <<.age].[0].first")
        assertEquals("Sjenkie", friendAtDynamicAge, "Friend the dynamic age, 37, first name, should be \"Sjenkie\"")

        val friendAgeAtDynamicAge = GSJson.get(selectJson, "friends.[age == <<.age].[0].age")
        assertEquals(37, friendAgeAtDynamicAge, "Friend the dynamic age, 37, age, should be 37")
        assertNotEquals("37", friendAgeAtDynamicAge, "Friend the dynamic age, 37, age, should be 37")

    }

    @Test
    fun testSpecialSelectors() {
        val constant = GSJson.get(selectJson, "\"value\"")
        assertEquals("value", constant, "Constant should be \"value\"")
    }

    @Test
    fun settingObjectJsonValues() {
        val age = GSJson.set("", "age", 37)
        assertEquals("""{"age":37}""".trimMargin(), age, "Age should be 37")
        assertNotEquals("""{"age":"37"}""", age, "Age should not be a string of \"37\"")

        val firstName = GSJson.set("", "name.first", "Tom")
        assertEquals("""{"name":{"first":"Tom"}}""", firstName, "First name should be \"Tom\"")

        val lastName = GSJson.set(firstName, "name.first", " Lastname")
        assertEquals("""{"name":{"first":"Tom Lastname"}}""", lastName, "First name should be \"Tom Lastname\"")

        val favMovie = GSJson.set("", "fav\\.movie", "Deer Hunter")
        assertEquals("""{"fav.movie":"Deer Hunter"}""", favMovie, "fav.movie should be \"Deer Hunter\"")
    }


    @Test
    fun settingArrayJsonValues() {
        val children = GSJson.set("", "children", listOf("Sara", "Alex", "Jack"))
        assertEquals("""{"children":["Sara","Alex","Jack"]}""", children, "The children array as a String")

        val childOne = GSJson.set("", "children.[0]", "Sara")
        assertEquals("""{"children":["Sara"]}""", childOne, "Child one should be \"Sara\"")

        val childTwo = GSJson.set(childOne, "children.[1]", "Alex")
        assertEquals("""{"children":["Sara","Alex"]}""", childTwo, "Child two should be \"Alex\"")

        val friendsOne = GSJson.set("", "friends.[0].first", "Dale")
        assertEquals("""{"friends":[{"first":"Dale"}]}""", friendsOne, "Friend one, first name, should be \"Dale\"")
    }
    
    @Test
    fun testWildcardSelectors() {
        val testJson = """
        {
            "test1": "value1",
            "test2": "value2", 
            "other": "value3"
        }
        """.trimIndent()
        
        val allTestKeys = GSJson.get(testJson, "test*")
        assertNotEquals(null, allTestKeys, "Wildcard should match multiple keys")
        
        val singleCharMatch = GSJson.get(testJson, "othe?")
        assertEquals("value3", singleCharMatch, "Single char wildcard should match 'other'")
    }
    
    @Test
    fun testArrayLengthAndChildPaths() {
        val arrayLength = GSJson.get(selectJson, "children.[#]")
        assertEquals(3, arrayLength, "Array length should be 3")
        
        val allFirstNames = GSJson.get(selectJson, "friends.[#.first]")
        assertNotEquals(null, allFirstNames, "Should get all first names")
    }
    
    @Test
    fun testAdditionalComparisonOperators() {
        val friendsOlderThan45 = GSJson.get(selectJson, "friends.[age > \"45\"].[0].first")
        assertNotEquals(null, friendsOlderThan45, "Should find friends older than 45")
        
        val friendsYoungerOrEqual44 = GSJson.get(selectJson, "friends.[age <= \"44\"].[0].first")
        assertEquals("Dale", friendsYoungerOrEqual44, "Should find Dale who is 44 or younger")
    }
    
    @Test
    fun testPatternMatching() {
        val matchingNames = GSJson.get(selectJson, "friends.[first % \"D*\"].[0].first")
        assertEquals("Dale", matchingNames, "Should match names starting with D")
        
        val notMatchingNames = GSJson.get(selectJson, "friends.[first !% \"D*\"].[0].first")
        assertNotEquals("Dale", notMatchingNames, "Should not match Dale")
    }
    
    @Test
    fun testBuiltInModifiers() {
        val reversedChildren = GSJson.get(selectJson, "children|@reverse")
        assertNotEquals(null, reversedChildren, "Should reverse children array")
        
        val keys = GSJson.get(selectJson, "name|@keys")
        assertNotEquals(null, keys, "Should get object keys")
        
        val values = GSJson.get(selectJson, "name|@values")
        assertNotEquals(null, values, "Should get object values")
    }
    
    @Test
    fun testJsonLines() {
        val jsonLinesData = """
        {"name": "Alice", "age": 30}
        {"name": "Bob", "age": 25}
        {"name": "Charlie", "age": 35}
        """.trimIndent()
        
        val lineCount = GSJson.get(jsonLinesData, "..#")
        assertEquals(3, lineCount, "Should count 3 JSON lines")
        
        val secondName = GSJson.get(jsonLinesData, "..[1].name")
        assertEquals("Bob", secondName, "Should get Bob from second line")
        
        val allNames = GSJson.get(jsonLinesData, "..#.name")
        assertNotEquals(null, allNames, "Should get all names")
    }
    
    @Test
    fun testResultType() {
        val result = GSJson.getResult(selectJson, "age")
        assertEquals(37, result.int(), "Age should be 37 as int")
        assertEquals("37", result.string(), "Age should be \"37\" as string")
        assertTrue(result.exists, "Age should exist")
    }
    
    @Test
    fun testExistsMethod() {
        assertTrue(GSJson.exists(selectJson, "age"), "age should exist")
        assertFalse(GSJson.exists(selectJson, "nonexistent"), "nonexistent should not exist")
    }
    
    @Test
    fun testReducerModifiers() {
        // Test data with numeric arrays
        val numbersJson = """
        {
            "scores": [10, 20, 30, 40, 50],
            "prices": ["1.5", "2.0", "3.5"]
        }
        """.trimIndent()
        
        val sum = GSJson.get(numbersJson, "scores|@sum")
        assertEquals(150.0, sum, "Sum should be 150")
        
        val avg = GSJson.get(numbersJson, "scores|@avg")
        assertEquals(30.0, avg, "Average should be 30")
        
        val min = GSJson.get(numbersJson, "scores|@min")
        assertEquals(10.0, min, "Min should be 10")
        
        val max = GSJson.get(numbersJson, "scores|@max")
        assertEquals(50.0, max, "Max should be 50")
        
        val count = GSJson.get(numbersJson, "scores|@count")
        assertEquals(5, count, "Count should be 5")
        
        val joined = GSJson.get(numbersJson, "scores|@join")
        assertEquals("10,20,30,40,50", joined, "Should join with comma")
        
        val joinedWithDash = GSJson.get(numbersJson, "scores|@join:-")
        assertEquals("10-20-30-40-50", joinedWithDash, "Should join with dash")
        
        val stringSum = GSJson.get(numbersJson, "prices|@sum")
        assertEquals(7.0, stringSum, "Sum of string numbers should be 7.0")
    }
    
    @Test
    fun testReducerOnNestedArrays() {
        val totalFriendsAge = GSJson.get(selectJson, "friends.[#.age]|@sum")
        assertEquals(196.0, totalFriendsAge, "Total age of all friends should be 196")
        
        val avgFriendAge = GSJson.get(selectJson, "friends.[#.age]|@avg")
        assertEquals(49.0, avgFriendAge, "Average friend age should be 49")
        
        val allFirstNames = GSJson.get(selectJson, "friends.[#.first]|@join")
        assertEquals("Dale,Roger,Jane,Sjenkie", allFirstNames, "Should join all first names")
    }
    
    @Test
    fun testFallbackDefaultValues() {
        // Test with existing paths (should return actual values)
        val existingAge = GSJson.get(selectJson, "age", 99)
        assertEquals(37, existingAge, "Should return actual age, not default")
        
        val existingName = GSJson.get(selectJson, "name.first", "Unknown")
        assertEquals("Tom", existingName, "Should return actual name, not default")
        
        // Test with non-existing paths (should return defaults)
        val missingField = GSJson.get(selectJson, "nonexistent", "default_value")
        assertEquals("default_value", missingField, "Should return default for missing field")
        
        val missingNestedField = GSJson.get(selectJson, "name.middle", "J")
        assertEquals("J", missingNestedField, "Should return default for missing nested field")
        
        val missingArrayElement = GSJson.get(selectJson, "friends.[99].name", "Not Found")
        assertEquals("Not Found", missingArrayElement, "Should return default for missing array element")
        
        // Test with different data types as defaults
        val numericDefault = GSJson.get(selectJson, "missing.number", 42)
        assertEquals(42, numericDefault, "Should return numeric default")
        
        val booleanDefault = GSJson.get(selectJson, "missing.boolean", true)
        assertEquals(true, booleanDefault, "Should return boolean default")
        
        val listDefault = GSJson.get(selectJson, "missing.list", listOf("a", "b", "c"))
        assertEquals(listOf("a", "b", "c"), listDefault, "Should return list default")
    }
    
    @Test
    fun testMathematicalOperations() {
        val mathJson = """
        {
            "numbers": [10, 20, 30],
            "prices": ["1.5", "2.5", "3.0"],
            "single": 15,
            "negative": [-5, -10, 15]
        }
        """.trimIndent()
        
        // Test array operations only for now
        val multipliedArray = GSJson.get(mathJson, "numbers|@multiply:2")
        assertNotEquals(null, multipliedArray, "Should multiply array elements")
        
        val dividedArray = GSJson.get(mathJson, "numbers|@divide:2")
        assertNotEquals(null, dividedArray, "Should divide array elements")
        
        val addedArray = GSJson.get(mathJson, "numbers|@add:5")
        assertNotEquals(null, addedArray, "Should add to array elements")
        
        val subtractedArray = GSJson.get(mathJson, "numbers|@subtract:5")
        assertNotEquals(null, subtractedArray, "Should subtract from array elements")
        
        val absArray = GSJson.get(mathJson, "negative|@abs")
        assertNotEquals(null, absArray, "Should get absolute values of array")
        
        val roundedPrices = GSJson.get(mathJson, "prices|@round:0")
        assertNotEquals(null, roundedPrices, "Should round array values")
        
        // Chain mathematical operations
        val chainedOps = GSJson.get(mathJson, "numbers|@add:10|@multiply:2")
        assertNotEquals(null, chainedOps, "Should chain mathematical operations")
        
        // Math with string numbers
        val stringMath = GSJson.get(mathJson, "prices|@multiply:2")
        assertNotEquals(null, stringMath, "Should perform math on string numbers")
    }
}