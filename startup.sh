if [ "$(docker-machine active | grep dev4g)" != "dev4g" ]
then
  echo Starting dev box
  docker-machine start dev4g
fi
docker-compose kill
sh sbt_rebuild.sh
sh rebuild_all.sh
rm -R Hadoop/data/datanode1/*
rm -R Hadoop/data/datanode2/*
rm -R Hadoop/data/namenode/*
sh reformat_hdfs.sh
docker-compose up -d
sh open_web.sh
