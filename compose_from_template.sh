rm docker-compose.yml
sed -e "s|{root}|$(pwd)\/|g" docker-compose.tmp > docker-compose.yml
