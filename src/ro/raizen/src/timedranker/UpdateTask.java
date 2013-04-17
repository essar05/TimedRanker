package ro.raizen.src.timedranker;

import java.util.TimerTask;
import org.bukkit.entity.Player;

public class UpdateTask extends TimerTask {
	
	private TimedRanker plugin;
	private Player[] onlinePlayers;
	private TempData tempdata;
	
	public UpdateTask(TimedRanker plugin) {
		this.plugin = plugin;
		tempdata = plugin.getTempData();
	}
	
	public void run() {
		onlinePlayers = plugin.getServer().getOnlinePlayers(); //get online players array
		for(int i = 0; i < onlinePlayers.length; i++) { //iterate through the array
			String PlayerName = onlinePlayers[i].getName();
			int Playtime;
			if(tempdata.isset(PlayerName)) { 
				Playtime = tempdata.get(PlayerName); //get previous temp playtime
			} else {
				Playtime = 0; //set it to 0, if the player isn't in the temp data hashmap
			}
			
			int toCount = 1;
			
			if(plugin.isAfk(onlinePlayers[i])) {
				toCount = 0; //don't count player if he's AFK;
			}
			
			if(toCount == 1) {
				tempdata.set(PlayerName, Playtime + 1); //add 1 minute to the tempdata
			}
		}
		plugin.debugInfo("TempData updated");
	}
	
}
