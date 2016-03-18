#This is a script to automatically install Minecraftly on one Debian server
#! /bin/bash
#Update some stuffs
sudo -i
apt-get update -y
DEBIAN_FRONTEND=noninteractive apt-get -y -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" dist-upgrade
apt-get upgrade -y
apt-get dist-upgrade -y
apt-get install screen -y
apt-get install git -y

#Install MySQL, in this case MariaDB
apt-get install python-software-properties -y
apt-key adv --recv-keys --keyserver keyserver.ubuntu.com 0xcbcb082a1bb943db
add-apt-repository 'deb http://mariadb.biz.net.id//repo/10.1/debian sid main'
apt-get install software-properties-common -y
apt-get update -y
export DEBIAN_FRONTEND=noninteractive
sudo debconf-set-selections <<< 'mariadb-server-10.0 mysql-server/root_password password 123456'
sudo debconf-set-selections <<< 'mariadb-server-10.0 mysql-server/root_password_again password 123456'
mysql -uroot -pPASS -e "SET PASSWORD = PASSWORD('');"
sudo -E apt-get install mariadb-server mariadb-client -q -y

#Install Redis
apt-get install build-essential -y
wget http://redis.googlecode.com/files/redis-2.6.13.tar.gz
tar -xzf redis-2.6.13.tar.gz
cd redis-2.6.13
make install

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
bash -c "wget -P /minecraftly/bungeecord1 http://ci.md-5.net/job/BungeeCord/lastSuccessfulBuild/artifact/bootstrap/target/BungeeCord.jar"
bash -c "wget -P /minecraftly/bungeecord2 http://ci.md-5.net/job/BungeeCord/lastSuccessfulBuild/artifact/bootstrap/target/BungeeCord.jar"
bash -c "wget -P /minecraftly/buildtools https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar && cd /minecraftly/buildtools && java -jar BuildTools.jar"
cp /minecraftly/buildtools/spigot-1.9.jar /minecraftly/spigot1/spigot-1.9.jar
cp /minecraftly/buildtools/spigot-1.9.jar /minecraftly/spigot2/spigot-1.9.jar

#Configure some stuffs
sed -i "s/server-id:.*/server-id: localhost/" /minecraftly/bungeecord1/plugins/RedisBungee/config.yml
sed -i "s/server-id:.*/server-id: 127.0.0.1/" /minecraftly/bungeecord2/plugins/RedisBungee/config.yml

#Start servers for the first time to generate files
cd /minecraftly/bungeecord1 && screen -dmS bungeecord1 java -jar BungeeCord.jar
sleep 30
screen -r bungeecord1 -X stuff 'end'
cd /minecraftly/bungeecord2 && java -jar BungeeCord.jar
sleep 30
screen -r bungeecord2 -X stuff 'end'
cd /minecraftly/spigot1 && screen -dmS spigot1 java -Dcom.mojang.eula.agree=true -jar spigot-1.9.jar --world-dir /minecraftly/worlds --port 25567 --online-mode=false
sleep 30
screen -r spigot1 -X stuff 'stop'
cd /minecraftly/spigot2 && screen -dmS spigot2 java -Dcom.mojang.eula.agree=true -jar spigot-1.9.jar --world-dir /minecraftly/worlds --port 25568 --online-mode=false
sleep 30
screen -r spigot2 -X stuff 'stop'

#Download Minecraftly plugins
wget -P /minecraftly/bungeecord1/plugins https://ci.m.ly/job/Minecraftly/lastSuccessfulBuild/artifact/target/MinecraftlyBungee.jar
wget -P /minecraftly/bungeecord2/plugins https://ci.m.ly/job/Minecraftly/lastSuccessfulBuild/artifact/target/MinecraftlyBungee.jar
wget -P /minecraftly/spigot1/plugins https://ci.m.ly/job/Minecraftly/lastSuccessfulBuild/artifact/target/MinecraftlySpigot.jar
wget -P /minecraftly/spigot2/plugins https://ci.m.ly/job/Minecraftly/lastSuccessfulBuild/artifact/target/MinecraftlySpigot.jar

#Start servers for the second time to generate plugin files
cd /minecraftly/bungeecord1 && screen -dmS bungeecord1 java -jar BungeeCord.jar
sleep 30
screen -r bungeecord1 -X stuff 'end'
cd /minecraftly/bungeecord2 && java -jar BungeeCord.jar
sleep 30
screen -r bungeecord2 -X stuff 'end'
cd /minecraftly/spigot1 && screen -dmS spigot1 java -Dcom.mojang.eula.agree=true -jar spigot-1.9.jar --world-dir /minecraftly/worlds --port 25567 --online-mode=false
sleep 30
screen -r spigot1 -X stuff 'stop'
cd /minecraftly/spigot2 && screen -dmS spigot2 java -Dcom.mojang.eula.agree=true -jar spigot-1.9.jar --world-dir /minecraftly/worlds --port 25568 --online-mode=false
sleep 30
screen -r spigot2 -X stuff 'stop'
