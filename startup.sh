cd coreos-vagrant
vagrant up
cd ..
sh shipyard-bootup.sh
sh sbt_rebuild.sh
sh rebuild_all.sh
sh shipyard-bootup.sh
docker-compose up -d
