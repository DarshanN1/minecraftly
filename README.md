#Welcome
  These are the important information for Minecraftly Cloud Platform.  Read them carefully,
as they tell you what this is all about, explain how to install the
software, and what to do if something goes wrong. 

#What is Minecraftly?
  Minecraftly (Minecraftly Cloud Platform) is a free and open source (FOSS) alternative of the official Mojang's Minecraft Realms, designed and written from scratch by Viet Nguyen and Keir Nellyer, respectively, with assistance from some friendly developers and helpers from the internet. It aims towards creating an easy to run a distributed network, on any server, using traditional server system.

  It has all the features you would expect in a Minecraft server, with additional performance gain
  including async, cloud computing compatibility, on demand
  loading.
  
  Minecraftly can run on traditional Minecraft server setup, with local file system. The advantage is, it's able to run on one and multiple servers at the same time, sharing the same NFS, Redis, and MySQL servers as a way of communicating between servers.
  
  We currently support the latest version of Minecraft. The version is always up to date.
  
#Download
  You can download the already compiled version of Minecraftly at https://ci.m.ly
  
#How it works
  As an alternative of Mojang's Minecraft Realms, if you install Minecraftly in your server, then each of your player will have his/her own server, accessible via (player's MC username).(your domain name).com
  
#Architecture
  Minecraftly operates under the premise that everything can fail at anytime, so we focus on designing a high availability, fault tolerant system that can withstand failure at the server, database, or network level.
  
  First, let's visualize:
  
             Network Load Balancer
      |              |              |
      |              |              |
   BungeeCord 1 BungeeCord 2  BungeeCord 3
      |              |              |
      |              |              |
   Spigot 1      Spigot 2        Spigot 3
      |              |              |
      |              |              |
      +----- NFS, MySQL & Redis ----+
  
  According to the drawing above, you can clearly see that all BungeeCord & Spigot servers share the same NFS, MySQL and Redis servers. In this case, we call such shared server "endpoints" (because behind the endpoints maybe a cluster of servers as well.
  
#Contributing
  Minecraftly is licensed under the GNU General Public License version 3 (GNU GPLv3), and we welcome anybody to fork and submit a Pull Request back with their changes, and if you want to join as a permanent member we can add you to the team.
  
  This is a "copyleft" license, which means if you publish the modified work as your own, you must open source it as well. It benefits the educational purpose of the software and helps everyone build better software that work on both traditional and cloud infrastructure.

Check out the CONTRIBUTING.md file for more details.

#Managed Hosting
  Besides the free and open source version, we also offer a value added hosted service at https://m.ly, so you can play with friends and don't have to setup server.
  
#Special Thanks To
  Andrew, Keir, Tux, Michael, Devin, Snivell, and many others who have been helping me over the years to make this happen.
