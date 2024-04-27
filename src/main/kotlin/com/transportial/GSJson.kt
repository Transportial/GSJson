package com.transportial

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Getting/Setting mapping language in JSON
 */
object GSJson {

    /**
     * The three accepted Data Types for both the input and output
     */
    enum class DataType { STRING, GSON, JACKSON; }


    private val objectMapper = ObjectMapper()
    private var inputType: DataType = DataType.STRING
        set(value) {
            outputType = value
            field = value
        }
    private var outputType: DataType = DataType.STRING

    /**
     * Getting JSON value from string and selection path
     *
     * @param json: as string
     * @param selection: The selection string
     *
     * @return value according to the selection path
     */
    fun get(json: String, selection: String): Any? {
        inputType = DataType.STRING
        return parseAndGet(objectMapper.readTree(json), selection)
    }

    /**
     * Getting JSON value from JSONObject and selection path
     *
     * @param json: as JSONObject
     * @param selection: The selection string
     *
     * @return value according to the selection path
     */
    fun get(json: JSONObject, selection: String): Any? {
        inputType = DataType.GSON
        return parseAndGet(objectMapper.readTree(json.toString()), selection)
    }

    /**
     * Getting JSON value from JSONArray and selection path
     *
     * @param json: as JSONArray
     * @param selection: The selection string
     *
     * @return value according to the selection path
     */
    fun get(json: JSONArray, selection: String): Any? {
        inputType = DataType.GSON
        return parseAndGet(objectMapper.readTree(json.toString()), selection)
    }

    /**
     * Getting JSON value from JsonNode and selection path
     *
     * @param json: JsonNode (Jackson JsonNode)
     * @param selection: The selection string
     *
     * @return value according to the selection path
     */
    fun get(json: JsonNode, selection: String): Any? {
        inputType = DataType.JACKSON
        return parseAndGet(json, selection)
    }


    /**
     * Parsing JSON and receiving value from JsonNode and selection path
     *
     * @param json: JsonNode (Jackson JsonNode)
     * @param selection: The selection string
     * @param contextNodes: Parent nodes
     * @param previousArrayLevel: Contextual (parent) index
     *
     * @return value according to the selection path
     */
    private fun parseAndGet(
        json: JsonNode,
        selection: String,
        contextNodes: List<JsonNode> = listOf(json),
        previousArrayLevel: Int = 0
    ): Any? {
        if (isConstantInstruction(selection)) return toConstant(selection)

        val instructions = selectionToInstructions(selection)

        val previousSelections = contextNodes.toMutableList()
        var currentSelection = json
        instructions.forEach { instruction ->
            when {
                isConstantInstruction(instruction) -> return toConstant(instruction)
                isBackReference(instruction) -> {
                    currentSelection = previousSelections[previousSelections.size - getBackReferencesCount(instruction)]
                }

                else -> {
                    currentSelection = select(
                        currentSelection,
                        instruction,
                        previousSelections.toMutableList().toList(),
                        previousArrayLevel
                    )
                    previousSelections.add(currentSelection.deepCopy())
                }
            }
        }

        return getValue(currentSelection)
    }


    /**
     * Setting JSON value from string and selection path
     *
     * @param json: as string
     * @param selection: The selection string
     * @param value: The value that is going to be set
     *
     * @return Json structure in String format
     */
    fun set(json: String, selection: String, value: Any): String {
        inputType = DataType.STRING
        return parseAndSet(objectMapper.readTree(json), selection, value).toString()
    }

    /**
     * Setting JSON value from JSONObject and selection path
     *
     * @param json: as JSONObject
     * @param selection: The selection string
     * @param value: The value that is going to be set
     *
     * @return Json structure in JSONObject format
     */
    fun set(json: JSONObject, selection: String, value: Any): Any {
        inputType = DataType.GSON
        return parseAndSet(objectMapper.readTree(json.toString()), selection, value)
    }

    /**
     * Getting JSON value from JSONArray and selection path
     *
     * @param json: as JSONArray
     * @param selection: The selection string
     * @param value: The value that is going to be set
     *
     * @return Json structure in JSONArray format
     */
    fun set(json: JSONArray, selection: String, value: Any): Any {
        inputType = DataType.GSON
        return parseAndSet(objectMapper.readTree(json.toString()), selection, value)
    }

