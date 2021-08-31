package com.palline.domain.statemachine.dao;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author zhangkai
 * @date 2021/8/24 16:09
 */
@Data
@TableName("job_definition")
@Accessors(chain = true)
public class JobDefinitionEntity {

  @TableId
  private String id;
  private String name;
  private String graphJson;
  private String listenerIds;
}
