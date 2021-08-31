package com.palline.domain.demov2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.palline.domain.demov1.bean.SqlNode;
import com.palline.domain.statemachine.dao.JobDefinitionEntity;
import com.palline.domain.statemachine.dao.JobMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhangkai
 * @date 2021/8/24 16:00
 */
@RestController
@RequestMapping("deprecated/demo/v2")
@Api(tags = "Demo V2")
@Deprecated
public class DemoV2Controller {

  @Value("${jet.jdbc.url}")
  private String jdbcUrl;

  @Autowired
  private JetInstance jetInstance;
  @Autowired
  private JobMapper jobMapper;

  @PostMapping("job/deploy")
  @ApiOperation("保存job")
  @ApiImplicitParams(value = {@ApiImplicitParam(name = "body", value = "{\n"
      + "  \"id\": \"2234\",\n"
      + "  \"name\": \"测试Job\",\n"
      + "  \"graph\": {\n"
      + "    \"nodes\": [\n"
      + "      {\n"
      + "        \"id\": \"1629706593196\",\n"
      + "        \"data\": {\n"
      + "          \"table_field\": \"income.amount\"\n"
      + "        }\n"
      + "      },\n"
      + "      {\n"
      + "        \"id\": \"1629706593196\",\n"
      + "        \"data\": {\n"
      + "          \"table_field\": \"income.amount2\"\n"
      + "        }\n"
      + "      }\n"
      + "    ]\n"
      + "  }\n"
      + "}"),})
  public void saveJob(@RequestBody JsonNode form) {
    String id = form.get("id").asText();
    String name = form.get("name").asText();
    String graph = form.get("graph").toString();
    JobDefinitionEntity foundJob = jobMapper.selectById(id);
    if (foundJob == null) {
      jobMapper.insert(new JobDefinitionEntity().setId(id).setName(name).setGraphJson(graph));
    } else {
      jobMapper.updateById(foundJob.setName(name).setGraphJson(graph));
    }
    jetInstance.getMap(id).clear();
    jetInstance.getMap(id).put("status", "ready");
  }

  @PutMapping("job/{jobId}/execute")
  @ApiOperation("执行job")
  public void execJob(@ApiParam("job id") @PathVariable String jobId) {
    jetInstance.getMap(jobId).clear();
    jetInstance.getMap(jobId).put("status", "running");
    try {
      Thread.sleep(1500); // 价值1500万的优化代码
      Optional.of(jobMapper.selectById(jobId)).ifPresent(definition -> {
        ObjectNode graph = JsonNodeFactory.instance.objectNode();
        graph.put("graph", getJsonNode(definition.getGraphJson()));
        execJob(jobId, graph);
      });
    } catch (Throwable e) {
      jetInstance.getMap(jobId).put("status", "fail");
      throw new RuntimeException(e);
    }
    jetInstance.getMap(jobId).put("status", "success");
  }

  private void execJob(String jobId, JsonNode form) {
    JsonNode nodes = form.get("graph").get("nodes");
    Pipeline pipeline = Pipeline.create();
    List<BatchStage<NodeAndAmount>> stages = new ArrayList<>();
    String sumId = "sum";
    for (JsonNode node : nodes) {
      String sum = tryGetResultNode(node);
      if (sum != null) {
        sumId = sum;
      }
    }
    for (JsonNode node : nodes) {
      SqlNode sqlNode = tryGetSql(node);
      if (sqlNode == null) {
        continue;
      }
      BatchSource<NodeAndAmount> jdbc = Sources.jdbc(jdbcUrl, sqlNode.getSql(),
          rs -> new NodeAndAmount().setAmount(rs.getBigDecimal(sqlNode.getField()))
              .setId(sqlNode.getId()));
      BatchStage<NodeAndAmount> stage = pipeline.readFrom(jdbc);
      stage.map(NodeAndAmount::getAmount)
          .aggregate(AggregateOperations.summingDouble(BigDecimal::doubleValue))
          .writeTo(Sinks.map(jetInstance.getMap(jobId), d -> sqlNode.getId(), d -> d));
      stages.add(stage);
    }
    String finalSumId = sumId;
    stages.stream().reduce(BatchStage::merge).ifPresent(
        st -> st.map(NodeAndAmount::getAmount)
            .aggregate(AggregateOperations.summingDouble(BigDecimal::doubleValue))
            .writeTo(Sinks.map(jetInstance.getMap(jobId), d -> finalSumId, d -> d)));
    jetInstance.newJob(pipeline);
  }

  private SqlNode tryGetSql(JsonNode node) {
    try {
      String[] split = node.get("data").get("table_field").asText().split("\\.");
      if (split.length != 2) {
        return null;
      }
      return new SqlNode()
          .setSql("select " + split[1] + " from " + split[0])
          .setField(split[1])
          .setId(node.get("id").asText());
    } catch (Throwable ignore) {
      return null;
    }
  }

  private String tryGetResultNode(JsonNode node) {
    try {
      if (node.get("codeName").asText().equals("source_sum")) {
        return node.get("id").asText();
      }
    } catch (Throwable ignore) {
    }
    return null;
  }

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private JsonNode getJsonNode(String json) {
    try {
      return objectMapper.readValue(json, JsonNode.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

}
