#Welcome
  These are the important information for Minecraftly Cloud Platform.  Read them carefully,
as they tell you what this is all about, explain how to install the
software, and what to do if something goes wrong. 

##What is Minecraftly?
  Minecraftly (Minecraftly Cloud Platform) is a free and open source (FOSS) alternative of the official Mojang's Minecraft Realms, designed and written from scratch by Viet Nguyen and Keir Nellyer, respectively, with assistance from some friendly developers and helpers from the internet. It aims towards creating an easy to run a distributed network, on any server, using traditional server system. Minecraftly Cloud Platform is now community-developed and will be remained free under GNU GPL license.
  
  Started out as a simple Minecraft game server like every other, I understood and saw the importance of education through gaming. The period of changing mindset from owning a proprietary software to deciding if I should open source it was not short of a challenge. In the end, I'm glad that I was able to open source it, and share it with passionate people who want to contribute to the community, to change the world via cloud computing, and with everyone who wants to learn.

  It has all the features you would expect in a Minecraft server, with additional performance gain
  including async, cloud computing compatibility, on demand
  loading.
  
  Minecraftly can run on traditional Minecraft server setup, with local file system. The advantage is, it's able to run on one and multiple servers at the same time, sharing the same NFS, Redis, and MySQL servers as a way of communicating between servers.
  
  We currently support the latest version of Minecraft. The version is always up to date.
  
##Why Open Source?
  I'm [Viet](https://twitter.com/vietdoge), a simple guy with love for cloud computing, the web, technologies, and Minecraft just like you. Ever since I first run a Minecraft server in 2012, I've always been looking for a way to scale Minecraft with high availability and fault tolerant. It took me years to think and build the first prototype after seeing that not many people in the community have a high availability mindset. I can't do it alone and need your contribution to make it better.
  
  I dedicated myself to cloud computing and passed my AWS Solutions Architect Certification exam in 2015. It helped construct my knowledge to build a simplier open source project that anyone can test, build, host their own network.
  
##Download
  You can download the already compiled version of Minecraftly at [https://ci.m.ly](https://ci.m.ly)
  
##How it works
  As an alternative of Mojang's Minecraft Realms, if you install Minecraftly in your server, then each of your player will have his/her own server, accessible via (player's MC username).(your domain name).com
  
##Player Flow
 Usually, in a normal Minecraft server, player flow is like this:
```ruby
                               Player A
                                  |
                                  |
                                  |
                                  ▼ 
                              Server 1
                                  |
                                  |
                                  |
                                  ▼ 
                               World 1
```
 With the old way of distributing players, a single machine gets filled up with players very quickly, and will crash at a certain amount of concurrent players online.

 Let's visualize a smarter way to distribute players. In Minecraftly, it's like this:
```ruby
                               Player A
                                  |
                                  |
                                  |
                                  ▼ 
                       Server 1 or 2 or 3 or ∞
                                  |
                                  |
                                  |
                                  ▼ 
                               World A
```
 As you can see, in this method, it doesn't matter which server brings player his/her own world when they log in, as long as the one server serves the correct world to the correct player at that correct moment.
 
 You will then ask: The visualization above shows one player and one world. What about multiple players at the same time? Here you go:
```ruby
     Player A          Player B          Player C          Player D          Player E
        |                 |                 |                 |                 |
        |                 |                 |                 |                 |
        |                 |                 |                 |                 |
        ▼                 ▼                 ▼                 ▼                 ▼
    Server 1 or 2 or 3 or 4 or 5 or 6 or 7 or 8 or 9 or 10 or 11 or 12 or 13 or ... or ∞
        |                 |                 |                 |                 |
        |                 |                 |                 |                 |
        |                 |                 |                 |                 |
        ▼                 ▼                 ▼                 ▼                 ▼
   +----------------------------------------------------------------------------------+
   |                                                                                  |
   | World A           World B            World C           World E           World F |
   |                                                                                  |
   +----------------------------------------------------------------------------------+
```
 How do I store world A, B, C, D, E, and F for the respective players and load them as fast as possible, using traditional way of saving worlds on disk?
 
 The answer is to have all servers using the same "global worlds" folder, storing all the worlds for the respective player there.
 
 For multiple servers to "share the same folder", the simpliest way is to use NFS (Network File System) server.
 
