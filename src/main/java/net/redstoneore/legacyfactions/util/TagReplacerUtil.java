package net.redstoneore.legacyfactions.util;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import net.redstoneore.legacyfactions.*;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.FPlayer;
import net.redstoneore.legacyfactions.entity.Faction;
import net.redstoneore.legacyfactions.entity.FactionColl;
import net.redstoneore.legacyfactions.integration.vault.VaultEngine;
import net.redstoneore.legacyfactions.integration.vault.VaultIntegration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Link between config and in-game messages<br> Changes based on faction / player<br> Interfaces the config lists with
 * {} variables to plugin
 */
public enum TagReplacerUtil {

    /**
     * Fancy variables, used by f show
     */
    ALLIES_LIST(TagType.FANCY, "{allies-list}"),
    ONLINE_LIST(TagType.FANCY, "{online-list}"),
    ENEMIES_LIST(TagType.FANCY, "{enemies-list}"),
    OFFLINE_LIST(TagType.FANCY, "{offline-list}"),

    /**
     * Player variables, require a player
     */
    PLAYER_GROUP(TagType.PLAYER, "{group}"),
    LAST_SEEN(TagType.PLAYER, "{lastSeen}"),
    PLAYER_BALANCE(TagType.PLAYER, "{balance}"),
    PLAYER_POWER(TagType.PLAYER, "{player-power}"),
    PLAYER_MAXPOWER(TagType.PLAYER, "{player-maxpower}"),
    PLAYER_KILLS(TagType.PLAYER, "{player-kills}"),
    PLAYER_DEATHS(TagType.PLAYER, "{player-deaths}"),

    /**
     * Faction variables, require at least a player
     */
    HOME_X(TagType.FACTION, "{x}"),
    HOME_Y(TagType.FACTION, "{y}"),
    HOME_Z(TagType.FACTION, "{z}"),
    CHUNKS(TagType.FACTION, "{chunks}"),
    WARPS(TagType.FACTION, "{warps}"),
    HEADER(TagType.FACTION, "{header}"),
    POWER(TagType.FACTION, "{power}"),
    MAX_POWER(TagType.FACTION, "{maxPower}"),
    POWER_BOOST(TagType.FACTION, "{power-boost}"),
    LEADER(TagType.FACTION, "{leader}"),
    JOINING(TagType.FACTION, "{joining}"),
    FACTION(TagType.FACTION, "{faction}"),
    PLAYER_NAME(TagType.FACTION, "{name}"),
    HOME_WORLD(TagType.FACTION, "{world}"),
    RAIDABLE(TagType.FACTION, "{raidable}"),
    PEACEFUL(TagType.FACTION, "{peaceful}"),
    PERMANENT(TagType.FACTION, "permanent"), // no braces needed
    TIME_LEFT(TagType.FACTION, "{time-left}"),
    LAND_VALUE(TagType.FACTION, "{land-value}"),
    DESCRIPTION(TagType.FACTION, "{description}"),
    CREATE_DATE(TagType.FACTION, "{create-date}"),
    LAND_REFUND(TagType.FACTION, "{land-refund}"),
    BANK_BALANCE(TagType.FACTION, "{faction-balance}"),
    ALLIES_COUNT(TagType.FACTION, "{allies}"),
    ENEMIES_COUNT(TagType.FACTION, "{enemies}"),
    ONLINE_COUNT(TagType.FACTION, "{online}"),
    OFFLINE_COUNT(TagType.FACTION, "{offline}"),
    FACTION_SIZE(TagType.FACTION, "{members}"),
    FACTION_KILLS(TagType.FACTION, "{faction-kills}"),
    FACTION_DEATHS(TagType.FACTION, "{faction-deaths}"),

    /**
     * General variables, require no faction or player
     */
    MAX_WARPS(TagType.GENERAL, "{max-warps}"),
    MAX_ALLIES(TagType.GENERAL, "{max-allies}"),
    MAX_ENEMIES(TagType.GENERAL, "{max-enemies}"),
    FACTIONLESS(TagType.GENERAL, "{factionless}"),
    TOTAL_ONLINE(TagType.GENERAL, "{total-online}");

    private TagType type;
    private String tag;

