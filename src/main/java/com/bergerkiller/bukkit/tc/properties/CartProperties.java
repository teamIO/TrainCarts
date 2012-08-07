package com.bergerkiller.bukkit.tc.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.BlockLocation;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.utils.EnumUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.tc.Permission;
import com.bergerkiller.bukkit.tc.Util;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.signactions.SignActionAnnounce;
import com.bergerkiller.bukkit.tc.storage.OfflineGroupManager;
import com.bergerkiller.bukkit.tc.storage.OfflineMember;
import com.bergerkiller.bukkit.tc.utils.SoftReference;

public class CartProperties extends CartPropertiesStore implements IProperties {
	public static final CartProperties EMPTY = new CartProperties(null, TrainProperties.EMPTY);

	protected CartProperties(UUID uuid, TrainProperties group) {
		this.uuid = uuid;
		this.group = group;
	}

	private final UUID uuid;
	private final Set<String> owners = new HashSet<String>();
	private final Set<String> tags = new HashSet<String>();
	private final Set<Material> blockBreakTypes = new HashSet<Material>();
	private boolean allowMobsEnter = true;
	private boolean allowPlayerExit = true;
	private boolean allowPlayerEnter = true;
	private String enterMessage = null;
	private String destination = "";
	private boolean isPublic = true;
	private boolean pickUp = false;
	private SoftReference<MinecartMember> member = new SoftReference<MinecartMember>();
	protected TrainProperties group = null;

	public TrainProperties getTrainProperties() {
		return this.group;
	}

	public MinecartMember getMember() {
		MinecartMember member = this.member.get();
		if (member == null || member.dead || !member.uniqueId.equals(this.uuid)) {
			return this.member.set(MinecartMember.get(this.uuid));
		} else {
			return member;
		}
	}
	public MinecartGroup getGroup() {
		MinecartMember member = this.getMember();
		if (member == null) {
			return this.group == null ? null : this.group.getGroup();
		} else {
			return member.getGroup();
		}
	}
	public UUID getUUID() {
		return this.uuid;
	}
	
	public void tryUpdate() {
		MinecartMember m = this.getMember();
		if (m != null) m.update();
	}
	
	/*
	 * Block obtaining
	 */
	public boolean canBreak(Block block) {
		if (this.blockBreakTypes.isEmpty()) return false;
		return this.blockBreakTypes.contains(block.getType());
	}

	/*
	 * Owners
	 */
	public boolean hasOwnership(Player player) {
		if (!canHaveOwnership(player)) return false;
		if (!this.hasOwners()) return true;
		if (hasGlobalOwnership(player)) return true;
		return this.isOwner(player);
	}
	public static boolean hasGlobalOwnership(Player player) {
		return Permission.COMMAND_GLOBALPROPERTIES.has(player);
	}
	public static boolean canHaveOwnership(Player player) {
		return Permission.COMMAND_PROPERTIES.has(player) || hasGlobalOwnership(player);
	}
	public boolean isOwner(Player player) {
		return this.isOwner(player.getName().toLowerCase());
	}
	public boolean isOwner(String player) {
		return this.owners.contains(player);
	}
	public void setOwner(String player) {
		this.setOwner(player, true);
	}
	public void setOwner(String player, boolean owner) {
		if (owner) {
			this.owners.add(player);
		} else {
			this.owners.add(player);
		}
	}
	public void setOwner(Player player) {
		this.setOwner(player, true);
	}
	public void setOwner(Player player, boolean owner) {
		this.setOwner(player.getName().toLowerCase(), owner);
	}
	public Set<String> getOwners() {
		return this.owners;
	}
	public void clearOwners() {
		this.owners.clear();
	}
	public boolean hasOwners() {
		return !this.owners.isEmpty();
	}
	public boolean sharesOwner(CartProperties properties) {
		if (!this.hasOwners()) return true;
		if (!properties.hasOwners()) return true;
		for (String owner : properties.owners) {
			if (properties.isOwner(owner)) return true;
		}
		return false;
	}

	/**
	 * Gets whether this Minecart can pick up nearby items
	 * 
	 * @return True if it can pick up items, False if not
	 */
	public boolean canPickup() {
		return this.pickUp;
	}

