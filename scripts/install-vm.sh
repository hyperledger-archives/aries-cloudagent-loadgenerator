echo ">> Update all dependencies"
sudo apt update

echo ">> Install docker if not yet installed"
if [ -x "$(command -v docker)" ]; then
    echo "docker is already installed."
else
    echo ">> Install docker"
    sudo apt install apt-transport-https ca-certificates curl software-properties-common
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
    sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu bionic stable"
    sudo apt update
    apt-cache policy docker-ce
    sudo apt -y install docker-ce

    echo ">> Add current user to the docker user-group to be able to run docker without sudo"
    sudo usermod -aG docker ${USER}

    echo ">> Install Loki Docker Plugin to store all logs in Loki"
    docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions
    docker plugin ls
fi

echo ">> Install docker-compose if not yet installed"
if [ -x "$(command -v docker-compose)" ]; then
    echo "docker-compose is already installed."
else
    echo ">> Install docker-compose"
    sudo curl -L "https://github.com/docker/compose/releases/download/1.28.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
fi

echo ">> Install Java 11"
sudo apt -y install openjdk-11-jre-headless

echo ">> Install jq"
sudo apt-get -y install jq

echo ">> Install ngrok"
sudo snap -y install ngrok

echo ">> Install screen"
sudo apt -y install screen


echo ">> Download node_exporter"
cd ~/
wget https://github.com/prometheus/node_exporter/releases/download/v1.3.1/node_exporter-1.3.1.linux-amd64.tar.gz
tar xvfz node_exporter-1.3.1.linux-amd64.tar.gz
cd node_exporter-1.3.1.linux-amd64
sudo mv ./node_exporter /usr/local/bin/

echo ">> Create a node_exporter user and service to run node exporter in the background"
sudo useradd -rs /bin/false node_exporter
sudo touch /etc/systemd/system/node_exporter.service
sudo cat > /etc/systemd/system/node_exporter.service << EOF
[Unit]
Description=Node Exporter
After=network.target

[Service]
User=node_exporter
Group=node_exporter
Type=simple
ExecStart=/usr/local/bin/node_exporter --web.listen-address="172.17.0.1:9100"

[Install]
WantedBy=multi-user.target
EOF

echo ">> Start Node Exporter"
sudo systemctl daemon-reload
sudo systemctl start node_exporter
systemctl status node_exporter

echo ">> Ensure that Node Exporter is started on startup"
sudo systemctl enable node_exporter



echo ">> TODO"
echo "1. 'sudo reboot' to reboot the VM and apply all installations"
echo "3. configure the ./setup/.env"
echo "4. run the Load Generator ./setup/manage.sh start"
