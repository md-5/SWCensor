package com.md_5.swcensor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Censor extends JavaPlugin implements Listener {

    private int minAge;
    private FileConfiguration config;
    private Map<String, Integer> ages = new HashMap<String, Integer>();
    private Map<String, List<String>> words = new HashMap<String, List<String>>();

    @Override
    public void onEnable() {
        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();
        minAge = config.getInt("minAge");
        ConfigurationSection wordSection = config.getConfigurationSection("words");
        for (String s : wordSection.getKeys(false)) {
            words.put(s, wordSection.getStringList(s));
        }
        ConfigurationSection ageSection = config.getConfigurationSection("ages");
        for (String s : ageSection.getKeys(false)) {
            ages.put(s, ageSection.getInt(s));
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = sender.getName();
        switch (args.length) {
            case 0:
                Object message = (ages.containsKey(name)) ? ages.get(name) : " <not set>";
                sender.sendMessage(ChatColor.BLUE + "Your age is currently set at: " + message);
                break;
            case 1:
                try {
                    int age = Integer.parseInt(args[0]);
                    ages.put(name, age);
                    config.set("ages." + name, age);
                    saveConfig();
                    sender.sendMessage(ChatColor.BLUE + "Your age has been set to: " + age);
                } catch (Exception ex) {
                    sender.sendMessage(ChatColor.RED + "Error! " + args[0] + " is not a number.");
                }
                break;
            default:
                sender.sendMessage(ChatColor.RED + "That is not a valid command");
                break;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        if (!ages.containsKey(name)) {
            player.sendMessage(ChatColor.RED + "You cannot talk until you have set your age. Use /age <age> to set it.");
            event.setCancelled(true);
            return;
        }
        String message = event.getMessage();
        String censored = event.getMessage();
        for (Map.Entry<String, List<String>> entry : words.entrySet()) {
            for (String w : entry.getValue()) {
                censored = censored.replaceAll("\\b" + w + "\\b", entry.getKey());
            }
        }
        message = String.format(event.getFormat(), event.getPlayer().getDisplayName(), message);
        censored = String.format(event.getFormat(), event.getPlayer().getDisplayName(), censored);
        Set<Player> recipients = event.getRecipients();
        for (Player p : recipients) {
            if (ages.get(p.getName()) > minAge) {
                p.sendMessage(message);
            } else {
                p.sendMessage(censored);
            }
        }
        event.setCancelled(true);
    }
}
