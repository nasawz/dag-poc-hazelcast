package com.palline.domain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.PathProvider;
import springfox.documentation.spring.web.paths.DefaultPathProvider;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author zhangkai
 * @date 2021/8/24 13:48
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

  @Value("${spring.webflux.base-path}")
  private String basePath;

  @Bean
  public PathProvider pathProvider() {
    return new DefaultPathProvider() {
      @Override
      public String getOperationPath(String operationPath) {
        return basePath + operationPath;
      }
    };
  }
}
