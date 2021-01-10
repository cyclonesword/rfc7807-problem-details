package org.javando.http.problem;

import java.text.SimpleDateFormat
import java.util.*


interface JsonProvider {
    fun toJson(problem: Problem): String
    fun toJson(element: JsonValue): String
    fun toJsonObject(problem: Problem): JsonObject

    fun get(): Any
    var dateFormatPattern: SimpleDateFormat
    var dateIdentifier: String

    fun fromJson(str: String) : Problem
    fun <T> fromJson(json: String, klass: Class<T>) : T
    fun <T> fromJson(json: JsonValue, klass: Class<T>) : T

    fun newValue(string: String) : JsonString
    fun newDateValue(dateString: String) : JsonDate
    fun newDateValue(value: Date): JsonDate
    fun newValue(int: Int) : JsonInt
    fun newValue(boolean: Boolean) : JsonBoolean
    fun newValue(float: Float) : JsonFloat
    fun newValue(double: Double) : JsonDouble
    fun newValue(any: Any): JsonValue
}

