/*
 * Copyright 2014 Cask Data, Inc.
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

package co.cask.cdap.client.rest;

import co.cask.cdap.security.authentication.client.AuthenticationClient;

import java.io.IOException;

/**
 * Container for REST client configuration properties.
 */
public class RestClientConnectionConfig {

  private final String host;
  private final int port;
  private final AuthenticationClient authClient;
  private final String apiKey;
  private final boolean ssl;
  private final String version;

  public RestClientConnectionConfig(String host, int port, AuthenticationClient authClient, String apiKey,
                                    boolean ssl, String version) {
    this.host = host;
    this.port = port;
    this.authClient = authClient;
    this.apiKey = apiKey;
    this.ssl = ssl;
    this.version = version;
  }

  public String getHost() {
    return host;
  }

  public String getVersion() {
    return version;
  }

  public boolean isSSL() {
    return ssl;
  }

  public String getAPIKey() {
    return apiKey;
  }

  public int getPort() {
    return port;
  }

  public boolean isAuthEnabled() throws IOException {
    return authClient != null && authClient.isAuthEnabled();
  }

  public String getAuthTokenType() throws IOException {
    return authClient.getAccessToken().getTokenType();
  }

  public String getAuthToken() throws IOException {
    return authClient.getAccessToken().getValue();
  }
}
