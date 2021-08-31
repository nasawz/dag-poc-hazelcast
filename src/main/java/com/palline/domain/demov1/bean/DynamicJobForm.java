package com.palline.domain.demov1.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author zhangkai
 * @date 2021/8/23 16:09
 */
@Data
@ApiModel("输入表单")
public class DynamicJobForm {

  @ApiModelProperty("源SQL")
  private String sourceSql;

  @ApiModelProperty("终SQL")
  private String sinkSql;
}
