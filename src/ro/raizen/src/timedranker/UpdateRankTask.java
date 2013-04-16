package ro.raizen.src.timedranker;

import java.sql.ResultSet;
import java.sql.SQLException;

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
		plugin.getServer().getScheduler().runTaskLater(plugin, this, 6020); //run task after 5 minutes and 1 second;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		CheckPlayer(e.getPlayer()); //check if the player has to be ranked up when he joins
	}
	
	@Override
	public void run() {
		//save tempdata to db, purge tempdata and then check all player online for rank-up
		tempdata.save();
		tempdata.purge();
		Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
		for(int i=0; i<onlinePlayers.length; i++) {
			CheckPlayer(onlinePlayers[i]);
		}
		plugin.getServer().getScheduler().runTaskLater(plugin, this, 6020); //schedule it to run 5 minutes and 1 second later
	}
	
	public void CheckPlayer(Player p) {
		try {
			ResultSet result = plugin.sqlite.query("SELECT COUNT(id) AS cnt,playtime FROM playtime WHERE playername = '" + p.getName() + "';");
			if(result.getInt("cnt") > 0) { //check if player has an entry in the database
				String currentGroup = perms.getPrimaryGroup(p); //get player's current group
				if(currentGroup == null) currentGroup = "default"; //if it's null, we'll set it to default
				
				if(plugin.getConfig().contains("promote." + currentGroup)) { //if the current group of this player, has a promote entry in the config
					int minReq = plugin.getConfig().getInt("promote." + currentGroup + ".minutesreq"); //get the required minutes played from the config
					String promoteTo = plugin.getConfig().getString("promote." + currentGroup + ".to"); //get the group the player must be ranked-up to
					if(result.getInt("playtime") >= minReq) { //if his total playtime is higher than the minimum required
						String world = null;
						perms.playerAddGroup(world, p.getName(), promoteTo); //add new group
						perms.playerRemoveGroup(world, p.getName(), currentGroup); //remove old group
						p.sendMessage("Congratulations ! You have been promoted to " + promoteTo + " !");
					}
				}
			}
			result.close();
		} catch (SQLException e1) {
			plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e1.getMessage()));
		} 
	}
	
}
