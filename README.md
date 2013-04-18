![TimedRanker](http://i49.tinypic.com/16067hs.png)
==============
What is Timed Ranker
--------------------

TimedRanker is a simple Bukkit plugin that allows your server to automatically promote/rank-up players based on their total online time.

Features
--------

* Records total minutes online on server
* Easy to use configuration
* Per World (World Specific) promotions supported (if Permission plugin supports it as well)
* Customize how often promotion check is run to optimize plugin for your server
* Essentials AFK Integration
* RoyalCommands AFK Integration
* lang.yml file for easy message customization
* Uses Vault for permission management
* Stores total gameplay time in an SQLite .db file

Changelog
---------

**v1.3**

*Added per world promotions
*Added option to choose how often the plugin checks for promotions
*Added perworld.yml for setting up world specific promotions
*Minor fixes and code clean-up
*Changed debugInfo to debugMode in config.yml
*Added defaultGroup in config.yml
*Made config.yml easier to read and added extra comments to help server admins

**v1.2**

*Added RoyalCommands AFK Integration
*Added lang.yml for message configuration

**v1.1**

*Added Essentials AFK Integration
*Added command /tranker left
*Added debugInfo configuration option
*Changed how time required for rankup is set in the config file
*Some minor fixes for some commands

**v1.0**

*Initial Release

Dependencies
------------

You need to compile against Bukkit 1.5.1-R.02 and the following:

* [Vault] (http://dev.bukkit.org/server-mods/vault/)
* [SQLibrary] (http://dev.bukkit.org/server-mods/sqlibrary/)
* [Essentials] (http://dev.bukkit.org/server-mods/essentials/)
* [RoyalCommands] (http://dev.bukkit.org/server-mods/royalcommands/) (Note: You need 0.2.8 for Bukkit 1.5.1-R0.2, which can be currently downloaded [here] (http://build.royaldev.org/browse/RBUILDS-RCMDS-50/artifact/JOB1/Plugin-Jars/target))
