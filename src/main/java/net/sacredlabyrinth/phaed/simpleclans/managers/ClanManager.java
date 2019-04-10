package net.sacredlabyrinth.phaed.simpleclans.managers;

import net.sacredlabyrinth.phaed.simpleclans.*;
import net.sacredlabyrinth.phaed.simpleclans.uuid.UUIDMigration;
import net.sacredlabyrinth.phaed.simpleclans.events.CreateClanEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.text.MessageFormat;
import java.util.*;
import net.sacredlabyrinth.phaed.simpleclans.events.ChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * @author phaed
 */
public final class ClanManager {

    private SimpleClans plugin;
    private HashMap<String, Clan> clans = new HashMap<>();
    private HashMap<String, ClanPlayer> clanPlayers = new HashMap<>();

    /**
     *
     */
    public ClanManager() {
        plugin = SimpleClans.getInstance();
    }

    /**
     * Deletes all clans and clan players in memory
     */
    public void cleanData() {
        clans.clear();
        clanPlayers.clear();
    }

    /**
     * Import a clan into the in-memory store
     *
     * @param clan
     */
    public void importClan(Clan clan) {
        this.clans.put(clan.getTag(), clan);
    }

    /**
     * Import a clan player into the in-memory store
     *
     * @param cp
     */
    public void importClanPlayer(ClanPlayer cp) {
        if (SimpleClans.getInstance().hasUUID()) {
            if (cp.getUniqueId() != null) {
                this.clanPlayers.put(cp.getUniqueId().toString(), cp);
            }
        } else {
            this.clanPlayers.put(cp.getCleanName(), cp);
        }
    }

    /**
     * Create a new clan
     *
     * @param player
     * @param colorTag
     * @param name
     */
    public void createClan(Player player, String colorTag, String name) {
        ClanPlayer cp;
        if (SimpleClans.getInstance().hasUUID()) {
            cp = getCreateClanPlayer(player.getUniqueId());
        } else {
            cp = getCreateClanPlayer(player.getName());
        }

        boolean verified = !plugin.getSettingsManager().isRequireVerification() || plugin.getPermissionsManager().has(player, "simpleclans.mod.verify");

        Clan clan = new Clan(colorTag, name, verified);
        clan.addPlayerToClan(cp);
        cp.setLeader(true);

        plugin.getStorageManager().insertClan(clan);
        importClan(clan);
        plugin.getStorageManager().updateClanPlayer(cp);

        SimpleClans.getInstance().getPermissionsManager().updateClanPermissions(clan);
        SimpleClans.getInstance().getServer().getPluginManager().callEvent(new CreateClanEvent(clan));
    }
    
    /**
     * Reset a player's kdr
     * @param cp 
     */
    public void resetKdr(ClanPlayer cp) {
        cp.setCivilianKills(0);
        cp.setNeutralKills(0);
        cp.setRivalKills(0);
        cp.setDeaths(0);
        plugin.getStorageManager().updateClanPlayerAsync(cp);
    }

    /**
     * Delete a players data file
     *
     * @param cp
     */
    public void deleteClanPlayer(ClanPlayer cp) {
        clanPlayers.remove(cp.getCleanName());
        plugin.getStorageManager().deleteClanPlayer(cp);
    }

    /**
     * Delete a player data from memory
     *
     * @param playerUniqueId
     */
    public void deleteClanPlayerFromMemory(UUID playerUniqueId) {
        clanPlayers.remove(playerUniqueId.toString());
    }

    /**
     * Remove a clan from memory
     *
     * @param tag
     */
    public void removeClan(String tag) {
        clans.remove(tag);
    }

    /**
     * Whether the tag belongs to a clan
     *
     * @param tag
     * @return
     */
    public boolean isClan(String tag) {
        return clans.containsKey(Helper.cleanTag(tag));

    }

    /**
     * Returns the clan the tag belongs to
     *
     * @param tag
     * @return
     */
    public Clan getClan(String tag) {
        return clans.get(Helper.cleanTag(tag));
    }

    /**
     * Get a player's clan
     *
     * @param playerName
     * @return null if not in a clan
     */
    @Deprecated
    public Clan getClanByPlayerName(String playerName) {
        ClanPlayer cp = getClanPlayer(playerName);

        if (cp != null) {
            return cp.getClan();
        }

        return null;
    }

    /**
     * Get a player's clan
     *
     * @param playerUniqueId
     * @return null if not in a clan
     */
    public Clan getClanByPlayerUniqueId(UUID playerUniqueId) {
        ClanPlayer cp = getClanPlayer(playerUniqueId);

        if (cp != null) {
            return cp.getClan();
        }

        return null;
    }

    /**
     * @return the clans
     */
    public List<Clan> getClans() {
        return new ArrayList<>(clans.values());
    }

    /**
     * Returns the collection of all clan players, including the disabled ones
     *
     * @return
     */
    public List<ClanPlayer> getAllClanPlayers() {
        return new ArrayList<>(clanPlayers.values());
    }

