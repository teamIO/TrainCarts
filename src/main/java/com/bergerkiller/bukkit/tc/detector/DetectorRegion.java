package com.bergerkiller.bukkit.tc.detector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import net.minecraft.server.ChunkCoordinates;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.bergerkiller.bukkit.common.BlockMap;
import com.bergerkiller.bukkit.common.config.DataReader;
import com.bergerkiller.bukkit.common.config.DataWriter;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.StreamUtil;
import com.bergerkiller.bukkit.tc.TrainCarts;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;

public final class DetectorRegion {
	private static List<DetectorListener> listenerBuffer = new ArrayList<DetectorListener>();
	private static HashMap<UUID, DetectorRegion> regionsById = new HashMap<UUID, DetectorRegion>();
	private static BlockMap<List<DetectorRegion>> regions = new BlockMap<List<DetectorRegion>>();
	public static List<DetectorRegion> handleMove(MinecartMember mm, Block from, Block to) {
		if (from == to) {
		} else if (from.getWorld() != to.getWorld()) {
			handleLeave(mm, from);
		} else {
			List<DetectorRegion> list = regions.get(from);
			//Leave the regions if the to-location is not contained
			if (list != null) {
				ChunkCoordinates toCoords = BlockUtil.getCoordinates(to);
				for (DetectorRegion region : list) {
					if (!region.coordinates.contains(toCoords) && region.members.remove(mm)) {
						region.onLeave(mm);
					}
				}
			}
		}
		//Enter possible new locations
		return handleEnter(mm, to);
	}
	public static List<DetectorRegion> handleLeave(MinecartMember mm, Block block) {
		List<DetectorRegion> list = regions.get(block);
		if (list != null) {
			for (DetectorRegion region : list) {
				if (region.members.remove(mm)) {
					region.onLeave(mm);
				}
			}
		}
		return list;
	}
	public static List<DetectorRegion> handleEnter(MinecartMember mm, Block block) {
		List<DetectorRegion> list = regions.get(block);
		if (list != null) {
			for (DetectorRegion region : list) {
				if (region.members.add(mm)) {
					region.onEnter(mm);
				}
			}
		}
		return list;
	}
	
	public static DetectorRegion create(Collection<Block> blocks) {
		if (blocks.isEmpty()) return null;
		World world = null;
		Set<ChunkCoordinates> coords = new HashSet<ChunkCoordinates>(blocks.size());
		for (Block b : blocks) {
			if (world == null) {
				world = b.getWorld();
			} else if (world != b.getWorld()) {
				continue;
			}
			coords.add(BlockUtil.getCoordinates(b));
		}
		return create(world, coords);
	}
	public static DetectorRegion create(World world, final Set<ChunkCoordinates> coordinates) {
		return create(world.getName(), coordinates);
	}
	public static DetectorRegion create(final String world, final Set<ChunkCoordinates> coordinates) {
		//first check if this region is not already defined
		for (ChunkCoordinates coord : coordinates) {
			List<DetectorRegion> list = regions.get(world, coord);
			if (list != null) {
				for (DetectorRegion region : list) {
					if (!region.coordinates.containsAll(coordinates)) continue;
					if (!coordinates.containsAll(region.coordinates)) continue;
					return region;
				}
			}
			break;
		}
		return new DetectorRegion(UUID.randomUUID(), world, coordinates);
	}
	public static List<DetectorRegion> getRegions(Block at) {
		List<DetectorRegion> rval = regions.get(at);
		if (rval == null) {
			return new ArrayList<DetectorRegion>(0);
		} else {
			return rval;
		}
	}
	public static DetectorRegion getRegion(UUID uniqueId) {
		return regionsById.get(uniqueId);
	}
	private DetectorRegion(final UUID uniqueId, final String world, final Set<ChunkCoordinates> coordinates) {
		this.world = world;
		this.id = uniqueId;
		this.coordinates = coordinates;
		regionsById.put(this.id, this);
		for (ChunkCoordinates coord : this.coordinates) {
			List<DetectorRegion> list = regions.get(world, coord);
			if (list == null) {
				list = new ArrayList<DetectorRegion>(1);
				regions.put(world, coord, list);
			}
			list.add(this);
		}
		//load members
		World w = Bukkit.getServer().getWorld(this.world);
		if (w != null) {
			for (ChunkCoordinates coord : this.coordinates) {
				MinecartMember mm = MinecartMember.getAt(w, coord, false);
				if (mm != null && this.members.add(mm)) {
					this.onEnter(mm);
				}
			}
		}
	}
	private final UUID id;
	private final String world;
	private final Set<ChunkCoordinates> coordinates;
	private final Set<MinecartMember> members = new HashSet<MinecartMember>();
	private final List<DetectorListener> listeners = new ArrayList<DetectorListener>(1);
	public String getWorldName() {
		return this.world;
	}
	public Set<ChunkCoordinates> getCoordinates() {
		return this.coordinates;
	}
	public Set<MinecartMember> getMembers() {
		return this.members;
	}
	public Set<MinecartGroup> getGroups() {
		Set<MinecartGroup> rval = new HashSet<MinecartGroup>();
		for (MinecartMember mm : this.members) {
			if (mm.getGroup() == null) continue;
			rval.add(mm.getGroup());
		}
		return rval;
	}
	public UUID getUniqueId() {
		return this.id;
	}
	public void register(DetectorListener listener) {
		this.listeners.add(listener);
		listener.onRegister(this);
		for (MinecartMember mm : this.members) {
			listener.onEnter(mm);
		}
	}
	public void unregister(DetectorListener listener) {
		this.listeners.remove(listener);
		listener.onUnregister(this);
		for (MinecartMember mm : this.members) {
			listener.onLeave(mm);
		}
	}
	public boolean isRegistered() {
		return !this.listeners.isEmpty();
	}
	private void onLeave(MinecartMember mm) {
		this.setListenerBuffer();
		for (DetectorListener listener : listenerBuffer) {
			listener.onLeave(mm);
		}
		for (MinecartMember ex : this.members) {
			if (ex != mm && ex.getGroup() == mm.getGroup()) return;
		}
		for (DetectorListener listener : listenerBuffer) {
			listener.onLeave(mm.getGroup());
		}
	}
	private void onEnter(MinecartMember mm) {
		this.setListenerBuffer();
		for (DetectorListener listener : listenerBuffer) {
			listener.onEnter(mm);
		}
		for (MinecartMember ex : this.members) {
			if (ex != mm && ex.getGroup() == mm.getGroup()) return;
		}
		for (DetectorListener listener : listenerBuffer) {
			listener.onEnter(mm.getGroup());
		}
	}
	private void setListenerBuffer() {
		listenerBuffer.clear();
		listenerBuffer.addAll(this.listeners);
	}

