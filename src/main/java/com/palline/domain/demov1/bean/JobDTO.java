package com.palline.domain.demov1.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@ApiModel("任务(工作Job)")
public class JobDTO {

  private String id;

  @ApiModelProperty("名称")
  private String name;

  @ApiModelProperty("状态")
  private String state;
}
