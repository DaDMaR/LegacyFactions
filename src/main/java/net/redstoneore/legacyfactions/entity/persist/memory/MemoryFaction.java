package net.redstoneore.legacyfactions.entity.persist.memory;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.redstoneore.legacyfactions.*;
import net.redstoneore.legacyfactions.entity.Board;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.FPlayer;
import net.redstoneore.legacyfactions.entity.FPlayerColl;
import net.redstoneore.legacyfactions.entity.Faction;
import net.redstoneore.legacyfactions.entity.FactionColl;
import net.redstoneore.legacyfactions.integration.vault.VaultEngine;
import net.redstoneore.legacyfactions.util.LazyLocation;
import net.redstoneore.legacyfactions.util.MiscUtil;
import net.redstoneore.legacyfactions.util.RelationUtil;
import net.redstoneore.legacyfactions.warp.FactionWarps;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class MemoryFaction implements Faction, EconomyParticipator {
	
	// -------------------------------------------------- //
	// FIELDS
	// -------------------------------------------------- //
	
	protected String id = null;
	protected boolean peacefulExplosionsEnabled;
	protected boolean permanent;
	protected String tag;
	protected String description;
	protected boolean open;
	protected boolean peaceful;
	protected Integer permanentPower;
	protected LazyLocation home;
	protected long foundedDate;
	protected transient long lastPlayerLoggedOffTime;
	protected double money;
	protected double powerBoost;
	protected Map<String, Relation> relationWish = new HashMap<>();
	protected Map<FLocation, Set<String>> claimOwnership = new ConcurrentHashMap<>();
	protected transient Set<FPlayer> fplayers = new HashSet<>();
	protected Set<String> invites = new HashSet<>();
	protected HashMap<String, List<String>> announcements = new HashMap<>();
	protected ConcurrentHashMap<String, LazyLocation> warps = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<String, String> warpPasswords = new ConcurrentHashMap<>();
	private long lastDeath;
	protected int maxVaults;
	
	private transient FactionWarps factionWarps = new FactionWarps(this);
	
	// -------------------------------------------------- //
	// METHODS
	// -------------------------------------------------- //
		
	@Override
	public FactionWarps warps() {
		return factionWarps;
	}
	
	public HashMap<String, List<String>> getAnnouncements() {
		return this.announcements;
	}

	public void addAnnouncement(FPlayer fplayer, String msg) {
		List<String> list = announcements.containsKey(fplayer.getId()) ? announcements.get(fplayer.getId()) : new ArrayList<>();
		list.add(msg);
		announcements.put(fplayer.getId(), list);
	}

	public void sendUnreadAnnouncements(FPlayer fplayer) {
		if (!announcements.containsKey(fplayer.getId())) {
			return;
		}
		fplayer.sendMessage(ChatColor.LIGHT_PURPLE + "--Unread Faction Announcements--");
		for (String s : announcements.get(fplayer.getPlayer().getUniqueId().toString())) {
			fplayer.sendMessage(s);
		}
		fplayer.sendMessage(ChatColor.LIGHT_PURPLE + "--Unread Faction Announcements--");
		announcements.remove(fplayer.getId());
	}

	public void removeAnnouncements(FPlayer fplayer) {
		if (announcements.containsKey(fplayer.getId())) {
			announcements.remove(fplayer.getId());
		}
	}

	public ConcurrentHashMap<String, LazyLocation> getAllWarps() {
		return this.warps;
	}

	public LazyLocation getWarp(String name) {
		return this.warps.get(name);
	}
	
	public Optional<String> getWarpPassword(String warpName) {
		if (!this.warpPasswords.containsKey(warpName)) return Optional.empty();
		
		return Optional.of(this.warpPasswords.get(warpName).toLowerCase());
	}

	public void setWarp(String name, LazyLocation loc, String password) {
		if (password != null) {
			this.warpPasswords.put(name, password.toLowerCase());
		}
		
		this.warps.put(name, loc);
	}

	public boolean isWarp(String name) {
		return this.warps.containsKey(name);
	}

	public boolean removeWarp(String name) {
		if (this.warps.remove(name) == null) return false;
		
		if (this.warpPasswords.contains(name)) this.warpPasswords.remove(name);
		
		return true;
		
	}

	public void clearWarps() {
		this.warps.clear();
		this.warpPasswords.clear();
	}

	public int getMaxVaults() {
		return this.maxVaults;
	}

	public void setMaxVaults(int value) {
		this.maxVaults = value;
	}

	public Set<String> getInvites() {
		return invites;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void invite(FPlayer fplayer) {
		this.invites.add(fplayer.getId());
	}

	public void deinvite(FPlayer fplayer) {
		this.invites.remove(fplayer.getId());
	}

	public boolean isInvited(FPlayer fplayer) {
		return this.invites.contains(fplayer.getId());
	}

	public boolean getOpen() {
		return open;
	}

	public void setOpen(boolean isOpen) {
		open = isOpen;
	}

	public boolean isPeaceful() {
		return this.peaceful;
	}

	public void setPeaceful(boolean isPeaceful) {
		this.peaceful = isPeaceful;
	}

	public void setPeacefulExplosionsEnabled(boolean val) {
		peacefulExplosionsEnabled = val;
	}

	public boolean getPeacefulExplosionsEnabled() {
		return this.peacefulExplosionsEnabled;
	}

	public boolean noExplosionsInTerritory() {
		return (this.peaceful && !peacefulExplosionsEnabled) || this.isSafeZone();
	}
	
	public boolean noCreeperExplosions(Location location) {
		return (
			this.isWilderness() && Conf.wildernessBlockCreepers && !Conf.worldsNoWildernessProtection.contains(location.getWorld().getName()) ||
			this.isNormal() && (this.hasPlayersOnline() ? Conf.territoryBlockCreepers : Conf.territoryBlockCreepersWhenOffline) ||
			this.isWarZone() && Conf.warZoneBlockCreepers ||
			this.isSafeZone()
		);
	}

	public boolean isPermanent() {
		return permanent || !this.isNormal();
	}

	public void setPermanent(boolean isPermanent) {
		permanent = isPermanent;
	}

	public String getTag() {
		return this.tag;
	}

	public String getTag(String prefix) {
		return prefix + this.tag;
	}

	public String getTag(Faction otherFaction) {
		if (otherFaction == null) {
			return getTag();
		}
		return this.getTag(this.getColorTo(otherFaction).toString());
	}

	public String getTag(FPlayer otherFplayer) {
		if (otherFplayer == null) {
			return getTag();
		}
		return this.getTag(this.getColorTo(otherFplayer).toString());
	}

	public void setTag(String str) {
		if (Conf.factionTagForceUpperCase) {
			str = str.toUpperCase();
		}
		this.tag = str;
	}

	public String getComparisonTag() {
		return MiscUtil.getComparisonString(this.tag);
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String value) {
		this.description = value;
	}

	public void setHome(Location home) {
		this.home = new LazyLocation(home);
	}

	public boolean hasHome() {
		return this.getHome() != null;
	}

	public Location getHome() {
		confirmValidHome();
		return (this.home != null) ? this.home.getLocation() : null;
	}

	public long getFoundedDate() {
		if (this.foundedDate == 0) {
			setFoundedDate(System.currentTimeMillis());
		}
		return this.foundedDate;
	}

	public void setFoundedDate(long newDate) {
		this.foundedDate = newDate;
	}

	public void confirmValidHome() {
		if (!Conf.homesMustBeInClaimedTerritory || this.home == null || (this.home.getLocation() != null && Board.get().getFactionAt(new FLocation(this.home.getLocation())) == this)) {
			return;
		}

		msg("<b>Your faction home has been un-set since it is no longer in your territory.");
		this.home = null;
	}

	public String getAccountId() {
		String aid = "faction-" + this.getId();

		// We need to override the default money given to players.
		if (!VaultEngine.hasAccount(aid)) {
			VaultEngine.setBalance(aid, 0);
		}

		return aid;
	}

	public Integer getPermanentPower() {
		return this.permanentPower;
	}

	public void setPermanentPower(Integer permanentPower) {
		this.permanentPower = permanentPower;
	}

	public boolean hasPermanentPower() {
		return this.permanentPower != null;
	}

	public double getPowerBoost() {
		return this.powerBoost;
	}

	public void setPowerBoost(double powerBoost) {
		this.powerBoost = powerBoost;
	}

	public boolean isPowerFrozen() {
		if (Conf.raidablePowerFreeze == 0) return false;

		return System.currentTimeMillis() - lastDeath < Conf.raidablePowerFreeze * 1000;
	}

	public void setLastDeath(long time) {
		this.lastDeath = time;
	}

	public long getLastDeath() {
		return this.lastDeath;
	}

	public int getKills() {
		int kills = 0;
		for (FPlayer fplayer : this.getFPlayers()) {
			kills += fplayer.getKills();
		}

		return kills;
	}

	public int getDeaths() {
		int deaths = 0;
		for (FPlayer fplayer : this.getFPlayers()) {
			deaths += fplayer.getDeaths();
		}

		return deaths;
	}

	// -------------------------------------------- //
	// Construct
	// -------------------------------------------- //
	public MemoryFaction() {
	}

	public MemoryFaction(String id) {
		this.id = id;
		this.open = Conf.newFactionsDefaultOpen;
		this.tag = "???";
		this.description = Lang.GENERIC_DEFAULTDESCRIPTION.toString();
		this.lastPlayerLoggedOffTime = 0;
		this.peaceful = false;
		this.peacefulExplosionsEnabled = false;
		this.permanent = false;
		this.money = 0.0;
		this.powerBoost = 0.0;
		this.foundedDate = System.currentTimeMillis();
		this.maxVaults = Conf.defaultMaxVaults;
	}

	public MemoryFaction(MemoryFaction old) {
		id = old.id;
		peacefulExplosionsEnabled = old.peacefulExplosionsEnabled;
		permanent = old.permanent;
		tag = old.tag;
		description = old.description;
		open = old.open;
		foundedDate = old.foundedDate;
		peaceful = old.peaceful;
		permanentPower = old.permanentPower;
		home = old.home;
		lastPlayerLoggedOffTime = old.lastPlayerLoggedOffTime;
		money = old.money;
		powerBoost = old.powerBoost;
		relationWish = old.relationWish;
		claimOwnership = old.claimOwnership;
		fplayers = new HashSet<>();
		invites = old.invites;
		announcements = old.announcements;
	}

	// -------------------------------------------- //
	// Extra Getters And Setters
	// -------------------------------------------- //
	public boolean noPvPInTerritory() {
		return isSafeZone() || (peaceful && Conf.peacefulTerritoryDisablePVP);
	}

	public boolean noMonstersInTerritory() {
		return isSafeZone() || (peaceful && Conf.peacefulTerritoryDisableMonsters);
	}

	// -------------------------------
	// Understand the types
	// -------------------------------

	public boolean isNormal() {
		return !(this.isWilderness() || this.isSafeZone() || this.isWarZone());
	}

	public boolean isNone() {
		return this.getId().equals("0");
	}

	public boolean isWilderness() {
		return this.getId().equals("0");
	}

	public boolean isSafeZone() {
		return this.getId().equals("-1");
	}

	public boolean isWarZone() {
		return this.getId().equals("-2");
	}

	public boolean isPlayerFreeType() {
		return this.isSafeZone() || this.isWarZone();
	}

	// -------------------------------
	// Relation and relation colors
	// -------------------------------

	@Override
	public String describeTo(RelationParticipator that, boolean ucfirst) {
		return RelationUtil.describeThatToMe(this, that, ucfirst);
	}

	@Override
	public String describeTo(RelationParticipator that) {
		return RelationUtil.describeThatToMe(this, that);
	}

	@Override
	public Relation getRelationTo(RelationParticipator rp) {
		return RelationUtil.getRelationTo(this, rp);
	}

	@Override
	public Relation getRelationTo(RelationParticipator rp, boolean ignorePeaceful) {
		return RelationUtil.getRelationTo(this, rp, ignorePeaceful);
	}

	@Override
	public ChatColor getColorTo(RelationParticipator rp) {
		return RelationUtil.getColorOfThatToMe(this, rp);
	}

	public Relation getRelationWish(Faction otherFaction) {
		if (this.relationWish.containsKey(otherFaction.getId())) {
			return this.relationWish.get(otherFaction.getId());
		}
		return Relation.fromString(Conf.factionDefaultRelation); // Always default to old behavior.
	}

	public void setRelationWish(Faction otherFaction, Relation relation) {
		if (this.relationWish.containsKey(otherFaction.getId()) && relation.equals(Relation.NEUTRAL)) {
			this.relationWish.remove(otherFaction.getId());
		} else {
			this.relationWish.put(otherFaction.getId(), relation);
		}
	}

	public int getRelationCount(Relation relation) {
		int count = 0;
		for (Faction faction : FactionColl.get().getAllFactions()) {
			if (faction.getRelationTo(this) == relation) {
				count++;
			}
		}
		return count;
	}

	public boolean hasMaxRelations(Faction them, Relation rel, Boolean silent) {
		if (!Conf.maxRelations.containsKey(rel)) return false;
		if (Conf.maxRelations.get(rel) < 0) return false;
		
		int maxRelations = Conf.maxRelations.get(rel);
		
		if (this.getRelationCount(rel) >= maxRelations) {
		 	if (!silent) this.msg(Lang.COMMAND_RELATIONS_EXCEEDS_ME, maxRelations, rel.getPluralTranslation());
			return true;
		}
			
		if (them.getRelationCount(rel) > maxRelations) {
			if (!silent) this.msg(Lang.COMMAND_RELATIONS_EXCEEDS_THEY, maxRelations, rel.getPluralTranslation());
			return true;
		}
		
		return false;
	}
	
	// ----------------------------------------------//
	// Power
	// ----------------------------------------------//
	public double getPower() {
		if (this.hasPermanentPower()) {
			return this.getPermanentPower();
		}

		double ret = 0;
		for (FPlayer fplayer : fplayers) {
			ret += fplayer.getPower();
		}
		if (Conf.powerFactionMax > 0 && ret > Conf.powerFactionMax) {
			ret = Conf.powerFactionMax;
		}
		return ret + this.powerBoost;
	}

	public double getPowerMax() {
		if (this.hasPermanentPower()) {
			return this.getPermanentPower();
		}

		double ret = 0;
		for (FPlayer fplayer : fplayers) {
			ret += fplayer.getPowerMax();
		}
		if (Conf.powerFactionMax > 0 && ret > Conf.powerFactionMax) {
			ret = Conf.powerFactionMax;
		}
		return ret + this.powerBoost;
	}

	public int getPowerRounded() {
		return (int) Math.round(this.getPower());
	}

	public int getPowerMaxRounded() {
		return (int) Math.round(this.getPowerMax());
	}

	public int getLandRounded() {
		return Board.get().getFactionCoordCount(this);
	}

	public int getLandRoundedInWorld(String worldName) {
		return this.getLandRoundedInWorld(Bukkit.getWorld(worldName));
	}

	public int getLandRoundedInWorld(World world) {
		return Board.get().getFactionCoordCountInWorld(this, world);
	}

	public boolean hasLandInflation() {
		return this.getLandRounded() > this.getPowerRounded();
	}

	// -------------------------------
	// FPlayers
	// -------------------------------

	// maintain the reference list of FPlayers in this faction
	public void refreshFPlayers() {
		fplayers.clear();
		if (this.isPlayerFreeType()) {
			return;
		}

		for (FPlayer fplayer : FPlayerColl.all()) {
			if (fplayer.getFactionId().equalsIgnoreCase(id)) {
				fplayers.add(fplayer);
			}
		}
	}

	public boolean addFPlayer(FPlayer fplayer) {
		return !this.isPlayerFreeType() && fplayers.add(fplayer);

	}

	public boolean removeFPlayer(FPlayer fplayer) {
		return !this.isPlayerFreeType() && fplayers.remove(fplayer);

	}

	public int getSize() {
		return fplayers.size();
	}

	public Set<FPlayer> getFPlayers() {
		// return a shallow copy of the FPlayer list, to prevent tampering and
		// concurrency issues
		return new HashSet<>(this.fplayers);
	}

	public Set<FPlayer> getFPlayersWhereOnline(boolean online) {
		return this.getWhereOnline(online);
	}
	
	public Set<FPlayer> getWhereOnline(boolean online) {
		if (!this.isNormal()) return new HashSet<>();
		
		return this.getFPlayers().stream()
				.filter(fplayer -> fplayer.isOnline() == online)
				.collect(Collectors.toSet());
	}

	public FPlayer getFPlayerAdmin() {
		return this.getOwner();
	}
	
	public FPlayer getOwner() {
		if (!this.isNormal()) return null;
		
		return this.getFPlayers().stream()
				.filter(fplayer -> fplayer.getRole() == Role.ADMIN)
				.findFirst()
				.orElse(null);
	}
	
	public ArrayList<FPlayer> getFPlayersWhereRole(Role role) {
		return this.getWhereRole(role);
	}
	
	public ArrayList<FPlayer> getWhereRole(Role role) {
		if (!this.isNormal()) return new ArrayList<>();

		return this.getFPlayers().stream()
				.filter(fplayer -> fplayer.getRole() == role)
				.collect(Collectors.toCollection(ArrayList::new));

	}

	public ArrayList<Player> getOnlinePlayers() {
		if (this.isPlayerFreeType()) return new ArrayList<>();

		return Factions.get().getServer().getOnlinePlayers().stream()
				.filter(player -> FPlayerColl.get(player).getFaction() == this)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	// slightly faster check than getOnlinePlayers() if you just want to see if
	// there are any players online
	public boolean hasPlayersOnline() {
		// only real factions can have players online, not safe zone / war zone
		if (this.isPlayerFreeType()) return false;
		
		Optional<? extends Player> found = Factions.get().getServer().getOnlinePlayers().stream()
				.filter(player -> FPlayerColl.get(player) != null && FPlayerColl.get(player) == this)
				.findFirst();
		
		if (found.isPresent()) return true;
		
		// even if all players are technically logged off, maybe someone was on
		// recently enough to not consider them officially offline yet
		return Conf.considerFactionsReallyOfflineAfterXMinutes > 0 && System.currentTimeMillis() < lastPlayerLoggedOffTime + (Conf.considerFactionsReallyOfflineAfterXMinutes * 60000);
	}

	public void memberLoggedOff() {
		if (!this.isNormal()) return;
		lastPlayerLoggedOffTime = System.currentTimeMillis();
	}

	// used when current leader is about to be removed from the faction;
	// promotes new leader, or disbands faction if no other members left
	public void promoteNewLeader() {
		if (!this.isNormal()) {
			return;
		}
		if (this.isPermanent() && Conf.permanentFactionsDisableLeaderPromotion) {
			return;
		}

		FPlayer oldLeader = this.getFPlayerAdmin();

		// get list of moderators, or list of normal members if there are no moderators
		ArrayList<FPlayer> replacements = this.getFPlayersWhereRole(Role.MODERATOR);
		if (replacements == null || replacements.isEmpty()) {
			replacements = this.getFPlayersWhereRole(Role.NORMAL);
		}

		if (replacements == null || replacements.isEmpty()) { // faction admin  is the only  member; one-man  faction
			if (this.isPermanent()) {
				if (oldLeader != null) {
					oldLeader.setRole(Role.NORMAL);
				}
				return;
			}

			// no members left and faction isn't permanent, so disband it
			if (Conf.logFactionDisband) {
				Factions.get().log("The faction " + this.getTag() + " (" + this.getId() + ") has been disbanded since it has no members left.");
			}

			for (FPlayer fplayer : FPlayerColl.all()) {
				fplayer.msg("The faction %s<i> was disbanded.", this.getTag(fplayer));
			}

			FactionColl.get().removeFaction(getId());
		} else { // promote new faction admin
			if (oldLeader != null) {
				oldLeader.setRole(Role.NORMAL);
			}
			replacements.get(0).setRole(Role.ADMIN);
			//TODO:TL
			this.msg("<i>Faction admin <h>%s<i> has been removed. %s<i> has been promoted as the new faction admin.", oldLeader == null ? "" : oldLeader.getName(), replacements.get(0).getName());
			Factions.get().log("Faction " + this.getTag() + " (" + this.getId() + ") admin was removed. Replacement admin: " + replacements.get(0).getName());
		}
	}

	// ----------------------------------------------//
	// Messages
	// ----------------------------------------------//
	public void msg(String message, Object... args) {
		message = Factions.get().getTextUtil().parse(message, args);

		for (FPlayer fplayer : this.getFPlayersWhereOnline(true)) {
			fplayer.sendMessage(message);
		}
	}

	public void msg(Lang translation, Object... args) {
		msg(translation.toString(), args);
	}

	public void sendMessage(String message) {
		for (FPlayer fplayer : this.getFPlayersWhereOnline(true)) {
			fplayer.sendMessage(message);
		}
	}

	public void sendMessage(List<String> messages) {
		for (FPlayer fplayer : this.getFPlayersWhereOnline(true)) {
			fplayer.sendMessage(messages);
		}
	}

	// ----------------------------------------------//
	// Ownership of specific claims
	// ----------------------------------------------//

	public Map<FLocation, Set<String>> getClaimOwnership() {
		return claimOwnership;
	}

	public void clearAllClaimOwnership() {
		claimOwnership.clear();
	}

	public void clearClaimOwnership(FLocation loc) {
		claimOwnership.remove(loc);
	}

	public void clearClaimOwnership(FPlayer player) {
		if (id == null || id.isEmpty()) {
			return;
		}

		Set<String> ownerData;

		for (Entry<FLocation, Set<String>> entry : claimOwnership.entrySet()) {
			ownerData = entry.getValue();

			if (ownerData == null) {
				continue;
			}

			Iterator<String> iter = ownerData.iterator();
			while (iter.hasNext()) {
				if (iter.next().equals(player.getId())) {
					iter.remove();
				}
			}

			if (ownerData.isEmpty()) {
				claimOwnership.remove(entry.getKey());
			}
		}
	}

	public int getCountOfClaimsWithOwners() {
		return claimOwnership.isEmpty() ? 0 : claimOwnership.size();
	}

	public boolean doesLocationHaveOwnersSet(FLocation loc) {
		if (claimOwnership.isEmpty() || !claimOwnership.containsKey(loc)) {
			return false;
		}

		Set<String> ownerData = claimOwnership.get(loc);
		return ownerData != null && !ownerData.isEmpty();
	}

	public boolean isPlayerInOwnerList(FPlayer player, FLocation loc) {
		if (claimOwnership.isEmpty()) {
			return false;
		}
		Set<String> ownerData = claimOwnership.get(loc);
		if (ownerData == null) {
			return false;
		}
		return ownerData.contains(player.getId());
	}

	public void setPlayerAsOwner(FPlayer player, FLocation loc) {
		Set<String> ownerData = claimOwnership.get(loc);
		if (ownerData == null) {
			ownerData = new HashSet<>();
		}
		ownerData.add(player.getId());
		claimOwnership.put(loc, ownerData);
	}

	public void removePlayerAsOwner(FPlayer player, FLocation loc) {
		Set<String> ownerData = claimOwnership.get(loc);
		if (ownerData == null) {
			return;
		}
		ownerData.remove(player.getId());
		claimOwnership.put(loc, ownerData);
	}

	public Set<String> getOwnerList(FLocation loc) {
		return claimOwnership.get(loc);
	}

	public String getOwnerListString(FLocation loc) {
		Set<String> ownerData = claimOwnership.get(loc);
		if (ownerData == null || ownerData.isEmpty()) {
			return "";
		}

		String ownerList = "";

		Iterator<String> iter = ownerData.iterator();
		while (iter.hasNext()) {
			if (!ownerList.isEmpty()) {
				ownerList += ", ";
			}
			OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(iter.next()));
			//TODO:TL
			ownerList += offlinePlayer != null ? offlinePlayer.getName() : "null player";
		}
		return ownerList;
	}

	public boolean playerHasOwnershipRights(FPlayer fplayer, FLocation loc) {
		// in own faction, with sufficient role or permission to bypass
		// ownership?
		if (fplayer.getFaction() == this && (fplayer.getRole().isAtLeast(Conf.ownedAreaModeratorsBypass ? Role.MODERATOR : Role.ADMIN) || Permission.OWNERSHIP_BYPASS.has(fplayer.getPlayer()))) {
			return true;
		}

		// make sure claimOwnership is initialized
		if (claimOwnership.isEmpty()) return true;
		

		// need to check the ownership list, then
		Set<String> ownerData = claimOwnership.get(loc);

		// if no owner list, owner list is empty, or player is in owner list,
		// they're allowed
		return ownerData == null || ownerData.isEmpty() || ownerData.contains(fplayer.getId());
	}

	
	// ----------------------------------------------//
	// Persistance and entity management
	// ----------------------------------------------//
	public void remove() {
		if (VaultEngine.shouldBeUsed()) {
			VaultEngine.setBalance(getAccountId(), 0);
		}

		// Clean the board
		((MemoryBoard) Board.get()).clean(id);

		for (FPlayer fplayer : fplayers) {
			fplayer.resetFactionData(false);
		}
	}

	public Set<FLocation> getAllClaims() {
		return Board.get().getAllClaims(this);
	}
	
	
	public MemoryFaction asMemoryFaction() {
		return (MemoryFaction) this;
	}
	
}