	public void update(MinecartMember member) {
		for (DetectorListener list : this.listeners) {
			list.onUpdate(member);
		}
	}
	public void update(MinecartGroup group) {
		for (DetectorListener list : this.listeners) {
			list.onUpdate(group);
		}
	}
	
	public void remove() {
		Iterator<MinecartMember> iter = this.members.iterator();
		while (iter.hasNext()) {
			this.onLeave(iter.next());
			iter.remove();
		}
		regionsById.remove(this.id);
		for (ChunkCoordinates coord : this.coordinates) {
			List<DetectorRegion> list = regions.get(this.world, coord);
			if (list == null) continue;
			list.remove(this);
			if (list.isEmpty()) {
				regions.remove(this.world, coord);
			}
		}
	}
	
	public static void init(String filename) {
		regionsById.clear();
		regions.clear();
		new DataReader(filename) {
			public void read(DataInputStream stream) throws IOException {
				int count = stream.readInt();
				int coordcount;
				for (;count > 0; --count) {
					//get required info
					UUID id = StreamUtil.readUUID(stream);
					String world = stream.readUTF();
					coordcount = stream.readInt();
					Set<ChunkCoordinates> coords = new HashSet<ChunkCoordinates>(coordcount);
					for (;coordcount > 0; --coordcount) {
						coords.add(StreamUtil.readCoordinates(stream));
					}
					//create
					new DetectorRegion(id, world, coords);
				}
				if (regionsById.size() == 1) {
					TrainCarts.plugin.log(Level.INFO, regionsById.size() + " detector rail region loaded covering " + regions.size() + " blocks");
				} else {
					TrainCarts.plugin.log(Level.INFO, regionsById.size() + " detector rail regions loaded covering " + regions.size() + " blocks");
				}
			}
		}.read();
	}
	public static void deinit(String filename) {
		new DataWriter(filename) {
			public void write(DataOutputStream stream) throws IOException {
				stream.writeInt(regionsById.size());
				for (DetectorRegion region : regionsById.values()) {
					StreamUtil.writeUUID(stream, region.id);
					stream.writeUTF(region.world);
					stream.writeInt(region.coordinates.size());
					for (ChunkCoordinates coord : region.coordinates) {
						StreamUtil.writeCoordinates(stream, coord);
					}
				}
			}
		}.write();
	}
	
}
