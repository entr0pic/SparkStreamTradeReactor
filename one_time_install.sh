echo Checking if homebrew is isntalled
brew_location=`which brew'
if [[ ! $brew_location == *"usr"* ]]
then
  ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
fi

echo Install dependencies
brew install node
brew install sbt

echo Installing vagrant and docker...
brew cask install vagrant docker docker-compose
cd coreos-vagrant
echo Booting up a new CoreOS VM
vagrant init
vagrant up

vagrant_int_ip=$(vagrant ssh -c "ip address show eth0 | grep 'inet ' | sed -e 's/^.*inet //' -e 's/\/.*$//'")
vagrant_ext_ip=$(vagrant ssh -c "ip address show eth1 | grep 'inet ' | sed -e 's/^.*inet //' -e 's/\/.*$//'")
echo vagrant IP address is $vagrant_ext_ip, this is used to connect to services running in the docker environment. Good idea to add this to your /etc/hosts file as 'vagrant'

vagrant ssh
systemctl enable docker-tcp.socket
systemctl stop docker
systemctl start docker-tcp.socket
systemctl start docker
exit

cd ..

DOCKER_HOST=tcp://$vagrant_ext_ip:2375
echo export DOCKER_HOST=$DOCKER_HOST >> ~/.bash_profile
. ~/.bash_profile

echo Setting up docker local registry
mkdir registry
current_dir=`pwd`
docker run -d -p 5000:5000 -e REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY=/var/lib/registry -v $current_dir/registry:/var/lib/registry --restart=always --name registry registry:2
sh docker_cleaner.sh

echo Building all project docker containers
sh rebuild_all.sh

echo Starting shipyard
sh shipyard_bootup.sh

echo Shipyard provides an online GUI to monitor docker containers.
echo You will need to add the docker engine for it to work.
echo Engine name: docker-host
echo Labels: base
echo CPU: 6
echo Memory: 4096 (if you want to add more you will need to add more memory to the vagrant file)
echo Host: http://$vagrant_int_ip:2375

echo You can access shipyard at http://$vagrant_ext_ip:88

echo Booting up project containers (use "docker-compose up -d" to start them up and "docker-compose kill" to shut them down)
docker-compose up -d