    protected enum TagType {
        FANCY(0), PLAYER(1), FACTION(2), GENERAL(3);
        public int id;

        TagType(int id) {
            this.id = id;
        }
    }

    TagReplacerUtil(TagType type, String tag) {
        this.type = type;
        this.tag = tag;
    }

    /**
     * Protected access to this generic server related variable
     *
     * @return value for this generic server related variable<br>
     */
    protected String getValue() {
        switch (this) {
            case TOTAL_ONLINE:
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            case FACTIONLESS:
                return String.valueOf(FactionColl.get().getWilderness().getWhereOnline(true).size());
            case MAX_ALLIES:
            	if (Conf.maxRelations.containsKey(Relation.ALLY) && Conf.maxRelations.get(Relation.ALLY) > -1) {
            		return String.valueOf(Conf.maxRelations.get(Relation.ALLY));
            	}
                return Lang.GENERIC_INFINITY.toString();
            case MAX_ENEMIES:
            	if (Conf.maxRelations.containsKey(Relation.ENEMY) && Conf.maxRelations.get(Relation.ENEMY) > -1) {
            		return String.valueOf(Conf.maxRelations.get(Relation.ENEMY));
            	}

                return Lang.GENERIC_INFINITY.toString();
            case MAX_WARPS:
                return String.valueOf(Conf.warpsMax);
            default:
            	return null;
        }
    }

