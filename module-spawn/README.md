Spawn
=====

Handles generation and player management for the "chat mode" world.

To generate a world with the custom world generator, add this line to bottom of your bukkit.yml file:

```
worlds:
  chat_1:
    generator: Minecraftly:Spawn,BEDROCK
```

where:

* "**chat_1**" - is the world (make sure any previous worlds with the same name have been removed so it can be regenerated)
* "**Minecraftly**" - tells Bukkit which plugin is in charge of the world generation
* "**Spawn**" - tells Minecraftly which module is responsible for world generation
* "**BEDROCK**" - the material to make the spawn out of