package com.bergerkiller.bukkit.tc.itemanimation;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityItem;
import net.minecraft.server.World;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.utils.ItemUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

/**
 * A dummy class that basically does nothing :)
 */
public class VirtualItem extends EntityItem {

	public VirtualItem(Location location, ItemStack itemstack) {
		this(location.getWorld(), location.getX(), location.getY(), location.getZ(), itemstack);
	}
	public VirtualItem(org.bukkit.World world, double x, double y, double z, ItemStack itemstack) {
		this(WorldUtil.getNative(world), x, y, z, ItemUtil.getNative(itemstack));
	}
	public VirtualItem(World world, double x, double y, double z, net.minecraft.server.ItemStack itemstack) {
		super(world, x, y, z, itemstack);
		WorldUtil.getTracker(world).track(this);
	}
	
	@Override
	public void a_(EntityHuman entityhuman) {};
	
	@Override
	public void F_() {
		this.lastX = this.locX;
		this.lastY = this.locY;
		this.lastZ = this.locZ;
		this.locX += this.motX;
		this.locY += this.motY;
		this.locZ += this.motZ;
	};
	
	public void die() {
		super.die();
		WorldUtil.getTracker(world).untrackEntity(this);
	}

}
