#  Copyright © 2014 Cask Data, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License"); you may not
#  use this file except in compliance with the License. You may obtain a copy of
#  the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations under
#  the License.

import json


class Config:

    def __init__(self):
        self.__host = 'localhost'
        self.__port = 10000
        self.__ssl = False
        self.__authClient = None

    def set_auth_client(self, client):
        self.__authClient = client

    @property
    def host(self):
        return self.__host

    @host.setter
    def host(self, hostname):
        self.__host = hostname

    @property
    def port(self):
        return self.__port

    @port.setter
    def port(self, port):
        self.__port = port

    @property
    def ssl(self):
        return self.__ssl

    @ssl.setter
    def ssl(self, ssl):
        self.__ssl = ssl

    @property
    def auth_token(self):
        try:
            return self.__authClient.get_access_token()
        except IOError:
            return u''

    def read_from_file(filename):
        newConfig = Config()
        jsonConfig = None

        with open(file) as configFile:
            jsonConfig = json.loads(configFile.read())

        newConfig.host = jsonConfig['hostname']
        newConfig.port = jsonConfig['port']
        newConfig.ssl = jsonConfig['SSL']

        authClient = BasicAuthenticationClient()
        authClient.set_connection_info(newConfig.host, newConfig.port,
                                              newConfig.ssl)
        authClient.configure(filename)

        newConfig.set_auth_client(authClient)

        return newConfig
