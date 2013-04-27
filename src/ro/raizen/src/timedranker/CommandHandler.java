package ro.raizen.src.timedranker;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
	
	private TimedRanker plugin;
	private Permission perms;
	
	public CommandHandler(TimedRanker plugin, Permission perms) {
		this.plugin = plugin;
		this.perms = perms;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]) 
	{
			//If there are no arguments, show a list of subcommands
			if(args.length < 1) {
				if(sender.hasPermission("tranker.playtime.others")) {
					sender.sendMessage(plugin.parseString("") + "/tranker playtime <player>- See player's gameplay time");	
				}
				else if((sender instanceof Player) && sender.hasPermission("tranker.playtime.me")) {
					sender.sendMessage(plugin.parseString("") + "/tranker playtime - See your gameplay time");
				}
				if(sender.hasPermission("tranker.top")) {
					sender.sendMessage(plugin.parseString("") + "/tranker top - View a top 10 of most-online players");
				}
				if(sender.hasPermission("tranker.left.others")) {
					sender.sendMessage(plugin.parseString("") + "/tranker left <player> [world] - View time left until the player's next promotion");
				} else if(sender.hasPermission("tranker.left")) {
					sender.sendMessage(plugin.parseString("") + "/tranker left - View time left until your next promotion");
				}
				if(sender.hasPermission("tranker.settime")) { 
					sender.sendMessage(plugin.parseString("") + "/tranker settime <player> <time> - Set minutes played for a player");
				}
				if(sender.hasPermission("tranker.purge")) {
					sender.sendMessage(plugin.parseString("") + "/tranker purge - Purge database");
				}
				if(sender.hasPermission("tranker.reload")) {
					sender.sendMessage(plugin.parseString("") + "/tranker reload - Reload configuration");
				}
				return true;
			}
			
			try 
			{
				String subCommand = args[0].toLowerCase();
				//most of it is self-explanatory
				switch(subCommand) {
					case "playtime":
						if(sender.hasPermission("tranker.playtime")) {
							if((sender instanceof Player) && (args.length == 1 || ( args.length == 2 && args[1].equalsIgnoreCase( ((Player) sender ).getName() ) ) ) ) {
									ResultSet result = plugin.sqlite.query("SELECT COUNT(*) as cnt, playtime FROM playtime WHERE UPPER(playername) = UPPER('" + sender.getName() + "');");
									if(result.getInt("cnt") > 0) {
										int playtime = result.getInt("playtime");
										sender.sendMessage(String.format(plugin.getLang("PlaytimeMe"), parseTime(playtime)));
									} else {
										sender.sendMessage(plugin.parseString("") + "Invalid player name or database not updated.");
									}
									result.close();
							} else if(sender.hasPermission("tranker.playtime.others") && args.length == 2) {
								ResultSet result = plugin.sqlite.query("SELECT COUNT(*) as cnt, playtime FROM playtime WHERE UPPER(playername) = UPPER('" + args[1] + "');");
								if(result.getInt("cnt") > 0) {
									int playtime = result.getInt("playtime");
									sender.sendMessage(String.format(plugin.getLang("PlaytimeOthers"), args[1], parseTime(playtime)));
								} else {
									sender.sendMessage(plugin.parseString("") + "Invalid player name or database not updated.");
								}
								result.close();
							} else if(!(sender instanceof Player)) {
								sender.sendMessage(plugin.parseString("") + "This command can only be run by a player");
							} else {
								sender.sendMessage(plugin.getLang("noPermission"));
							}
						} else {
							sender.sendMessage(plugin.getLang("noPermission"));
						}
						break;
					case "settime":
						if(sender instanceof Player && !sender.hasPermission("tranker.settime")) {
							sender.sendMessage(plugin.getLang("noPermission"));
						}
						else {
							if(args.length == 3) {
								String player = args[1];
								int minutes;
								try {
									minutes = new Integer(args[2]);
									ResultSet result = plugin.sqlite.query("SELECT COUNT(*) as cnt FROM playtime WHERE  UPPER(playername) =  UPPER('" + player + "');");
									if(result.getInt("cnt") > 0) {
										plugin.sqlite.query("UPDATE playtime SET playtime='" + minutes +"' WHERE playername = '" + player + "';");
										sender.sendMessage(String.format(plugin.getLang("SetTimeUpdate"), parseTime(minutes), player));
									}
									else {
										plugin.sqlite.query("INSERT INTO playtime (playername, playtime) VALUES('" + player + "', '" + minutes + "');");
										sender.sendMessage(String.format(plugin.getLang("SetTimeCreate"), player, parseTime(minutes)));
									}
									result.close();
								} catch (NumberFormatException ex) {
									sender.sendMessage(plugin.parseString("") + "The minutes parameter must be an integer");
							    }
								
							} else {
								sender.sendMessage(plugin.parseString("") + "Usage: /tranker settime <player> <minutes>");
							}
						}
						break;
					case "top":
						if(sender.hasPermission("tranker.top")) {
							ResultSet result = plugin.sqlite.query("SELECT * FROM playtime ORDER BY playtime DESC LIMIT 10");
							ResultSet result2 = plugin.sqlite.query("SELECT count(*) as cnt FROM playtime");
							int count = 0;
							if(result2.getInt("cnt") > 0) {
								while(result.next() && count < 10) {
									count++;
									sender.sendMessage(String.format(plugin.getLang("TopPlayersTemplate"), count, result.getString("playername"), parseTime(result.getInt("playtime"))));
								}
							} else {
								sender.sendMessage(plugin.getLang("TopPlayersEmpty"));
							}
						} else {
							sender.sendMessage(plugin.getLang("noPermission"));
						}
						break;
					case "left": //here comes the ugly one
							if(sender.hasPermission("tranker.left")) {
								
								//if the sender is a player and number of arguments is 1 or (if it's greater or equal than 2 AND he second argument is the sender's name)
								//than we make the command check time left for the player sending it.
								if((sender instanceof Player) && ( args.length == 1 || (args.length >= 2 && args[1].equalsIgnoreCase( ( (Player) sender).getName() ) ) ) ) { 
									
									//if worldSpecificGroups is true, we check the promotions in the current world only (i'm lazy)
									if(plugin.cfg.getConfig().getBoolean("worldSpecificGroups")) {
										
										String world = ((Player) sender).getWorld().getName();
										String player = ((Player) sender).getName();
										
										String currentGroup = perms.getPrimaryGroup(world, player); //get player's current group in current world
										if(currentGroup == null) {
											if(plugin.cfg.getConfig().getString("defaultGroup") != "" && plugin.cfg.getConfig().getString("defaultGroup") != null) {
												currentGroup = plugin.cfg.getConfig().getString("defaultGroup");
											}
											else {
												currentGroup = "default";
											}
										}
										
										if(plugin.perworld.getConfig().contains("promote." + world + "." + currentGroup)) {
											int minReq = plugin.timeInMinutes(plugin.perworld.getConfig().getString("promote." + world + "." + currentGroup + ".timeReq")); //get the required minutes played from the config
											String promoteTo = plugin.perworld.getConfig().getString("promote." + world + "." + currentGroup + ".to"); //get the group the player must be ranked-up to
											ResultSet result = plugin.sqlite.query("SELECT COUNT(*) as cnt, playtime FROM playtime WHERE  UPPER(playername) =  UPPER('" + sender.getName() + "');");
											if(result.getInt("cnt") > 0) {
												if(minReq - result.getInt("playtime") > 0) {
													String minLeft = parseTime(minReq - result.getInt("playtime")); //minutes left to play
													sender.sendMessage(String.format(plugin.getLang("Left"), minLeft, promoteTo));
												}
												else {
													sender.sendMessage(String.format(plugin.getLang("LeftShortly"), promoteTo));
												}
											} else {
												String minLeft = parseTime(minReq); //minutes left to play
												sender.sendMessage(String.format(plugin.getLang("Left"), minLeft, promoteTo));
											}
										}
										else {
											sender.sendMessage(plugin.getLang("LeftNone"));
										}
									
									} else {
										String world = null;
										String currentGroup = perms.getPrimaryGroup(world, ((Player) sender).getName()); //get player's current group
										if(currentGroup == null) {
											if(plugin.cfg.getConfig().getString("defaultGroup") != "" && plugin.cfg.getConfig().getString("defaultGroup") != null) {
												currentGroup = plugin.cfg.getConfig().getString("defaultGroup");
											}
											else {
												currentGroup = "default";
											}
										}
										
										if(plugin.cfg.getConfig().contains("promote." + currentGroup)) {
											int minReq = plugin.timeInMinutes(plugin.cfg.getConfig().getString("promote." + currentGroup + ".timeReq")); //get the required minutes played from the config
											String promoteTo = plugin.cfg.getConfig().getString("promote." + currentGroup + ".to"); //get the group the player must be ranked-up to
											ResultSet result = plugin.sqlite.query("SELECT COUNT(*) as cnt, playtime FROM playtime WHERE  UPPER(playername) =  UPPER('" + sender.getName() + "');");
											if(result.getInt("cnt") > 0) {
												if(minReq - result.getInt("playtime") > 0) {
													String minLeft = parseTime(minReq - result.getInt("playtime")); //minutes left to play
													sender.sendMessage(String.format(plugin.getLang("Left"), minLeft, promoteTo));
												}
												else {
													sender.sendMessage(String.format(plugin.getLang("LeftShortly"), promoteTo));
												}
											} else {
												String minLeft = parseTime(minReq); //minutes left to play
												sender.sendMessage(String.format(plugin.getLang("Left"), minLeft, promoteTo));
											}
										}
										else {
											sender.sendMessage(plugin.getLang("LeftNone"));
										}
										
									}
								}
								//if the argument number is greater or equal than 2 and the sender's name is not the same
								//with the player checked.
								else if(sender.hasPermission("tranker.left.others") && args.length >= 2) {
									
									//if worldspecific groups is set and the arguments length is 3, we check promotions in the world specified
									if(args.length == 3 && plugin.cfg.getConfig().getBoolean("worldSpecificGroups")) {
										
										String world = args[2];
										String player = args[1];
										
										String currentGroup = perms.getPrimaryGroup(world, player); //get player's current group in current world
										if(currentGroup == null) {
											if(plugin.cfg.getConfig().getString("defaultGroup") != "" && plugin.cfg.getConfig().getString("defaultGroup") != null) {
												currentGroup = plugin.cfg.getConfig().getString("defaultGroup");
											}
											else {
												currentGroup = "default";
											}
										}
										
										if(plugin.perworld.getConfig().contains("promote." + world + "." + currentGroup)) {
											int minReq = plugin.timeInMinutes(plugin.perworld.getConfig().getString("promote." + world + "." + currentGroup + ".timeReq")); //get the required minutes played from the config
											String promoteTo = plugin.perworld.getConfig().getString("promote." + world + "." + currentGroup + ".to"); //get the group the player must be ranked-up to
											ResultSet result = plugin.sqlite.query("SELECT COUNT(*) as cnt, playtime FROM playtime WHERE  UPPER(playername) =  UPPER('" + player + "');");
											if(result.getInt("cnt") > 0) {
												if(minReq - result.getInt("playtime") > 0) {
													String minLeft = parseTime(minReq - result.getInt("playtime")); //minutes left to play
													sender.sendMessage(String.format(plugin.getLang("LeftOthers"), player, minLeft, promoteTo) + " " + String.format(plugin.lang.getConfig().getString("InWorld"), world));
												}
												else {
													sender.sendMessage(String.format(plugin.getLang("LeftOthersShortly"), player, promoteTo)  + " " + String.format(plugin.lang.getConfig().getString("InWorld"), world));
												}
											} else {
												String minLeft = parseTime(minReq); //minutes left to play
												sender.sendMessage(String.format(plugin.getLang("LeftOthers"), player, minLeft, promoteTo) + " " + String.format(plugin.lang.getConfig().getString("InWorld"), world));
											}
										}
										else {
											sender.sendMessage(String.format(plugin.getLang("LeftOthersNone"), player)  + " " + String.format(plugin.lang.getConfig().getString("InWorld"), world));
										}
									
									} else { //otherwise, check the player without looking at the world. This might be a bit buggy ? Je ne sais pas, yet.
										String world = null;
										String player = args[1];
										String currentGroup = perms.getPrimaryGroup(world, player); //get player's current group
										if(currentGroup == null) {
											if(plugin.cfg.getConfig().getString("defaultGroup") != "" && plugin.cfg.getConfig().getString("defaultGroup") != null) {
												currentGroup = plugin.cfg.getConfig().getString("defaultGroup");
											}
											else {
												currentGroup = "default";
											}
										}
										
										if(plugin.cfg.getConfig().contains("promote." + currentGroup)) {
											int minReq = plugin.timeInMinutes(plugin.cfg.getConfig().getString("promote." + currentGroup + ".timeReq")); //get the required minutes played from the config
											String promoteTo = plugin.cfg.getConfig().getString("promote." + currentGroup + ".to"); //get the group the player must be ranked-up to
											ResultSet result = plugin.sqlite.query("SELECT COUNT(*) as cnt, playtime FROM playtime WHERE  UPPER(playername) =  UPPER('" + player + "');");
											if(result.getInt("cnt") > 0) {
												if(minReq - result.getInt("playtime") > 0) {
													String minLeft = parseTime(minReq - result.getInt("playtime")); //minutes left to play
													sender.sendMessage(String.format(plugin.getLang("LeftOthers"), player, minLeft, promoteTo));
												}
												else {
													sender.sendMessage(String.format(plugin.getLang("LeftOthersShortly"), player, promoteTo));
												}
											} else {
												String minLeft = parseTime(minReq); //minutes left to play
												sender.sendMessage(String.format(plugin.getLang("LeftOthers"), player, minLeft, promoteTo));
											}
										}
										else {
											sender.sendMessage(String.format(plugin.getLang("LeftOthersNone"), player));
										}
										
									}
									
								} else if(!(sender instanceof Player)) {
									sender.sendMessage(plugin.parseString("") + "This command can only be run by a player"); 
								} else {
									sender.sendMessage(plugin.getLang("noPermission"));
								}
							} else {
								sender.sendMessage(plugin.getLang("noPermission"));
							}
						break;
						//the ugly part ends here.
					case "purge":
						if(sender.hasPermission("tranker.purge")) {
							plugin.sqlite.close();
							plugin.sqlConnection();
							plugin.sqlite.query("DROP TABLE playtime");
							plugin.sqlTableCheck();
							sender.sendMessage(plugin.getLang("Purge"));
						} else {
							sender.sendMessage(plugin.getLang("noPermission"));
						}
						break;
					case "reload":
						if(sender.hasPermission("tranker.reload")) {
							plugin.cfg.reloadConfig();
							if(plugin.cfg.getConfig().getBoolean("worldSpecificGroups")) {
								plugin.perworld.reloadConfig();
							}
							plugin.lang.reloadConfig();
							sender.sendMessage(plugin.getLang("Reload"));
						} else {
							sender.sendMessage(plugin.getLang("noPermission"));
						}
						break;
				}
				
			} catch(IllegalArgumentException e) {
				plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e.getMessage()));
				return false;
			} catch (SQLException e2) {
				plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e2.getMessage()));
				return false;
			}
			return true;
	}
	
	//parse the time played to show days and hours when neccesary
	public String parseTime(int minutes) {
		int hours=0, days=0;
		String formatted = "";
		if(minutes>=60) {
			hours = (int) minutes/60;
			minutes = minutes - hours*60;
			if(hours>=24) {
				days = (int) hours/24;
				hours = hours - days*24;
			}
		}
		
		if(days > 0) {
			formatted += String.format(" %s " + plugin.lang.getConfig().getString("Days"), days);
		}
		
		if(hours > 0) {
			formatted += String.format(" %s " + plugin.lang.getConfig().getString("Hours"), hours);
		}
		
		if(minutes > 0) {
			formatted += String.format(" %s " + plugin.lang.getConfig().getString("Minutes"), minutes);
		}
		
		return formatted;
	}
	
}
