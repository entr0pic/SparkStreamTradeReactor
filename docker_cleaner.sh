docker kill docker-cleanup
docker rm docker-cleanup
docker run -d  -v /var/run/docker.sock:/var/run/docker.sock:rw --restart=always --name docker-cleanup  meltwater/docker-cleanup:latest
