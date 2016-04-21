package com.gmail.filoghost.holographicdisplays.object.line;

import com.gmail.filoghost.holographicdisplays.HolographicDisplays;
import com.gmail.filoghost.holographicdisplays.api.handler.TouchHandler;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import com.gmail.filoghost.holographicdisplays.nms.interfaces.entity.NMSCanMount;
import com.gmail.filoghost.holographicdisplays.nms.interfaces.entity.NMSEntityBase;
import com.gmail.filoghost.holographicdisplays.nms.interfaces.entity.NMSNameable;
import com.gmail.filoghost.holographicdisplays.object.CraftHologram;
import com.gmail.filoghost.holographicdisplays.placeholder.PlaceholdersManager;
import com.gmail.filoghost.holographicdisplays.util.Offsets;
import org.bukkit.Location;
import org.bukkit.World;

public class CraftTextLine extends CraftTouchableLine implements TextLine {

	private String text;

	private NMSNameable nmsNameblev17;
	private NMSNameable nmsNameblev18;

	// Legacy code for < 1.7, not used in 1.8 and greater
	private NMSEntityBase nmsSkullVehicle;

	public CraftTextLine(CraftHologram parent, String text) {
		super(0.23, parent);
		setText(text);
	}

	@Override
	public String getText() {
		return this.text;
	}

	@Override
	public void setText(String text) {
		this.text = text;

		if (text != null && !text.isEmpty()) {
			if (this.nmsNameblev17 != null) {
				this.nmsNameblev17.setCustomNameNMS(text);
			}
			if (this.nmsNameblev18 != null) {
				this.nmsNameblev18.setCustomNameNMS(text);
			}
			if (getParent().isAllowPlaceholders()) {
				PlaceholdersManager.trackIfNecessary(this);
			}
		}
		else {
			if (this.nmsNameblev17 != null) {
				this.nmsNameblev17.setCustomNameNMS(""); // It will not appear
			}
			if (this.nmsNameblev18 != null) {
				this.nmsNameblev18.setCustomNameNMS(""); // It will not appear
			}
			if (getParent().isAllowPlaceholders()) {
				PlaceholdersManager.untrack(this);
			}
		}
	}

	public void setTouchHandler(TouchHandler touchHandler) {
		if (true) {
			throw new UnsupportedOperationException();
		}

		if (this.nmsNameblev18 != null) {
			Location loc = this.nmsNameblev18.getBukkitEntityNMS().getLocation();
			super.setTouchHandler(touchHandler, loc.getWorld(), loc.getX(), loc.getY() - this.getTextOffset(), loc.getZ());
		}
		else {
			super.setTouchHandler(touchHandler, null, 0, 0, 0);
		}
	}

	@Override
	public void spawn(World world, double x, double y, double z) {
		super.spawn(world, x, y, z);

		this.nmsNameblev17 = HolographicDisplays.getNMSManager().spawnNMSHorse(world, x, y + Offsets.WITHER_SKULL_WITH_HORSE, z, this);
		this.nmsNameblev18 = HolographicDisplays.getNMSManager().spawnNMSArmorStand(world, x, y + this.getTextOffset(), z, this);
		this.nmsSkullVehicle = HolographicDisplays.getNMSManager().spawnNMSWitherSkull(world, x, y + Offsets.WITHER_SKULL_WITH_HORSE, z, this);

		// In 1.7 it must be an instanceof NMSCanMount
		((NMSCanMount) this.nmsNameblev17).setPassengerOfNMS(this.nmsSkullVehicle);

		if (this.text != null && !this.text.isEmpty()) {
			this.nmsNameblev17.setCustomNameNMS(this.text);
			this.nmsNameblev18.setCustomNameNMS(this.text);
		}

		this.nmsNameblev17.setLockTick(true);
		this.nmsNameblev18.setLockTick(true);
		this.nmsSkullVehicle.setLockTick(true);
	}

	@Override
	public void despawn() {
		super.despawn();

		if (this.nmsNameblev17 != null) {
			this.nmsNameblev17.killEntityNMS();
			this.nmsNameblev17 = null;
		}
		if (this.nmsNameblev18 != null) {
			this.nmsNameblev18.killEntityNMS();
			this.nmsNameblev18 = null;
		}
		if (this.nmsSkullVehicle != null) {
			this.nmsSkullVehicle.killEntityNMS();
			this.nmsSkullVehicle = null;
		}
	}

	@Override
	public void teleport(double x, double y, double z) {
		super.teleport(x, y, z);

		if (this.nmsNameblev17 != null) {
			this.nmsNameblev17.setLocationNMS(x, y + Offsets.WITHER_SKULL_WITH_HORSE, z);
		}
		if (this.nmsNameblev18 != null) {
			this.nmsNameblev18.setLocationNMS(x, y + this.getTextOffset(), z);
		}
		if (this.nmsSkullVehicle != null) {
			this.nmsSkullVehicle.setLocationNMS(x, y + Offsets.WITHER_SKULL_WITH_HORSE, z);
		}
	}

	@Override
	public int[] getEntitiesIDs() {
		if (isSpawned()) {
			return new int[] {this.nmsNameblev17.getIdNMS(), this.nmsNameblev18.getIdNMS(), this.nmsSkullVehicle.getIdNMS()};
		}
		else {
			return new int[0];
		}
	}

	public NMSNameable getNmsNameblev17() {
		return this.nmsNameblev17;
	}

	public NMSNameable getNmsNameblev18() {
		return this.nmsNameblev18;
	}

	public NMSEntityBase getNmsSkullVehicle() {
		return this.nmsSkullVehicle;
	}

	private double getTextOffset() {
		if (HolographicDisplays.is19orGreater()) {
			return Offsets.ARMOR_STAND_ALONE_1_9;
		}
		else if (HolographicDisplays.is18orGreater()) {
			return Offsets.ARMOR_STAND_ALONE;
		}
		else {
			return Offsets.WITHER_SKULL_WITH_HORSE;
		}
	}

	@Override
	public String toString() {
		return "CraftTextLine [text=" + text + "]";
	}
}
