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

package co.cask.cdap.filetailer.config;

import co.cask.cdap.filetailer.config.exception.ConfigurationLoadingException;

import java.io.File;

/**
 * ConfigurationLoader is designed for loading properties from specified file.
 */
public interface ConfigurationLoader {

  /**
   * Initialize configuration loader with properties from configuration file.
   *
   * @param file the configuration file
   * @return configuration loaded from file with specified path
   * @throws co.cask.cdap.filetailer.config.exception.ConfigurationLoadingException if error occurred
   *                                      (for example, file not exists)
   */
  Configuration load(File file) throws ConfigurationLoadingException;
}
