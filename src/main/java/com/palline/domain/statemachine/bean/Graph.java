package com.palline.domain.statemachine.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.palline.domain.util.JsonUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * @author zhangkai
 * @date 2021/8/25 14:03
 */
@Getter
public class Graph {

  private String id;
  private String name;
  private final List<Node> nodes = new ArrayList<>();
  private final List<Link> links = new ArrayList<>();

  @JsonIgnore
  private final Map<String, Node> nodeDict = new HashMap<>();

  private Graph() {
  }

  public static Graph from(String json) {
    return from(JsonUtils.toJsonNode(json));
  }

  static Graph from(JsonNode json) {
    try {
      Graph graph = new Graph();
      graph.id = json.get("id").asText();
      graph.name = json.get("name").asText();
      JsonNode graphJson = json.get("graph");

      // nodes
      JsonNode nodesJson = graphJson.get("nodes");
      for (JsonNode nodeJson : nodesJson) {
        Node node = Node.from(nodeJson);
        graph.nodes.add(node);
        graph.nodeDict.put(node.getId(), node);
      }

      // links
      JsonNode linksJson = graphJson.get("links");
      for (JsonNode linkJson : linksJson) {
        Link link = new Link(linkJson);
        graph.links.add(link);
      }

      //添加链接关系
      graph.links.forEach(link -> {
        Node source = graph.nodeDict.get(link.getSource());
        Node target = graph.nodeDict.get(link.getTarget());
        source.getNextNodes().add(target);
      });
      return graph;
    } catch (Throwable e) {
      throw new RuntimeException("获得图失败！", e);
    }
  }
}

@Getter
class Link {

  private final String source;
  private final String target;

  Link(JsonNode link) {
    source = link.get("source").asText();
    target = link.get("target").asText();
  }

}

