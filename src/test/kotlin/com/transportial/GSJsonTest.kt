package com.transportial

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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
}