	public void setPickup(boolean pickup) {
		this.pickUp = pickup;
	}

	@Override
	public boolean isPublic() {
		return this.isPublic;
	}

	@Override
	public void setPublic(boolean state) {
		this.isPublic = state;
	}

	@Override
	public boolean matchTag(String tag) {
		return Util.matchText(this.tags, tag);
	}

	@Override
	public boolean hasTags() {
		return !this.tags.isEmpty();
	}

	@Override
	public void setTags(String... tags) {
		this.tags.clear();
		this.addTags(tags);
	}

	@Override
	public void clearTags() {
		this.tags.clear();
	}

	@Override
	public void addTags(String... tags) {
		for (String tag : tags) {
			this.tags.add(tag);
		}
	}

	@Override
	public void removeTags(String... tags) {
		for (String tag : tags) {
			this.tags.remove(tag);
		}
	}

	@Override
	public Set<String> getTags() {
		return this.tags;
	}

	@Override
	public BlockLocation getLocation() {
		MinecartMember member = this.getMember();
		if (member != null) {
			return new BlockLocation(member.getLocation().getBlock());
		} else {
			// Offline member?
			OfflineMember omember = OfflineGroupManager.findMember(this.getTrainProperties().getTrainName(), this.getUUID());
			if (omember == null) {
				return null;
			} else {
				// Find world
				World world = Bukkit.getWorld(omember.group.worldUUID);
				if (world == null) {
					return new BlockLocation("Unknown", omember.cx << 4, 0, omember.cz << 4);
				} else {
					return new BlockLocation(world, omember.cx << 4, 0, omember.cz << 4);
				}
			}
		}
	}

	/**
	 * Tests whether the Minecart has block types it can break
	 * 
	 * @return True if materials are contained, False if not
	 */
	public boolean hasBlockBreakTypes() {
		return !this.blockBreakTypes.isEmpty();
	}

	/**
	 * Clears all the materials this Minecart can break
	 */
	public void clearBlockBreakTypes() {
		this.blockBreakTypes.clear();
	}

	/**
	 * Gets a Collection of materials this Minecart can break
	 * 
	 * @return a Collection of blocks that are broken
	 */
	public Collection<Material> getBlockBreakTypes() {
		return this.blockBreakTypes;
	}

	/**
	 * Gets the Enter message that is currently displayed when a player enters
	 * 
	 * @return Enter message
	 */
	public String getEnterMessage() {
		return this.enterMessage;
	}

	/**
	 * Gets whether an Enter message is set
	 * 
	 * @return True if a message is set, False if not
	 */
	public boolean hasEnterMessage() {
		return this.enterMessage != null && !this.enterMessage.equals("");
	}

	/**
	 * Shows the enter message to the player specified
	 * 
	 * @param player to display the message to
	 */
	public void showEnterMessage(Player player) {
		if (this.hasEnterMessage()) {
			SignActionAnnounce.sendMessage(ChatColor.YELLOW + SignActionAnnounce.getMessage(enterMessage), player);
		}
	}

	@Override
	public void setEnterMessage(String message) {
		this.enterMessage = message;
	}

	public void clearDestination() {
		this.destination = "";
	}

	@Override
	public boolean hasDestination() {
		return this.destination.length() != 0;
	}

	@Override
	public void setDestination(String destination) {
		if (destination!=null)
			this.destination = destination;
		else
			this.destination = "";
	}

	@Override
	public String getDestination() {
		return this.destination;
	}

	@Override
	public void parseSet(String key, String arg) {
		if (key.equals("addtag")) {
			this.addTags(arg);
		} else if (key.equals("settag")) {
			this.setTags(arg);
		} else if (key.equals("destination")) {
			this.setDestination(arg);
		} else if (key.equals("remtag")) {
			this.removeTags(arg);
		} else if (key.equals("mobenter") || key.equals("mobsenter")) {
			this.setMobsEnter(StringUtil.getBool(arg));
		} else if (key.equals("playerenter")) {
			this.setPlayersEnter(StringUtil.getBool(arg));
		} else if (key.equals("playerexit")) {
			this.setPlayersExit(StringUtil.getBool(arg));
		} else if (key.equals("setowner")) {
			arg = arg.toLowerCase();
			this.getOwners().clear();
			this.getOwners().add(arg);
		} else if (key.equals("addowner")) {
			arg = arg.toLowerCase();
			this.getOwners().add(arg);
		} else if (key.equals("remowner")) {
			arg = arg.toLowerCase();
			this.getOwners().remove(arg);
		} else {
			return;
		}
		this.tryUpdate();
	}

