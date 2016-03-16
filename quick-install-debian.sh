#This is a script to automatically install Minecraftly on one Debian server
#! /bin/bash
#Update some stuffs
sudo -i
apt-get update -y
sudo DEBIAN_FRONTEND=noninteractive apt-get -y -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" dist-upgrade
apt-get upgrade -y
apt-get dist-upgrade -y

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
mkdir /minecraftly/spigot1
mkdir /minecraftly/spigot2

#Configure some stuffs
sed -i "s/server-id:.*/server-id: localhost/" /minecraftly/bungeecord1/plugins/RedisBungee/config.yml
sed -i "s/server-id:.*/server-id: 127.0.0.1/" /minecraftly/bungeecord2/plugins/RedisBungee/config.yml
