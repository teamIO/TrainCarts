package com.bergerkiller.bukkit.tc;

import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.utils.FaceUtil;

public enum Direction {
	NORTH("n", "north"), EAST("e", "east"), SOUTH("s", "south"), 
	WEST("w", "west"), LEFT("l", "left"), RIGHT("r", "right"), 
	FORWARD("f", "front", "forward", "forwards", "continue"), 
	BACKWARD("b", "back", "backward", "backwards", "reverse"), 
	NONE("", "n", "none");
	
	private final String[] aliases;
	private Direction(String... aliases) {
		this.aliases = aliases;
	}
	
	public BlockFace getDirection(BlockFace signfacing) {
		return getDirection(signfacing, signfacing.getOppositeFace());
	}

	public BlockFace getDirection(BlockFace signfacing, BlockFace cartdirection) {
		switch (this) {
		case NORTH : return BlockFace.NORTH;
		case EAST : return BlockFace.EAST;
		case SOUTH : return BlockFace.SOUTH;
		case WEST : return BlockFace.WEST;
		case LEFT : return FaceUtil.rotate(signfacing, 2);
		case RIGHT : return FaceUtil.rotate(signfacing, -2);
		case FORWARD : return cartdirection;
		case BACKWARD : return cartdirection.getOppositeFace();
		default : return cartdirection;
		}
	}
	
	public boolean match(char character) {
		for (String alias : this.aliases) {
			if (alias.length() == 1 && alias.charAt(0) == character) {
				return true;
			}
		}
		return false;
	}
	
	public boolean match(String text) {
		for (String alias : this.aliases) {
			if (alias.equalsIgnoreCase(text)) return true;
		}
		return false;
	}
	
	public static Direction parse(char character) {
		for (Direction dir : values()) {
			if (dir.match(character)) return dir;
		}
		return NONE;
	}
	
	public static Direction parse(String text) {
		for (Direction dir : values()) {
			if (dir.match(text)) return dir;
		}
		return NONE;
	}
}
