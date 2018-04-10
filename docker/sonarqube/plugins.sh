#!/usr/bin/env bash
# Copyright 2018 Red Hat, Inc.
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


set -e
# set -x	## Uncomment for debugging

printf 'Downloading plugin details\n'

sleep 20

curl -L -sS -o /tmp/pluginList.txt https://update.sonarsource.org/update-center.properties
printf "Downloading additional plugins\n"
for PLUGIN in "$@"
do
	printf '\tExtracting plugin download location - %s\n' ${PLUGIN}
	MATCH_STRING=$(cat /tmp/pluginList.txt | grep requiredSonarVersions | grep "[=,]6\.3\(\.1\)\?" | sed 's@\.requiredSonarVersions.*@@g' | sort -V | grep "^${PLUGIN}\." | tail -n 1 | sed 's@$@.downloadUrl@g')

	if ! [[ -z "${MATCH_STRING}" ]]; then
		DOWNLOAD_URL=$(cat /tmp/pluginList.txt | grep ${MATCH_STRING} | awk -F"=" '{print $2}' | sed 's@\\:@:@g')

		## Check to see if plugin exists, attempt to download the plugin if it does exist.
		if ! [[ -z "${DOWNLOAD_URL}" ]]; then
			printf "\t\t%-15s" ${PLUGIN}
			curl -L -sS -o /opt/sonarqube/extensions-init/plugins/${PLUGIN}.jar ${DOWNLOAD_URL} && printf "%10s" "DONE" || printf "%10s" "FAILED"
			printf "\n"
		else
			## Plugin was not found in the plugin inventory
			printf "\t\t%-15s%10s\n" "${PLUGIN}" "NOT FOUND"
		fi
	else
	    ## Build Breaker plugin is no longer listed in Update Center, have to add it by URL
		if [[ "${PLUGIN}" == "buildbreaker" ]]; then
			BUILD_BREAKER_URL=https://github.com/SonarQubeCommunity/sonar-build-breaker/releases/download/2.2/sonar-build-breaker-plugin-2.2.jar
			printf "\t\t%-15s" ${PLUGIN}
			curl -L -sS -o /opt/sonarqube/extensions-init/plugins/${PLUGIN}.jar ${BUILD_BREAKER_URL} && printf "%10s" "DONE" || printf "%10s" "FAILED"
			printf "\n"
		else
			printf "\t\t%-15s%10s\n" $PLUGIN "NOT FOUND"
		fi
	fi
done

rm -f /tmp/pluginList.txt
