package com.gmail.filoghost.holographicdisplays.nms.v1_8_R3;

import com.gmail.filoghost.holographicdisplays.nms.interfaces.entity.NMSEntityBase;
import com.gmail.filoghost.holographicdisplays.nms.interfaces.entity.NMSHorse;
import com.gmail.filoghost.holographicdisplays.object.line.CraftHologramLine;
import com.gmail.filoghost.holographicdisplays.util.DebugHandler;
import com.gmail.filoghost.holographicdisplays.util.ReflectionUtils;
import net.minecraft.server.v1_8_R3.DamageSource;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityHorse;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;

public class EntityNMSHorse extends EntityHorse implements NMSHorse {

	private boolean lockTick;
	private CraftHologramLine parentPiece;

	public EntityNMSHorse(World world, CraftHologramLine parentPiece) {
		super(world);
		super.ageLocked = true;
		super.persistent = true;
		super.a(0.0F, 0.0F);
		super.a(new NullBoundingBox());
		this.setAge(-1700000); // This is a magic value. No one will see the real horse.

		this.parentPiece = parentPiece;
	}

	@Override
	public void setAge(int age) {
		this.datawatcher.watch(12, age);
		this.a = age;
		this.a(this.isBaby());
	}

	@Override
	public void t_() {
		// Checks every 20 ticks.
		if (ticksLived % 20 == 0) {
			// The horse dies without a vehicle.
			if (this.vehicle == null) {
				die();
			}
		}

		if (!lockTick) {
			super.t_();
		}
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
	public void setCustomName(String customName) {
		// Locks the custom name.
	}

	@Override
	public void setCustomNameVisible(boolean visible) {
		// Locks the custom name.
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
	public void setCustomNameNMS(String name) {
		if (name != null && name.length() > 300) {
			name = name.substring(0, 300);
		}
		super.setCustomName(name);
		super.setCustomNameVisible(name != null && !name.isEmpty());
	}

	@Override
	public CraftEntity getBukkitEntity() {
		if (super.bukkitEntity == null) {
			this.bukkitEntity = new CraftNMSHorse(this.world.getServer(), this);
		}
		return this.bukkitEntity;
	}

	@Override
	public boolean isDeadNMS() {
		return super.dead;
	}

	@Override
	public String getCustomNameNMS() {
		return super.getCustomName();
	}

	@Override
	public void killEntityNMS() {
		this.die();
	}

	@Override
	public void setLocationNMS(double x, double y, double z) {
		super.setPosition(x, y, z);
	}

	@Override
	public int getIdNMS() {
		return this.getId();
	}

	@Override
	public CraftHologramLine getHologramLine() {
		return parentPiece;
	}

	@Override
	public org.bukkit.entity.Entity getBukkitEntityNMS() {
		return getBukkitEntity();
	}

	@Override
	public void setPassengerOfNMS(NMSEntityBase vehicleBase) {
		if (vehicleBase == null || !(vehicleBase instanceof Entity)) {
			// It should never dismount
			return;
		}

		Entity entity = (Entity) vehicleBase;

		try {
			ReflectionUtils.setPrivateField(Entity.class, this, "ar", (double) 0.0);
			ReflectionUtils.setPrivateField(Entity.class, this, "as", (double) 0.0);
		} catch (Exception ex) {
			DebugHandler.handleDebugException(ex);
		}

		if (this.vehicle != null) {
			this.vehicle.passenger = null;
		}

		this.vehicle = entity;
		entity.passenger = this;
	}
}
