package com.palline.domain.demov2;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author zhangkai
 * @date 2021/8/24 17:32
 */
@Data
@Accessors(chain = true)
public class NodeAndAmount implements Serializable {

  private String id;
  private BigDecimal amount;
}
