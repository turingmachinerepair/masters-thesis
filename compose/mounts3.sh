
export HOME=/root
mkdir /tmp/s3mount0
echo minioadmin:minioadmin > ${HOME}/.passwd-s3fs
chmod 600 ${HOME}/.passwd-s3fs
s3fs src /tmp/s3mount0 -o passwd_file=${HOME}/.passwd-s3fs -o url=http://localhost:9000 -o use_path_request_style -o umask=0000
