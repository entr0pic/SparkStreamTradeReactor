
if [ ! -f /base_files/spark-$SPARK_PACKAGE.tgz ]; then
    curl -sL --retry 3 \
    "http://mirrors.ibiblio.org/apache/spark/spark-$SPARK_VERSION/spark-$SPARK_PACKAGE.tgz" > /base_files/spark-$SPARK_PACKAGE.tgz
fi

cat /base_files/spark-$SPARK_PACKAGE.tgz | gunzip | tar x -C /usr/
ln -s $SPARK_HOME /usr/spark
