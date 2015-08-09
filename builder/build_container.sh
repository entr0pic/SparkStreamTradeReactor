docker kill builder
docker rm builder
docker run -it -v /var/run/docker.sock:/var/run/docker.sock:rw -v /media/storage/repos:/repos --name builder builder
