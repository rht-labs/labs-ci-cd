#!/bin/bash
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


######################################################################################
#
# The objective of this script is to:
# 1) make the docker run command easier to use.
# 2) run ansible-galaxy install if required roles are missing.
# 3) print ASCII art.
# 
# Please do not introduce any other logic
# 
######################################################################################
set -e

function printBanner(){
    # fpr info on colors, see https://stackoverflow.com/questions/5947742/how-to-change-the-output-color-of-echo-in-linux
    LIGHT_BLUE='\033[1;34m'
    GREEN='\033[0;32m'
    RED='\033[0;31m'
    WHITE='\033[1;37m'
    NC='\033[0m'

    printf "
    ${RED} _          _          ${LIGHT_BLUE}  ____ ___ ${WHITE}    __${GREEN} ____ ____   \n\
    ${RED}| |    __ _| |__  ___  ${LIGHT_BLUE} / ___|_ _|${WHITE}   / /${GREEN}/ ___|  _ \  \n\
    ${RED}| |   / _' | '  \/ __| ${LIGHT_BLUE}| |    | | ${WHITE}  / /${GREEN}| |   | | | | \n\
    ${RED}| |__| (_| | |_) \__ \ ${LIGHT_BLUE}| |___ | | ${WHITE} / / ${GREEN}| |___| |_| | \n\
    ${RED}|_____\__,_|_.__/|___/ ${LIGHT_BLUE} \____|___|${WHITE}/_/  ${GREEN} \____|____/  \n\
    \n\n${NC}"
}

###########################
# Begin Script            #
###########################
printBanner

ansible-galaxy install -r requirements.yml --roles-path=galaxy

if [[ "$#" == 0 ]]; then
    DOCKER_RUN_COMMAND='ansible-playbook -i /tmp/src/inventory /tmp/src/galaxy/openshift-applier/playbooks/openshift-cluster-seed.yml'
    printf "no arguments passed to run.sh. using default docker run command:\n- $DOCKER_RUN_COMMAND\n\n"
else
    DOCKER_RUN_COMMAND="$@"
    printf "using arguments passed to run.sh for docker run command:\n\t$DOCKER_RUN_COMMAND\n\n"
    
fi 

docker run --rm -i \
    -v $(pwd):/tmp/src:z \
    -v $HOME/.kube:/root/.kube:z \
    -t redhatcop/openshift-applier \
    $DOCKER_RUN_COMMAND