    /**
     * Getting JSON value from JsonNode and selection path
     *
     * @param json: JsonNode (Jackson JsonNode)
     * @param selection: The selection string
     * @param value: The value that is going to be set
     *
     *
     */
    fun set(json: JsonNode, selection: String, value: Any): Any {
        inputType = DataType.JACKSON
        return parseAndSet(json, selection, value)
    }


    /**
     * Parsing JSON and settings the value in a JsonNode and selection path
     *
     * @param json: JsonNode (Jackson JsonNode)
     * @param selection: The selection string
     * @param value: The value that is going to be set
     */
    private fun parseAndSet(
        json: JsonNode = objectMapper.createObjectNode(),
        selection: String,
        value: Any,
        contextNodes: List<JsonNode> = listOf(json),
        previousArrayLevel: Int = 0,
        topLevelArrayLevel: Int = 0,
    ): JsonNode {
        var currentArrayLevel = previousArrayLevel
        val baseJsonNode = if (json is MissingNode) objectMapper.createObjectNode() else json
        val instructions = selectionToInstructions(selection)

        val previousNodes = contextNodes.toMutableList()
        var currentNode = baseJsonNode
        instructions
            .filter { it.isNotBlank() }
            .forEachIndexed { instructionIndex, instruction ->
                val nextInstruction = instructions.getOrNull(instructionIndex + 1) ?: ""

                when {
                    isBackReference(instruction) -> {
                        currentNode = previousNodes[previousNodes.size - getBackReferencesCount(instruction)]
                    }

                    // on the last element, the value has to be added
                    currentNode is ObjectNode && instructionIndex == instructions.size - 1 -> {
                        if (currentNode.has(instruction)) {
                            // Property already has a value
                            val prevValue = getValue(currentNode.get(instruction))
                            (currentNode as ObjectNode).put(instruction, "$prevValue$value")
                        } else {
                            // New property within Object
                            setValue(currentNode as ObjectNode, instruction, value)
                        }
                    }

                    currentNode is ArrayNode && instructionIndex == instructions.size - 1 -> {
                        val jsonIndex = if (instructionIndex == 0) currentArrayLevel else getJsonArrayIndex(
                            currentNode as ArrayNode,
                            instruction,
                            previousNodes,
                            currentArrayLevel
                        )
                        if ((currentNode as ArrayNode).has(jsonIndex)) {
                            if (isArrayInstruction(instruction)) {
                                setValue(currentNode as ArrayNode, null, value)
                            } else {
                                setValue(
                                    currentNode as ArrayNode,
                                    null,
                                    setValue(objectMapper.createObjectNode(), instruction, value)
                                )
                            }

                        } else {
                            if (isArrayInstruction(instruction)) {
                                val prevValue = (currentNode as ArrayNode).get(jsonIndex)?.toString() ?: ""
                                setValue(currentNode as ArrayNode, jsonIndex, "$prevValue$value")
                            } else {
                                if (((currentNode as ArrayNode).get(jsonIndex) as? ObjectNode)?.has(instruction) == true) {
                                    val prevValue = ((currentNode as ArrayNode).get(jsonIndex) as? ObjectNode)
                                        ?: objectMapper.createObjectNode()
                                    val prevPropertyValue = prevValue?.get(instruction).toString()

                                    (currentNode as ArrayNode).add(
                                        prevValue?.put(instruction, "$prevPropertyValue$value")
                                    )
                                } else {
                                    val prevValue = (currentNode as ArrayNode).get(jsonIndex) as? ObjectNode
                                        ?: objectMapper.createObjectNode()

                                    (currentNode as ArrayNode).add(
                                        prevValue.put(instruction, value.toString())
                                    )
                                }

                            }
                        }
                    }

                    currentNode is ArrayNode && topLevelArrayLevel > 0 -> {
                        if ((currentNode as? ArrayNode)?.has(topLevelArrayLevel) == true) {
                            currentNode = addJsonPart(
                                instruction,
                                (currentNode as ArrayNode),
                                if (isArrayInstruction(nextInstruction)) objectMapper.createArrayNode() else objectMapper.createObjectNode(),
                                topLevelArrayLevel
                            )
                        }
                        currentNode = (currentNode as ArrayNode).get(topLevelArrayLevel)
                        previousNodes.add(currentNode)
                    }

                    currentNode is ObjectNode -> {
                        if (!currentNode.has(instruction)) {
                            currentNode = addJsonPart(
                                instruction,
                                currentNode,
                                if (isArrayInstruction(nextInstruction)) objectMapper.createArrayNode() else objectMapper.createObjectNode(),
                                previousArrayLevel
                            )
                        }

                        currentNode = if (currentNode is ArrayNode) {
                            currentNode.get(
                                getJsonArrayIndex(
                                    currentNode as ArrayNode,
                                    cleanInstruction(instruction),
                                    previousNodes,
                                    previousArrayLevel
                                )
                            )
                        } else {
                            currentNode.get(cleanInstruction(instruction))
                        }

                        previousNodes.add(currentNode)
                    }

                    currentNode is ArrayNode -> {
                        val jsonIndex =
                            getJsonArrayIndex(currentNode as ArrayNode, instruction, previousNodes, previousArrayLevel)

                        if ((currentNode as ArrayNode).has(jsonIndex)) {
                            currentNode = addJsonPart(
                                instruction,
                                (currentNode as ArrayNode),
                                if (isArrayInstruction(nextInstruction)) objectMapper.createArrayNode() else objectMapper.createObjectNode(),
                                jsonIndex
                            )
                        }

                        currentNode = if (currentNode is ArrayNode) {
                            (currentNode as ArrayNode).get(jsonIndex) ?: addJsonPart(
                                instruction,
                                currentNode,
                                if (isArrayInstruction(nextInstruction)) objectMapper.createArrayNode() else objectMapper.createObjectNode(),
                                jsonIndex
                            ).get(jsonIndex)
                        } else {
                            (currentNode as ObjectNode).get(cleanInstruction(instruction)) ?: addJsonPart(
                                cleanInstruction(instruction),
                                currentNode,
                                if (isArrayInstruction(nextInstruction)) objectMapper.createArrayNode() else objectMapper.createObjectNode(),
                                jsonIndex
                            ).get(cleanInstruction(instruction))
                        }

                        previousNodes.add(currentNode)

                        if (isAutoArray(instruction)) {
                            currentArrayLevel++
                        }
                    }

                }

            }

        return baseJsonNode
    }

