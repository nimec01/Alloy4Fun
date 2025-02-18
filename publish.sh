#!/usr/bin/env zsh

set -eux

# Pack api

TEMP_API_DIR=$(mktemp -d)

cp alloy4fun-api.jar $TEMP_API_DIR

echo "/usr/bin/java -Djava.net.preferIPv4Stack=true -Dthorntail.http.port=\${PORT:=8080} -jar alloy4fun-api.jar" > $TEMP_API_DIR/start.sh

chmod +x $TEMP_API_DIR/start.sh

mkdir -p $TEMP_API_DIR/config

cp config/api.keter.yaml $TEMP_API_DIR/config/keter.yaml

tar -C $TEMP_API_DIR -czf alloy4fun-api.keter .

rm -rf $TEMP_API_DIR alloy4fun-api.jar

# Pack meteor

TEMP_METEOR_DIR=$(mktemp -d)

cp -r meteor-dist/* $TEMP_METEOR_DIR

echo "/usr/bin/node /opt/alloy4fun-meteor/bundle/main.js" > $TEMP_METEOR_DIR/start.sh

chmod +x $TEMP_METEOR_DIR/start.sh

mkdir -p $TEMP_METEOR_DIR/config

cp config/meteor.keter.yaml $TEMP_METEOR_DIR/config/keter.yaml

tar -C $TEMP_METEOR_DIR -czf alloy4fun-meteor.keter .

rm -rf $TEMP_METEOR_DIR meteor-dist/

# Deploy (assumes that java-8 and Node.js v14 are installed on the server and included in PATH)

USER=
GROUP=${USER}
SSH_USER=${USER}
SERVER=
JUMP_HOST=
PORT=22

API_KET=alloy4fun-api.keter
API_TARGET_FOLDER=/opt/alloy4fun-api

METEOR_KET=alloy4fun-meteor.keter
METEOR_TARGET_FOLDER=/opt/alloy4fun-meteor

LC_ALL=C

TIME=`date +"%Y-%m-%d-%H.%M.%S"`
if ! [[ -v REMOTE_DIR ]]
then
  API_REMOTE_DIR=${API_TARGET_FOLDER}${TIME}
  METEOR_REMOTE_DIR=${METEOR_TARGET_FOLDER}${TIME}
fi

ssh ${JUMP_HOST:+-J} ${JUMP_HOST:+"${JUMP_HOST}"}\
  -p "${PORT}" "${SSH_USER}@${SERVER}" -C "mkdir -p ${API_REMOTE_DIR} ${METEOR_REMOTE_DIR}"

scp ${JUMP_HOST:+-J} ${JUMP_HOST:+"${JUMP_HOST}"}\
  -P "${PORT}" "$API_KET" "${SSH_USER}@${SERVER}:${API_REMOTE_DIR}/$API_KET"

scp ${JUMP_HOST:+-J} ${JUMP_HOST:+"${JUMP_HOST}"}\
  -P "${PORT}" "$METEOR_KET" "${SSH_USER}@${SERVER}:${METEOR_REMOTE_DIR}/$METEOR_KET"

ssh ${JUMP_HOST:+-J} ${JUMP_HOST:+"${JUMP_HOST}"}\
  -p "${PORT}" "${SSH_USER}@${SERVER}" -C "set -x trace\
  && mkdir ${METEOR_REMOTE_DIR}/meteor\
  && cd ${METEOR_REMOTE_DIR}/meteor\
  && tar xf ../${METEOR_KET}\
  && cd ${METEOR_REMOTE_DIR}/meteor/bundle/programs/server\
  && npm install --only=production\
  && chown -R ${USER}:${GROUP} ${METEOR_REMOTE_DIR}\
  && rm -rf ${METEOR_REMOTE_DIR}/${METEOR_KET} ${METEOR_TARGET_FOLDER}\
  && ln -sf ${METEOR_REMOTE_DIR}/meteor ${METEOR_TARGET_FOLDER}\
  && tar czf ${METEOR_REMOTE_DIR}/${METEOR_KET} -C ${METEOR_REMOTE_DIR}/meteor config/ start.sh\
  && rm -rf ${METEOR_REMOTE_DIR}/meteor/config ${METEOR_REMOTE_DIR}/meteor/start.sh\
  && chown -R ${USER}:${GROUP} ${METEOR_REMOTE_DIR}/${METEOR_KET}\
  && mv ${METEOR_REMOTE_DIR}/${METEOR_KET} /opt/keter/incoming/\
  && mv ${API_REMOTE_DIR}/${API_KET} /opt/keter/incoming/\
  && service keter restart"
