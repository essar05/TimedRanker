package ro.raizen.src.timedranker;

import java.sql.SQLException;
import java.util.Timer;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;

import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ro.raizen.src.timedranker.config.ConfigHandler;

public class TimedRanker extends JavaPlugin {
	
	public Logger clog = Logger.getLogger("Minecraft");
	public SQLite sqlite;
	public TempData tempdata;
	public static Permission perms = null;
	private Timer timer;
	private UpdateRankTask rankupdate;
	
	@Override
	public void onEnable() {
		
		int hasDependencies = 1;
		
		//Check if Vault is installed
		if(getServer().getPluginManager().getPlugin("Vault") == null) {
			clog.severe(String.format("[%s] Dependency Vault not found", getDescription().getName()));
			hasDependencies = 0;
			getPluginLoader().disablePlugin(this);
		}
		
		//Check if SQLibrary is installed
		if(getServer().getPluginManager().getPlugin("SQLibrary") == null) {
			clog.severe(String.format("[%s] Dependency SQLibrary not found", getDescription().getName()));
			hasDependencies = 0;
			getPluginLoader().disablePlugin(this);
		}
		
		//Dependencies found, proceed to enabling plugin
		if(hasDependencies == 1) {
			//Setting up Vault Permissions
			setupPermissions();
			//Connect to database and check structure
			sqlConnection();
			sqlTableCheck();
			
			tempdata = new TempData(this); //Init the object that holds temporary data
			getCommand("tranker").setExecutor(new CommandHandler(this)); //Init the executor for tranker command
			rankupdate = new UpdateRankTask(this, perms, tempdata); //Init the object that updates ranks, when neccesary
			PluginManager pm = getServer().getPluginManager();
			pm.registerEvents(this.rankupdate, this); //Register the rank updater as event listener
		
			ConfigHandler cfg = new ConfigHandler(this); //Init the config handler
			cfg.saveDefaultConfiguration(); 
			clog.info(String.format("[%s] Config loaded", getDescription().getName()));
			
			timer = new Timer(); 
			UpdateTask update = new UpdateTask(this); 
			timer.scheduleAtFixedRate(update, 60000, 60000); //Set up the UpdateTask to run every minute, updating temp data.
		}
	}

	@Override
	public void onDisable() {
		if(sqlite != null) {
			sqlite.close();
		}
		if(timer != null) {
			timer.cancel();
		}
		this.getServer().getScheduler().cancelAllTasks();
	}
	
	public void sqlTableCheck() {
		if(sqlite.isTable("playtime")) {
			return;
		} else {
			try {
				sqlite.query("CREATE TABLE playtime (id INTEGER PRIMARY KEY AUTOINCREMENT, playername VARCHAR(50), playtime INT);");
				clog.info(String.format("[%s] Table 'playtime' has been created.", getDescription().getName()));
			} catch (SQLException e) {
				clog.info(String.format("[%s] %s", getDescription().getName(), e.getMessage()));
			}
		}
	}

	public void sqlConnection() {
		sqlite = new SQLite(clog, "Timed Ranker", this.getDataFolder().getAbsolutePath(), "playtime");
		try {
			sqlite.open();
		} catch (Exception e) {
			clog.info(e.getMessage());
			this.getPluginLoader().disablePlugin(this);
		}
	}
	
	public TempData getTempData() {
		return tempdata;
	}
	
	private boolean setupPermissions() {
		//Vault permissions
		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
        if (rsp != null) {
            perms = rsp.getProvider();
        }
        return (perms != null);    
	}
	
}
