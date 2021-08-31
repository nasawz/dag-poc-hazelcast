package com.palline.domain.demov1;

import com.fasterxml.jackson.databind.JsonNode;
import com.palline.domain.demov1.bean.Income;
import com.palline.domain.demov1.bean.JobDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("demo/v1")
@Api(tags = "Demo V1")
public class DemoV1Controller {

  private final DemoJob demoJob;

  public DemoV1Controller(DemoJob demoJob) {
    this.demoJob = demoJob;
  }

  @PostMapping("dynamic_job")
  @ApiOperation("动态Job")
  public void dynamicJob(@RequestBody @ApiParam JsonNode form) {
    demoJob.dynamicJob(form);
  }

  @PostMapping("print_job")
  @ApiOperation("增加打印任务（纯测试）")
  public void createPrintJob(@ApiParam("任务名称") String name) {
    demoJob.createPrintJob(name);
  }

  @GetMapping
  @ApiOperation("获得任务列表")
  public Flux<JobDTO> listDemoJobs() {
    return Flux.fromStream(demoJob.list());
  }

  @DeleteMapping("{jobId}")
  @ApiOperation("取消任务（停止任务）")
  public void cancel(@PathVariable String jobId) {
    demoJob.cancel(jobId);
  }

  @GetMapping("income1")
  @ApiOperation("获得income1表数据")
  public Flux<Income> listIncome1() {
    return Flux.fromStream(demoJob.listIncome1());
  }

  @PostMapping("income1")
  @ApiOperation("增加收入（向income1表插入数据）")
  public void addIncome1(@RequestBody @ApiParam Income income) {
    demoJob.addIncome1(income);
  }

  @PostMapping("sum_income1")
  @ApiOperation("创建任务累加任务（查income1表，sum到 aggregation_sum）")
  public void createSum1Job(@ApiParam("任务名称") String name) {
    demoJob.createSum1Job(name);
  }

  @GetMapping("income2")
  @ApiOperation("获得income2表数据")
  public Flux<Income> listIncome2() {
    return Flux.fromStream(demoJob.listIncome2());
  }

  @PostMapping("income2")
  @ApiOperation("增加收入（向income2表插入数据）")
  public void addIncome2(@RequestBody @ApiParam Income income) {
    demoJob.addIncome2(income);
  }

  @PostMapping("sum_income2")
  @ApiOperation("创建监听任务（监听income2表，如有数据 sum到 aggregation_sum）")
  public void createSum2Job(@ApiParam("任务名称") String name) {
    demoJob.createSum2Job(name);
  }

  @GetMapping("aggregation_sum")
  @ApiOperation("获得aggregation_sum表数据")
  public Flux<Income> listSum() {
    return Flux.fromStream(demoJob.listSum());
  }
}
