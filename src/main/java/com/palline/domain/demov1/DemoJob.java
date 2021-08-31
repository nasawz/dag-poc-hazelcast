package com.palline.domain.demov1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.test.TestSources;
import com.palline.domain.demov1.bean.Income;
import com.palline.domain.demov1.bean.JobDTO;
import com.palline.domain.demov1.bean.SqlNode;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DemoJob implements Serializable {

  private final Properties props = new Properties();
  private KafkaProducer<String, String> producer;

  @Value("${jet.kafka.bootstrap_servers}")
  private String kafkaBootstrapServers;
  @Value("${jet.jdbc.url}")
  private String jdbcUrl;

  @PostConstruct
  public void init() {
    props.setProperty("bootstrap.servers", kafkaBootstrapServers);
    props.setProperty("key.serializer", StringSerializer.class.getCanonicalName());
    props.setProperty("key.deserializer", StringDeserializer.class.getCanonicalName());
    props.setProperty("value.serializer", StringSerializer.class.getCanonicalName());
    props.setProperty("value.deserializer", StringDeserializer.class.getCanonicalName());
    producer = new KafkaProducer<>(props);
  }


  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(SerializationFeature.INDENT_OUTPUT, true);
  private static final Map<String, Job> demoJobStore = new ConcurrentHashMap<>();
  private final JetInstance jetInstance;
  private final JdbcTemplate jdbcTemplate;

  public DemoJob(JetInstance jetInstance, JdbcTemplate jdbcTemplate) {
    this.jetInstance = jetInstance;
    this.jdbcTemplate = jdbcTemplate;
  }

  // 管道任务
  public void createPrintJob(String name) {
    if (name == null || name.trim().equals("")) {
      throw new RuntimeException("任务名称不能为空");
    }
    Pipeline pipeline = Pipeline.create();
    pipeline.readFrom(TestSources.itemStream(2))
        .withoutTimestamps()
        .writeTo(Sinks.logger());
    Job job = jetInstance.newJob(pipeline, new JobConfig().setName(name));
    demoJobStore.put(job.getIdString(), job);
  }

  public void createSum1Job(String name) {
    BatchSource<Income> source = Sources.jdbc(
        jdbcUrl, "select * from income1",
        rs -> new Income()
            .setId(rs.getString("id"))
            .setName(rs.getString("name"))
            .setAmount(rs.getBigDecimal("amount")));

    Sink<BigDecimal> sink = Sinks
        .jdbc("insert into aggregation_sum(id,name,amount,create_time) values(?,?,?,?)", jdbcUrl,
            (PreparedStatement stmt, BigDecimal sum) -> {
              stmt.setString(1, UUID.randomUUID().toString());
              stmt.setString(2, "income1");
              stmt.setBigDecimal(3, sum);
              stmt.setTimestamp(4, Timestamp.from(Instant.now()));
            });

    Pipeline pipeline = Pipeline.create();

    pipeline.readFrom(source)
        .map(Income::getAmount)
        .aggregate(AggregateOperations.summingDouble(BigDecimal::doubleValue))
        .map(BigDecimal::valueOf)
        .writeTo(sink);

    Job job = jetInstance.newJob(pipeline, new JobConfig().setName(name));
    demoJobStore.put(job.getIdString(), job);
  }

  public void createSum2Job(String name) {

    StreamSource<Entry<String, String>> source = KafkaSources.kafka(props, "income2");
    BigDecimal latestAmount = jdbcTemplate.queryForObject(
        "select amount from aggregation_sum order by create_time desc limit 1",
        BigDecimal.class);

    Sink<BigDecimal> sink = Sinks.jdbc(
        "insert into aggregation_sum(id,name,amount,create_time) "
            + "values(?,?,?,?)",
        jdbcUrl, (PreparedStatement stmt, BigDecimal sum) -> {
          stmt.setString(1, UUID.randomUUID().toString());
          stmt.setString(2, "income2");
          stmt.setBigDecimal(3, latestAmount.add(sum));
          stmt.setTimestamp(4, Timestamp.from(Instant.now()));
        });

    Pipeline pipeline = Pipeline.create();
    pipeline.readFrom(source)
        .withoutTimestamps()
        .map(e -> BigDecimal.valueOf(Double.parseDouble(e.getValue())))
        .writeTo(sink);
    Job job = jetInstance.newJob(pipeline, new JobConfig().setName(name));
    demoJobStore.put(job.getIdString(), job);
  }

  // 展示执行任务
  public Stream<JobDTO> list() {
    return demoJobStore.values().stream()
        .map(j -> new JobDTO()
            .setId(j.getIdString())
            .setName(j.getName())
            .setState(j.getStatus().name()));
  }

  public void addIncome1(Income income) {
    String id = UUID.randomUUID().toString();
    jdbcTemplate
        .update("insert into income1(id,name,amount,create_time) values (?,?,?,?)",
            id, income.getName(), income.getAmount(), Timestamp.from(Instant.now()));
  }

  public void addIncome2(Income income) {
    String id = UUID.randomUUID().toString();
    jdbcTemplate
        .update("insert into income2(id,code,cost,create_time,update_time) values (?,?,?,?,?)",
            id, income.getName().hashCode(), income.getAmount(),
            Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));

    ProducerRecord<String, String> record = new ProducerRecord<>("income2", id,
        income.getAmount().toString());
    producer.send(record);
  }

  // 取消任务
  public void cancel(String id) {
    Job job = demoJobStore.get(id);
    if (job == null) {
      throw new RuntimeException("Job: " + id + " 不存在");
    }
    demoJobStore.remove(id);
    job.cancel();
  }

  public static void main(String[] args) {
    System.out.println(UUID.randomUUID());
  }

  public Stream<Income> listIncome1() {
    return jdbcTemplate.query("select * from income1 order by create_time desc",
        (rs, i) -> new Income()
            .setId(rs.getString("id"))
            .setName(rs.getString("name"))
            .setAmount(rs.getBigDecimal("amount"))
            .setCreateTime(rs.getTimestamp("create_time").toLocalDateTime())).stream();
  }

  public Stream<Income> listIncome2() {
    return jdbcTemplate.query("select * from income2 order by create_time desc",
        (rs, i) -> new Income()
            .setId(rs.getString("id"))
            .setName("" + rs.getInt("code"))
            .setAmount(rs.getBigDecimal("cost"))
            .setCreateTime(rs.getTimestamp("create_time").toLocalDateTime())).stream();
  }

  public Stream<Income> listSum() {
    return jdbcTemplate.query("select * from aggregation_sum order by create_time desc",
        (rs, i) -> new Income()
            .setId(rs.getString("id"))
            .setName(rs.getString("name"))
            .setAmount(rs.getBigDecimal("amount"))
            .setCreateTime(rs.getTimestamp("create_time").toLocalDateTime())).stream();
  }

  private static String toJson(Object o) {
    try {
      return OBJECT_MAPPER.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void dynamicJob(JsonNode form) {
    JsonNode nodes = form.get("graph").get("nodes");
    Pipeline pipeline = Pipeline.create();
    List<BatchStage<BigDecimal>> stages = new ArrayList<>();
    for (JsonNode node : nodes) {
      SqlNode sqlNode = tryGetSql(node);
      if (sqlNode == null) {
        continue;
      }
      BatchSource<BigDecimal> jdbc = Sources.jdbc(jdbcUrl, sqlNode.getSql(),
          rs -> rs.getBigDecimal(sqlNode.getField()));
      BatchStage<BigDecimal> stage = pipeline.readFrom(jdbc);
      stage.writeTo(Sinks.logger());
      stages.add(stage);
    }
    stages.stream().reduce(BatchStage::merge).ifPresent(
        st -> st.aggregate(AggregateOperations.summingDouble(BigDecimal::doubleValue))
            .writeTo(Sinks.map(jetInstance.getMap("test_table_field"), d -> "sum", d -> d)));
    jetInstance.newJob(pipeline);
  }

  public SqlNode tryGetSql(JsonNode node) {
    try {
      String[] split = node.get("data").get("table_field").asText().split("\\.");
      if (split.length != 2) {
        return null;
      }
      return new SqlNode()
          .setSql("select " + split[1] + " from " + split[0])
          .setField(split[1]);
    } catch (Throwable ignore) {
      return null;
    }
  }
}

