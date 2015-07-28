docker kill registry
docker rm registry
docker run -d -p 5000:5000 -e REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY=/var/lib/registry -v `pwd`/registry:/var/lib/registry --restart=always --name registry registry:2
