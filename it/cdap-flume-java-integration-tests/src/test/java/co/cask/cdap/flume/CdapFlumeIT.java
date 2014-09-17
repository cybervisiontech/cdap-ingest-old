
/*
 * Copyright © 2014 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package co.cask.cdap.flume;

import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.agent.embedded.EmbeddedAgent;
import org.apache.flume.agent.embedded.EmbeddedAgentConfiguration;
import org.apache.flume.event.SimpleEvent;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class CdapFlumeIT {

  private static final int eventNumber = 100;
  private static final String CONFIG_NAME = "cdapFlumeITConfig";

  @Test
  public void baseEventProcessingTest() throws Exception {

    EmbeddedAgent agent = new EmbeddedAgent("test-flume");
    Properties properties = getProperties(System.getProperty(CONFIG_NAME));
    Map propertyMap = new HashMap<String, String>();
    for (final String name: properties.stringPropertyNames()) {
      propertyMap.put(name, properties.getProperty(name));
    }

    agent.configure(propertyMap);
    agent.start();
    for (int i = 0; i < eventNumber; i++) {
      Event event = new SimpleEvent();
      agent.put(event);
    }
      agent.stop();
  }
  @Test(expected = EventDeliveryException.class)
  public void failEventProcessingTest() throws Exception {

    EmbeddedAgent agent = new EmbeddedAgent("test-flume");
    Properties properties = getProperties(System.getProperty(CONFIG_NAME));
    properties.setProperty("sink1.port", "11111");
    Map propertyMap = new HashMap<String, String>();
    for (final String name: properties.stringPropertyNames()) {
      propertyMap.put(name, properties.getProperty(name));
    }

    agent.configure(propertyMap);
    agent.start();
    for (int i = 0; i < eventNumber; i++) {
      Event event = new SimpleEvent();
      agent.put(event);
    }
    agent.stop();
  }


  @Before
  public void setUP() throws Exception {
    Field field = EmbeddedAgentConfiguration.class.getDeclaredField("ALLOWED_SINKS");
    field.setAccessible(true);

    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    String []sinkList = {"co.cask.cdap.flume.StreamSink"};
    field.set(null, sinkList);
}

  private Properties getProperties(String fileName) throws IOException {
    Properties properties = new Properties();
    properties.load(CdapFlumeIT.class.getClassLoader().getResourceAsStream(fileName));
    return properties;
  }
}
