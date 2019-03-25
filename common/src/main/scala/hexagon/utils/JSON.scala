package hexagon.utils

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import hexagon.exceptions.HexagonException

object JSON {

  private val mapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
    .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)


  def toJson(value: AnyRef): String = {
    try {
      mapper.writeValueAsString(value)
    } catch {
      case t: Throwable => throw new HexagonException(s"Cannot serialize an object '$value' into json string.", t)
    }
  }


  def fromJson[T](json: String, tr: TypeReference[T]): T = {
    try {
      mapper.readValue(json, tr)
    } catch {
      case t: Throwable => throw new HexagonException(s"Cannot parse json string: '$json' .", t)
    }
  }


  def fromJson[T](json: String, valueType: Class[T]): T = {
    try {
      mapper.readValue(json, valueType)
    } catch {
      case t: Throwable => throw new HexagonException(s"Cannot parse json string: '$json' .", t)
    }
  }


  def toMap(json: String): Map[String, Any] = {
    val tr = new TypeReference[Map[String, Any]] {}
    try {
      fromJson(json, tr)
    } catch {
      case t: Throwable => throw new HexagonException(s"Cannot parse json string: '$json' .", t)
    }
  }


  def toBytes(value: AnyRef): Array[Byte] = {
    try {
      mapper.writeValueAsBytes(value)
    } catch {
      case t: Throwable => throw new HexagonException(s"Cannot serialize an object '$value' into byte array.", t)
    }
  }

}