##Architecture
  Minecraftly operates under the premise that everything can fail at anytime, so we focus on designing a high availability, fault tolerant system that can withstand failure at the server, database, or network level.
  
  First, let's visualize the stack:
  
```ruby
                           Minecraft Players
                                  ▲
                                  |
                                  |
                                  |
                                  ▼
      +----------------- Network Load Balancer -----------------+
      |                           |                             |
      |                           |                             |
      |                           |                             |
   BungeeCord 1              BungeeCord 2                 BungeeCord n
      |                           |                             |
      |                           |                             |
      |                           |                             |
   Spigot 1                    Spigot 2                      Spigot n
      |                           |                             |
      |                           |                             |
      |                           |                             |
      +------------------- NFS, MySQL & Redis ------------------+
      
```
  
  According to the drawing above, you can clearly see that all BungeeCord & Spigot servers share the same NFS, MySQL and Redis servers. In this case, we call such shared server "endpoints" (because behind the endpoints maybe a cluster of servers as well.

  There are an infinite amount of BungeeCord and Spigot servers, having the same exact configuration. It doesn't matter how many BungeeCord or Spigot servers out there. As long as they use the same NFS mount point, the same MySQL server, and the same Redis server, then the player's experience will be unified.
  
##Single Machine Environment (for testing)
To build a single machine test environment, it will be visualized like this:

```ruby
                           Minecraft Players
                                  ▲
                                  |
                                  |
                                  |
                                  ▼
   BungeeCord 1 (port 25565)                    BungeeCord 2 (port 25566)
      |                                                         |
      |                                                         |
      |                                                         |
   Spigot 1 (port 25567)                           Spigot 2 (port 25568)
      |                                                         |
      |                                                         |
      |                                                         |
      +--------------------- MySQL & Redis ---------------------+
      
```
  
  This testing environment is very simple. It works with traditional server cluster (VM, dedicated servers, etc...). It can also work with cloud servers (Amazon Web Services, Google Cloud Platform, Microsoft Azure).
  
##Requirements
 * BungeeCord: serve as a proxy server
 * Spigot: serve as Minecraft server
 * MySQL Server
 * Redis Server (Will plan to not depend on Redis and RedisBungee)
 * RedisBungee plugin (Will plan to not depend on Redis and RedisBungee)
 * Vault plugin (for permissions hook)
 * ProtocolLib plugin (Will plan to not depend on ProtocolLib)

##How is it better than [Minecraft Realms](https://minecraft.net/realms)?
 Minecraftly is better than Minecraft Realms in many ways. First, let's look at how complex and redundant Minecraft Realms is
![screen shot Minecraft Realms Architecture](https://media.amazonwebservices.com/blog/2014/minecraft_realms_arch_2.png)
 Cloud computing doesn't need to be that complicated. It's complicated mainly because of intellectual properties. Since we're open source, we can make it as simple and as extendable as possible.
 
 Here are some simple comparisons:
 * Minecraftly doesn't need Frontend, Manager, Controller, and Amazon S3 object storage
 * Minecraftly doesn't need move world from object storage back to local block storage, which means players can load world right away.
 * Minecraftly lets players jump from server to server in real time, embracing the feeling of having many people playing with you at the same time.
 * Minecraftly saves server cost more efficiently than Minecraft Realms. While Realms creates a separated server for each paid player which is costly, we use one server for many free and paid players, and still deliver better and more seamless performance.
 
##Contributing
  Minecraftly is licensed under the GNU General Public License version 3 (GNU GPLv3), and we welcome anybody to fork and submit a Pull Request back with their changes, and if you want to join as a permanent member we can add you to the team.
  
  This is a "copyleft" license, which means if you publish the modified work as your own, you must open source it as well. It benefits the educational purpose of the software and helps everyone build better software that work on both traditional and cloud infrastructure.
  
  For details of license, check the [LICENSE](LICENSE) file.

  For contributing information, check out the [CONTRIBUTING.md](CONTRIBUTING.md) file for more details.

##Managed Hosting
  Besides the free and open source version, we also offer a value added hosted service at [https://m.ly](https://m.ly). You can play with friends and don't have to setup server.
  
##Special Thanks To
  Andrew, Keir, Tux, Michael, Devin, Snivell, Ben (redraskal) and many others who have been helping me over the years to make this happen.

#License
 Minecraftly is licensed under the [GNU General Public License version 3 (GNU GPLv3)](LICENSE)
