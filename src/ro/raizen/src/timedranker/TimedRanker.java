package ro.raizen.src.timedranker;

import java.sql.SQLException;
import java.util.Timer;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;

import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.royaldev.royalcommands.AFKUtils;

import com.earth2me.essentials.IEssentials;

import ro.raizen.src.timedranker.config.ConfigHandler;

public class TimedRanker extends JavaPlugin {
	
	public Logger clog = Logger.getLogger("Minecraft");
	public SQLite sqlite;
	public TempData tempdata;
	public static Permission perms = null;
	private Timer timer;
	private UpdateRankTask rankupdate;
	private IEssentials ess;
	public ConfigHandler cfg;
	public ConfigHandler perworld;
	public ConfigHandler lang;
	
	@Override
	public void onEnable() {
		
		cfg = new ConfigHandler(this, "config.yml"); //Init the config handler
		cfg.saveDefaultConfig(); 
		lang = new ConfigHandler(this, "lang.yml"); //Init the config handler
		lang.saveDefaultConfig();
		perworld = new ConfigHandler(this, "perworld.yml"); //Init the config handler
		perworld.saveDefaultConfig(); 
		clog.info(String.format("[%s] Config files loaded", getDescription().getName()));
		
		
		int hasDependencies = 1;
		
		//Check if Essentials is installed
		if(getConfig().getBoolean("essentialsAfk") == true) {
			if(getServer().getPluginManager().getPlugin("Essentials") == null) {
				clog.severe(String.format("[%s] Dependency Essentials not found", getDescription().getName()));
				hasDependencies = 0;
				getPluginLoader().disablePlugin(this);
			}
		}
		
		//Check if RoyalCommands is installed
		if(getConfig().getBoolean("rcmdsAfk") == true) {
			if(getServer().getPluginManager().getPlugin("RoyalCommands") == null) {
				clog.severe(String.format("[%s] Dependency RoyalCommands not found", getDescription().getName()));
				hasDependencies = 0;
				getPluginLoader().disablePlugin(this);
			}
		}

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
			
			if(getConfig().getBoolean("essentialsAfk") == true) {
				ess = (IEssentials) getServer().getPluginManager().getPlugin("Essentials");
			}
			
			tempdata = new TempData(this); //Init the object that holds temporary data
			getCommand("tranker").setExecutor(new CommandHandler(this, perms)); //Init the executor for tranker command
			rankupdate = new UpdateRankTask(this, perms, tempdata); //Init the object that updates ranks, when neccesary
			PluginManager pm = getServer().getPluginManager();
			pm.registerEvents(this.rankupdate, this); //Register the rank updater as event listener
		
			timer = new Timer(); 
			UpdateTask update = new UpdateTask(this); 
			timer.scheduleAtFixedRate(update, 60000, 60000); //Set up the UpdateTask to run every minute, updating temp data.
			
		}
	}

	@Override
	public void onDisable() {
		tempdata.save();
		tempdata.purge();
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
			clog.info(String.format("[%s] %s", getDescription().getName(), e.getMessage()));
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
	
	public boolean isAfk(Player player) {
		boolean afk = false, afk2 = false;
		if(cfg.getConfig().getBoolean("essentialsAfk") == true) {
			afk = ess.getOfflineUser(player.getName()).isAfk();
		} else if(cfg.getConfig().getBoolean("rcmdsAfk") == true) {
			//return rcmds.isAfk(player);
			afk2 = AFKUtils.isAfk(player);
		}
		if(afk == true || afk2 == true) {
			return true;
		}
		else return false;
	}
	
	public int timeInMinutes(String s) {
		int d = 0, h = 0, m = 0;
		String[] s2 = s.split(" ");
		
		try {
			for(String x : s2) {
				if(x.contains("m")) {
					m = Integer.parseInt(x.split("m")[0]);
				} else if (x.contains("h")) {
					h = Integer.parseInt(x.split("h")[0]);
				} else if (x.contains("d")) {
					d = Integer.parseInt(x.split("d")[0]);
			    }			
			}
		} catch(NumberFormatException e) {
			e.printStackTrace();
		}
		
		return (m + h*60 + d*24*60);
	}
	
	public void debugInfo(String s) {
		if(cfg.getConfig().getBoolean("debugMode") == true) {
			clog.info(String.format("[%s][Debug] %s", getDescription().getName(), s));
		}
	}

	public String Prefix() {
		if(lang.getConfig().getString("Prefix") == "" || lang.getConfig().getString("Prefix") == null)
			return "";
		else return lang.getConfig().getString("Prefix") + " ";
	}
	
}