    /**
     * Gets the value for this (as in the instance this is called from) variable!
     *
     * @param fac Target faction
     * @param fp  Target player (can be null)
     *
     * @return the value for this enum!
     */
    protected String getValue(Faction fac, FPlayer fp) {
        if (this.type == TagType.GENERAL) {
            return getValue();
        }

        if (fp != null) {
            switch (this) {
                case HEADER:
                    return Factions.get().getTextUtil().titleize(fac.getTag(fp));
                case PLAYER_NAME:
                    return fp.getName();
                case FACTION:
                    return !fac.isWilderness() ? fac.getTag(fp) : Lang.GENERIC_FACTIONLESS.toString();
                case LAST_SEEN:
                    String humanized = DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - fp.getLastLoginTime(), true, true) + Lang.COMMAND_STATUS_AGOSUFFIX;
                    return fp.isOnline() ? ChatColor.GREEN + Lang.COMMAND_STATUS_ONLINE.toString() : (System.currentTimeMillis() - fp.getLastLoginTime() < 432000000 ? ChatColor.YELLOW + humanized : ChatColor.RED + humanized);
                case PLAYER_GROUP:
                    return VaultIntegration.get().getPrimaryGroup(Bukkit.getOfflinePlayer(UUID.fromString(fp.getId())));
                case PLAYER_BALANCE:
                    return VaultEngine.isSetup() ? VaultEngine.getFriendlyBalance(fp) : Lang.ECON_OFF.format("balance");
                case PLAYER_POWER:
                    return String.valueOf(fp.getPowerRounded());
                case PLAYER_MAXPOWER:
                    return String.valueOf(fp.getPowerMaxRounded());
                case PLAYER_KILLS:
                    return String.valueOf(fp.getKills());
                case PLAYER_DEATHS:
                    return String.valueOf(fp.getDeaths());
                default:
            }
        }
        switch (this) {
            case DESCRIPTION:
                return fac.getDescription();
            case FACTION:
                return fac.getTag();
            case JOINING:
                return (fac.getOpen() ? Lang.COMMAND_SHOW_UNINVITED.toString() : Lang.COMMAND_SHOW_INVITATION.toString());
            case PEACEFUL:
                return fac.isPeaceful() ? Conf.colorNeutral + Lang.COMMAND_SHOW_PEACEFUL.toString() : "";
            case PERMANENT:
                return fac.isPermanent() ? "permanent" : "{notPermanent}";
            case CHUNKS:
                return String.valueOf(fac.getLandRounded());
            case POWER:
                return String.valueOf(fac.getPowerRounded());
            case MAX_POWER:
                return String.valueOf(fac.getPowerMaxRounded());
            case POWER_BOOST:
                double powerBoost = fac.getPowerBoost();
                return (powerBoost == 0.0) ? "" : (powerBoost > 0.0 ? Lang.COMMAND_SHOW_BONUS.toString() : Lang.COMMAND_SHOW_PENALTY.toString() + powerBoost + ")");
            case LEADER:
                FPlayer fAdmin = fac.getOwner();
                return fAdmin == null ? "Server" : fAdmin.getName().substring(0, fAdmin.getName().length() > 14 ? 13 : fAdmin.getName().length());
            case WARPS:
                return String.valueOf(fac.warps().size());
            case CREATE_DATE:
                return Lang.sdf.format(fac.getFoundedDate());
            case RAIDABLE:
                boolean raid = Conf.raidable && fac.getLandRounded() >= fac.getPowerRounded();
                return raid ? Lang.RAIDABLE_TRUE.toString() : Lang.RAIDABLE_FALSE.toString();
            case HOME_WORLD:
                return fac.hasHome() ? fac.getHome().getWorld().getName() : Conf.showMinimal ? null : "{ig}";
            case HOME_X:
                return fac.hasHome() ? String.valueOf(fac.getHome().getBlockX()) : Conf.showMinimal ? null : "{ig}";
            case HOME_Y:
                return fac.hasHome() ? String.valueOf(fac.getHome().getBlockY()) : Conf.showMinimal ? null : "{ig}";
            case HOME_Z:
                return fac.hasHome() ? String.valueOf(fac.getHome().getBlockZ()) : Conf.showMinimal ? null : "{ig}";
            case LAND_VALUE:
                return VaultEngine.shouldBeUsed() ? VaultEngine.moneyString(VaultEngine.calculateTotalLandValue(fac.getLandRounded())) : Conf.showMinimal ? null : Lang.ECON_OFF.format("value");
            case LAND_REFUND:
                return VaultEngine.shouldBeUsed() ? VaultEngine.moneyString(VaultEngine.calculateTotalLandRefund(fac.getLandRounded())) : Conf.showMinimal ? null : Lang.ECON_OFF.format("refund");
            case BANK_BALANCE:
                if (VaultEngine.shouldBeUsed()) {
                    return Conf.bankEnabled ? VaultEngine.moneyString(VaultEngine.getBalance(fac.getAccountId())) : Conf.showMinimal ? null : Lang.ECON_OFF.format("balance");
                }
                return Conf.showMinimal ? null : Lang.ECON_OFF.format("balance");
            case ALLIES_COUNT:
                return String.valueOf(fac.getRelationCount(Relation.ALLY));
            case ENEMIES_COUNT:
                return String.valueOf(fac.getRelationCount(Relation.ENEMY));
            case ONLINE_COUNT:
                return String.valueOf(fac.getOnlinePlayers().size());
            case OFFLINE_COUNT:
                return String.valueOf(fac.getFPlayers().size() - fac.getOnlinePlayers().size());
            case FACTION_SIZE:
                return String.valueOf(fac.getFPlayers().size());
            case FACTION_KILLS:
                return String.valueOf(fac.getKills());
            case FACTION_DEATHS:
                return String.valueOf(fac.getDeaths());
            default:
            	return null;
        }
    }

    /**
     * Returns a list of all the variables we can use for this type<br>
     *
     * @param type the type we want
     *
     * @return a list of all the variables with this type
     */
    protected static List<TagReplacerUtil> getByType(TagType type) {
        List<TagReplacerUtil> tagReplacers = new ArrayList<TagReplacerUtil>();
        for (TagReplacerUtil tagReplacer : TagReplacerUtil.values()) {
            if (type == TagType.FANCY) {
                if (tagReplacer.type == TagType.FANCY) {
                    tagReplacers.add(tagReplacer);
                }
            } else if (tagReplacer.type.id >= type.id) {
                tagReplacers.add(tagReplacer);
            }
        }
        return tagReplacers;
    }

    /**
     * @param original raw line with variables
     * @param value    what to replace var in raw line with
     *
     * @return the string with the new value
     */
    public String replace(String original, String value) {
        return original.replace(tag, value);
    }

    /**
     * @param toSearch raw line with variables
     *
     * @return if the raw line contains this enums variable
     */
    public boolean contains(String toSearch) {
        return toSearch.contains(tag);
    }

    /**
     * Gets the tag associated with this enum that we should replace
     *
     * @return the {....} variable that is located in config
     */
    public String getTag() {
        return this.tag;
    }
}