	/**
	 * Loads the information from the properties specified
	 * 
	 * @param properties to load from
	 */
	public void load(CartProperties from) {
		this.destination = from.destination;
		this.owners.clear();
		this.owners.addAll(from.owners);
		this.tags.clear();
		this.tags.addAll(from.tags);
		this.allowMobsEnter = from.allowMobsEnter;
		this.allowPlayerEnter = from.allowPlayerEnter;
		this.allowPlayerExit = from.allowPlayerExit;
	}

	@Override
	public void load(ConfigurationNode node) {
		for (String owner : node.getList("owners", String.class)) {
			this.owners.add(owner.toLowerCase());
		}
		for (String tag : node.getList("tags", String.class)) {
			this.tags.add(tag);
		}
		if (this.destination==null || this.destination.isEmpty())
			this.destination = node.get("destination", this.destination);
		this.allowMobsEnter = node.get("allowMobsEnter", this.allowMobsEnter);
		this.allowPlayerEnter = node.get("allowPlayerEnter", this.allowPlayerEnter);
		this.allowPlayerExit = node.get("allowPlayerExit", this.allowPlayerExit);
		this.isPublic = node.get("isPublic", this.isPublic);
		this.pickUp = node.get("pickUp", this.pickUp);
		for (String blocktype : node.getList("blockBreakTypes", String.class)) {
			Material mat = EnumUtil.parseMaterial(blocktype, null);
			if (mat != null) this.blockBreakTypes.add(mat);
		}
	}

	@Override
	public void save(ConfigurationNode node, boolean minimal) {
		if (minimal) {
			node.set("owners", this.owners.isEmpty() ? null : new ArrayList<String>(this.owners));
			node.set("tags", this.tags.isEmpty() ? null : new ArrayList<String>(this.tags));
			node.set("allowPlayerEnter", this.allowPlayerEnter ? null : false);
			node.set("allowPlayerExit", this.allowPlayerExit ? null : false);	
			node.set("allowMobsEnter", this.allowMobsEnter ? null : false);
			node.set("isPublic", this.isPublic ? null : false);
			node.set("pickUp", this.pickUp ? true : null);
		} else {
            node.set("owners", new ArrayList<String>(this.owners));
            node.set("tags", new ArrayList<String>(this.tags));
            node.set("allowPlayerEnter", this.allowPlayerEnter);
            node.set("allowPlayerExit", this.allowPlayerExit);
            node.set("allowMobsEnter", this.allowMobsEnter);
            node.set("isPublic", this.isPublic);
            node.set("pickUp", this.pickUp);
		}
		if (!minimal || !this.blockBreakTypes.isEmpty()) {
			List<String> items = node.getList("blockBreakTypes", String.class);
			for (Material mat : this.blockBreakTypes) {
				items.add(mat.toString());
			}
		} else {
			node.remove("blockBreakTypes");
		}
		node.set("destination", this.hasDestination() ? this.destination : null);
		node.set("enterMessage", this.hasEnterMessage() ? this.enterMessage : null);
	}

	@Override
	public boolean getMobsEnter() {
		return this.allowMobsEnter;
	}

	@Override
	public void setMobsEnter(boolean state) {
		this.allowMobsEnter = state;
	}

	@Override
	public boolean getPlayersEnter() {
		return this.allowPlayerEnter;
	}

	@Override
	public void setPlayersEnter(boolean state) {
		this.allowPlayerEnter = state;
	}

	@Override
	public boolean getPlayersExit() {
		return this.allowPlayerExit;
	}

	@Override
	public void setPlayersExit(boolean state) {
		this.allowPlayerExit = state;
	}
}
