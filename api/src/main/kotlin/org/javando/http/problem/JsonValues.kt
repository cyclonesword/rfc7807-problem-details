package org.javando.http.problem

interface JsonValueKt {
    //val jsonString: String
    val isObject: Boolean
        get() = false
    val isArray: Boolean
        get() = false
    val isPrimitive: Boolean
        get() = false

    fun asArray(): JsonArray = throw UnsupportedOperationException("Not implemented")
    fun asObject(): JsonObject = throw UnsupportedOperationException("Not implemented")

    fun asString() = runCatching { this as JsonString }.getOrElse { throw ClassCastException("Cannot cast a ${this::class.java.simpleName} instance to a JsonString object") }
    fun asInt() = runCatching { this as JsonInt }.getOrElse { throw ClassCastException("Cannot cast a ${this::class.java.simpleName} instance to a JsonInt object") }
    fun asFloat() = runCatching { this as JsonFloat }.getOrElse { throw ClassCastException("Cannot cast a ${this::class.java.simpleName} instance to a JsonFloat object") }
    fun asDouble() = runCatching { this as JsonDouble }.getOrElse { throw ClassCastException("Cannot cast a ${this::class.java.simpleName} instance to a JsonDouble object") }
    fun asBoolean() = runCatching { this as JsonBoolean }.getOrElse { throw ClassCastException("Cannot cast a ${this::class.java.simpleName} instance to a JsonBoolean object") }
}

interface JsonObject : JsonValue {
    fun  <T> readValue(name: String, klass: Class<T>) : T
}

interface JsonArray : JsonValue {
    fun <T> readValue(position: Int, klass: Class<T>) : T
}

interface JsonString : JsonValue {
    val string: String
}

interface JsonInt : JsonValue {
    val int: Int
}

interface JsonFloat : JsonValue {
    val float: Float
}

interface JsonDouble : JsonValue {
    val double: Double
}

interface JsonBoolean : JsonValue {
    val boolean: Boolean
}

interface JsonAny : JsonValue {
    val any: Any
}