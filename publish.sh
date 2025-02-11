
# Pack api

TEMP_API_DIR=$(mktemp -d)

cp alloy4fun-api.jar $TEMP_API_DIR

echo "/usr/bin/java -Djava.net.preferIPv4Stack=true -Dthorntail.http.port=\${PORT:=8080} -jar alloy4fun-api.jar" > $TEMP_API_DIR/start.sh

chmod +x $TEMP_API_DIR/start.sh

mkdir -p $TEMP_API_DIR/config

cp config/api.keter.yaml $TEMP_API_DIR/config/keter.yaml

tar -C $TEMP_API_DIR -czf alloy4fun-api.keter .

rm -rf $TEMP_API_DIR
