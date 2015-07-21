docker kill shipyard-rethinkdb-data
docker kill shipyard-rethinkdb
docker kill shipyard
docker rm shipyard-rethinkdb-data
docker rm shipyard-rethinkdb
docker rm shipyard
docker run -it -d --name shipyard-rethinkdb-data --entrypoint /bin/bash shipyard/rethinkdb -l
docker run -it -P -d --name shipyard-rethinkdb --volumes-from shipyard-rethinkdb-data shipyard/rethinkdb
docker run -it -p 88:8080 -d --name shipyard --link shipyard-rethinkdb:rethinkdb shipyard/shipyard
