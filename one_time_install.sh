
current_dir=`pwd`
brew_location=`which brew`

RED='\033[1;31m'
BLUE='\033[0;34m'
LBLUE='\033[1;34m'
YEL='\033[1;33m'
NC='\033[0m' # No Color
#printf "I ${RED}love${NC} Stack Overflow\n"

printf "${LBLUE}Updating homebrew${NC}\n"
if [[ ! $brew_location == *"usr"* ]]
then
  ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
fi
brew update
brew install caskroom/cask/brew-cask

printf "${LBLUE}Install dependencies${NC}\n"
brew install node
brew install sbt

printf "${LBLUE}Installing vagrant and docker...${NC}\n"
brew install docker
brew install docker-compose
#brew cask install vagrant
brew cask install docker-machine
#vagrant plugin install vagrant-vmware-appcatalyst

sudo pip install pyopenssl ndg-httpsclient pyasn1

printf "${LBLUE}How much memory can you spare?${NC}\n"
read memory_limit
docker-machine create --driver virtualbox --virtualbox-cpu-count 4 --virtualbox-memory "$memory" dev4g
eval "$(docker-machine env dev4g)"

#cd coreos-vagrant
#printf "${LBLUE}Booting up a new CoreOS VM${NC}\n"
#sed -i '.bk' "s|\(\(\$shared_folders = \){\(.*\)}\).*|\2{\3,\"${current_dir}\" => \"${current_dir}\"}|g" Vagrantfile

#printf "${LBLUE}Creating and AppCatalyst VM${NC}\n"
#cd appcatalyst
#sed "s|\({{SHARE}}\)|${current_dir}|g" Vagrantfile.template > Vagrantfile

#
#
#vagrant up
#vagrant_int_ip=$(vagrant ssh -c "ip address show eth0 | grep 'inet ' | sed -e 's/^.*inet //' -e 's/\/.*$//'")
#vagrant_ext_ip=$(vagrant ssh -c "ip address show eth1 | grep 'inet ' | sed -e 's/^.*inet //' -e 's/\/.*$//'")
#
#vagrant_int_ip=${vagrant_int_ip//$'\r'/}
#vagrant_ext_ip=${vagrant_ext_ip//$'\r'/}
#
#printf "${LBLUE}Vagrant IP address is ${YEL}$vagrant_ext_ip${LBLUE}, this is used to connect to services running in the docker environment. Good idea to add this to your /etc/hosts file as 'vagrant'${NC}\n"
#
#vagrant ssh -c "sudo systemctl stop docker-tcp.socket"
#vagrant ssh -c "sudo rm /etc/systemd/system/docker-tcp.socket"
#vagrant ssh -c "sudo touch /etc/systemd/system/docker-tcp.socket && sudo chmod 777 /etc/systemd/system/docker-tcp.socket && sudo echo [Unit] >> /etc/systemd/system/docker-tcp.socket && sudo echo Description=Docker Socket for the API >> /etc/systemd/system/docker-tcp.socket && sudo echo  >> /etc/systemd/system/docker-tcp.socket && sudo echo [Socket] >> /etc/systemd/system/docker-tcp.socket && sudo echo ListenStream=2375 >> /etc/systemd/system/docker-tcp.socket && sudo echo BindIPv6Only=both >> /etc/systemd/system/docker-tcp.socket && sudo echo Service=docker.service >> /etc/systemd/system/docker-tcp.socket && sudo echo  >> /etc/systemd/system/docker-tcp.socket && sudo echo [Install] >> /etc/systemd/system/docker-tcp.socket && sudo echo WantedBy=sockets.target >> /etc/systemd/system/docker-tcp.socket"
#
#vagrant ssh -c "sudo systemctl enable docker-tcp.socket"
#vagrant ssh -c "sudo systemctl stop docker"
#vagrant ssh -c "sudo systemctl start docker-tcp.socket"
#vagrant ssh -c "sudo systemctl start docker"

#cd ..
#DOCKER_HOST=tcp://$vagrant_ext_ip:2375
#sed 's/.*DOCKER_HOST.*//g' ~/.bash_profile > ~/.bash_profile
#echo export DOCKER_HOST=$DOCKER_HOST >> ~/.bash_profile
#. ~/.bash_profile

#printf "${LBLUE}Setting up docker local registry${NC}\n"
mkdir registry

sh docker_registry.sh
#docker run -d -p 5000:5000 -e REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY=/var/lib/registry -v $current_dir/registry:/var/lib/registry --restart=always --name registry registry:2

sh docker_cleaner.sh

printf "${LBLUE}Building all project docker containers${NC}\n"
sh rebuild_all.sh

#printf "${LBLUE}Starting shipyard${NC}\n"
#sh shipyard-bootup.sh

#printf "${LBLUE}Shipyard provides an online GUI to monitor docker containers.${NC}\n"
#printf "${LBLUE}You will need to add the docker engine for it to work.${NC}\n"
#printf "${YEL}Engine name: docker-host${NC}\n"
#printf "${YEL}Labels: base${NC}\n"
#printf "${YEL}CPU: 6${NC}\n"
#printf "${YEL}Memory: 4096 (if you want to add more you will need to add more memory to the vagrant file)${NC}\n"
#printf "${YEL}Host: http://$vagrant_int_ip:2375${NC}\n"
#
#printf "${LBLUE}You can access shipyard at ${YEL}http://$vagrant_ext_ip:88${NC}\n"
#printf "${LBLUE}Username is ${YEL}admin${LBLUE} and password is ${YEL}shipyard${NC}\n"

printf "${LBLUE}Booting up project containers (use \"${YEL}docker-compose up -d${LBLUE}\" to start them up and \"${YEL}docker-compose kill${LBLUE}\" to shut them down)${NC}\n"
docker-compose up -d

open http://`docker-machine ip dev4g`:8888
open http://`docker-machine ip dev4g`:9000
