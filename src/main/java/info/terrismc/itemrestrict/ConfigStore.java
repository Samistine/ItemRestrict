package info.terrismc.itemrestrict;

import java.util.List;
import java.util.logging.Level;
import org.bukkit.ChatColor;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class ConfigStore {
    private ItemRestrict plugin;
    private FileConfiguration config;

    // Cache config values
    private List<String> worldList;
    private List<String> usageBans;
    private List<String> equipBans;
    private List<String> craftingBans;
    private List<String> ownershipBans;
    private List<String> worldBans;

    public ConfigStore(ItemRestrict plugin) {
        this.plugin = plugin;

        // Force reload plugin
        reloadConfig();
    }

    public final void reloadConfig() {
        // Config operations
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        // Config variables
        config = plugin.getConfig();
        worldList = config.getStringList("Worlds");
        usageBans = config.getStringList("Bans.Usage");
        equipBans = config.getStringList("Bans.Equip");
        craftingBans = config.getStringList("Bans.Crafting");
        ownershipBans = config.getStringList("Bans.Ownership");
        worldBans = config.getStringList("Bans.World");
    }

    private boolean isEnabledWorld(World world) {
        return worldList.contains("All") || worldList.contains(world.getName());
    }

    private boolean isBanned(Block block, ActionType actionType) {
        return isBanned(getConfigString(block), actionType) ? true : isBanned(getConfigStringParent(block), actionType);
    }

    private boolean isBanned(ItemStack item, ActionType actionType) {
        return isBanned(getConfigString(item), actionType) ? true : isBanned(getConfigStringParent(item), actionType);
    }

    private boolean isBanned(String configString, ActionType actionType) {
        // Select proper HashMap
        switch (actionType) {
            case Usage:
                return usageBans.contains(configString);
            case Equip:
                return equipBans.contains(configString);
            case Crafting:
                return craftingBans.contains(configString);
            case Ownership:
                return ownershipBans.contains(configString);
            case World:
                return worldBans.contains(configString);
            default:
                // Should never reach here if all enum cases covered
                ItemRestrict.logger.log(Level.WARNING, "Unknown ActionType detected: {0}", actionType);
                return false;
        }
    }

    public boolean isBannable(Player player, ItemStack item, ActionType actionType) {
        // Check null
        if( item == null ) return false;

        // Check ban list
        if (isBanned(item, actionType)) {
            // Player checks
            if (player != null) {
                    // Check world
                    if( !isEnabledWorld( player.getWorld() ) ) return false;

                    // Check exclude permission
                    if( player.hasPermission("ItemRestrict.bypass." + getActionTypeString( actionType ) + "." + getConfigString( item ) ) ) return false;

                    // Check exclude parent permission
                    if( player.hasPermission("ItemRestrict.bypass." + getActionTypeString( actionType ) + "." + getConfigStringParent( item ) ) ) return false;
            }
            return true;
        }
        return false;
    }
	
    public boolean isBannable(Player player, Block block, ActionType actionType) {
        // Check null
        if( block == null ) return false;

        // Check ban list
        if (isBanned(block, actionType)) {
            // Player checks
            if (player != null) {
                // Check world
                if( !isEnabledWorld( player.getWorld() ) ) return false;

                // Check exclude permission
                if( player.hasPermission("ItemRestrict.bypass." + getActionTypeString( actionType ) + "." + getConfigString( block ) ) ) return false;

                // Check exclude parent permission
                if( player.hasPermission("ItemRestrict.bypass." + getActionTypeString( actionType ) + "." + getConfigStringParent( block ) ) ) return false;
            }
            return true;
        }
        return false;
    }
	
    public String getLabel(Block block) {
        String label = config.getString("Messages.labels." + getConfigString(block));
        if (label != null) {
            return translateColorCodes(label);
        }
        label = config.getString("Messages.labels." + getConfigStringParent(block));
        if (label != null) {
            return translateColorCodes(label);
        }
        return block.getType().name() + " (" + getConfigString(block) + ")";
    }

    public String getLabel(ItemStack item) {
        String label = config.getString("Messages.labels." + getConfigString(item));
        if (label != null) {
            return translateColorCodes(label);
        }
        label = config.getString("Messages.labels." + getConfigStringParent(item));
        if (label != null) {
            return translateColorCodes(label);
        }
        return item.getType().name() + " (" + getConfigString(item) + ")";
    }
	
    public String getReason(Block block) {
        String reason = config.getString("Messages.reasons." + getConfigString(block));
        if (reason != null) {
            return translateColorCodes(reason);
        }
        reason = config.getString("Messages.reasons." + getConfigStringParent(block));
        if (reason != null) {
            return translateColorCodes(reason);
        }
        return "Ask your server administrator.";
    }
    
    private String translateColorCodes(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public String getReason(ItemStack item) {
        String reason = config.getString("Messages.reasons." + getConfigString(item));
        if (reason != null) {
            return translateColorCodes(reason);
        }
        reason = config.getString("Messages.reasons." + getConfigStringParent(item));
        if (reason != null) {
            return translateColorCodes(reason);
        }
        return "Ask your server administrator.";
    }
	
    private String getActionTypeString(ActionType actionType) {
        // Select proper string
        switch (actionType) {
            case Usage:
                return "Usage";
            case Equip:
                return "Equip";
            case Crafting:
                return "Crafting";
            case Ownership:
                return "Ownership";
            case World:
                return "World";
            default:
                // Should never reach here if all enum cases covered
                ItemRestrict.logger.log(Level.WARNING, "Unknown ActionType detected: {0}", actionType);
                return "";
        }
    }

    private boolean isConfigString(String configString) {
        String[] magicNumbers = configString.split("-");

        // Check partition amount
        if( magicNumbers.length > 2 ) return false;

        // Check for integers
        for (String magicNumber : magicNumbers) {
            try {
                Integer.parseInt(magicNumber);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
	
    @SuppressWarnings("deprecation")
    private String getConfigString(Block block) {
        // Config version string of block id and data value 
        return "" + block.getTypeId() + "-" + block.getData();
    }

    @SuppressWarnings("deprecation")
    private String getConfigStringParent(Block block) {
        // Config version string of block id 
        return "" + block.getTypeId();
    }

    @SuppressWarnings("deprecation")
    private String getConfigString(ItemStack item) {
        // Config version string of item id and data value
        MaterialData matData = item.getData();
        return "" + matData.getItemTypeId() + "-" + matData.getData();
    }
	
    @SuppressWarnings("deprecation")
    private String getConfigStringParent(ItemStack item) {
        // Config version string of item id and data value
        return "" + item.getTypeId();
    }

    public double getScanFrequencyOnPlayerJoin() {
        return config.getDouble("Scanner.event.onPlayerJoin");
    }

    public double getScanFrequencyOnChunkLoad() {
        return config.getDouble("Scanner.event.onChunkLoad");
    }

    public int getBanListSize(ActionType actionType) {
        // Select proper HashMap
        switch (actionType) {
            case Usage:
                return usageBans.size();
            case Equip:
                return equipBans.size();
            case Crafting:
                return craftingBans.size();
            case Ownership:
                return ownershipBans.size();
            case World:
                return worldBans.size();
            default:
                // Should never reach here if all enum cases covered
                ItemRestrict.logger.log(Level.WARNING, "Unknown ActionType detected: {0}", actionType);
                return 0;
        }
    }

    public void addBan(CommandSender sender, ActionType actionType, String configString) {
        // Check valid actionType
        if (actionType == null) {
            sender.sendMessage("Invalid ban type. Valid ban types: Usage, Equip, Crafting, Ownership, World");
            return;
        }

        // Check valid config string
        if (!isConfigString(configString)) {
            sender.sendMessage(configString + "  is not a valid item");
            return;
        }

        switch (actionType) {
            case Usage:
                usageBans.add(configString);
                config.set("Bans.Usage", usageBans);
                break;
            case Equip:
                equipBans.add(configString);
                config.set("Bans.Equip", equipBans);
                break;
            case Crafting:
                craftingBans.add(configString);
                config.set("Bans.Crafting", craftingBans);
                break;
            case Ownership:
                ownershipBans.add(configString);
                config.set("Bans.Ownership", ownershipBans);
                break;
            case World:
                worldBans.add(configString);
                config.set("Bans.World", worldBans);
                break;
            default:
                // Should never reach here if all enum cases covered
                ItemRestrict.logger.log(Level.WARNING, "Unknown ActionType detected: {0}", actionType);
                return;
        }
        plugin.saveConfig();
        sender.sendMessage("Item Banned");
    }

    public void removeBan(CommandSender sender, ActionType actionType, String configString) {
        // Check valid actionType
        if (actionType == null) {
            sender.sendMessage("Invalid ban type. Valid ban types: Usage, Equip, Crafting, Ownership, World");
            return;
        }

        // Check valid config string
        if (!isConfigString(configString)) {
            sender.sendMessage(configString + " is not a valid item");
            return;
        }

        switch (actionType) {
            case Usage:
                usageBans.remove(configString);
                config.set("Bans.Usage", usageBans);
                break;
            case Equip:
                equipBans.remove(configString);
                config.set("Bans.Equip", equipBans);
                break;
            case Crafting:
                craftingBans.remove(configString);
                config.set("Bans.Crafting", craftingBans);
                break;
            case Ownership:
                ownershipBans.remove(configString);
                config.set("Bans.Ownership", ownershipBans);
                break;
            case World:
                worldBans.remove(configString);
                config.set("Bans.World", worldBans);
                break;
            default:
                // Should never reach here if all enum cases covered
                ItemRestrict.logger.log(Level.WARNING, "Unknown ActionType detected: {0}", actionType);
                return;
        }
        plugin.saveConfig();
        sender.sendMessage("Item Unbanned");
    }
}
