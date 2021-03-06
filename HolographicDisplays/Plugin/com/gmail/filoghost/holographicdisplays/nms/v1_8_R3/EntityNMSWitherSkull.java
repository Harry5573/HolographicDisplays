package com.gmail.filoghost.holographicdisplays.nms.v1_8_R3;

import com.gmail.filoghost.holographicdisplays.nms.interfaces.entity.NMSWitherSkull;
import com.gmail.filoghost.holographicdisplays.object.line.CraftHologramLine;
import com.gmail.filoghost.holographicdisplays.util.Utils;
import net.minecraft.server.v1_8_R3.DamageSource;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityWitherSkull;
import net.minecraft.server.v1_8_R3.MathHelper;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;

public class EntityNMSWitherSkull extends EntityWitherSkull implements NMSWitherSkull {

	private boolean lockTick;
	private CraftHologramLine parentPiece;

	public EntityNMSWitherSkull(World world, CraftHologramLine parentPiece) {
		super(world);
		super.motX = 0.0;
		super.motY = 0.0;
		super.motZ = 0.0;
		super.dirX = 0.0;
		super.dirY = 0.0;
		super.dirZ = 0.0;
		super.a(0.0F, 0.0F);
		super.a(new NullBoundingBox());
		this.parentPiece = parentPiece;
	}

	@Override
	public void b(NBTTagCompound nbttagcompound) {
		// Do not save NBT.
	}

	@Override
	public boolean c(NBTTagCompound nbttagcompound) {
		// Do not save NBT.
		return false;
	}

	@Override
	public boolean d(NBTTagCompound nbttagcompound) {
		// Do not save NBT.
		return false;
	}

	@Override
	public void e(NBTTagCompound nbttagcompound) {
		// Do not save NBT.
	}

	@Override
	public boolean isInvulnerable(DamageSource source) {
		/*
		 * The field Entity.invulnerable is private.
		 * It's only used while saving NBTTags, but since the entity would be killed
		 * on chunk unload, we prefer to override isInvulnerable().
		 */
		return true;
	}

	@Override
	public int getId() {

		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		if (elements.length > 2 && elements[2] != null && elements[2].getFileName().equals("EntityTrackerEntry.java") && elements[2].getLineNumber() > 137 && elements[2].getLineNumber() < 147) {
			// Then this method is being called when creating a new packet, we return a fake ID!
			return -1;
		}
		if (elements.length > 2 && elements[2] != null && elements[2].getFileName().equals("EntityTrackerEntry.java") && elements[2].getLineNumber() > 134 && elements[2].getLineNumber() < 144) {
			// Then this method is being called when creating a new packet, we return a fake ID!
			return -1;
		}

		return super.getId();
	}

	@Override
	public void t_() {
		if (!lockTick) {
			super.t_();
		}
	}

	@Override
	public void makeSound(String sound, float volume, float pitch) {
		// Remove sounds.
	}

	@Override
	public void setLockTick(boolean lock) {
		lockTick = lock;
	}

	@Override
	public void die() {
		setLockTick(false);
		super.die();
	}

	@Override
	public CraftEntity getBukkitEntity() {
		if (super.bukkitEntity == null) {
			this.bukkitEntity = new CraftNMSWitherSkull(this.world.getServer(), this);
		}
		return this.bukkitEntity;
	}

	@Override
	public void killEntityNMS() {
		die();
	}

	@Override
	public void setLocationNMS(double x, double y, double z) {
		super.setPosition(x, y, z);

		// Send a packet near to update the position.
		PacketPlayOutEntityTeleport teleportPacket = new PacketPlayOutEntityTeleport(
			getIdNMS(),
			MathHelper.floor(this.locX * 32.0D),
			MathHelper.floor(this.locY * 32.0D),
			MathHelper.floor(this.locZ * 32.0D),
			(byte) (int) (this.yaw * 256.0F / 360.0F),
			(byte) (int) (this.pitch * 256.0F / 360.0F),
			this.onGround
		);

		for (Object obj : this.world.players) {
			if (obj instanceof EntityPlayer) {
				EntityPlayer nmsPlayer = (EntityPlayer) obj;

				double distanceSquared = Utils.square(nmsPlayer.locX - this.locX) + Utils.square(nmsPlayer.locZ - this.locZ);
				if (distanceSquared < 8192 && nmsPlayer.playerConnection != null) {
					nmsPlayer.playerConnection.sendPacket(teleportPacket);
				}
			}
		}
	}

	@Override
	public boolean isDeadNMS() {
		return super.dead;
	}

	@Override
	public int getIdNMS() {
		return super.getId();
	}

	@Override
	public CraftHologramLine getHologramLine() {
		return parentPiece;
	}

	@Override
	public org.bukkit.entity.Entity getBukkitEntityNMS() {
		return getBukkitEntity();
	}
}
