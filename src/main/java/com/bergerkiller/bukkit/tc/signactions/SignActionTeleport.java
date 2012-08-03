package com.bergerkiller.bukkit.tc.signactions;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.tc.Permission;
import com.bergerkiller.bukkit.tc.TrainCarts;
import com.bergerkiller.bukkit.tc.Util;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.utils.BlockTimeoutMap;
import com.bergerkiller.bukkit.common.utils.BlockUtil;

public class SignActionTeleport extends SignAction {

	private BlockTimeoutMap teleportTimes = new BlockTimeoutMap();

	@Override
	public void execute(SignActionEvent info) {
		if (!TrainCarts.MyWorldsEnabled) return;
		if (!info.getLine(0).equalsIgnoreCase("[portal]")) return;
		if (info.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && info.hasGroup()) {
			if (info.isPowered()) {
				if (!info.hasRails()) return;
				Portal portal = Portal.get(info.getLocation());
				if (portal != null) {
					String destname = portal.getDestinationName();
					Location dest = Portal.getPortalLocation(destname, info.getGroup().getWorld().getName());
					if (dest != null) {
						//Teleport the ENTIRE train to the destination...
						Block sign = dest.getBlock();
						sign.getChunk(); //load the chunk
						if (BlockUtil.isSign(sign)) {
							BlockFace facing = BlockUtil.getFacing(sign);
							BlockFace direction = facing;
							Block destinationRail = Util.getRailsFromSign(sign);
							if (destinationRail == null) {
								return;
							}
							boolean isPlate = Util.isPressurePlate(destinationRail.getTypeId());
							if (isPlate || BlockUtil.isRails(destinationRail)) {
								//rail aligned at sign?
								if (facing == BlockFace.NORTH) facing = BlockFace.SOUTH;
								if (facing == BlockFace.EAST) facing = BlockFace.WEST;
								if (isPlate || facing == BlockUtil.getRails(destinationRail).getDirection()) {
									//Allowed?
									if (!this.teleportTimes.isMarked(info.getBlock(), MyWorlds.teleportInterval)) {
										info.getGroup().teleportAndGo(destinationRail, direction);
										this.teleportTimes.mark(sign);
									}
								}
							}
						}
					}
				}
			}
		}	
	}
	@Override
	public boolean build(SignChangeActionEvent event) {
		if (event.getLine(0).equalsIgnoreCase("[portal]")) {
			if (event.hasRails()) {
				return handleBuild(event, Permission.BUILD_TELEPORTER, "train teleporter", "teleport trains large distances to another teleporter sign");
			}
		}
		return false;
	}
}
