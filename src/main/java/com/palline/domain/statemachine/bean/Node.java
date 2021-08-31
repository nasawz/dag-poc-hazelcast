package com.palline.domain.statemachine.bean;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class Node {

  private String id;
  private String name;
  private String express;         // 表达式

  private NodeType type = NodeType.UNKNOWN;
  private Double defaultValue = 0.0;

  public String getKey() {
    return "s_" + id;
  }

  private final List<Node> nextNodes = new ArrayList<>();

  private Node() {
  }

  static Node from(JsonNode nodeJson) {
    Node node = new Node();
    node.id = nodeJson.get("id").asText();
    node.name = nodeJson.get("name").asText();
    JsonNode codeName = nodeJson.get("codeName");
    if (codeName != null) {
      String codeNameStr = codeName.asText();
      if (codeNameStr.equals("constant")) {
        node.type = NodeType.CONSTANT;
        if (nodeJson.get("data") != null && nodeJson.get("data").get("constant") != null) {
          node.defaultValue = nodeJson.get("data").get("constant").asDouble();
        }
        //} else if (codeNameStr.equals("express")) {
      }

      //express
      //data:{express:""}
      //constant
      //data:{constant:0.0}
    }

    JsonNode data = nodeJson.get("data");
    JsonNode nodeExp = data.get("express");
    if (nodeExp != null) {
      node.express = nodeExp.asText();
    }
    return node;
  }

  public static enum NodeType {
    VARIABLE,
    CONSTANT,
    UNKNOWN,
  }
}
