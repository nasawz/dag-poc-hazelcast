package com.palline.domain.statemachine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import com.palline.domain.statemachine.bean.Graph;
import com.palline.domain.statemachine.bean.Node;
import com.palline.domain.statemachine.bean.Node.NodeType;
import com.palline.domain.statemachine.dao.JobDefinitionEntity;
import com.palline.domain.statemachine.dao.JobMapper;
import com.palline.domain.util.JsonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhangkai
 * @date 2021/8/25 9:29
 */
@RestController
@Api(tags = "状态机(代表计算过程抽象)")
@RequestMapping("demo/v2")
@Slf4j
public class StateMachineController {

  @Autowired
  private JobMapper jobMapper;
  @Autowired
  private JetInstance jetInstance;

  @PostMapping("job/deploy")
  @ApiOperation("保存状态机")
  @ApiImplicitParam(name = "body", value = "{}")
  @Transactional(rollbackFor = Throwable.class)
  public void saveStateMachine(@RequestBody String graphJson) {
    Graph graph = Graph.from(graphJson);
    JobDefinitionEntity foundJobDef = jobMapper.selectById(graph.getId());
    if (foundJobDef == null) {
      // 插入定义
      jobMapper.insert(new JobDefinitionEntity()
          .setId(graph.getId())
          .setName(graph.getName())
          .setGraphJson(graphJson));
    } else {
      // 更新定义
      jobMapper.updateById(foundJobDef
          .setName(graph.getName())
          .setGraphJson(graphJson));
    }
    jetInstance.getMap(graph.getId()).clear();
    deploy(graph.getId());
  }

  private void deploy(String statemachineId) {
    JobDefinitionEntity foundJobDef = jobMapper.selectById(statemachineId);
    if (foundJobDef == null) {
      throw new RuntimeException("没有该[graph]数据");
    } else {
      // 去掉上次监听
      tryRemoveListeners(foundJobDef.getListenerIds());
      // 解析定义
      Graph graph = Graph.from(foundJobDef.getGraphJson());
      // 监听节点
      Map<String, UUID> listenerIds = new HashMap<>();
      graph.getNodes().forEach(selfDef -> {
        if (selfDef.getType() == NodeType.CONSTANT) {
          selfDef.getNextNodes().forEach(next ->
              jetInstance.getMap(next.getId()).put(selfDef.getKey(), selfDef.getDefaultValue()));
        }
        IMap<String, Object> selfMap = jetInstance.getMap(selfDef.getId());
        EntryChangeListener entryChangeListener = event -> {
          if (event.getKey().equals(selfDef.getKey())) {
            // 防止自己设置自己，造成无限循环
            return;
          }
          Object result = event.getValue();
          if (selfDef.getExpress() != null) {
            ExpressionParser parser = new SpelExpressionParser();
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariables(selfMap);
            result = parser.parseExpression(selfDef.getExpress()).getValue(context);
            log.info("Execute express: {} --> Result: {}", selfDef.getExpress(), result);
          }
          if (result != null) {
            selfMap.put(selfDef.getKey(), result);
            out(foundJobDef.getId(), selfDef.getId(), result);
            for (Node nextNode : selfDef.getNextNodes()) {
              log.info("Send [{}:{}] from: {} to: {}",
                  selfDef.getKey(), result, selfDef.getId(), nextNode.getId());
              jetInstance.getMap(nextNode.getId()).put(selfDef.getKey(), result);
            }
          }
        };
        UUID listenerId = selfMap.addEntryListener(entryChangeListener, true);
        log.info("Add listener: {} to {}", listenerId, selfDef.getId());
        listenerIds.put(selfDef.getId(), listenerId);
      });
      foundJobDef.setListenerIds(JsonUtils.toJson(listenerIds));
      jobMapper.updateById(foundJobDef);
    }
  }

  @PutMapping("job/{statemachineId}/execute")
  @ApiOperation("部署状态机")
  @Transactional(rollbackFor = Throwable.class)
  public void execStateMachine(@PathVariable String statemachineId) {
    JobDefinitionEntity foundJob = jobMapper.selectById(statemachineId);
    Graph graph = Graph.from(foundJob.getGraphJson());
    graph.getNodes().stream()
        .filter(node -> node.getType() == NodeType.CONSTANT)
        .forEach(
            node -> jetInstance.getMap(node.getId()).put(node.getId(), node.getDefaultValue()));
  }

  private void tryRemoveListeners(String listenerIds) {
    try {
      TypeReference<Map<String, UUID>> mapTypeReference = new TypeReference<Map<String, UUID>>() {
      };
      Map<String, UUID> ids = JsonUtils.toBean(listenerIds, mapTypeReference);
      ids.forEach((k, v) -> jetInstance.getMap(k).removeEntryListener(v));
    } catch (Throwable e) {
      log.error("err when remove listeners", e);
    }
  }

  private void out(String jobId, String nodeId, Object value) {
    jetInstance.getMap(jobId).put(nodeId, value);
  }

  interface EntryChangeListener extends EntryUpdatedListener<String, Object>,
      EntryAddedListener<String, Object> {

    void changed(EntryEvent<String, Object> event);

    @Override
    default void entryUpdated(EntryEvent<String, Object> event) {
      changed(event);
    }

    @Override
    default void entryAdded(EntryEvent<String, Object> event) {
      changed(event);
    }
  }
}
