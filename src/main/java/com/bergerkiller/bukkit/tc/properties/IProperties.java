package com.bergerkiller.bukkit.tc.properties;

import java.util.Collection;

import com.bergerkiller.bukkit.common.BlockLocation;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;

/**
 * Contains train or cart property getters and setters
 */
public interface IProperties {

	/**
	 * Tests if the tag specified matches one of the tags set
	 * 
	 * @param tag to match
	 * @return True if matched, False if not
	 */
	public boolean matchTag(String tag);

	/**
	 * Tests if tags are contained
	 * 
	 * @return True if there are tags, False if not
	 */
	public boolean hasTags();

	/**
	 * Clears all set tags
	 */
	public void clearTags();

	/**
	 * Removes one or more tags
	 * 
	 * @param tags to remove
	 */
	public void removeTags(String... tag);
	
	/**
	 * Adds one or more tags
	 * 
	 * @param tags to add
	 */
	public void addTags(String... tags);

	/**
	 * Sets all the tags contained
	 * 
	 * @param tags to set to
	 */
	public void setTags(String... tags);

	/**
	 * Gets all the tags set
	 * 
	 * @return collection of set tags
	 */
	public Collection<String> getTags();

	/**
	 * Sets whether nearby items are picked up by the Minecart(s)
	 * 
	 * @param pickup True if items are picked up, False if not
	 */
	public void setPickup(boolean pickup);

	/**
	 * Gets whether it can be publicly accessed
	 * 
	 * @return True or False
	 */
	public boolean isPublic();

	/**
	 * Sets whether it can be publicly accessed
	 * 
	 * @param state to set to
	 */
	public void setPublic(boolean state);

	/**
	 * Gets whether mobs can enter
	 * 
	 * @return True or False
	 */
	public boolean getMobsEnter();

	/**
	 * Sets whether mobs can enter
	 * 
	 * @param state to set to
	 */
	public void setMobsEnter(boolean state);

	/**
	 * Gets whether players can enter
	 * 
	 * @return True or False
	 */
	public boolean getPlayersEnter();

	/**
	 * Sets whether players can enter
	 * 
	 * @param state to set to
	 */
	public void setPlayersEnter(boolean state);

	/**
	 * Gets whether players can exit
	 * 
	 * @return True or False
	 */
	public boolean getPlayersExit();

	/**
	 * Sets whether players can exit
	 * 
	 * @param state to set to
	 */
	public void setPlayersExit(boolean state);

	/**
	 * Clears the destination set
	 */
	public void clearDestination();

	/**
	 * Gets whether a destination is set
	 * 
	 * @return True if a destination is set, False if not
	 */
	public boolean hasDestination();

	/**
	 * Sets a destination
	 * 
	 * @param destination to set to
	 */
	public void setDestination(String destination);

	/**
	 * Gets the destination
	 * 
	 * @return the destination that is set
	 */
	public String getDestination();

	/**
	 * Sets the Enter Message displayed when a player enters
	 * 
	 * @param message to set to
	 */
	public void setEnterMessage(String message);

	/**
	 * Gets a predicted location of the Minecart (and thus Train)
	 * 
	 * @return Block location of the minecart
	 */
	public BlockLocation getLocation();

	/**
	 * Sets a property denoted by the key by parsing the args specified
	 * 
	 * @param key of the property
	 * @param value to set to
	 */
	public void parseSet(String key, String args);

	/**
	 * Loads the information from the Configuration Node specified
	 * 
	 * @param node to use
	 */
	public void load(ConfigurationNode node);

	/**
	 * Saves the information to the Configuration Node specified
	 * 
	 * @param node to save to
	 * @param minimal True to only save changed values, False to save everything
	 */
	public void save(ConfigurationNode node, boolean minimal);
}
