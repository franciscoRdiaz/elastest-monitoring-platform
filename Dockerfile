# Copyright (c) 2017. ZHAW - ICCLab
# All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# Author: Piyush Harsh,
# URL: piyush-harsh.info
#
# Thanks to: https://hub.docker.com/r/frolvlad/alpine-oraclejdk8/
FROM frolvlad/alpine-oraclejdk8
EXPOSE 9000
RUN apk --update add sqlite curl
ADD application.properties application.properties
ADD sentinel-0.1.jar sentinel-0.1.jar
ADD keystore.p12 keystore.p12
ADD init-dashboard.sh init-dashboard.sh
ADD dashboard.json dashboard.json
ADD start.sh start.sh
RUN chmod +x init-dashboard.sh start.sh
CMD ["/bin/ash", "start.sh"]