    /**
     * Create JsonNode
     *
     * @param instruction: The instruction string
     * @param json: The base level json node that has to be added on
     * @param add: The JsonNode that is going to be added
     * @param index: The index at which a node may be added
     *
     * @return the result JsonNode after the addition
     */
    private fun addJsonPart(instruction: String, json: JsonNode?, add: JsonNode, index: Int = 0): JsonNode {
        when (json) {
            is ObjectNode -> json.putIfAbsent(instruction, add)
            is ArrayNode -> json.insert(index, add)
        }
        return json ?: objectMapper.createObjectNode()
    }

    /**
     * Select the next value in the NodeTree
     *
     * @param json: JsonNode (Jackson JsonNode)
     * @param instruction: The instruction string
     * @param contextNodes: The parent, previously
     */
    private fun select(
        json: JsonNode,
        instruction: String,
        contextNodes: List<JsonNode> = listOf(),
        previousArrayLevel: Int = 0
    ): JsonNode {
        return try {
            return when (json) {
                is ArrayNode -> when {
                    isArrayInstruction(instruction) -> selectArrayElement(
                        json,
                        instruction,
                        contextNodes,
                        previousArrayLevel
                    )

                    json.has(instruction) -> json.get(instruction)
                    else -> json
                }

                is ObjectNode -> {
                    when {
                        json.has(instruction) -> json.get(instruction)
                        else -> json
                    }
                }

                else -> throw Exception("Could not select according to given instruction")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            json
        }
    }

    /**
     * Get Value of nodes
     */
    private fun getValue(json: JsonNode): Any? {
        return when (json) {
            is IntNode -> json.intValue()
            is DoubleNode -> json.doubleValue()
            is BooleanNode -> json.booleanValue()
            is TextNode -> json.textValue()
            is ArrayNode -> when (inputType) {
                DataType.GSON -> JSONArray(json.toString())
                DataType.STRING -> json.toString()
                DataType.JACKSON -> json
            }

            is ObjectNode -> when (inputType) {
                DataType.GSON -> JSONObject(json.toString())
                DataType.STRING -> json.toString()
                DataType.JACKSON -> json
            }

            else -> null
        }
    }

    /**
     * Set Value of node
     */
    private fun setValue(json: ObjectNode, instruction: String, value: Any): JsonNode {
        return try {
            when (value) {
                is String -> json.put(instruction, value)
                is Int -> json.put(instruction, value)
                is Double -> json.put(instruction, value)
                is Float -> json.put(instruction, value)
                is Boolean -> json.put(instruction, value)
                is Instant -> json.put(instruction, value.toString())
                is Short -> json.put(instruction, value)
                is Long -> json.put(instruction, value)
                is ByteArray -> json.put(instruction, value)
                is ArrayList<*> -> {
                    val listValue = objectMapper.createArrayNode()
                    (value as? ArrayList<*>)?.forEach { v ->
                        setValue(listValue, null, v)
                    }
                    json.putIfAbsent(instruction, listValue)
                }
                is List<*> -> {
                    val listValue = objectMapper.createArrayNode()
                    (value as? List<*>)?.toTypedArray()?.forEach { v ->
                        setValue(listValue, null, v)
                    }
                    json.putIfAbsent(instruction, listValue)
                }
            }

            json
        } catch (e: Exception) {
            e.printStackTrace()

            json
        }
    }

    /**
     * Set Value of node
     */
    private fun setValue(json: ArrayNode, index: Int? = 0, value: Any?): JsonNode {
        if (value == null) return json

        return when (value) {
            is String -> if (index == null) json.add(value) else json.insert(index, value)
            is Int -> if (index == null) json.add(value) else json.insert(index, value)
            is Double -> if (index == null) json.add(value) else json.insert(index, value)
            is Float -> if (index == null) json.add(value) else json.insert(index, value)
            is Boolean -> if (index == null) json.add(value) else json.insert(index, value)
            is Instant -> if (index == null) json.add(value.toString()) else json.insert(index, value.toString())
            is Short -> if (index == null) json.add(value) else json.insert(index, value)
            is Long -> if (index == null) json.add(value) else json.insert(index, value)
            is ByteArray -> if (index == null) json.add(value) else json.insert(index, value)
            else -> json
        }
    }

    /**
     * Split selection into instructions
     *
     * @param selection: the selection input
     *
     * @return list of selection instructions
     */
    private fun selectionToInstructions(selection: String): List<String> {
        var copiedSelection = String(selection.toCharArray())

        val arrayReferenceRegex = "\\[([^]]*)]"
        val arrayReferences = mutableMapOf<String, Any>()
        arrayReferenceRegex.toRegex().findAll(copiedSelection).forEach { match ->
            val reference = "array${HelperUtils.number(10)}"
            arrayReferences[reference] = match.value

            copiedSelection = copiedSelection.replace(match.value, reference)
        }

        val functionalReferenceRegex = "\\(([^)]*)\\)"
        val functionalReferences = mutableMapOf<String, Any>()
        functionalReferenceRegex.toRegex().findAll(copiedSelection).forEach { match ->
            val reference = "function${HelperUtils.number(10)}"
            functionalReferences[reference] = match.value

            copiedSelection = copiedSelection.replace(match.value, reference)
        }


        val dotRegex = "(?<!\\\\)\\Q.\\E".toRegex()
        val dashRegex = "(?<!\\\\)\\Q|\\E".toRegex()

        return dotRegex.split(copiedSelection)
            .flatMap { dashRegex.split(it) }
            .map {
                var instruction = it.replace("\\.", ".")
                    .replace("\\|", "|")

                arrayReferences.forEach {
                    instruction = instruction.replace(it.key, it.value.toString())
                }

                functionalReferences.forEach {
                    instruction = instruction.replace(it.key, it.value.toString())
                }

                instruction
            }
    }


    private fun selectArrayElement(
        jsonArray: ArrayNode,
        instruction: String,
        contextNodes: List<JsonNode> = listOf(),
        default: Int = 0
    ): JsonNode {

        if (isComparisonInstruction(instruction)) {
            return objectMapper.createArrayNode()
                .addAll(jsonArray.filter { compareSelectors(it, instruction, contextNodes, default) })
        }

        val jsonIndex = getJsonArrayIndex(jsonArray, instruction, contextNodes, default)
        if (jsonArray.has(jsonIndex)) return jsonArray.get(jsonIndex)

        return jsonArray
    }

    private fun getJsonArrayIndex(
        jsonArray: ArrayNode,
        instruction: String,
        contextNodes: List<JsonNode> = listOf(),
        default: Int = 0
    ): Int {
        if (isComparisonInstruction(instruction)) {
            val index = jsonArray
                .indexOfFirst {
                    compareSelectors(it, instruction, contextNodes, default)
                }
            return if (index >= 0) index else default
        }

        val numberString = instruction.filter { it.isDigit() }
        return if (numberString.isBlank()) 0 else numberString.toInt()
    }


    /**
     * Compare a comparison selector according to different
     */
    private fun compareSelectors(
        jsonNode: JsonNode,
        instruction: String,
        contextNodes: List<JsonNode> = listOf(),
        previousArrayLevel: Int = 0
    ): Boolean {

        val matches = "([^\\s]+)".toRegex().findAll(cleanInstruction(instruction))
        val leftSide = matches.firstOrNull()?.value?.trim() ?: ""
        val comparison = matches.elementAtOrNull(1)?.value?.trim() ?: "=="
        val rightSide = matches.elementAtOrNull(2)?.value?.trim() ?: ""


        return when (comparison) {
            "==" -> parseAndGet(jsonNode, leftSide, contextNodes, previousArrayLevel).toString() == parseAndGet(
                jsonNode,
                rightSide,
                contextNodes,
                previousArrayLevel
            ).toString()

            "!=" -> parseAndGet(jsonNode, leftSide, contextNodes, previousArrayLevel).toString() != parseAndGet(
                jsonNode,
                rightSide,
                contextNodes,
                previousArrayLevel
            ).toString()

            else -> false
        }
    }


    /**
     * Clean the instructions
     *
     * @param instruction: the instruction string
     * @return cleaned instruction
     */
    private fun cleanInstruction(instruction: String): String {
        return instruction
            .removeSurrounding("[", "]")
            .removeSurrounding("(", ")")
    }

    /**
     * Check if an instruction is a comparison
     *
     * @param instruction: the instruction string
     * @return boolean of if the instruction is a comparison
     */
    private fun isComparisonInstruction(instruction: String): Boolean {
        val matches = "([^\\s]+)".toRegex().findAll(instruction)
        return matches.count() == 3
    }

    /**
     * Is a back-reference, to earlier layered data
     *
     * @param instruction: the instruction string
     * @return boolean of if the instruction is a back reference
     */
    private fun isBackReference(instruction: String): Boolean {
        return instruction.all { it == "<".first() }
    }

    /**
     * The amount of back referenced
     *
     * @param instruction: the instruction string
     * @return the amount of back-tracking needed
     */
    private fun getBackReferencesCount(instruction: String): Int {
        return instruction.count { it == "<".first() }
    }

    /**
     * Instruction is a constant, to earlier layered data
     *
     * @param instruction: the instruction string
     * @return boolean of if the instruction is a constant
     */
    private fun isConstantInstruction(instruction: String): Boolean {
        return instruction.startsWith("\"") && instruction.endsWith("\"")
    }

    private fun toConstant(instruction: String): Any {
        return instruction.removeSurrounding("\"")
    }

    private fun isArrayInstruction(instruction: String): Boolean {
        return instruction.startsWith("[") && instruction.endsWith("]")
    }

    private fun isAutoArray(mappingPath: String): Boolean {
        return mappingPath.contains("[]")
    }

    private fun isFunctionalInstruction(mappingPart: String): Boolean {
        return mappingPart.startsWith("(") && mappingPart.endsWith(")")
    }
}