package com.example.demo

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals

@SpringBootTest
class DemoApplicationTests {

    @Autowired
    lateinit var json: Json

    @Test
    fun testMessage() {
        val encodedMessage = json.encodeToString(Message("hello world"))
        println("encoded message: $encodedMessage")
        assertEquals(
            """
            {
                "body": "hello world"
            }
        """.trimIndent(),
            encodedMessage
        )

        val decodedMessage = json.decodeFromString<Message>(encodedMessage)
        println("decoded message: $decodedMessage")
    }

    @Test
    fun testKotlinxSerializationJson() {
        val encodedPerson = json.encodeToString(Person("Hantsy Bai", LocalDate.of(1970, 1, 1)))
        println("encoded person: $encodedPerson")

        assertEquals(
            """
            {
                "name": "Hantsy Bai",
                "birth_date": "1970-01-01",
                "gender": "MALE"
            }
        """.trimIndent(), encodedPerson
        )

        val decodedPerson = json.decodeFromString<Person>(encodedPerson)
        println("decoded person: $decodedPerson")
        assertEquals(Gender.MALE, decodedPerson.gender)
    }

    @Test
    fun testWithFullnameAndDefault() {

        val jsonPerson = """
             {
                "full_name": "Hantsy Bai",
                "birth_date": "1970-01-01"
            }
        """.trimIndent()

        val decodedPerson2 = json.decodeFromString<Person>(jsonPerson)
        println("decoded person: $decodedPerson2")
        assertEquals("Hantsy Bai", decodedPerson2.name)
        assertEquals(Gender.MALE, decodedPerson2.gender)
    }

}

@Serializable
data class Message(val body: String)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("person")
data class Person(
    @JsonNames("full_name") val name: String,

    @Serializable(LocalDateSerializer::class)
    val birthDate: LocalDate,

    val gender: Gender = Gender.MALE
)

enum class Gender {
    MALE, FEMALE;
}

class LocalDateSerializer : KSerializer<LocalDate> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): LocalDate {
        val string = decoder.decodeString()
        return LocalDate.parse(string, formatter)
    }

    override fun serialize(encoder: Encoder, value: LocalDate) {
        val result = value.format(formatter)
        encoder.encodeString(result)
    }
}