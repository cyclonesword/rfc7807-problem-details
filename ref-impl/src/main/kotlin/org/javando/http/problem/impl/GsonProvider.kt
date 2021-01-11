package org.javando.http.problem.impl

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.javando.http.problem.*
import org.javando.http.problem.JsonObject
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*


class GsonProvider @JvmOverloads constructor(gson: Gson = Gson(),
                                             override var dateIdentifier: String = "date",
                                             datePattern: String = "dd/MM/yyyy hh:mm:ss") : JsonProvider {

    private lateinit var jsonValue: JsonValue
    private lateinit var gsonElement: JsonElement
    private var gson: Gson = gson.newBuilder()
        .registerTypeAdapter(Problem::class.java, ProblemTypeAdapter(this))
        .setDateFormat(datePattern)
        .create()
        set(value) {
            field = value.newBuilder()
                .registerTypeAdapter(Problem::class.java, ProblemTypeAdapter(this))
                .setDateFormat(dateFormatPattern.toPattern())
                .create()
        }

    init {
        JsonValueKt.provider = this
    }

    override var dateFormatPattern = SimpleDateFormat(datePattern)

    override fun toJson(problem: Problem): String {
        return gson.toJson(problem, Problem::class.java)
    }

    override fun toJson(element: JsonValue): String {
        return gson.toJson(element as GsonJsonValue)
    }

    override fun fromJson(str: String): Problem {
        log.trace("fromJson in ${this::class.java.simpleName} implementation called with string $str");
        return gson.fromJson(str, Problem::class.java)
    }

    override fun toJsonObject(problem: Problem): JsonObject {
        val jsonString = gson.toJson(problem, com.google.gson.JsonObject::class.java)
        return GsonJsonObject(gson.fromJson(jsonString, com.google.gson.JsonObject::class.java))
    }

    override fun get(): Gson {
        return gson
    }

    override fun <T> fromJson(json: String, klass: Class<T>): T {
        gsonElement = gson.fromJson(json, JsonElement::class.java)
        jsonValue = parse(gsonElement)
        return gson.fromJson(json, klass)
    }

    override fun <T> fromJson(json: JsonValue, klass: Class<T>): T {
        this.jsonValue = json
        if (json !is GsonJsonValue)
            throw IllegalArgumentException("json '$json' value must be of type GsonJsonValue")
        return gson.fromJson(json.element, klass)
    }

    override fun newValue(string: String): JsonString {
        return GsonJsonString(string)
    }

    override fun newValue(int: Int): JsonInt {
        return GsonJsonInt(int)
    }

    override fun newValue(float: Float): JsonFloat {
        return GsonJsonFloat(float)
    }

    override fun newValue(double: Double): JsonDouble {
        return GsonJsonDouble(double)
    }

    override fun newValue(boolean: Boolean): JsonBoolean {
        return GsonJsonBoolean(boolean)
    }

    override fun newValue(any: Any): JsonValue {
        val value = gson.toJsonTree(any)
        return parse(value)
    }

    internal fun parse(element: JsonElement, name: String? = null): JsonValue {
        val cannotParseException = IllegalArgumentException("Cannot parse json element $element")

        return when (element) {
            is com.google.gson.JsonObject -> GsonJsonObject(element)
            is com.google.gson.JsonArray -> GsonJsonArray(element)
            is com.google.gson.JsonPrimitive ->
                when {
                    element.isBoolean -> JsonValue.of(element.asBoolean)
                    element.isString ->
                        element.asString.let { str ->
                            if (name?.toLowerCase()?.contains(dateIdentifier) == true)
                                kotlin.runCatching { JsonValue.ofDate(str) }.also {
                                    it.recover { e -> e.printStackTrace(); log.warn("Found a string with 'date' as substring that is invalid: '$str'") }
                                }.getOrNull() ?: JsonValue.of(str)
                            else
                                JsonValue.of(str)
                        }
                    element.isNumber -> {
                        element.runCatching { JsonValue.of(asInt) }.getOrNull()
                            ?: element.runCatching { JsonValue.of(asFloat) }.getOrNull()
                            ?: element.runCatching { JsonValue.of(asDouble) }.getOrNull()
                            ?: throw  cannotParseException
                    }
                    else -> throw  cannotParseException
                }
            else -> throw  cannotParseException
        }
    }

    override fun newDateValue(dateString: String): JsonDate {
        return GsonJsonDateInput(dateString, null)
    }

    override fun newDateValue(value: Date): JsonDate {
        return GsonJsonDateInput(null, value)
    }
}


class ProblemTypeAdapter(private val provider: GsonProvider) : TypeAdapter<Problem>() {

    override fun write(out: JsonWriter, p: Problem) {
        out.beginObject()
            .name("type").value(p.type.toString())
            .name("title").value(p.title)
            .name("details").value(p.details ?: "")
            .name("status").value(p.status)
            .name("instance").value(p.instance?.path ?: "")


        p.extensions.forEach { other ->
            val name = other.key
            var value = other.value as GsonJsonValue
            val element = value.element

            if(value is JsonDate) {
                out.name(name).value(value.string)
                //provider.dateFormatPattern?.also { out.name(name).value(it.format(value.date)) }
            } else
                provider.get().toJson(element, out.name(name))
        }

        out.endObject()
    }

    override fun read(reader: JsonReader): Problem {

        val parser = JsonParser.parseReader(reader)
        val obj = parser.asJsonObject
        val problem = Problem.create()

        val type = URI(obj.get("type").asString)
        val title = obj.get("title").asString
        val detail = obj.get("details")?.asString ?: ""
        val instance = URI(obj.get("instance")?.asString ?: "")
        val status = obj.get("status")?.runCatching { asInt }?.getOrNull()
            ?: throw IllegalStateException("HTTP Status code cannot be null")

        val reserved = listOf("type", "title", "details", "instance", "status")

        problem.withType(type)
            .withTitle(title)
            .withDetails(detail)
            .withInstance(instance)
            .withStatus(status)

        // val entries = obj.entrySet()
        val keys = obj.keySet()

        keys.filter { it !in reserved }.forEach {
            val element = obj.get(it)
            problem.addExtensions(Pair(it, provider.parse(element, it)))
        }

        return problem.build()
    }
}


//    class ProblemGsonNoArgsConstructor {
//
//        var type: URL? = null
//        var title: String = ""
//        var details: String = ""
//        var instance: URI? = null
//
//
//        constructor(problem: Problem) {
//            this.details = problem.details
//            this.title = problem.title
//            this.type = problem.type
//            this.instance = problem.instance
//        }
//
//        fun toProblem(customValues: List<Pair<String, JsonValue>>) {
//            Problem.create()
//                .withTitle(title)
//                .withDetails(details)
//                .withInstance(instance!!)
//                .withType(type!!)
//                .withCustomValues(customValues)
//                .build()
//        }
//    }