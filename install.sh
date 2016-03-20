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

#Install MySQL, in this case MariaDB, with username "root", no password, and database name "minecraftly"
export DEBIAN_FRONTEND=noninteractive
debconf-set-selections <<< 'mariadb-server-10.0 mysql-server/root_password password 123456'
debconf-set-selections <<< 'mariadb-server-10.0 mysql-server/root_password_again password 123456'
apt-get install mariadb-server-10.0 -y
apt-get install mariadb-client-10.0 -y
mysql -u root -p123456 -e "create database minecraftly;"
mysqladmin -u root -p123456 password ''

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
mkdir /m
mkdir /m/b1
mkdir /m/b2
mkdir /m/buildtools
mkdir /m/s1
mkdir /m/s2
mkdir /m/worlds

#Download some files
bash -c "wget -P /m/b1 https://m.ly/BungeeCord.jar"
bash -c "wget -P /m/b2 https://m.ly/BungeeCord.jar"
bash -c "wget -P /m/s1 https://m.ly/spigot.jar"
bash -c "wget -P /m/s2 https://m.ly/spigot.jar"
#bash -c "wget -P /m/buildtools https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar && java -jar /m/buildtools/BuildTools.jar"
#cp /m/buildtools/spigot-1.9.jar /m/spigot1/spigot-1.9.jar
#cp /m/buildtools/spigot-1.9.jar /m/spigot2/spigot-1.9.jar

#Start servers for the first time to generate files
cd /m/b1 && screen -dmS b1 java -jar BungeeCord.jar
sleep 30
screen -r b1 -X stuff 'end\n'
cd /m/b2 && screen -dmS b2 java -jar BungeeCord.jar
sleep 30
screen -r b2 -X stuff 'end\n'
cd /m/s1 && screen -dmS s1 java -Dcom.mojang.eula.agree=true -jar spigot.jar --world-dir /m/worlds --port 25567
sleep 30
screen -r s1 -X stuff 'stop\n'
cd /m/s2 && screen -dmS s2 java -Dcom.mojang.eula.agree=true -jar spigot.jar --world-dir /m/worlds --port 25568
sleep 30
screen -r s2 -X stuff 'stop\n'

#Configure BungeeCord config
sed -i "s/ host: 0.0.0.0:.*/ host: 0.0.0.0:25565/" /m/b1/config.yml
sed -i "s/ host: 0.0.0.0:.*/ host: 0.0.0.0:25566/" /m/b2/config.yml
sed -i "s/ip_forward: .*/ip_forward: true/" /m/b1/config.yml
sed -i "s/ip_forward: .*/ip_forward: true/" /m/b2/config.yml
sed -i "s/address: localhost:.*/address: localhost:25567/" /m/b1/config.yml
sed -i "s/address: localhost:.*/address: localhost:25568/" /m/b2/config.yml
sed -i "s/level-name=.*/level-name=world1/" /m/s1/server.properties
sed -i "s/level-name=.*/level-name=world2/" /m/s2/server.properties
sed -i "s/online-mode=.*/online-mode=false/" /m/s1/server.properties
sed -i "s/online-mode=.*/online-mode=false/" /m/s2/server.properties
sed -i "s/bungeecord: .*/bungeecord: true/" /m/s1/spigot.yml
sed -i "s/bungeecord: .*/bungeecord: true/" /m/s2/spigot.yml

#Download & configure RedisBungee
wget -P /m/b1/plugins https://m.ly/RedisBungee.jar
wget -P /m/b2/plugins https://m.ly/RedisBungee.jar
cd /m/b1 && screen -dmS b1 java -jar BungeeCord.jar
sleep 30
screen -r b1 -X stuff 'end\n'
cd /m/b2 && screen -dmS b2 java -jar BungeeCord.jar
sleep 30
screen -r b2 -X stuff 'end\n'
sed -i "s/server-id:.*/server-id: b1/" /m/b1/plugins/RedisBungee/config.yml
sed -i "s/server-id:.*/server-id: b2/" /m/b2/plugins/RedisBungee/config.yml

#Download Minecraftly plugins
wget -P /m/b1/plugins https://m.ly/MinecraftlyBungee.jar
wget -P /m/b2/plugins https://m.ly/MinecraftlyBungee.jar
wget -P /m/s1/plugins https://m.ly/Minecraftly.jar
wget -P /m/s2/plugins https://m.ly/Minecraftly.jar
wget -P /m/s1/plugins https://m.ly/ProtocolLib.jar
wget -P /m/s2/plugins https://m.ly/ProtocolLib.jar
wget -P /m/s1/plugins http://dev.bukkit.org/media/files/894/359/Vault.jar
wget -P /m/s2/plugins http://dev.bukkit.org/media/files/894/359/Vault.jar

#Start servers for the second time to generate plugins files
cd /m/b1 && screen -dmS b1 java -jar BungeeCord.jar
sleep 30
screen -r b1 -X stuff 'end\n'
cd /m/b2 && screen -dmS b2 java -jar BungeeCord.jar
sleep 30
screen -r b2 -X stuff 'end\n'
cd /m/s1 && screen -dmS s1 java -Dcom.mojang.eula.agree=true -jar spigot.jar --world-dir /m/worlds --port 25567
sleep 30
screen -r s1 -X stuff 'stop\n'
cd /m/s2 && screen -dmS s2 java -Dcom.mojang.eula.agree=true -jar spigot.jar --world-dir /m/worlds --port 25568
sleep 30
screen -r s2 -X stuff 'stop\n'

#Configure stuffs
rm -rf //m/b1/plugins/MinecraftlyBungee/config.yml
rm -rf //m/b2/plugins/MinecraftlyBungee/config.yml
wget -P /m/b1/plugins/MinecraftlyBungee https://m.ly/config.yml
wget -P /m/b2/plugins/MinecraftlyBungee https://m.ly/config.yml
wget -P /m/b1/plugins/MinecraftlyBungee https://m.ly/motd.yml
wget -P /m/b2/plugins/MinecraftlyBungee https://m.ly/motd.yml
