#!/bin/sh

echo "Bring testnet online";
docker stack deploy -c docker-compose.yml testnet
sleep 2

echo "Deploy server";
java -jar ./functionary-0.0.1-SNAPSHOT.jar 1>1_functionary.log 2>2_functionary.log &
sleep 2

echo "Deploy dispatcher";
java -jar ./quadomizer-0.0.1-SNAPSHOT.jar 1>1_functionary.log 2>2_functionary.log &
sleep 2


echo "System deployed";
