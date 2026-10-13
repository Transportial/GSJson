package com.transportial

import com.fasterxml.jackson.databind.node.NullNode
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("1.2.8 read fixes")
class GSJsonReadFixesTest {

    private val json = """
        {
          "millis": 1760428800000, "huge": 123456789012345678901234567890,
          "price": 12.50, "rate": 0.5, "sci": 1e5, "count": 3, "nothing": null, "name": "x",
          "items": [
            {"code": "A", "cost": {"amount": "1.5"}, "entity": {"goods": [{"x": 1}]}, "owner": "Smith John", "nets": ["fb", "ig"]},
            {"code": "B", "cost": {"amount": 2}, "entity": {"goods": [{"x": 2}]}, "nets": ["tw"]}
          ]
        }
    """.trimIndent()

    @Test
    fun `numbers outside int range and exact decimals keep their value`() {
        assertEquals(1760428800000L, GSJson.get(json, "millis"))
        assertEquals(BigInteger("123456789012345678901234567890"), GSJson.get(json, "huge"))
        assertEquals(1760428800000L, GSJson.get(JSONObject(json), "millis"))
        assertEquals(1760428800000L, GSJson.get(GSJson.parse(json), "millis"))
        // org.json input keeps decimals as org.json holds them; a Double only where its text is identical.
        val orgJson = JSONObject().put("price", BigDecimal("12.50")).put("rate", BigDecimal("0.5")).put("sci", BigDecimal("1E+5"))
        assertEquals(BigDecimal("12.50"), GSJson.get(orgJson, "price"))
        assertEquals(0.5, GSJson.get(orgJson, "rate"))
        assertEquals("1E+5", GSJson.get(orgJson, "sci").toString())
        assertEquals("12.50", GSJson.get(GSJson.parse(orgJson), "price").toString())
    }

    @Test
    fun `an explicit null is the input family's null, a missing path is null`() {
        assertEquals(JSONObject.NULL, GSJson.get(JSONObject(json), "nothing"))
        assertEquals(NullNode.instance, GSJson.get(GSJson.parse(json), "nothing"))
        assertNull(GSJson.get(json, "nothing"))
        assertNull(GSJson.get(JSONObject(json), "absent"))
        assertEquals("fallback", GSJson.get(JSONObject(json), "nothing", "fallback"))
    }

    @Test
    fun `a path through a scalar or a missing node is missing`() {
        assertNull(GSJson.get(json, "name.deeper.still"))
        assertNull(GSJson.get(json, "absent.deeper"))
        assertNull(GSJson.get(json, "nothing.deeper"))
        assertNull(GSJson.get(json, "<<<.name"))
    }

    @Test
    fun `a plain number on an array is an index, any other key reads the first element`() {
        assertEquals("B", GSJson.get(json, "items.1.code"))
        assertNull(GSJson.get(json, "items.7.code"))
        assertEquals(1, GSJson.get(json, "items.entity.goods.x"))
        assertEquals("A", GSJson.get(json, "items.[cost.amount == \"1.5\"].code"))
        assertNull(GSJson.get("""{"items": []}""", "items.code"))
    }

    @Test
    fun `hash maps the rest of the path bracketed or not, nested brackets included`() {
        assertEquals("""["1.5",2]""", GSJson.get(json, "items.#.cost.amount"))
        assertEquals("""["1.5",2]""", GSJson.get(json, "items.[#.cost.amount]"))
        assertEquals("[[{\"x\":1}],[{\"x\":2}]]", GSJson.get(json, "items.[#.entity.goods]"))
        assertEquals("[1,2]", GSJson.get(json, "items.[#.entity.goods.[0].x]"))
        assertEquals("[1,2]", GSJson.get(json, "items.#.entity.goods.[0].x"))
        assertTrue(JSONArray("""[{"x":1},{"x":2}]""").similar(GSJson.get(JSONObject(json), "items.#.entity.goods.[0]")))
        assertEquals("[]", GSJson.get(json, "items.#.missing"))
        assertEquals(2, GSJson.get(json, "items.#"))
        assertEquals(3.5, GSJson.get(json, "items.#.cost.amount|@sum"))
    }

    @Test
    fun `modifiers chain after a reducer`() {
        assertEquals(4.0, GSJson.get(json, "items.[#.cost.amount]|@sum|@round"))
        assertEquals(3.5, GSJson.get(json, "items.[#.cost.amount]|@sum|@abs"))
        assertEquals(7.0, GSJson.get(json, "items.[#.cost.amount]|@sum|@multiply:2"))
    }

    @Test
    fun `comparisons need no spaces and quoted constants may hold spaces dots and brackets`() {
        assertEquals("B", GSJson.get(json, "items.[code==\"B\"].[0].code"))
        assertEquals("B", GSJson.get(json, "items.[code!=\"A\"].[0].code"))
        assertEquals("A", GSJson.get(json, "items.[owner == \"Smith John\"].[0].code"))
        assertEquals("[]", GSJson.get(json, "items.[owner == \"a.b|c]\"]"))
        assertEquals("B", GSJson.get(json, "items.[cost.amount>=\"2\"].[0].code"))
        // A missing operand compares as "", not as "null".
        assertEquals("B", GSJson.get(json, "items.[owner == \"\"].[0].code"))
        assertEquals("[]", GSJson.get(json, "items.[owner == \"null\"]"))
        // Nested sub-array query keeps the elements on which it finds something.
        assertEquals("A", GSJson.get(json, "items.[nets.[# == \"ig\"]].[0].code"))
    }

    @Test
    fun `writes find comparison slots without spaces too`() {
        val written = GSJson.set("""{"items":[{"code":"A"},{"code":"B"}]}""", "items.[code==\"B\"].qty", 5)
        assertEquals("""{"items":[{"code":"A"},{"code":"B","qty":5}]}""", written)
    }

    @Test
    fun `return types do not leak between threads`() {
        val pool = Executors.newFixedThreadPool(8)
        val failures = java.util.concurrent.atomic.AtomicInteger()
        repeat(2_000) { i ->
            pool.submit {
                if (i % 2 == 0) {
                    if (GSJson.get(JSONObject(json), "items") !is JSONArray) failures.incrementAndGet()
                } else {
                    if (GSJson.get(json, "items") !is String) failures.incrementAndGet()
                }
            }
        }
        pool.shutdown()
        pool.awaitTermination(1, TimeUnit.MINUTES)
        assertEquals(0, failures.get())
    }
}
