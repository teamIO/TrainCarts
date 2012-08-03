package com.bergerkiller.bukkit.tc.signactions;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.material.Directional;

import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.tc.Permission;
import com.bergerkiller.bukkit.tc.Util;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.utils.BlockTimeoutMap;
import com.bergerkiller.bukkit.tc.utils.TrackIterator;

public class SignActionElevator extends SignAction {
		
	public static boolean isElevator(Sign sign) {
		if (SignActionMode.fromSign(sign) != SignActionMode.NONE) {
			if (sign.getLine(1).toLowerCase().startsWith("elevator")) {
				return true;
			}
		}
		return false;
	}
	
	public static Block findElevator(Block from, BlockFace mode) {
		while ((from = Util.findRailsVertical(from, mode)) != null) {
			for (Block signblock : Util.getSignsFromRails(from)) {
				if (isElevator(BlockUtil.getSign(signblock))) {
					return from;
				}
			}
		}
		return null;
	}
	
	public static Block findElevator(Block from, BlockFace mode, int elevatorCount) {
		while ((from = findElevator(from, mode)) != null) {
			if (--elevatorCount <= 0) {
				return from;
			}
		}
		return null;
	}
	
	public static BlockTimeoutMap ignoreTimes = new BlockTimeoutMap();
	
	public static BlockFace getSpawnDirection(Block destrail) {
		return getSpawnDirection(destrail, FaceUtil.getFaces(BlockUtil.getRails(destrail).getDirection().getOppositeFace()));
	}
	
	public static BlockFace getSpawnDirection(Block destrail, BlockFace[] possible) {
		//find out which direction is best for this occasion
		BlockFace rval = possible[0];
		int dist = 0;
		int i = 0;
		for (BlockFace f : possible) {
			TrackIterator iter = new TrackIterator(destrail, f);
			final int lim = 4;
			for (i = 0; i < lim && iter.hasNext(); i++) iter.next();
			if (i > dist) {
				rval = f;
				dist = i;
			}
		}
		return rval;
	}
	
	@Override
	public void execute(SignActionEvent info) {
		if (!info.isType("elevator")) return;
		if (info.getMode() != SignActionMode.NONE && info.hasRails() && info.hasMember() && info.isPowered()) {
			if (info.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_CHANGE)) {
				//is it allowed?
				if (ignoreTimes.isMarked(info.getRails(), 1000)) {
					return;
				}
				
				//where to go?
				boolean forced = false;
				BlockFace mode = BlockFace.UP;
				if (info.isLine(2, "down")) {
					mode = BlockFace.DOWN;
					forced = true;
				} else if (info.isLine(2, "up")) {
					forced = true;
				}
				//possible amounts to skip?
				int elevatorCount = Util.parse(info.getLine(2), 1);
				Block dest = findElevator(info.getRails(), mode, elevatorCount);
				if (!forced && dest == null) {
					dest = findElevator(info.getRails(), mode.getOppositeFace(), elevatorCount);
				}
				if (dest != null) {
					ignoreTimes.mark(dest);
					
					//get the direction to spawn and launch to
					
					//first, use the sign direction
					Sign destsign = null;
					for (Block signblock : Util.getSignsFromRails(dest)) {
						if (isElevator(destsign = BlockUtil.getSign(signblock))) {
							break;
						}
					}
					
					//facing towards a rail direction?
					BlockFace[] startDirs = FaceUtil.getFaces(BlockUtil.getRails(dest).getDirection().getOppositeFace());

					BlockFace launchDir = null;
					if (destsign != null) {
						BlockFace signdir = ((Directional) destsign.getData()).getFacing();
						if (startDirs[0] == signdir || startDirs[1] == signdir) {
							launchDir = signdir;
						}
					}
					if (launchDir == null) {
						//find out which direction is best
						launchDir = getSpawnDirection(dest, startDirs);
					}
					
					//teleport train
					info.getGroup().teleportAndGo(dest, launchDir);
				}
			}
		}
	}

	@Override
	public boolean build(SignChangeActionEvent event) {
		if (event.getMode() != SignActionMode.NONE) {
			if (event.isType("elevator")) {
				return handleBuild(event, Permission.BUILD_ELEVATOR, "train elevator", "teleport trains vertically");
			}
		}
		return false;
	}
}
