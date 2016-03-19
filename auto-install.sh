#This is a script to automatically install Minecraftly on a single server for testing
#Requirements: Debian 8 or higher. Server needs at least 1GB Ram
#! /bin/bash
#Update some stuffs
sudo -i
apt-get update -y
DEBIAN_FRONTEND=noninteractive apt-get -y -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" dist-upgrade
apt-get upgrade -y
apt-get dist-upgrade -y
apt-get install screen -y
apt-get install git -y

#Install MySQL, in this case MariaDB, with username "root" and password "123456"
export DEBIAN_FRONTEND=noninteractive
debconf-set-selections <<< 'mariadb-server-10.0 mysql-server/root_password password 123456'
debconf-set-selections <<< 'mariadb-server-10.0 mysql-server/root_password_again password 123456'
apt-get install mariadb-server-10.0 -y
apt-get install mariadb-client-10.0 -y
mysql -uroot -pPASS -e "SET PASSWORD = PASSWORD('');"

#Install Redis server
apt-get install redis-server -y

#Change ulimit
ulimit -n 1048576
ulimit -c unlimited
echo -e "*             -       nofile          1048576" >> /etc/security/limits.conf
echo -e "*             soft    nofile          65536" >> /etc/security/limits.conf
echo -e "*             hard    nofile          65536" >> /etc/security/limits.conf

#Install Latest Java Version
echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list
echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list
apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
apt-get update -y
apt-get install oracle-java8-set-default -y

#Make some directories
mkdir /minecraftly
mkdir /minecraftly/bungeecord1
mkdir /minecraftly/bungeecord2
mkdir /minecraftly/buildtools
mkdir /minecraftly/spigot1
mkdir /minecraftly/spigot2
mkdir /minecraftly/worlds

#Download some files
bash -c "wget -P /minecraftly/bungeecord1 https://m.ly/BungeeCord.jar"
bash -c "wget -P /minecraftly/bungeecord2 https://m.ly/BungeeCord.jar"
bash -c "wget -P /minecraftly/spigot1 https://m.ly/spigot.jar"
bash -c "wget -P /minecraftly/spigot2 https://m.ly/spigot.jar"
bash -c "wget -P /minecraftly/buildtools https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar && java -jar /minecraftly/buildtools/BuildTools.jar"
cp /minecraftly/buildtools/spigot-1.9.jar /minecraftly/spigot1/spigot-1.9.jar
cp /minecraftly/buildtools/spigot-1.9.jar /minecraftly/spigot2/spigot-1.9.jar

#Start servers for the first time to generate files
cd /minecraftly/bungeecord1 && screen -dmS bungeecord1 java -jar BungeeCord.jar
sleep 30
screen -r bungeecord1 -X stuff 'end\n'
cd /minecraftly/bungeecord2 && screen -dmS bungeecord2 java -jar BungeeCord.jar
sleep 30
screen -r bungeecord2 -X stuff 'end\n'
cd /minecraftly/spigot1 && screen -dmS spigot1 java -Dcom.mojang.eula.agree=true -jar spigot.jar --world-dir /minecraftly/worlds --port 25567 --online-mode=false
sleep 30
screen -r spigot1 -X stuff 'stop\n'
cd /minecraftly/spigot2 && screen -dmS spigot2 java -Dcom.mojang.eula.agree=true -jar spigot.jar --world-dir /minecraftly/worlds --port 25568 --online-mode=false
sleep 30
screen -r spigot2 -X stuff 'stop\n'

#Configure BungeeCord config
sed -i "s/ host: 0.0.0.0:.*/ host: 0.0.0.0:25565/" /minecraftly/bungeecord1/config.yml
sed -i "s/ host: 0.0.0.0:.*/ host: 0.0.0.0:25566/" /minecraftly/bungeecord2/config.yml
sed -i "s/level-name=.*/level-name=world1/" /minecraftly/spigot1/server.properties
sed -i "s/level-name=.*/level-name=world2/" /minecraftly/spigot2/server.properties

#Download & configure RedisBungee
wget -P /minecraftly/bungeecord1/plugins https://m.ly/RedisBungee.jar
wget -P /minecraftly/bungeecord2/plugins https://m.ly/RedisBungee.jar
cd /minecraftly/bungeecord1 && screen -dmS bungeecord1 java -jar BungeeCord.jar
sleep 30
screen -r bungeecord1 -X stuff 'end\n'
cd /minecraftly/bungeecord2 && screen -dmS bungeecord2 java -jar BungeeCord.jar
sleep 30
screen -r bungeecord2 -X stuff 'end\n'
sed -i "s/server-id:.*/server-id: localhost/" /minecraftly/bungeecord1/plugins/RedisBungee/config.yml
sed -i "s/server-id:.*/server-id: 127.0.0.1/" /minecraftly/bungeecord2/plugins/RedisBungee/config.yml

#Download Minecraftly plugins
wget -P /minecraftly/bungeecord1/plugins https://m.ly/MinecraftlyBungee.jar
wget -P /minecraftly/bungeecord2/plugins https://m.ly/MinecraftlyBungee.jar
wget -P /minecraftly/spigot1/plugins https://m.ly/MinecraftlySpigot.jar
wget -P /minecraftly/spigot2/plugins https://m.ly/MinecraftlySpigot.jar

#Start servers for the second time to generate Minecraftly plugin files
cd /minecraftly/bungeecord1 && screen -dmS bungeecord1 java -jar BungeeCord.jar
sleep 30
screen -r bungeecord1 -X stuff 'end\n'
cd /minecraftly/bungeecord2 && screen -dmS bungeecord2 java -jar BungeeCord.jar
sleep 30
screen -r bungeecord2 -X stuff 'end\n'
cd /minecraftly/spigot1 && screen -dmS spigot1 java -Dcom.mojang.eula.agree=true -jar spigot.jar --world-dir /minecraftly/worlds --port 25567 --online-mode=false
sleep 30
screen -r spigot1 -X stuff 'stop\n'
cd /minecraftly/spigot2 && screen -dmS spigot2 java -Dcom.mojang.eula.agree=true -jar spigot.jar --world-dir /minecraftly/worlds --port 25568 --online-mode=false
sleep 30
screen -r spigot2 -X stuff 'stop\n'
