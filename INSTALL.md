```
sudo apt-get install openjdk-11-jdk
sudo apt-get install neo4j=1:4.4.7
sudo service neo4j start
sudo apt install postgresql
sudo systemctl start postgresql@12-main
sudo apt install postgis
sudo -i -u postgres
createdb ukr
psql ukr
CREATE ROLE urk WITH
NOSUPERUSER
CREATEDB
CREATEROLE
NOINHERIT
LOGIN
NOREPLICATION
NOBYPASSRLS
CONNECTION LIMIT -1;

CREATE EXTENSION postgis;

exit;
logout

cypher-shell
username: neo4j
password: neo4j
Новый пароль - меняем на...

wget https://download.geofabrik.de/europe/ukraine-latest.osm.pbf .

tmux

sudo java -jar geograph-0.0.1-SNAPSHOT.jar --command=import --in-file=ukraine-latest.osm.pbf
```

Если ставим elastic
```
Подключаемся к инету через VPN - иначе не дает скачивать
затем
docker pull docker.elastic.co/elasticsearch/elasticsearch:8.3.2
docker network create elastic
на винде
wsl -d docker-desktop
    в консоли
    sysctl -w vm.max_map_count=262144
docker run --name es01 --net elastic -p 9200:9200 -p 9300:9300 -it docker.elastic.co/elasticsearch/elasticsearch:8.3.2
```
