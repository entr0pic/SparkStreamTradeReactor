docker run -d -p 5000:5000 -e REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY=/var/lib/registry -v `pwd`/registry:/var/lib/registry --restart=always --name registry registry:2
docker run -d  -v /var/run/docker.sock:/var/run/docker.sock:rw --restart=always --name docker-cleanup  meltwater/docker-cleanup:latest
