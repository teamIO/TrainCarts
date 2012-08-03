package com.bergerkiller.bukkit.tc.signactions.spawner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.StreamUtil;
import com.bergerkiller.bukkit.tc.TrainCarts;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignActionSpawn;
import com.bergerkiller.bukkit.tc.storage.OfflineSign;

import net.minecraft.server.ChunkCoordinates;

public class SpawnSign extends OfflineSign {

	private final long interval;
	private long lastSpawnTime;
	private String world;
	private World lastWorld;
	private Task task = null;

	public SpawnSign(Block block, long interval) {
		this(BlockUtil.getCoordinates(block), block.getWorld().getName(), interval);
	}

	public SpawnSign(ChunkCoordinates location, String worldname, long interval) {
		super(location);
		this.interval = interval;
		this.world = worldname;
		this.lastSpawnTime = System.currentTimeMillis();
	}

	public void write(DataOutputStream stream) throws IOException {
		StreamUtil.writeCoordinates(stream, this.getLocation());
		stream.writeUTF(this.world);
		stream.writeLong(this.interval);
		stream.writeLong(this.getRemaining());
	}

	public static SpawnSign read(DataInputStream stream) throws IOException {
		ChunkCoordinates coord = StreamUtil.readCoordinates(stream);
		String world = stream.readUTF();
		long interval = stream.readLong();
		SpawnSign sign = new SpawnSign(coord, world, interval);
		sign.lastSpawnTime = System.currentTimeMillis() - (interval - stream.readLong());
		return sign;
	}

	public String getWorldName() {
		return this.world;
	}

	public World getWorld() {
		if (this.lastWorld == null) {
			this.lastWorld = Bukkit.getWorld(this.world);
		}
		return this.lastWorld;
	}

	public void cleanWorld() {
		this.lastWorld = null;
	}

	public boolean isLoaded() {
		return this.isLoaded(this.getWorld());
	}

	public long getRemaining() {
		long time = this.interval - (System.currentTimeMillis() - lastSpawnTime);
		return time < 0 ? 0 : time;
	}

	public long getRemainingTicks() {
		return (long) Math.floor((double) this.getRemaining() / 50d);
	}

	public long getSpawnInterval() {
		return this.interval;
	}

	public void start() {
		this.stop();
		this.task = new Task(TrainCarts.plugin, this) {
			public void run() {
				// Start a new task which fires more often
				SpawnSign sign = arg(0, SpawnSign.class);
				sign.task = new Task(TrainCarts.plugin, sign) {
					public void run() {
						SpawnSign sign = arg(0, SpawnSign.class);
						sign.updateSpawn();
					}
				}.start(0, 5);
			}
		}.start(this.getRemainingTicks());
	}

	public void stop() {
		if (this.task != null) {
			this.task.stop();
			this.task = null;
		}
		this.lastSpawnTime = System.currentTimeMillis();
	}

	/**
	 * Updates the spawning state, called in the last few seconds before spawning<br>
	 * Spawns the train itself
	 */
	public void updateSpawn() {
		World world = this.getWorld();
		if (world == null) {
			this.start();
		} else if (this.getRemaining() == 0) {
			this.cleanWorld();
			this.loadChunks(world);
			SignActionEvent event = this.getSignEvent(world);
			if (event != null) {
				// Spawn a train at the sign
				SignActionSpawn.spawn(event);
			}
			this.start();
		}
	}

	@Override
	public boolean validate(SignActionEvent event) {
		return SignActionSpawn.isValid(event);
	}

	@Override
	public void onRemove(Block signBlock) {
		if (this.task != null) {
			this.task.stop();
		}
		SignActionSpawn.remove(signBlock);
	}
}
