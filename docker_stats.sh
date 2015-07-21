docker stats $(docker ps|grep -v "NAMES"|awk '{ print $NF }'|tr "\n" " ")
