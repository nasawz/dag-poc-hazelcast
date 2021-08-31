package com.palline.domain.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;

/**
 * JSON工具类
 *
 * @author zhangkai
 * @date 2021/6/8 16:05
 */
public class JsonUtils {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private static final ObjectMapper OBJECT_MAPPER_INDENT = new ObjectMapper()
      .configure(SerializationFeature.INDENT_OUTPUT, true);

  public static String toJson(Object o) {
    try {
      return OBJECT_MAPPER.writeValueAsString(o);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T toBean(String json, TypeReference<T> type) {
    try {
      return OBJECT_MAPPER.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T toBean(String json, Class<T> tClass) {
    try {
      return OBJECT_MAPPER.readValue(json, tClass);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T toBean(String json, JavaType javaType) {
    try {
      return OBJECT_MAPPER.readValue(json, javaType);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static JsonNode toJsonNode(String json) {
    try {
      return OBJECT_MAPPER.readTree(json);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static JsonNode toJsonNode(InputStream json) {
    try {
      return OBJECT_MAPPER.readTree(json);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static ObjectNode toObjectNode(String json) {
    JsonNode jsonNode = toJsonNode(json);
    if (jsonNode.getNodeType() == JsonNodeType.OBJECT) {
      return (ObjectNode) jsonNode;
    }
    throw new RuntimeException("Json convert to ObjectNode FAILED!");
  }

  public static ObjectNode toObjectNode(InputStream json) {
    JsonNode jsonNode = toJsonNode(json);
    if (jsonNode.getNodeType() == JsonNodeType.OBJECT) {
      return (ObjectNode) jsonNode;
    }
    throw new RuntimeException("Json convert to ObjectNode FAILED!");
  }

  public static ArrayNode toArrayNode(String json) {
    JsonNode jsonNode = toJsonNode(json);
    if (jsonNode.getNodeType() == JsonNodeType.ARRAY) {
      return (ArrayNode) jsonNode;
    }
    throw new RuntimeException("Json convert to ArrayNode FAILED!");
  }

  public static ArrayNode toArrayNode(InputStream json) {
    JsonNode jsonNode = toJsonNode(json);
    if (jsonNode.getNodeType() == JsonNodeType.ARRAY) {
      return (ArrayNode) jsonNode;
    }
    throw new RuntimeException("Json convert to ArrayNode FAILED!");
  }


  public static String format(Object o) {
    try {
      return OBJECT_MAPPER_INDENT.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static void print(Object o) {
    System.out.println(format(o));
  }
}
