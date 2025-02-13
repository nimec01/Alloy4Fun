
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