    /**
     * Gets the ClanPlayer data object if a player is currently in a clan, null
     * if he's not in a clan Used for BungeeCord Reload ClanPlayer and your Clan
     *
     * @param player
     * @return
     */
    public ClanPlayer getClanPlayerJoinEvent(Player player) {
        SimpleClans.getInstance().getStorageManager().importFromDatabaseOnePlayer(player);
        if (SimpleClans.getInstance().hasUUID()) {
            return getClanPlayer(player.getUniqueId());
        } else {
            return getClanPlayer(player.getName());
        }
    }

    /**
     * Gets the ClanPlayer data object if a player is currently in a clan, null
     * if he's not in a clan
     *
     * @param player
     * @return
     */
    public ClanPlayer getClanPlayer(OfflinePlayer player) {
        if (SimpleClans.getInstance().hasUUID()) {
            return getClanPlayer(player.getUniqueId());
        } else {
            return getClanPlayer(player.getName());
        }
    }
    
    
    /**
     * Gets the ClanPlayer data object if a player is currently in a clan, null
     * if he's not in a clan
     *
     * @param player
     * @return
     */
    public ClanPlayer getClanPlayer(Player player) {
        return getClanPlayer((OfflinePlayer) player);
    }

    /**
     * Gets the ClanPlayer data object if a player is currently in a clan, null
     * if he's not in a clan
     *
     * @param playerName
     * @return
     */
    @Deprecated
    public ClanPlayer getClanPlayer(String playerName) {
        ClanPlayer cp;
        if (SimpleClans.getInstance().hasUUID()) {
            cp = getClanPlayerName(playerName);
        } else {
            cp = clanPlayers.get(playerName.toLowerCase());
        }

        if (cp == null) {
            return null;
        }

        if (cp.getClan() == null) {
            return null;
        }

        return cp;
    }

    /**
     * Gets the ClanPlayer data object if a player is currently in a clan, null
     * if he's not in a clan
     *
     * @param playerUniqueId
     * @return
     */
    public ClanPlayer getClanPlayer(UUID playerUniqueId) {
        ClanPlayer cp = clanPlayers.get(playerUniqueId.toString());

        if (cp == null) {
            return null;
        }

        if (cp.getClan() == null) {
            return null;
        }

        return cp;
    }

    /**
     * Gets the ClanPlayer data object if a player is currently in a clan, null
     * if he's not in a clan
     *
     * @param playerDisplayName
     * @return
     */
    public ClanPlayer getClanPlayerName(String playerDisplayName) {
        UUID uuid = UUIDMigration.getForcedPlayerUUID(playerDisplayName);

        if (uuid == null) {
            return null;
        }

        ClanPlayer cp = clanPlayers.get(uuid.toString());

        if (cp == null) {
            return null;
        }

        if (cp.getClan() == null) {
            return null;
        }

        return cp;
    }

    /**
     * Gets the ClanPlayer data object for the player, will retrieve disabled
     * clan players as well, these are players who used to be in a clan but are
     * not currently in one, their data file persists and can be accessed. their
     * clan will be null though.
     *
     * @param playerName
     * @return
     */
    @Deprecated
    public ClanPlayer getAnyClanPlayer(String playerName) {
        if (SimpleClans.getInstance().hasUUID()) {
            return getClanPlayerName(playerName);
        } else {
            return clanPlayers.get(playerName.toLowerCase());
        }
    }

    /**
     * Gets the ClanPlayer data object for the player, will retrieve disabled
     * clan players as well, these are players who used to be in a clan but are
     * not currently in one, their data file persists and can be accessed. their
     * clan will be null though.
     *
     * @param playerUniqueId
     * @return
     */
    public ClanPlayer getAnyClanPlayer(UUID playerUniqueId) {
        return clanPlayers.get(playerUniqueId.toString());
    }

    /**
     * Gets the ClanPlayer object for the player, creates one if not found
     *
     * @param playerName
     * @return
     */
    @Deprecated
    public ClanPlayer getCreateClanPlayer(String playerName) {
        if (clanPlayers.containsKey(playerName.toLowerCase())) {
            return clanPlayers.get(playerName.toLowerCase());
        }

        ClanPlayer cp = new ClanPlayer(playerName);

        plugin.getStorageManager().insertClanPlayer(cp);
        importClanPlayer(cp);

        return cp;
    }

    /**
     * Gets the ClanPlayer object for the player, creates one if not found
     *
     * @param playerDisplayName
     * @return
     */
    public ClanPlayer getCreateClanPlayerUUID(String playerDisplayName) {
        if (SimpleClans.getInstance().hasUUID()) {
            UUID playerUniqueId = UUIDMigration.getForcedPlayerUUID(playerDisplayName);
            if (playerUniqueId != null) {
                return getCreateClanPlayer(playerUniqueId);
            } else {
                return null;
            }
        } else {
            return getCreateClanPlayer(playerDisplayName);
        }
    }

