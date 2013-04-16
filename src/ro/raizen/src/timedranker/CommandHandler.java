package ro.raizen.src.timedranker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
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
					sender.sendMessage(String.format("[%s] /tranker playtime <player> - See player's gameplay time", plugin.getDescription().getName()));	
				}
				else if((sender instanceof Player) && sender.hasPermission("tranker.playtime.me")) {
					sender.sendMessage(String.format("[%s] /tranker playtime - See your gameplay time", plugin.getDescription().getName()));
				}
				if(sender.hasPermission("tranker.top")) {
					sender.sendMessage(String.format("[%s] /tranker top - View a top 10 of most-online players", plugin.getDescription().getName()));
				}
				if(sender.hasPermission("tranker.left")) {
					sender.sendMessage(String.format("[%s] /tranker left - View time left until next promotion", plugin.getDescription().getName()));
				}
				if(sender.hasPermission("tranker.settime")) { 
					sender.sendMessage(String.format("[%s] /tranker settime <player> <time> - Set minutes played for a player", plugin.getDescription().getName()));
				}
				if(sender.hasPermission("tranker.purge")) {
					sender.sendMessage(String.format("[%s] /tranker purge - Purge database", plugin.getDescription().getName()));
				}
				if(sender.hasPermission("tranker.reload")) {
					sender.sendMessage(String.format("[%s] /tranker reload - Reload configuration", plugin.getDescription().getName()));
				}
				return true;
			}
			
			try 
			{
				String subCommand = args[0].toLowerCase();
				//most of it is self-explanatory
				switch(subCommand) {
					case "playtime":
						if(args.length == 1) {
							if(sender instanceof Player) {
								if(sender.hasPermission("tranker.playtime.me")) {  
									ResultSet result = plugin.sqlite.query("SELECT COUNT(*) as cnt, playtime FROM playtime WHERE playername = '" + sender.getName() + "';");
									if(result.getInt("cnt") > 0) {
										int playtime = result.getInt("playtime");
										sender.sendMessage(String.format("[%s] Your gameplay time is%s", plugin.getDescription().getName(), parseTime(playtime)));
									} else {
										sender.sendMessage(String.format("[%s] Invalid player name or database not updated.", plugin.getDescription().getName()));
									}
									result.close();
								}
								else {
									sender.sendMessage(String.format("[%s] You don't have permission to use this command", plugin.getDescription().getName()));
								}
							}
							else {
								sender.sendMessage(String.format("[%s] You must specify a player's name", plugin.getDescription().getName()));
							}
						} else if(args.length == 2) {
							if(sender instanceof Player && !sender.hasPermission("tranker.playtime.others")) {
								sender.sendMessage(String.format("[%s] You don't have permission to use this command", plugin.getDescription().getName()));
							}
							else {
								ResultSet result = plugin.sqlite.query("SELECT COUNT(*) as cnt, playtime FROM playtime WHERE playername = '" + args[1] + "';");
								if(result.getInt("cnt") > 0) {
									int playtime = result.getInt("playtime");
									sender.sendMessage(String.format("[%s] %s's gameplay time is%s", plugin.getDescription().getName(), args[1], parseTime(playtime)));
								} else {
									sender.sendMessage(String.format("[%s] Invalid player name or database not updated.", plugin.getDescription().getName()));
								}
								result.close();
							}
						}
						break;
					case "settime":
						if(sender instanceof Player && !sender.hasPermission("tranker.settime")) {
							sender.sendMessage(String.format("[%s] You don't have permission to use this command", plugin.getDescription().getName()));
						}
						else {
							if(args.length == 3) {
								String player = args[1];
								int minutes;
								try {
									minutes = new Integer(args[2]);
									ResultSet result = plugin.sqlite.query("SELECT COUNT(*) as cnt FROM playtime WHERE playername = '" + player + "';");
									if(result.getInt("cnt") > 0) {
										plugin.sqlite.query("UPDATE playtime SET playtime='" + minutes +"' WHERE playername = '" + player + "';");
										sender.sendMessage(String.format("[%s] Set%s played for player %s", plugin.getDescription().getName(), parseTime(minutes), player));
									}
									else {
										plugin.sqlite.query("INSERT INTO playtime (playername, playtime) VALUES('" + player + "', '" + minutes + "');");
										sender.sendMessage(String.format("[%s] Created entry for %s with%s played", plugin.getDescription().getName(), player, parseTime(minutes)));
									}
									result.close();
								} catch (NumberFormatException ex) {
									sender.sendMessage(String.format("[%s] The minutes parameter must be an integer", plugin.getDescription().getName()));
							    }
								
							} else {
								sender.sendMessage(String.format("[%s] Usage: /tranker settime <player> <minutes>", plugin.getDescription().getName()));
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
									sender.sendMessage(String.format("[%s] %s. %s - %s", plugin.getDescription().getName(), count, result.getString("playername"), parseTime(result.getInt("playtime"))));
								}
							} else {
								sender.sendMessage(String.format("[%s] No players to show", plugin.getDescription().getName()));
							}
						} else {
							sender.sendMessage(String.format("[%s] You don't have permission to use this command", plugin.getDescription().getName()));
						}
						break;
					case "left":
						if(sender instanceof Player) {
							if(sender.hasPermission("tranker.left")) {
								String currentGroup = perms.getPrimaryGroup((Player) sender); //get player's current group
								if(currentGroup == null) currentGroup = "default"; //if it's null, we'll set it to default
								if(plugin.getConfig().contains("promote." + currentGroup)) {
									int minReq = plugin.timeInMinutes(plugin.getConfig().getString("promote." + currentGroup + ".timeReq")); //get the required minutes played from the config
									String promoteTo = plugin.getConfig().getString("promote." + currentGroup + ".to"); //get the group the player must be ranked-up to
									ResultSet result = plugin.sqlite.query("SELECT COUNT(*) as cnt, playtime FROM playtime WHERE playername = '" + sender.getName() + "';");
									if(result.getInt("cnt") > 0) {
										if(minReq - result.getInt("playtime") > 0) {
											String minLeft = parseTime(minReq - result.getInt("playtime")); //minutes left to play
											sender.sendMessage(String.format("[%s] You have%s left to play until you get promoted to %s", plugin.getDescription().getName(), minLeft, promoteTo));
										}
										else {
											sender.sendMessage(String.format("[%s] You should get promoted to %s in a few minutes", plugin.getDescription().getName(), promoteTo));
										}
									} else {
										String minLeft = parseTime(minReq); //minutes left to play
										sender.sendMessage(String.format("[%s] You have%s left to play until you get promoted to %s", plugin.getDescription().getName(), minLeft, promoteTo));
									}
								}
								else {
									sender.sendMessage(String.format("[%s] You don't have any promotions pending", plugin.getDescription().getName()));
								}
							} else {
								sender.sendMessage(String.format("[%s] You don't have permission to use this command", plugin.getDescription().getName()));
							}
						} else {
							sender.sendMessage(String.format("[%s] Only players can use this command", plugin.getDescription().getName()));
						}
						break;
					case "purge":
						if(sender.hasPermission("tranker.purge")) {
							plugin.sqlite.close();
							plugin.sqlConnection();
							plugin.sqlite.query("DROP TABLE playtime");
							plugin.sqlTableCheck();
							sender.sendMessage(String.format("[%s] Database purged", plugin.getDescription().getName()));
						} else {
							sender.sendMessage(String.format("[%s] You don't have permission to use this command", plugin.getDescription().getName()));
						}
						break;
					case "reload":
						if(sender.hasPermission("tranker.reload")) {
							plugin.getConfig().load(plugin.getDataFolder().getAbsolutePath() + File.separator + "config.yml");
							sender.sendMessage(String.format("[%s] Config reloaded", plugin.getDescription().getName()));
						} else {
							sender.sendMessage(String.format("[%s] You don't have permission to use this command", plugin.getDescription().getName()));
						}
						break;
				}
				
			} catch(IllegalArgumentException e) {
				plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e.getMessage()));
				return false;
			} catch (SQLException e) {
				plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e.getMessage()));
				return false;
			} catch (FileNotFoundException e) {
				plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e.getMessage()));
				return false;
			} catch (IOException e) {
				plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e.getMessage()));
				return false;
			} catch (InvalidConfigurationException e) {
				plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e.getMessage()));
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
			formatted += String.format(" %s day(s)", days);
		}
		
		if(hours > 0) {
			formatted += String.format(" %s hour(s)", hours);
		}
		
		if(minutes > 0) {
			formatted += String.format(" %s minute(s)", minutes);
		}
		
		return formatted;
	}
	
}
