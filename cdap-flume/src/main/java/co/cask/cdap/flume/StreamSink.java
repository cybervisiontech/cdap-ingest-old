/*
 * Copyright © 2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.flume;

import co.cask.cdap.client.StreamClient;
import co.cask.cdap.client.StreamWriter;
import co.cask.cdap.client.rest.RestStreamClient;
import co.cask.cdap.security.authentication.client.AuthenticationClient;
import co.cask.cdap.security.authentication.client.basic.BasicAuthenticationClient;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.flume.Channel;
import org.apache.flume.ChannelException;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Sink;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.lifecycle.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Properties;

/**
 * CDAP Sink, a Flume sink implementation.
 */
public class StreamSink implements Sink, LifecycleAware, Configurable {

  private static final Logger LOG = LoggerFactory.getLogger(StreamSink.class);

  private static final int DEFAULT_WRITER_POOL_SIZE = 1;

  private static final boolean DEFAULT_SSL = false;

  private static final String DEFAULT_VERSION = "v2";

  private static final int DEFAULT_PORT = 10000;

  private static final String DEFAULT_AUTH_CLIENT = BasicAuthenticationClient.class.getName();

  private String host;
  private Integer port;
  private boolean sslEnabled;
  private int writerPoolSize;
  private String version;
  private String streamName;
  private StreamWriter writer;
  private StreamClient streamClient;
  private String authClientClassName;
  private String authClientPropertiesPath;
  private String  securityAuthServerHost;
  private String  securityAuthServerPort;
  private boolean  authClientSslEnabled;
  private Channel channel;
  private String name;
  private LifecycleState lifecycleState;
  private EventWriteStatusCode writeStatus;

  /**
   * Stream writer result status code
   */
  public static enum EventWriteStatusCode {
    OK, FAIL
  }

  @Override
  public void configure(Context context) {
    host = context.getString("host");
    port = context.getInteger("port", DEFAULT_PORT);
    sslEnabled = context.getBoolean("sslEnabled", DEFAULT_SSL);
    version = context.getString("version", DEFAULT_VERSION);
    writerPoolSize = context.getInteger("writerPoolSize", DEFAULT_WRITER_POOL_SIZE);
    streamName = context.getString("streamName");
    authClientClassName = context.getString("authClientClass", DEFAULT_AUTH_CLIENT);
    authClientPropertiesPath = context.getString("authClientProperties", "");
    Preconditions.checkState(host != null, "No hostname specified");
    Preconditions.checkState(streamName != null, "No stream name specified");

  }

  @Override
  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  @Override
  public Channel getChannel() {
    return channel;
  }

  @Override
  public Status process() throws EventDeliveryException {

    Status status = Status.READY;
    Channel channel = getChannel();
    Transaction transaction = channel.getTransaction();

    try {
      tryReopenClientConnection();
      transaction.begin();
      Event event = channel.take();
      if (event != null) {
        ListenableFuture future = writer.write(ByteBuffer.wrap(event.getBody()), event.getHeaders());
        Futures.addCallback(future, new FutureCallback<Void>() {
          @Override
          public void onSuccess(Void result) {
            writeStatus = EventWriteStatusCode.OK;
            LOG.info("Success write to stream {}", streamName);
          }

          @Override
          public void onFailure(Throwable t) {
            writeStatus = EventWriteStatusCode.FAIL;
            LOG.error("Error during writing event to stream {}", streamName, t);
          }
        });
        future.get();
        if (writeStatus == EventWriteStatusCode.FAIL) {
          throw new EventDeliveryException("Failed to send events to stream: " + streamName);
        }
      }
      transaction.commit();

    } catch (Throwable t) {
      transaction.rollback();
      if (t instanceof Error) {
        throw (Error) t;
      } else if (t instanceof ChannelException) {
        LOG.error("Stream Sink {}: Unable to get event from channel {} ", getName(), channel.getName());
        status = Status.BACKOFF;
      } else {
        LOG.debug("Closing writer due to stream error ", t);
        closeClientQuietly();
        closeWriterQuietly();
        throw new EventDeliveryException("Sink event sending error", t);
      }
    } finally {
      transaction.close();
    }
    return status;
  }


  private void tryReopenClientConnection() throws IOException {
    if (writer == null) {
      LOG.debug("Trying to reopen stream writer {} ", streamName);
      try {
        createStreamClient();
      } catch (IOException e) {
        writer = null;
        LOG.error("Error during reopening client by name: {} for host: {}, port: {}. Reason: {} ",
                  new Object[]{streamName, host, port, e.getMessage(), e});
        throw e;
      }
    }
  }

  private void createStreamClient() throws IOException {

    if (streamClient == null) {
      RestStreamClient.Builder builder = RestStreamClient.builder(host, port);

      builder.ssl(sslEnabled);

      builder.writerPoolSize(writerPoolSize);
      builder.version(version);
      AuthenticationClient authClient = null;
      InputStream inStream = null;
      try {
        authClient = (AuthenticationClient) Class.forName(authClientClassName).newInstance();

        authClient.setConnectionInfo(host, port, sslEnabled);
        Properties properties = new Properties();
        inStream = new FileInputStream(authClientPropertiesPath);
        properties.load(inStream);
        authClient.configure(properties);
        builder.authClient(authClient);
      } catch (ReflectiveOperationException e) {
        LOG.error("Can not resolve class {}: {}", new Object[]{authClientClassName, host, port, e.getMessage(), e});
      } catch (IOException e) {
        LOG.error("Cannot load properties", e);
      } finally {
        try {
          if (inStream != null) {
            inStream.close();
          }
        } catch (IOException e) {
          LOG.warn("Error during closing input stream. {}", e.getMessage(), e);
        }
      }
      streamClient = builder.build();
    }
      try {
        if (writer == null) {
          writer = streamClient.createWriter(streamName);
        }
      } catch (Throwable t) {
        closeWriterQuietly();
        throw new IOException("Can not create stream writer by name: " + streamName, t);
      }

  }

  private void closeClientQuietly() {
    if (streamClient != null) {
      try {
        streamClient.close();
      } catch (Throwable t) {
        LOG.error("Error closing stream client. {}", t.getMessage(), t);
      }
      streamClient = null;
    }
  }

  private void closeWriterQuietly() {
    try {
      if (writer != null) {
        writer.close();
      }
    } catch (Throwable t) {
      LOG.error("Error closing writer. {}", t.getMessage(), t);
    }
    writer = null;
  }

  public synchronized void start() {

    Preconditions.checkState(channel != null, "No channel configured");
    try {
      createStreamClient();
    } catch (Throwable t) {
      LOG.error("Unable to create Stream client by name: {} for host: {}, port: {}. Reason: {} ",
                new Object[]{streamName, host, port, t.getMessage(), t});
      closeWriterQuietly();
      closeClientQuietly();
      lifecycleState = LifecycleState.ERROR;
      return;
    }
    LOG.info("StreamSink {} started.", getName());
    lifecycleState = LifecycleState.START;
  }

  public synchronized void stop() {
    LOG.info("StreamSink {} stopping...", getName());
    closeWriterQuietly();
    closeClientQuietly();
  }

  @Override
  public LifecycleState getLifecycleState() {
    return lifecycleState;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }
}

