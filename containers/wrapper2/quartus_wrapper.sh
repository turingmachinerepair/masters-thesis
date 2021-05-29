#!/bin/sh

PROJECT=$1
PROJECTNAME=$2
FLOW=$3
UUID=$4
RESPONSE_ADDR=$5

echo "STEP 0 - define locals"
echo "ARGS:"
echo "PROJECT: ${PROJECT}"
echo "PROJECTNAME: ${PROJECTNAME}"
echo "FLOW: ${FLOW}"
echo "UUID: ${UUID}"
echo "RESPONSE_ADDR ${RESPONSE_ADDR}"
QUARTUS_DIR=/opt/quartus/quartus
LOGS=/prototype_root/logs
SRC_DIR=/prototype_root
RES_DIR=/prototype_root/results/${PROJECTNAME}-${UUID}

echo "LOGS: ${LOGS}"
echo "SRC_DIR: ${SRC_DIR}"
echo "RES_DIR: ${RES_DIR}"
echo "QUARTUS_DIR: ${QUARTUS_DIR}"

echo "STEP 1 - make local copy of project and link against sources"

echo "STEP 1.1 - make local copies of critical stuff"
mkdir -p ${LOGS}
mkdir -p ${RES_DIR}
mkdir -p /quartus-wd/processor_test/FPGA/
mkdir -p /quartus-wd/library
cp -r ${PROJECT} /quartus-wd/processor_test/FPGA/
cp -r ${SRC_DIR}/library /quartus-wd/library

chmod 777 -R /quartus-wd/


echo "STEP 1.2 - link non-critical stuff"
ln -s  ${SRC_DIR}/parcing /quartus-wd/parcing

echo "STEP 2 - execute flow"
cd /quartus-wd/processor_test/FPGA/${PROJECTNAME}/

pwd
ls -la
head ${PROJECTNAME}.qsf

${FLOW} >> ${LOGS}/${UUID}-oplog.log
RES=$?

echo "STEP 3 - copy results"

mkdir -p ${RES_DIR}
cp -r /quartus-wd/processor_test/FPGA/${PROJECTNAME}/* ${RES_DIR}
if [[ $RES -eq 0 ]];
then
	echo "Update working copy"
	cp -r /quartus-wd/processor_test/FPGA/${PROJECTNAME}/* ${PROJECT}
fi

curl --header "Content-Type: application/json" \
  --request POST \
  --data ${UUID} \
  ${RESPONSE_ADDR}/task_finished

exit $RES
