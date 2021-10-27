package com.KAMKEEL.PluginHealth;

import org.bukkit.plugin.java.*;
import org.bukkit.entity.*;
import org.bukkit.permissions.*;
import java.util.*;
import org.bukkit.plugin.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.command.*;
import org.bukkit.*;

public class PluginHealth extends JavaPlugin implements Listener
{
    // Keep checking these players
    public HashMap<String, Integer> TaskIDList;

    // Keep track of Permission Health changes
    public HashMap<String, Double> PermissionHealth;
    
    public PluginHealth() {
        this.TaskIDList = new HashMap<String, Integer>();
        this.PermissionHealth = new HashMap<String, Double>();
    }

    public void setMaxHealth(final Player p) {
        double total = getPermissionHealth(p);

        if(total != this.PermissionHealth.get(p.getName())) {
            // Get Current Max Health
            double currentHealth = p.getHealth();
            if (total > 0) {
                p.setMaxHealth(total);
                p.setHealth(Math.min(currentHealth, p.getMaxHealth()));
                PermissionHealth.put(p.getName(), total);
            } else {
                p.setMaxHealth(20.0);
                p.setHealth(Math.min(currentHealth, 20.0));
                PermissionHealth.put(p.getName(), 20.0);
            }
        }
    }

    public double getPermissionHealth(final Player p){
        final HashMap<Integer, Integer> checkList = new HashMap<>();
        double total = 0;
        for (final PermissionAttachmentInfo perm : p.getEffectivePermissions()) {
            try {
                if (!perm.getPermission().toLowerCase().startsWith("maxhealth.")) {
                    continue;
                }
                final String[] permission = perm.getPermission().toLowerCase().split("\\.");
                final int permLength = permission.length - 1;

                // final int sl = perm.getPermission().toLowerCase().length() - perm.getPermission().toLowerCase().replace(".", "").length();
                if(permLength == 1){
                    if (permission[1].equals("reload")) {
                        continue;
                    }
                    try {
                        total += Integer.parseInt(permission[1]);
                    }
                    catch (NumberFormatException e) {
                        System.out.println(String.valueOf(perm.getPermission()) + " is not a vaild permission!");
                    }
                }
                else if (permLength == 2){
                    try {
                        final int priority = Integer.parseInt(permission[1]);
                        final int health = Integer.parseInt(permission[2]);
                        checkList.merge(priority, health, Integer::sum);
                    }
                    catch (NumberFormatException e) {
                        System.out.println(String.valueOf(perm.getPermission()) + " is not a vaild permission!");
                    }
                }
//                if (sl == 1) {
//                    try {
//                        total += Integer.parseInt(perm.getPermission().toLowerCase().substring(10));
//                    }
//                    catch (NumberFormatException e) {
//                        if (perm.getPermission().toLowerCase().substring(10).equals("reload")) {
//                            continue;
//                        }
//                        System.out.println(String.valueOf(perm.getPermission()) + " is not a vaild permission!");
//                    }
//                }
//                else if (sl == 2) {
//                    try {
//                        final int pr = Integer.parseInt(perm.getPermission().substring(10, perm.getPermission().indexOf(".", 10)));
//                        final int t = Integer.parseInt(perm.getPermission().substring(perm.getPermission().indexOf(".", 10) + 1));
//                        if (checkList.get(pr) != null) {
//                            checkList.put(pr, checkList.get(pr) + t);
//                        }
//                        else {
//                            checkList.put(pr, t);
//                        }
//                    }
//                    catch (NumberFormatException e) {
//                        System.out.println(String.valueOf(perm.getPermission()) + " is not a vaild permission!");
//                    }
//                }
                else {
                    System.out.println(String.valueOf(perm.getPermission()) + " is not a vaild permission!");
                }
            }
            catch (IndexOutOfBoundsException ex) {}
        }
        final SortedSet<Integer> keys = new TreeSet<Integer>(checkList.keySet());
        if (keys.size() > 0) {
            total += checkList.get(keys.toArray()[keys.toArray().length - 1]);
        }
        return total;
    }


    public void checkSetMaxHealth(final Player p) {
        this.PermissionHealth.put(p.getName(), 20.0);
        this.TaskIDList.put(p.getName(), Bukkit.getScheduler().scheduleAsyncRepeatingTask((Plugin)this, (Runnable)new Runnable() {
            @Override
            public void run() {
                PluginHealth.this.setMaxHealth(p);
            }
        }, 0L, (long)this.getConfig().getInt("CheckDelay")));
    }
    
    public void onEnable() {
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        for (final Player p : Bukkit.getOnlinePlayers()) {
            this.checkSetMaxHealth(p);
        }
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        System.out.println("[PluginHealth]: Plugin have been enabled!");
    }
    
    public void onDisable() {
        this.getServer().getScheduler().cancelAllTasks();
        this.TaskIDList = new HashMap<String, Integer>();
        System.out.println("[PluginHealth]: Plugin have been disabled!");
    }
    
    @EventHandler
    public void onJoin(final PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        this.checkSetMaxHealth(p);
        p.setHealth(p.getMaxHealth());
    }
    
    @EventHandler
    public void onRespawn(final PlayerRespawnEvent e) {
        final Player p = e.getPlayer();
        p.setMaxHealth(PermissionHealth.get(p.getName()));
        p.setHealth(p.getMaxHealth());
    }
    
    @EventHandler
    public void onQuit(final PlayerQuitEvent e) {
        final Player p = e.getPlayer();
        if (this.TaskIDList.get(p.getName()) != null) {
            Bukkit.getScheduler().cancelTask((int)this.TaskIDList.get(p.getName()));
            this.TaskIDList.remove(p.getName());
        }
        if (this.PermissionHealth.get(p.getName()) != null) {
            this.PermissionHealth.remove(p.getName());
        }
    }
    
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (label.equalsIgnoreCase("maxhealth")) {
            boolean c = false;
            if (!(sender instanceof Player)) {
                c = true;
            }
            else if (((Player)sender).hasPermission("maxhealth.reload")) {
                c = true;
            }
            if (c) {
                boolean c2 = false;
                if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                    c2 = true;
                }
                if (!c2) {
                    if (sender instanceof Player) {
                        ((Player)sender).sendMessage(ChatColor.RED + "Usage: " + ChatColor.GREEN + "/maxhealth reload");
                    }
                    else {
                        this.getServer().getConsoleSender().sendMessage(ChatColor.RED + "Usage: " + ChatColor.GREEN + "/maxhealth reload");
                    }
                    return true;
                }
                this.reloadConfig();
                if (sender instanceof Player) {
                    ((Player)sender).sendMessage(ChatColor.RED + "[PluginHealth]: " + ChatColor.GREEN + "Config reloaded");
                }
                else {
                    this.getServer().getConsoleSender().sendMessage(ChatColor.RED + "[PluginHealth]: " + ChatColor.GREEN + "Config reloaded");
                }
                return true;
            }
        }
        return false;
    }
}
