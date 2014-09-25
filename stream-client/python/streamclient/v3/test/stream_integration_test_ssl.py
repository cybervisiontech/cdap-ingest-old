#! /usr/bin/env python3
# -*- coding: utf-8 -*-

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

import unittest
import requests

from basicreactor import BasicReactor


class TestStreamClient(unittest.TestCase, StreamTestBase):

    def setUp(self):
        self.config_file = 'cdap_ssl_config.json'
        self.base_set_up()

if '__main__' == __name__:
    unittest.main(warnings=False)