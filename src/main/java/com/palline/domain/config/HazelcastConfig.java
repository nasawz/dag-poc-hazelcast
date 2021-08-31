package com.palline.domain.config;

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;
import java.io.InputStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhangkai
 * @date 2021/8/23 15:54
 */
@Configuration
public class HazelcastConfig {

  @Bean
  public JetInstance instance() {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream yamlConfig = classLoader.getResourceAsStream("hazelcast.yml");
    assert yamlConfig != null;
    JetConfig jetConfig = JetConfig.loadYamlFromStream(yamlConfig);
    return Jet.newJetInstance(jetConfig);
  }
}
