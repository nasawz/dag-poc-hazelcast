package com.palline.domain.demov1.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@ApiModel("收入")
public class Income {

  private String id;

  @ApiModelProperty("收入项")
  private String name;

  @ApiModelProperty("金额")
  private BigDecimal amount;

  private LocalDateTime createTime;
}
