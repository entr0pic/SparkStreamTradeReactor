docker save $(docker images | grep localhost | awk '{print $3}') > image_backups/backup.tar
