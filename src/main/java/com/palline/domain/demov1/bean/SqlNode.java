package com.palline.domain.demov1.bean;

import java.io.Serializable;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author zhangkai
 * @date 2021/8/23 17:15
 */
@Data
@Accessors(chain = true)
public class SqlNode implements Serializable {

  private String sql;
  private String field;
  private String id;
  private String codeName;
}