    /**
     * Gets the ClanPlayer object for the player, creates one if not found
     *
     * @param playerUniqueId
     * @return
     */
    public ClanPlayer getCreateClanPlayer(UUID playerUniqueId) {
        if (clanPlayers.containsKey(playerUniqueId.toString())) {
            return clanPlayers.get(playerUniqueId.toString());
        }

        ClanPlayer cp = new ClanPlayer(playerUniqueId);

        plugin.getStorageManager().insertClanPlayer(cp);
        importClanPlayer(cp);

        return cp;
    }

    /**
     * Announce message to the server
     *
     * @param msg
     */
    public void serverAnnounce(String msg) {
        Collection<Player> players = Helper.getOnlinePlayers();

        for (Player player : players) {
            ChatBlock.sendMessage(player, ChatColor.DARK_GRAY + "* " + ChatColor.AQUA + msg);
        }

        SimpleClans.getInstance().getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "[" + plugin.getLang("server.announce") + "] " + ChatColor.WHITE + msg);
    }

    /**
     * Update the players display name with his clan's tag
     *
     * @param player
     */
    public void updateDisplayName(Player player) {
        // do not update displayname if in compat mode

        if (plugin.getSettingsManager().isCompatMode()) {
            return;
        }

        if (player == null) {
            return;
        }

        if (plugin.getSettingsManager().isChatTags()) {
            String prefix = plugin.getPermissionsManager().getPrefix(player);
            String suffix = plugin.getPermissionsManager().getSuffix(player);
            String lastColor = plugin.getSettingsManager().isUseColorCodeFromPrefix() ? Helper.getLastColorCode(prefix) : ChatColor.WHITE + "";
            String fullName = player.getName();

            ClanPlayer cp = plugin.getClanManager().getAnyClanPlayer(player.getName());

            if (cp == null) {
                return;
            }

            if (cp.isTagEnabled()) {
                Clan clan = cp.getClan();

                if (clan != null) {
                    fullName = clan.getTagLabel(cp.isLeader()) + lastColor + fullName + ChatColor.WHITE;
                }

                player.setDisplayName(fullName);
            } else {
                player.setDisplayName(lastColor + fullName + ChatColor.WHITE);
            }
        }
    }

    /**
     * Process a player and his clan's last seen date
     *
     * @param player
     */
    public void updateLastSeen(Player player) {
        ClanPlayer cp = getAnyClanPlayer(player.getName());

        if (cp != null) {
            cp.updateLastSeen();
            plugin.getStorageManager().updateClanPlayerAsync(cp);

            Clan clan = cp.getClan();

            if (clan != null) {
                clan.updateLastUsed();
                plugin.getStorageManager().updateClanAsync(clan);
            }
        }
    }

    /**
     * @param playerName
     */
    public void ban(String playerName) {
        ClanPlayer cp = getClanPlayer(playerName);
        Clan clan = cp.getClan();

        if (clan != null) {
            if (clan.getSize() == 1) {
                clan.disband();
            } else {
                cp.setClan(null);
                cp.addPastClan(clan.getColorTag() + (cp.isLeader() ? ChatColor.DARK_RED + "*" : ""));
                cp.setLeader(false);
                cp.setJoinDate(0);
                clan.removeMember(playerName);

                plugin.getStorageManager().updateClanPlayer(cp);
                plugin.getStorageManager().updateClan(clan);
            }
        }

        plugin.getSettingsManager().addBanned(playerName);
    }

    /**
     * Get a count of rivable clans
     *
     * @return
     */
    public int getRivableClanCount() {
        int clanCount = 0;

        for (Clan tm : clans.values()) {
            if (!SimpleClans.getInstance().getSettingsManager().isUnrivable(tm.getTag())) {
                clanCount++;
            }
        }

        return clanCount;
    }

    /**
     * Returns a formatted string detailing the players armor
     *
     * @param inv
     * @return
     */
    public String getArmorString(PlayerInventory inv) {
        String out = "";

        ItemStack h = inv.getHelmet();

        if (h != null) {
            if (h.getType().equals(Material.CHAINMAIL_HELMET)) {
                out += ChatColor.WHITE + plugin.getLang("armor.h");
            } else if (h.getType().equals(Material.DIAMOND_HELMET)) {
                out += ChatColor.AQUA + plugin.getLang("armor.h");
            } else if (h.getType().equals(Material.GOLDEN_HELMET)) {
                out += ChatColor.YELLOW + plugin.getLang("armor.h");
            } else if (h.getType().equals(Material.IRON_HELMET)) {
                out += ChatColor.GRAY + plugin.getLang("armor.h");
            } else if (h.getType().equals(Material.LEATHER_HELMET)) {
                out += ChatColor.GOLD + plugin.getLang("armor.h");
            } else if (h.getType().equals(Material.AIR)) {
                out += ChatColor.BLACK + plugin.getLang("armor.h");
            } else {
                out += ChatColor.RED + plugin.getLang("armor.h");
            }
        }
        ItemStack c = inv.getChestplate();

        if (c != null) {
            if (c.getType().equals(Material.CHAINMAIL_CHESTPLATE)) {
                out += ChatColor.WHITE + plugin.getLang("armor.c");
            } else if (c.getType().equals(Material.DIAMOND_CHESTPLATE)) {
                out += ChatColor.AQUA + plugin.getLang("armor.c");
            } else if (c.getType().equals(Material.GOLDEN_CHESTPLATE)) {
                out += ChatColor.YELLOW + plugin.getLang("armor.c");
            } else if (c.getType().equals(Material.IRON_CHESTPLATE)) {
                out += ChatColor.GRAY + plugin.getLang("armor.c");
            } else if (c.getType().equals(Material.LEATHER_CHESTPLATE)) {
                out += ChatColor.GOLD + plugin.getLang("armor.c");
            } else if (c.getType().equals(Material.AIR)) {
                out += ChatColor.BLACK + plugin.getLang("armor.c");
            } else {
                out += ChatColor.RED + plugin.getLang("armor.c");
            }
        }
        ItemStack l = inv.getLeggings();

        if (l != null) {
            if (l.getType().equals(Material.CHAINMAIL_LEGGINGS)) {
                out += ChatColor.WHITE + plugin.getLang("armor.l");
            } else if (l.getType().equals(Material.DIAMOND_LEGGINGS)) {
                out += plugin.getLang("armor.l");
            } else if (l.getType().equals(Material.GOLDEN_LEGGINGS)) {
                out += plugin.getLang("armor.l");
            } else if (l.getType().equals(Material.IRON_LEGGINGS)) {
                out += plugin.getLang("armor.l");
            } else if (l.getType().equals(Material.LEATHER_LEGGINGS)) {
                out += plugin.getLang("armor.l");
            } else if (l.getType().equals(Material.AIR)) {
                out += plugin.getLang("armor.l");
            } else {
                out += plugin.getLang("armor.l");
            }
        }
        ItemStack b = inv.getBoots();

        if (b != null) {
            if (b.getType().equals(Material.CHAINMAIL_BOOTS)) {
                out += ChatColor.WHITE + plugin.getLang("armor.B");
            } else if (b.getType().equals(Material.DIAMOND_BOOTS)) {
                out += ChatColor.AQUA + plugin.getLang("armor.B");
            } else if (b.getType().equals(Material.GOLDEN_BOOTS)) {
                out += ChatColor.YELLOW + plugin.getLang("armor.B");
            } else if (b.getType().equals(Material.IRON_BOOTS)) {
                out += ChatColor.WHITE + plugin.getLang("armor.B");
            } else if (b.getType().equals(Material.LEATHER_BOOTS)) {
                out += ChatColor.GOLD + plugin.getLang("armor.B");
            } else if (b.getType().equals(Material.AIR)) {
                out += ChatColor.BLACK + plugin.getLang("armor.B");
            } else {
                out += ChatColor.RED + plugin.getLang("armor.B");
            }
        }

        if (out.length() == 0) {
            out = ChatColor.BLACK + "None";
        }

        return out;
    }

    /**
     * Returns a formatted string detailing the players weapons
     *
     * @param inv
     * @return
     */
    public String getWeaponString(PlayerInventory inv) {
        String headColor = plugin.getSettingsManager().getPageHeadingsColor();

        String out = "";

        int count = getItemCount(inv.all(Material.DIAMOND_SWORD));

        if (count > 0) {
            String countString = count > 1 ? count + "" : "";
            out += ChatColor.AQUA + plugin.getLang("weapon.S") + headColor + countString;
        }

        count = getItemCount(inv.all(Material.GOLDEN_SWORD));

        if (count > 0) {
            String countString = count > 1 ? count + "" : "";
            out += ChatColor.YELLOW + plugin.getLang("weapon.S") + headColor + countString;
        }

        count = getItemCount(inv.all(Material.IRON_SWORD));

        if (count > 0) {
            String countString = count > 1 ? count + "" : "";
            out += ChatColor.WHITE + plugin.getLang("weapon.S") + headColor + countString;
        }

        count = getItemCount(inv.all(Material.STONE_SWORD));

        if (count > 0) {
            String countString = count > 1 ? count + "" : "";
            out += ChatColor.GRAY + plugin.getLang("weapon.S") + headColor + countString;
        }

        count = getItemCount(inv.all(Material.WOODEN_SWORD));

        if (count > 0) {
            String countString = count > 1 ? count + "" : "";
            out += ChatColor.GOLD + plugin.getLang("weapon.S") + headColor + countString;
        }

        count = getItemCount(inv.all(Material.BOW));

        if (count > 0) {
            String countString = count > 1 ? count + "" : "";
            out += ChatColor.GOLD + plugin.getLang("weapon.B") + headColor + countString;
        }

        count = getItemCount(inv.all(Material.ARROW));
        count += getItemCount(inv.all(Material.SPECTRAL_ARROW));
        count += getItemCount(inv.all(Material.TIPPED_ARROW));

        if (count > 0) {
            out += ChatColor.WHITE + headColor + count;
        }

        if (out.length() == 0) {
            out = ChatColor.BLACK + "None";
        }

        return out;
    }

    private int getItemCount(HashMap<Integer, ? extends ItemStack> all) {
        int count = 0;

        for (ItemStack is : all.values()) {
            count += is.getAmount();
        }

        return count;
    }

    private double getFoodPoints(PlayerInventory inv, Material material, int points, double saturation) {
        return getItemCount(inv.all(material)) * (points + saturation);
    }

    private double getFoodPoints(PlayerInventory inv, Material material, int type, int points, double saturation) {
        return getItemCount(inv.all(new ItemStack(material, 1, (short) type))) * (points + saturation);
    }

    /**
     * Returns a formatted string detailing the players food
     *
     * @param inv
     * @return
     */
    public String getFoodString(PlayerInventory inv) {

        double count = getFoodPoints(inv, Material.APPLE, 4, 2.4);
        count += getFoodPoints(inv, Material.BAKED_POTATO, 5, 6);
        count += getFoodPoints(inv, Material.BEETROOT, 1, 1.2);
        count += getFoodPoints(inv, Material.BEETROOT_SOUP, 6, 7.2);
        count += getFoodPoints(inv, Material.BREAD, 5, 6);
        count += getFoodPoints(inv, Material.CAKE, 14, 2.8);
        count += getFoodPoints(inv, Material.CARROT, 3, 3.6);
        count += getFoodPoints(inv, Material.CHORUS_FRUIT, 4, 2.4);
        count += getFoodPoints(inv, Material.TROPICAL_FISH, 5, .2);
        count += getFoodPoints(inv, Material.COOKED_CHICKEN, 6, 7.2);
        count += getFoodPoints(inv, Material.COOKED_MUTTON, 6, 9.6);
        count += getFoodPoints(inv, Material.COOKED_PORKCHOP, 8, 12.8);
        count += getFoodPoints(inv, Material.COOKED_RABBIT, 5, 6);
        count += getFoodPoints(inv, Material.COOKED_SALMON, 6, 9.6);
        count += getFoodPoints(inv, Material.COOKIE, 2, .4);
        count += getFoodPoints(inv, Material.GOLDEN_APPLE, 4, 9.6);
        count += getFoodPoints(inv, Material.GOLDEN_CARROT, 6, 14.4);
        count += getFoodPoints(inv, Material.MELON, 2, 1.2);
        count += getFoodPoints(inv, Material.MUSHROOM_STEW, 6, 7.2);
        count += getFoodPoints(inv, Material.POISONOUS_POTATO, 2, 1.2);
        count += getFoodPoints(inv, Material.POTATO, 1, 0.6);
        count += getFoodPoints(inv, Material.PUFFERFISH, 1, 0.2);
        count += getFoodPoints(inv, Material.PUMPKIN_PIE, 8, 4.8);
        count += getFoodPoints(inv, Material.RABBIT_STEW, 10, 12);
        count += getFoodPoints(inv, Material.BEEF, 3, 1.8);
        count += getFoodPoints(inv, Material.CHICKEN, 2, 1.2);
        count += getFoodPoints(inv, Material.MUTTON, 2, 1.2);
        count += getFoodPoints(inv, Material.PORKCHOP, 3, 1.8);
        count += getFoodPoints(inv, Material.RABBIT, 3, 1.8);
        count += getFoodPoints(inv, Material.SALMON, 1, .4);
        count += getFoodPoints(inv, Material.ROTTEN_FLESH, 4, .8);
        count += getFoodPoints(inv, Material.SPIDER_EYE, 2, 3.2);
        count += getFoodPoints(inv, Material.COOKED_BEEF, 8, 12.8);

        if (count == 0) {
            return ChatColor.BLACK + plugin.getLang("none");
        } else {
            return ((int) count) + "" + ChatColor.GOLD + "p";
        }
    }

    /**
     * Returns a formatted string detailing the players health
     *
     * @param health
     * @return
     */
    public String getHealthString(double health) {
        String out = "";

        if (health >= 16) {
            out += ChatColor.GREEN;
        } else if (health >= 8) {
            out += ChatColor.GOLD;
        } else {
            out += ChatColor.RED;
        }

        for (int i = 0; i < health; i++) {
            out += '|';
        }

        return out;
    }

    /**
     * Returns a formatted string detailing the players hunger
     *
     * @param health
     * @return
     */
    public String getHungerString(int health) {
        String out = "";

        if (health >= 16) {
            out += ChatColor.GREEN;
        } else if (health >= 8) {
            out += ChatColor.GOLD;
        } else {
            out += ChatColor.RED;
        }

        for (int i = 0; i < health; i++) {
            out += '|';
        }

        return out;
    }

    /**
     * Sort clans by KDR
     *
     * @param clans
     * @return
     */
    public void sortClansByKDR(List<Clan> clans) {
        Collections.sort(clans, new Comparator<Clan>() {

            @Override
            public int compare(Clan c1, Clan c2) {
                Float o1 = c1.getTotalKDR();
                Float o2 = c2.getTotalKDR();

                return o2.compareTo(o1);
            }
        });
    }

    /**
     * Sort clans by KDR
     *
     * @param clans
     */
    public void sortClansBySize(List<Clan> clans) {
        Collections.sort(clans, new Comparator<Clan>() {

            @Override
            public int compare(Clan c1, Clan c2) {
                Integer o1 = c1.getAllMembers().size();
                Integer o2 = c2.getAllMembers().size();

                return o2.compareTo(o1);
            }
        });
    }

    /**
     * Sort clan players by KDR
     *
     * @param cps
     * @return
     */
    public void sortClanPlayersByKDR(List<ClanPlayer> cps) {
        Collections.sort(cps, new Comparator<ClanPlayer>() {

            @Override
            public int compare(ClanPlayer c1, ClanPlayer c2) {
                Float o1 = c1.getKDR();
                Float o2 = c2.getKDR();

                return o2.compareTo(o1);
            }
        });
    }

    /**
     * Sort clan players by last seen days
     *
     * @param cps
     */
    public void sortClanPlayersByLastSeen(List<ClanPlayer> cps) {
        Collections.sort(cps, new Comparator<ClanPlayer>() {

            @Override
            public int compare(ClanPlayer c1, ClanPlayer c2) {
                Double o1 = c1.getLastSeenDays();
                Double o2 = c2.getLastSeenDays();

                return o1.compareTo(o2);
            }
        });
    }

    /**
     * Purchase clan creation
     *
     * @param player
     * @return
     */
    public boolean purchaseCreation(Player player) {
        if (!plugin.getSettingsManager().isePurchaseCreation()) {
            return true;
        }

        double price = plugin.getSettingsManager().getCreationPrice();

        if (plugin.getPermissionsManager().hasEconomy()) {
            if (plugin.getPermissionsManager().playerHasMoney(player, price)) {
                plugin.getPermissionsManager().playerChargeMoney(player, price);
                player.sendMessage(ChatColor.RED + MessageFormat.format(plugin.getLang("account.has.been.debited"), price));
            } else {
                player.sendMessage(ChatColor.RED + plugin.getLang("not.sufficient.money"));
                return false;
            }
        }

        return true;
    }

    /**
     * Purchase invite
     *
     * @param player
     * @return
     */
    public boolean purchaseInvite(Player player) {
        if (!plugin.getSettingsManager().isePurchaseInvite()) {
            return true;
        }

        double price = plugin.getSettingsManager().getInvitePrice();

        if (plugin.getPermissionsManager().hasEconomy()) {
            if (plugin.getPermissionsManager().playerHasMoney(player, price)) {
                plugin.getPermissionsManager().playerChargeMoney(player, price);
                player.sendMessage(ChatColor.RED + MessageFormat.format(plugin.getLang("account.has.been.debited"), price));
            } else {
                player.sendMessage(ChatColor.RED + plugin.getLang("not.sufficient.money"));
                return false;
            }
        }

        return true;
    }

    /**
     * Purchase Home Teleport
     *
     * @param player
     * @return
     */
    public boolean purchaseHomeTeleport(Player player) {
        if (!plugin.getSettingsManager().isePurchaseHomeTeleport()) {
            return true;
        }

        double price = plugin.getSettingsManager().getHomeTeleportPrice();

        if (plugin.getPermissionsManager().hasEconomy()) {
            if (plugin.getPermissionsManager().playerHasMoney(player, price)) {
                plugin.getPermissionsManager().playerChargeMoney(player, price);
                player.sendMessage(ChatColor.RED + MessageFormat.format(plugin.getLang("account.has.been.debited"), price));
            } else {
                player.sendMessage(ChatColor.RED + plugin.getLang("not.sufficient.money"));
                return false;
            }
        }

        return true;
    }

    /**
     * Purchase Home Teleport Set
     *
     * @param player
     * @return
     */
    public boolean purchaseHomeTeleportSet(Player player) {
        if (!plugin.getSettingsManager().isePurchaseHomeTeleportSet()) {
            return true;
        }

        double price = plugin.getSettingsManager().getHomeTeleportPriceSet();

        if (plugin.getPermissionsManager().hasEconomy()) {
            if (plugin.getPermissionsManager().playerHasMoney(player, price)) {
                plugin.getPermissionsManager().playerChargeMoney(player, price);
                player.sendMessage(ChatColor.RED + MessageFormat.format(plugin.getLang("account.has.been.debited"), price));
            } else {
                player.sendMessage(ChatColor.RED + plugin.getLang("not.sufficient.money"));
                return false;
            }
        }

        return true;
    }
    
    /**
     * Purchase Reset Kdr
     * 
     * @param player
     * @return
     */
    public boolean purchaseResetKdr(Player player) {
        if (!plugin.getSettingsManager().isePurchaseResetKdr()) {
            return true;
        }
        
        double price = plugin.getSettingsManager().geteResetKdr();
        
        if (plugin.getPermissionsManager().hasEconomy()) {
            if (plugin.getPermissionsManager().playerHasMoney(player, price)) {
                plugin.getPermissionsManager().playerChargeMoney(player, price);
                player.sendMessage(ChatColor.RED + MessageFormat.format(plugin.getLang("account.has.been.debited"), price));
            } else {
                player.sendMessage(ChatColor.RED + plugin.getLang("not.sufficient.money"));
                return false;
            }
        }

        return true;
    }

    /**
     * Purchase Home Regroup
     *
     * @param player
     * @return
     */
    public boolean purchaseHomeRegroup(Player player) {
        ClanPlayer cp = plugin.getClanManager().getClanPlayer(player);

        if (!plugin.getSettingsManager().isePurchaseHomeRegroup()) {
            return true;
        }

        double price = plugin.getSettingsManager().getHomeRegroupPrice();
        if (!plugin.getSettingsManager().iseUniqueTaxOnRegroup()) {
            price = price * cp.getClan().getOnlineMembers().size();
        }

        if (plugin.getSettingsManager().iseIssuerPaysRegroup()) {
            if (plugin.getPermissionsManager().hasEconomy()) {
                if (plugin.getPermissionsManager().playerHasMoney(player, price)) {
                    plugin.getPermissionsManager().playerChargeMoney(player, price);
                    player.sendMessage(ChatColor.RED + MessageFormat.format(plugin.getLang("account.has.been.debited"), price));
                } else {
                    player.sendMessage(ChatColor.RED + plugin.getLang("not.sufficient.money"));
                    return false;
                }
            }
        } else {
            Clan clan = cp.getClan();
            double balance = clan.getBalance();
            if (price > balance) {
                player.sendMessage(ChatColor.RED + plugin.getLang("clan.bank.not.enough.money"));
                return false;
            }
            clan.withdraw(price, player);
        }

        return true;
    }

    /**
     * Purchase clan verification
     *
     * @param player
     * @return
     */
    public boolean purchaseVerification(Player player) {
        if (!plugin.getSettingsManager().isePurchaseVerification()) {
            return true;
        }

        double price = plugin.getSettingsManager().getVerificationPrice();

        if (plugin.getPermissionsManager().hasEconomy()) {
            if (plugin.getPermissionsManager().playerHasMoney(player, price)) {
                plugin.getPermissionsManager().playerChargeMoney(player, price);
                player.sendMessage(ChatColor.RED + MessageFormat.format(plugin.getLang("account.has.been.debited"), price));
            } else {
                player.sendMessage(ChatColor.RED + plugin.getLang("not.sufficient.money"));
                return false;
            }
        }

        return true;
    }

    /**
     * Processes a clan chat command
     *
     * @param player
     * @param msg
     */
    public void processClanChat(Player player, String tag, String msg) {
        Clan clan = plugin.getClanManager().getClan(tag);

        if (clan == null || !clan.isMember(player)) {
            return;
        }

        processClanChat(player, msg);
    }

    /**
     * Processes a clan chat command
     *
     * @param player
     * @param msg
     */
    public void processClanChat(Player player, String msg) {
        ClanPlayer cp = plugin.getClanManager().getClanPlayer(player.getName());

        if (cp == null) {
            return;
        }

        String[] split = msg.split(" ");

        if (split.length == 0) {
            return;
        }

        String command = split[0];

        if (command.equals(plugin.getLang("on"))) {
            cp.setClanChat(true);
            plugin.getStorageManager().updateClanPlayer(cp);
            ChatBlock.sendMessage(player, ChatColor.AQUA + "You have enabled clan chat");
        } else if (command.equals(plugin.getLang("off"))) {
            cp.setClanChat(false);
            plugin.getStorageManager().updateClanPlayer(cp);
            ChatBlock.sendMessage(player, ChatColor.AQUA + "You have disabled clan chat");
        } else if (command.equals(plugin.getLang("join"))) {
            cp.setChannel(ClanPlayer.Channel.CLAN);
            plugin.getStorageManager().updateClanPlayer(cp);
            ChatBlock.sendMessage(player, ChatColor.AQUA + "You have joined clan chat");
        } else if (command.equals(plugin.getLang("leave"))) {
            cp.setChannel(ClanPlayer.Channel.NONE);
            plugin.getStorageManager().updateClanPlayer(cp);
            ChatBlock.sendMessage(player, ChatColor.AQUA + "You have left clan chat");
        } else if (command.equals(plugin.getLang("mute"))) {
            if (cp.isMuted()) {
                cp.setMuted(true);
                ChatBlock.sendMessage(player, ChatColor.AQUA + "You have muted clan chat");
            } else {
                cp.setMuted(false);
                ChatBlock.sendMessage(player, ChatColor.AQUA + "You have unmuted clan chat");
            }
        } else {
            List<ClanPlayer> receivers = new LinkedList<>();
            for (ClanPlayer p : cp.getClan().getOnlineMembers()) {
                if (!p.isMuted()) {
                    receivers.add(p);
                }
            }
            
            ChatEvent ce = new ChatEvent(msg, cp, receivers, ChatEvent.Type.CLAN);
            Bukkit.getServer().getPluginManager().callEvent(ce);
            
            if (ce.isCancelled()) {
                return;
            }
            
            String message = Helper.formatClanChat(cp, ce.getMessage(), ce.getPlaceholders());
            String eyeMessage = Helper.formatSpyClanChat(cp, message);
            plugin.getServer().getConsoleSender().sendMessage(eyeMessage);
            
            for (ClanPlayer p : ce.getReceivers()) {
                ChatBlock.sendMessage(p.toPlayer(), message);
            }
            
            sendToAllSeeing(eyeMessage, ce.getReceivers());
        }
    }

    public void sendToAllSeeing(String msg, List<ClanPlayer> cps) {
        Collection<Player> players = Helper.getOnlinePlayers();

        for (Player player : players) {
            if (plugin.getPermissionsManager().has(player, "simpleclans.admin.all-seeing-eye")) {
                boolean alreadySent = false;

                ClanPlayer cp = plugin.getClanManager().getClanPlayer(player);

                if (cp != null && cp.isMuted()) {
                    continue;
                }

                for (ClanPlayer cpp : cps) {
                    if (cpp.getName().equalsIgnoreCase(player.getName())) {
                        alreadySent = true;
                    }
                }

                if (!alreadySent) {
                    ChatBlock.sendMessage(player, msg);
                }
            }
        }
    }

    /**
     * Processes a ally chat command
     *
     * @param player
     * @param msg
     */
    public void processAllyChat(Player player, String msg) {
        ClanPlayer cp = plugin.getClanManager().getClanPlayer(player);

        if (cp == null) {
            return;
        }

        String[] split = msg.split(" ");

        if (split.length == 0) {
            return;
        }

        String command = split[0];

        if (command.equals(plugin.getLang("on"))) {
            cp.setAllyChat(true);
            plugin.getStorageManager().updateClanPlayer(cp);
            ChatBlock.sendMessage(player, ChatColor.AQUA + "You have enabled ally chat");
        } else if (command.equals(plugin.getLang("off"))) {
            cp.setAllyChat(false);
            plugin.getStorageManager().updateClanPlayer(cp);
            ChatBlock.sendMessage(player, ChatColor.AQUA + "You have disabled ally chat");
        } else if (command.equals(plugin.getLang("join"))) {
            cp.setChannel(ClanPlayer.Channel.ALLY);
            plugin.getStorageManager().updateClanPlayer(cp);
            ChatBlock.sendMessage(player, ChatColor.AQUA + "You have joined ally chat");
        } else if (command.equals(plugin.getLang("leave"))) {
            cp.setChannel(ClanPlayer.Channel.NONE);
            plugin.getStorageManager().updateClanPlayer(cp);
            ChatBlock.sendMessage(player, ChatColor.AQUA + "You have left ally chat");
        } else if (command.equals(plugin.getLang("mute"))) {
            if (!cp.isMutedAlly()) {
                cp.setMutedAlly(true);
                ChatBlock.sendMessage(player, ChatColor.AQUA + "You have muted ally chat");
            } else {
                cp.setMutedAlly(false);
                ChatBlock.sendMessage(player, ChatColor.AQUA + "You have unmuted ally chat");
            }
        } else {
            List<ClanPlayer> receivers = new LinkedList<>();
            Set<ClanPlayer> allies = cp.getClan().getAllAllyMembers();
            allies.addAll(cp.getClan().getMembers());
            for (ClanPlayer ally : allies) {
                if (ally.isMutedAlly()) {
                    continue;
                }
                if (SimpleClans.getInstance().hasUUID()) {
                    if (player.getUniqueId().equals(ally.getUniqueId())) {
                        continue;
                    }
                } else {
                    if (player.getName().equalsIgnoreCase(ally.getName())) {
                        continue;
                    }
                }
                receivers.add(ally);
            }
            
            ChatEvent ce = new ChatEvent(msg, cp, receivers, ChatEvent.Type.ALLY);
            Bukkit.getServer().getPluginManager().callEvent(ce);
            
            if (ce.isCancelled()) {
                return;
            }
            
            String message = Helper.formatAllyChat(cp, ce.getMessage(), ce.getPlaceholders());
            SimpleClans.log(message);

            Player self = cp.toPlayer();
            ChatBlock.sendMessage(self, message);

            for (ClanPlayer p : ce.getReceivers()) {
                ChatBlock.sendMessage(p.toPlayer(), message);
            }
        }
    }

    /**
     * Processes a global chat command
     *
     * @param player
     * @param msg
     * @return boolean
     */
    public boolean processGlobalChat(Player player, String msg) {
        ClanPlayer cp;
        if (SimpleClans.getInstance().hasUUID()) {
            cp = plugin.getClanManager().getClanPlayer(player.getUniqueId());
        } else {
            cp = plugin.getClanManager().getClanPlayer(player.getName());
        }

        if (cp == null) {
            return false;
        }

        String[] split = msg.split(" ");

        if (split.length == 0) {
            return false;
        }

        String command = split[0];

        if (command.equals(plugin.getLang("on"))) {
            cp.setGlobalChat(true);
            plugin.getStorageManager().updateClanPlayer(cp);
            ChatBlock.sendMessage(player, ChatColor.AQUA + "You have enabled global chat");
        } else if (command.equals(plugin.getLang("off"))) {
            cp.setGlobalChat(false);
            plugin.getStorageManager().updateClanPlayer(cp);
            ChatBlock.sendMessage(player, ChatColor.AQUA + "You have disabled global chat");
        } else {
            return true;
        }

        return false;
    }
}
