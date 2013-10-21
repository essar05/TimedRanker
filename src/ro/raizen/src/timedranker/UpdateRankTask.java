package ro.raizen.src.timedranker;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateRankTask implements Listener,Runnable {

	private TimedRanker plugin;
	private Permission perms;
	private TempData tempdata;
	
	public UpdateRankTask(TimedRanker plugin, Permission perms, TempData tempdata) {
		this.plugin = plugin;
		this.perms = perms;
		this.tempdata = tempdata;
		plugin.getServer().getScheduler().runTaskLater(plugin, this, getPeriod());
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		plugin.debugInfo("Player joined, checking for promotion");
		CheckPlayer(e.getPlayer()); //check if the player has to be ranked up when he joins
	}
	
	@Override
	public void run() {
		//save tempdata to db, purge tempdata and then check all player online for rank-up
		tempdata.save();
		tempdata.purge();
		plugin.debugInfo("Checking online players for promotions");
		Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
		for(int i=0; i<onlinePlayers.length; i++) {
			CheckPlayer(onlinePlayers[i]);
		}
		plugin.getServer().getScheduler().runTaskLater(plugin, this, getPeriod()); 
	}
	
	private void CheckPlayer(Player p) {
		try {
			ResultSet result = plugin.sql.query("SELECT COUNT(id) AS cnt,playtime FROM playtime WHERE playername = '" + p.getName() + "';");
			result.next();
			if(result.getInt("cnt") > 0) { //check if player has an entry in the database
				
				//Check if promotions should be checked/set per world
				if(plugin.cfg.getConfig().getBoolean("worldSpecificGroups")) {
					
					Set<String> worlds = plugin.perworld.getConfig().getConfigurationSection("promote").getKeys(false);
					for(String world : worlds) {
						String currentGroup = perms.getPrimaryGroup(world, p.getName());

						if(currentGroup == null) {
							if(plugin.cfg.getConfig().getString("defaultGroup") != "" && plugin.cfg.getConfig().getString("defaultGroup") != null) {
								currentGroup = plugin.cfg.getConfig().getString("defaultGroup");
							}
							else {
								currentGroup = "default";
							}
						}
						
						if(plugin.perworld.getConfig().contains("promote." + world + "." + currentGroup)) { //if the current group of this player, has a promote entry in the config
							int minReq = plugin.timeInMinutes(plugin.perworld.getConfig().getString("promote." + world + "." + currentGroup + ".timeReq")); //get the required minutes played from the config
							String promoteTo = plugin.perworld.getConfig().getString("promote." + world + "." + currentGroup + ".to"); //get the group the player must be ranked-up to
							if(result.getInt("playtime") >= minReq) { //if his total playtime is higher than the minimum required
								perms.playerAddGroup(world, p.getName(), promoteTo); //add new group
								if(plugin.getServer().getPluginManager().getPlugin("Privileges") == null) {
									perms.playerRemoveGroup(world, p.getName(), currentGroup); //remove old group
								}
								p.sendMessage(String.format(plugin.getLang("PromotionMessageWorld"), promoteTo, world));
								plugin.debugInfo(p.getName() + " promoted in world " + world);
							}
						}
					}
					
				} else {
					
					String world = null;
					String currentGroup = perms.getPrimaryGroup(world, p.getName());

					if(currentGroup == null) {
						if(plugin.cfg.getConfig().getString("defaultGroup") != "" && plugin.cfg.getConfig().getString("defaultGroup") != null) {
							currentGroup = plugin.cfg.getConfig().getString("defaultGroup");
						}
						else {
							currentGroup = "default";
						}
					}
					
					if(plugin.cfg.getConfig().contains("promote." + currentGroup)) { //if the current group of this player, has a promote entry in the config
						int minReq = plugin.timeInMinutes(plugin.cfg.getConfig().getString("promote." + currentGroup + ".timeReq")); //get the required minutes played from the config
						String promoteTo = plugin.cfg.getConfig().getString("promote." + currentGroup + ".to"); //get the group the player must be ranked-up to
						if(result.getInt("playtime") >= minReq) { //if his total playtime is higher than the minimum required
							perms.playerAddGroup(world, p.getName(), promoteTo); //add new group
							if(plugin.getServer().getPluginManager().getPlugin("Privileges") == null) {
								perms.playerRemoveGroup(world, p.getName(), currentGroup); //remove old group
							}
							p.sendMessage(String.format(plugin.getLang("PromotionMessage"), promoteTo));
							plugin.debugInfo(p.getName() + " promoted");
						}
					}
					
				}
			}
			result.close();
		} catch (SQLException e1) {
			plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e1.getMessage()));
		} 
	}
	
	private int getPeriod() {
		int period = 6005;
		if(plugin.cfg.getConfig().getInt("checkPeriod") > 0) {
			period = (plugin.cfg.getConfig().getInt("checkPeriod"))*60*20 + 5;
		}
		return period;
	}
	
}
