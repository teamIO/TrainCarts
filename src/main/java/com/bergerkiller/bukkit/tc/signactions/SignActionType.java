package com.bergerkiller.bukkit.tc.signactions;

public enum SignActionType {
	
	/**
	 * Redstone state changes
	 */
	REDSTONE_CHANGE,
	/**
	 * Powered by Redstone
	 */
	REDSTONE_ON,
	/**
	 * Not powered by Redstone
	 */
	REDSTONE_OFF,
	/**
	 * Cart member enters
	 */
	MEMBER_ENTER,
	/**
	 * Cart member moves
	 */
	MEMBER_MOVE,
	/**
	 * Cart member leaves
	 */
	MEMBER_LEAVE,
	/**
	 * Train enters
	 */
	GROUP_ENTER,
	/**
	 * Train leaves
	 */
	GROUP_LEAVE,
	/**
	 * Cart member is updated (properties?)
	 */
	MEMBER_UPDATE,
	/**
	 * Train member is updated (properties?)
	 */
	GROUP_UPDATE;
}