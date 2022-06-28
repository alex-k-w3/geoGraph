<pre>
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
</pre>