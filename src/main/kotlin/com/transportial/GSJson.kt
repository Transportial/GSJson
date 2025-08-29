package com.transportial

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import kotlin.math.*


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
        return if (isJsonLinesSelection(selection)) {
            handleJsonLines(json, selection)
        } else {
            parseAndGet(objectMapper.readTree(json), selection)
        }
    }
    
    /**
     * Getting JSON value from string and selection path with fallback default
     *
     * @param json: as string
     * @param selection: The selection string
     * @param defaultValue: Default value to return if path doesn't exist or is null
     *
     * @return value according to the selection path or default value
     */
    fun get(json: String, selection: String, defaultValue: Any): Any {
        inputType = DataType.STRING
        val result = if (isJsonLinesSelection(selection)) {
            handleJsonLines(json, selection)
        } else {
            parseAndGet(objectMapper.readTree(json), selection)
        }
        return result ?: defaultValue
    }
    
    /**
     * Getting JSON result from string and selection path
     *
     * @param json: as string
     * @param selection: The selection string
     *
     * @return GSJsonResult with enhanced functionality
     */
    fun getResult(json: String, selection: String): GSJsonResult {
        inputType = DataType.STRING
        val result = parseAndGet(objectMapper.readTree(json), selection)
        return createResult(result)
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
     * Getting JSON value from JSONObject and selection path with fallback default
     *
     * @param json: as JSONObject
     * @param selection: The selection string
     * @param defaultValue: Default value to return if path doesn't exist or is null
     *
     * @return value according to the selection path or default value
     */
    fun get(json: JSONObject, selection: String, defaultValue: Any): Any {
        inputType = DataType.GSON
        val result = parseAndGet(objectMapper.readTree(json.toString()), selection)
        return result ?: defaultValue
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
     * Getting JSON value from JSONArray and selection path with fallback default
     *
     * @param json: as JSONArray
     * @param selection: The selection string
     * @param defaultValue: Default value to return if path doesn't exist or is null
     *
     * @return value according to the selection path or default value
     */
    fun get(json: JSONArray, selection: String, defaultValue: Any): Any {
        inputType = DataType.GSON
        val result = parseAndGet(objectMapper.readTree(json.toString()), selection)
        return result ?: defaultValue
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
     * Getting JSON value from JsonNode and selection path with fallback default
     *
     * @param json: JsonNode (Jackson JsonNode)
     * @param selection: The selection string
     * @param defaultValue: Default value to return if path doesn't exist or is null
     *
     * @return value according to the selection path or default value
     */
    fun get(json: JsonNode, selection: String, defaultValue: Any): Any {
        inputType = DataType.JACKSON
        val result = parseAndGet(json, selection)
        return result ?: defaultValue
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
                isModifierInstruction(instruction) -> {
                    currentSelection = applyModifier(currentSelection, instruction)
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
                    isCountInstruction(instruction) -> {
                        objectMapper.valueToTree(json.size())
                    }
                    isAllElementsInstruction(instruction) -> {
                        val childPath = instruction.removePrefix("#.")
                        val resultArray = objectMapper.createArrayNode()
                        json.forEach { element ->
                            val childValue = parseAndGet(element, childPath, contextNodes, previousArrayLevel)
                            when (childValue) {
                                is JsonNode -> resultArray.add(childValue)
                                else -> resultArray.add(objectMapper.valueToTree(childValue) as JsonNode)
                            }
                        }
                        resultArray
                    }
                    json.has(instruction) -> json.get(instruction)
                    else -> json
                }

                is ObjectNode -> {
                    when {
                        json.has(instruction) -> json.get(instruction)
                        isWildcardInstruction(instruction) -> selectWildcardMatch(json, instruction)
                        else -> objectMapper.missingNode()
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
            is MissingNode -> null
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
        
        // Protect modifier arguments from being split
        val modifierReferenceRegex = "@\\w+:[^|]*"
        val modifierReferences = mutableMapOf<String, Any>()
        modifierReferenceRegex.toRegex().findAll(copiedSelection).forEach { match ->
            val reference = "modifier${HelperUtils.number(10)}"
            modifierReferences[reference] = match.value

            copiedSelection = copiedSelection.replace(match.value, reference)
        }


        val dotRegex = "(?<!\\\\)\\Q.\\E".toRegex()
        val pipeRegex = "(?<!\\\\)\\Q|\\E".toRegex()

        return dotRegex.split(copiedSelection)
            .flatMap { pipeRegex.split(it) }
            .map {
                var instruction = it.replace("\\.", ".")
                    .replace("\\|", "|")

                arrayReferences.forEach {
                    instruction = instruction.replace(it.key, it.value.toString())
                }

                functionalReferences.forEach {
                    instruction = instruction.replace(it.key, it.value.toString())
                }
                
                modifierReferences.forEach {
                    instruction = instruction.replace(it.key, it.value.toString())
                }

                instruction
            }
    }

    /**
     * Select element within an array
     */
    private fun selectArrayElement(
        jsonArray: ArrayNode,
        instruction: String,
        contextNodes: List<JsonNode> = listOf(),
        default: Int = 0
    ): JsonNode {
        val cleanedInstruction = cleanInstruction(instruction)
        
        if (cleanedInstruction == "#") {
            return objectMapper.valueToTree(jsonArray.size())
        }
        
        if (cleanedInstruction.startsWith("#.")) {
            val childPath = cleanedInstruction.removePrefix("#.")
            val resultArray = objectMapper.createArrayNode()
            jsonArray.forEach { element ->
                val childValue = parseAndGet(element, childPath, contextNodes, default)
                when (childValue) {
                    is JsonNode -> resultArray.add(childValue)
                    else -> resultArray.add(objectMapper.valueToTree(childValue) as JsonNode)
                }
            }
            return resultArray
        }

        if (isComparisonInstruction(cleanedInstruction)) {
            val filteredArray = objectMapper.createArrayNode()
                .addAll(jsonArray.filter { compareSelectors(it, cleanedInstruction, contextNodes, default) })
            return filteredArray
        }
        
        if (isMultiQueryInstruction(cleanedInstruction)) {
            return handleMultiQuery(jsonArray, cleanedInstruction, contextNodes, default)
        }

        val jsonIndex = getJsonArrayIndex(jsonArray, instruction, contextNodes, default)
        if (jsonArray.has(jsonIndex)) return jsonArray.get(jsonIndex)

        return objectMapper.missingNode()
    }

    /**
     * Get the index selection within the array
     */
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
     * Compare a comparison selector according to different operators
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

        val leftValue = parseAndGet(jsonNode, leftSide, contextNodes, previousArrayLevel)
        val rightValue = parseAndGet(jsonNode, rightSide, contextNodes, previousArrayLevel)
        
        val leftStr = leftValue.toString()
        val rightStr = rightValue.toString()

        return when (comparison) {
            "==" -> leftStr == rightStr
            "!=" -> leftStr != rightStr
            "<" -> compareNumericOrString(leftValue, rightValue) < 0
            "<=" -> compareNumericOrString(leftValue, rightValue) <= 0
            ">" -> compareNumericOrString(leftValue, rightValue) > 0
            ">=" -> compareNumericOrString(leftValue, rightValue) >= 0
            "%" -> leftStr.matches(wildcardToRegex(rightStr))
            "!%" -> !leftStr.matches(wildcardToRegex(rightStr))
            else -> false
        }
    }

    /**
     * Compare values numerically if possible, otherwise as strings
     */
    private fun compareNumericOrString(left: Any?, right: Any?): Int {
        return try {
            val leftNum = when (left) {
                is Number -> left.toDouble()
                is String -> left.toDouble()
                else -> left.toString().toDouble()
            }
            val rightNum = when (right) {
                is Number -> right.toDouble()
                is String -> right.toDouble()
                else -> right.toString().toDouble()
            }
            leftNum.compareTo(rightNum)
        } catch (e: Exception) {
            left.toString().compareTo(right.toString())
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

    /**
     * Check if instruction contains wildcards (* or ?)
     */
    private fun isWildcardInstruction(instruction: String): Boolean {
        return instruction.contains('*') || instruction.contains('?')
    }

    /**
     * Check if instruction is asking for count (#)
     */
    private fun isCountInstruction(instruction: String): Boolean {
        return instruction == "#"
    }

    /**
     * Check if instruction is asking for all elements with a child path (#.something)
     */
    private fun isAllElementsInstruction(instruction: String): Boolean {
        return instruction.startsWith("#.") && instruction.length > 2
    }

    /**
     * Select matching keys using wildcard patterns
     */
    private fun selectWildcardMatch(json: ObjectNode, pattern: String): JsonNode {
        val regex = wildcardToRegex(pattern)
        val matchingKeys = json.fieldNames().asSequence().filter { key ->
            regex.matches(key)
        }.toList()
        
        return when (matchingKeys.size) {
            0 -> objectMapper.createObjectNode()
            1 -> json.get(matchingKeys.first())
            else -> {
                val resultArray = objectMapper.createArrayNode()
                matchingKeys.forEach { key ->
                    resultArray.add(json.get(key))
                }
                resultArray
            }
        }
    }

    /**
     * Convert wildcard pattern to regex
     */
    private fun wildcardToRegex(pattern: String): Regex {
        val escaped = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return "^$escaped$".toRegex()
    }

    /**
     * Check if instruction contains multiple query patterns (for nested array queries)
     */
    private fun isMultiQueryInstruction(instruction: String): Boolean {
        return instruction.contains("#(") && instruction.endsWith(")#")
    }

    /**
     * Handle multiple query patterns like friends.#(nets.#(=="fb"))#.first
     */
    private fun handleMultiQuery(
        jsonArray: ArrayNode,
        instruction: String,
        contextNodes: List<JsonNode>,
        previousArrayLevel: Int
    ): JsonNode {
        val queryPattern = instruction.removePrefix("#(").removeSuffix(")#")
        val filteredArray = objectMapper.createArrayNode()
        
        jsonArray.forEach { element ->
            if (evaluateNestedQuery(element, queryPattern, contextNodes, previousArrayLevel)) {
                filteredArray.add(element)
            }
        }
        
        return filteredArray
    }

    /**
     * Evaluate nested queries recursively
     */
    private fun evaluateNestedQuery(
        element: JsonNode,
        query: String,
        contextNodes: List<JsonNode>,
        previousArrayLevel: Int
    ): Boolean {
        return if (query.contains("#(")) {
            val nestedPath = query.substringBefore(".#(")
            val nestedQuery = query.substringAfter(".#(").substringBeforeLast(")")
            
            val nestedArray = parseAndGet(element, nestedPath, contextNodes, previousArrayLevel)
            when (nestedArray) {
                is ArrayNode -> nestedArray.any { nestedElement ->
                    evaluateNestedQuery(nestedElement, nestedQuery, contextNodes, previousArrayLevel)
                }
                else -> false
            }
        } else {
            compareSelectors(element, query, contextNodes, previousArrayLevel)
        }
    }
    
    /**
     * Create a GSJsonResult from a value
     */
    private fun createResult(value: Any?): GSJsonResult {
        val type = when (value) {
            is String -> GSJsonResult.ResultType.STRING
            is Number -> GSJsonResult.ResultType.NUMBER
            is Boolean -> GSJsonResult.ResultType.BOOLEAN
            is ArrayNode, is List<*> -> GSJsonResult.ResultType.ARRAY
            is ObjectNode -> GSJsonResult.ResultType.OBJECT
            null -> GSJsonResult.ResultType.NULL
            else -> GSJsonResult.ResultType.STRING
        }
        
        val raw = value?.toString() ?: "null"
        val exists = value != null
        
        return GSJsonResult(value, raw, type, exists)
    }
    
    /**
     * Check if a path exists in the JSON
     */
    fun exists(json: String, selection: String): Boolean {
        return try {
            inputType = DataType.STRING
            val jsonTree = objectMapper.readTree(json)
            val result = parseAndGet(jsonTree, selection)
            result != null && result.toString() != "null" && result != MissingNode.getInstance()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if instruction is a modifier (starts with @)
     */
    private fun isModifierInstruction(instruction: String): Boolean {
        return instruction.startsWith("@")
    }
    
    /**
     * Apply built-in modifiers
     */
    private fun applyModifier(json: JsonNode, instruction: String): JsonNode {
        val parts = instruction.split(":", limit = 2)
        val modifier = parts[0]
        val argument = if (parts.size > 1) parts[1] else null
        
        return when (modifier) {
            "@reverse" -> when (json) {
                is ArrayNode -> {
                    val reversed = objectMapper.createArrayNode()
                    json.reversed().forEach { reversed.add(it) }
                    reversed
                }
                is ObjectNode -> {
                    val reversed = objectMapper.createObjectNode()
                    json.fieldNames().asSequence().toList().reversed().forEach { key ->
                        reversed.set<JsonNode>(key, json.get(key))
                    }
                    reversed
                }
                else -> json
            }
            "@keys" -> when (json) {
                is ObjectNode -> {
                    val keys = objectMapper.createArrayNode()
                    json.fieldNames().forEach { keys.add(it) }
                    keys
                }
                else -> objectMapper.createArrayNode()
            }
            "@values" -> when (json) {
                is ObjectNode -> {
                    val values = objectMapper.createArrayNode()
                    json.fields().forEach { values.add(it.value) }
                    values
                }
                else -> objectMapper.createArrayNode()
            }
            "@flatten" -> when (json) {
                is ArrayNode -> {
                    val flattened = objectMapper.createArrayNode()
                    json.forEach { element ->
                        when (element) {
                            is ArrayNode -> element.forEach { flattened.add(it) }
                            else -> flattened.add(element)
                        }
                    }
                    flattened
                }
                else -> json
            }
            "@this" -> json
            "@pretty" -> {
                val prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json)
                objectMapper.valueToTree(prettyJson)
            }
            "@ugly" -> {
                val compactJson = objectMapper.writeValueAsString(json)
                objectMapper.valueToTree(compactJson)
            }
            "@tostr" -> {
                objectMapper.valueToTree(json.toString())
            }
            "@fromstr" -> {
                try {
                    objectMapper.readTree(json.asText())
                } catch (e: Exception) {
                    json
                }
            }
            "@sum" -> when (json) {
                is ArrayNode -> {
                    val sum = json.sumOf { node ->
                        when {
                            node.isNumber -> node.asDouble()
                            node.isTextual -> node.asText().toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                    }
                    objectMapper.valueToTree(sum)
                }
                else -> objectMapper.valueToTree(0)
            }
            "@avg" -> when (json) {
                is ArrayNode -> {
                    if (json.size() == 0) {
                        objectMapper.valueToTree(0)
                    } else {
                        val sum = json.sumOf { node ->
                            when {
                                node.isNumber -> node.asDouble()
                                node.isTextual -> node.asText().toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            }
                        }
                        objectMapper.valueToTree(sum / json.size())
                    }
                }
                else -> objectMapper.valueToTree(0)
            }
            "@min" -> when (json) {
                is ArrayNode -> {
                    val minValue = json.minOfOrNull { node ->
                        when {
                            node.isNumber -> node.asDouble()
                            node.isTextual -> node.asText().toDoubleOrNull() ?: Double.MAX_VALUE
                            else -> Double.MAX_VALUE
                        }
                    }
                    objectMapper.valueToTree(minValue ?: 0)
                }
                else -> objectMapper.valueToTree(0)
            }
            "@max" -> when (json) {
                is ArrayNode -> {
                    val maxValue = json.maxOfOrNull { node ->
                        when {
                            node.isNumber -> node.asDouble()
                            node.isTextual -> node.asText().toDoubleOrNull() ?: Double.MIN_VALUE
                            else -> Double.MIN_VALUE
                        }
                    }
                    objectMapper.valueToTree(maxValue ?: 0)
                }
                else -> objectMapper.valueToTree(0)
            }
            "@count" -> when (json) {
                is ArrayNode -> objectMapper.valueToTree(json.size())
                is ObjectNode -> objectMapper.valueToTree(json.size())
                else -> objectMapper.valueToTree(1)
            }
            "@join" -> when (json) {
                is ArrayNode -> {
                    val separator = argument?.trim() ?: ","
                    val joinedString = json.joinToString(separator) { node ->
                        when {
                            node.isTextual -> node.asText()
                            node.isNumber -> node.asText()
                            else -> node.toString().removeSurrounding("\"")
                        }
                    }
                    objectMapper.valueToTree(joinedString)
                }
                else -> json
            }
            "@multiply", "@mul" -> when (json) {
                is ArrayNode -> {
                    val multiplier = argument?.toDoubleOrNull() ?: 1.0
                    val resultArray = objectMapper.createArrayNode()
                    json.forEach { node ->
                        val value = when {
                            node.isNumber -> node.asDouble() * multiplier
                            node.isTextual -> (node.asText().toDoubleOrNull() ?: 0.0) * multiplier
                            else -> 0.0
                        }
                        resultArray.add(value)
                    }
                    resultArray
                }
                else -> {
                    val multiplier = argument?.toDoubleOrNull() ?: 1.0
                    val value = when {
                        json.isNumber -> json.asDouble() * multiplier
                        json.isTextual -> (json.asText().toDoubleOrNull() ?: 0.0) * multiplier
                        else -> 0.0
                    }
                    objectMapper.valueToTree(value)
                }
            }
            "@divide", "@div" -> when (json) {
                is ArrayNode -> {
                    val divisor = argument?.toDoubleOrNull() ?: 1.0
                    if (divisor == 0.0) return json // Avoid division by zero
                    val resultArray = objectMapper.createArrayNode()
                    json.forEach { node ->
                        val value = when {
                            node.isNumber -> node.asDouble() / divisor
                            node.isTextual -> (node.asText().toDoubleOrNull() ?: 0.0) / divisor
                            else -> 0.0
                        }
                        resultArray.add(value)
                    }
                    resultArray
                }
                else -> {
                    val divisor = argument?.toDoubleOrNull() ?: 1.0
                    if (divisor == 0.0) return json // Avoid division by zero
                    val value = when {
                        json.isNumber -> json.asDouble() / divisor
                        json.isTextual -> (json.asText().toDoubleOrNull() ?: 0.0) / divisor
                        else -> 0.0
                    }
                    objectMapper.valueToTree(value)
                }
            }
            "@add", "@plus" -> when (json) {
                is ArrayNode -> {
                    val addend = argument?.toDoubleOrNull() ?: 0.0
                    val resultArray = objectMapper.createArrayNode()
                    json.forEach { node ->
                        val value = when {
                            node.isNumber -> node.asDouble() + addend
                            node.isTextual -> (node.asText().toDoubleOrNull() ?: 0.0) + addend
                            else -> addend
                        }
                        resultArray.add(value)
                    }
                    resultArray
                }
                else -> {
                    val addend = argument?.toDoubleOrNull() ?: 0.0
                    val value = when {
                        json.isNumber -> json.asDouble() + addend
                        json.isTextual -> (json.asText().toDoubleOrNull() ?: 0.0) + addend
                        else -> addend
                    }
                    objectMapper.valueToTree(value)
                }
            }
            "@subtract", "@sub" -> when (json) {
                is ArrayNode -> {
                    val subtrahend = argument?.toDoubleOrNull() ?: 0.0
                    val resultArray = objectMapper.createArrayNode()
                    json.forEach { node ->
                        val value = when {
                            node.isNumber -> node.asDouble() - subtrahend
                            node.isTextual -> (node.asText().toDoubleOrNull() ?: 0.0) - subtrahend
                            else -> -subtrahend
                        }
                        resultArray.add(value)
                    }
                    resultArray
                }
                else -> {
                    val subtrahend = argument?.toDoubleOrNull() ?: 0.0
                    val value = when {
                        json.isNumber -> json.asDouble() - subtrahend
                        json.isTextual -> (json.asText().toDoubleOrNull() ?: 0.0) - subtrahend
                        else -> -subtrahend
                    }
                    objectMapper.valueToTree(value)
                }
            }
            "@power", "@pow" -> when (json) {
                is ArrayNode -> {
                    val exponent = argument?.toDoubleOrNull() ?: 1.0
                    val resultArray = objectMapper.createArrayNode()
                    json.forEach { node ->
                        val value = when {
                            node.isNumber -> node.asDouble().pow(exponent)
                            node.isTextual -> (node.asText().toDoubleOrNull() ?: 0.0).pow(exponent)
                            else -> 0.0
                        }
                        resultArray.add(value)
                    }
                    resultArray
                }
                else -> {
                    val exponent = argument?.toDoubleOrNull() ?: 1.0
                    val value = when {
                        json.isNumber -> json.asDouble().pow(exponent)
                        json.isTextual -> (json.asText().toDoubleOrNull() ?: 0.0).pow(exponent)
                        else -> 0.0
                    }
                    objectMapper.valueToTree(value)
                }
            }
            "@round" -> when (json) {
                is ArrayNode -> {
                    val digits = argument?.toIntOrNull() ?: 0
                    val multiplier = 10.0.pow(digits.toDouble())
                    val resultArray = objectMapper.createArrayNode()
                    json.forEach { node ->
                        val value = when {
                            node.isNumber -> round(node.asDouble() * multiplier) / multiplier
                            node.isTextual -> round((node.asText().toDoubleOrNull() ?: 0.0) * multiplier) / multiplier
                            else -> 0.0
                        }
                        resultArray.add(value)
                    }
                    resultArray
                }
                else -> {
                    val digits = argument?.toIntOrNull() ?: 0
                    val multiplier = 10.0.pow(digits.toDouble())
                    val value = when {
                        json.isNumber -> round(json.asDouble() * multiplier) / multiplier
                        json.isTextual -> round((json.asText().toDoubleOrNull() ?: 0.0) * multiplier) / multiplier
                        else -> 0.0
                    }
                    objectMapper.valueToTree(value)
                }
            }
            "@abs" -> when (json) {
                is ArrayNode -> {
                    val resultArray = objectMapper.createArrayNode()
                    json.forEach { node ->
                        val value = when {
                            node.isNumber -> abs(node.asDouble())
                            node.isTextual -> abs(node.asText().toDoubleOrNull() ?: 0.0)
                            else -> 0.0
                        }
                        resultArray.add(value)
                    }
                    resultArray
                }
                else -> {
                    val value = when {
                        json.isNumber -> abs(json.asDouble())
                        json.isTextual -> abs(json.asText().toDoubleOrNull() ?: 0.0)
                        else -> 0.0
                    }
                    objectMapper.valueToTree(value)
                }
            }
            else -> json
        }
    }
    
    /**
     * Check if selection is for JSON Lines (starts with ..)
     */
    private fun isJsonLinesSelection(selection: String): Boolean {
        return selection.startsWith("..")
    }
    
    /**
     * Handle JSON Lines format
     */
    private fun handleJsonLines(jsonLines: String, selection: String): Any? {
        val lines = jsonLines.trim().split("\n").filter { it.isNotBlank() }
        val jsonArray = objectMapper.createArrayNode()
        
        lines.forEach { line ->
            try {
                val parsedLine = objectMapper.readTree(line)
                jsonArray.add(parsedLine)
            } catch (e: Exception) {
                // Skip invalid JSON lines
            }
        }
        
        val cleanedSelection = selection.removePrefix("..")
        return if (cleanedSelection.isEmpty()) {
            getValue(jsonArray)
        } else {
            inputType = DataType.JACKSON
            // Wrap the path in brackets for array access if it starts with a number
            val arraySelection = if (cleanedSelection.matches("^\\d+.*".toRegex())) {
                "[${cleanedSelection}]"
            } else {
                cleanedSelection
            }
            parseAndGet(jsonArray, arraySelection)
        }
    }
    
    /**
     * Iterate through JSON Lines
     */
    fun forEachLine(jsonLines: String, action: (GSJsonResult) -> Boolean) {
        val lines = jsonLines.trim().split("\n").filter { it.isNotBlank() }
        
        for (line in lines) {
            try {
                val parsedLine = objectMapper.readTree(line)
                val result = createResult(getValue(parsedLine))
                if (!action(result)) break
            } catch (e: Exception) {
                // Skip invalid JSON lines
            }
        }
    }
}