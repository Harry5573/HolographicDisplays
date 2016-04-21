package com.gmail.filoghost.holographicdisplays.bridge.protocollib.pre1_9;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.gmail.filoghost.holographicdisplays.api.line.ItemLine;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import com.gmail.filoghost.holographicdisplays.api.line.TouchableLine;
import com.gmail.filoghost.holographicdisplays.bridge.protocollib.ProtocolLibHook;
import com.gmail.filoghost.holographicdisplays.nms.interfaces.NMSManager;
import com.gmail.filoghost.holographicdisplays.nms.interfaces.entity.NMSEntityBase;
import com.gmail.filoghost.holographicdisplays.object.CraftHologram;
import com.gmail.filoghost.holographicdisplays.object.line.CraftHologramLine;
import com.gmail.filoghost.holographicdisplays.util.Offsets;
import com.google.common.primitives.Ints;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import uk.harry5573.cardboard.protocol.Protocol;

public class ProtocolLibHookImpl implements ProtocolLibHook {

	private NMSManager nmsManager;
	private final Set<Integer> processedPackets = new HashSet<>();

	@Override
	public boolean hook(Plugin plugin, NMSManager nmsManager) {
		this.nmsManager = nmsManager;

		if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
			return false;
		}

		plugin.getLogger().info("Found ProtocolLib, adding support for 1.7.x/1.9.x clients.");

		ProtocolLibrary.getProtocolManager().addPacketListener(
			new PacketAdapter(
				plugin,
				ListenerPriority.NORMAL,
				PacketType.Play.Server.SPAWN_ENTITY,
				PacketType.Play.Server.SPAWN_ENTITY_LIVING,
				PacketType.Play.Server.ENTITY_TELEPORT,
				PacketType.Play.Server.ENTITY_METADATA,
				PacketType.Play.Server.ENTITY_DESTROY) {

				@Override
				public void onPacketSending(PacketEvent event) {
					PacketContainer packet = event.getPacket();
					if (ProtocolLibHookImpl.this.processedPackets.remove(System.identityHashCode(packet.getHandle()))) {
						return;
					}

					if (event.isCancelled()) {
						return;
					}

					PacketType packetType = packet.getType();
					Player player = event.getPlayer();
					int clientVersion = ((CraftPlayer) player).getHandle().playerConnection.networkManager.getVersion();

					if (packetType == PacketType.Play.Server.ENTITY_DESTROY) {
						EquivalentConverter<Entity> converter = BukkitConverters.getEntityConverter(player.getWorld());

						Set<Integer> filteredEntityIDs = new HashSet<>();
						int[] entityIDs = packet.getIntegerArrays().read(0);
						for (int entityID : entityIDs) {
							Entity entity = converter.getSpecific(entityID);
							if (entity != null) {
								CraftHologramLine line = ProtocolLibHookImpl.this.getHologramLine(entity);
								if (line != null) {
									boolean v17 = Protocol.isv17(clientVersion);
									if ((v17 && entity.getType() == EntityType.ARMOR_STAND) || (!v17 && (entity.getType() == EntityType.HORSE || entity.getType() == EntityType.WITHER_SKULL))) {
										continue;
									}
								}
							}
							filteredEntityIDs.add(entityID);
						}

						event.setCancelled(true);

						if (filteredEntityIDs.isEmpty()) {
							return;
						}

						PacketContainer updatedPacket = packet.shallowClone();
						updatedPacket.getIntegerArrays().write(0, Ints.toArray(filteredEntityIDs));
						ProtocolLibHookImpl.this.sendProcessedPacket(player, updatedPacket);
						return;
					}

					Entity entity = packet.getEntityModifier(event).read(0);
					if (entity == null) {
						return;
					}

					CraftHologramLine line = ProtocolLibHookImpl.this.getHologramLine(entity);
					if (line == null) {
						return;
					}

					if (Protocol.isv17(clientVersion)) {
						// Hide 1.8.x and 1.9.x entities for 1.7.x clients
						if (entity.getType() == EntityType.ARMOR_STAND) {
							event.setCancelled(true);
						}
						return;
					}

					// Hide 1.7 entities for 1.8.x and 1.9.x clients
					if (entity.getType() == EntityType.HORSE || entity.getType() == EntityType.WITHER_SKULL) {
						event.setCancelled(true);
						return;
					}

					if (!Protocol.isv19(clientVersion)) {
						return;
					}

					// Set marker flag for 1.9.x clients
					if (packetType == PacketType.Play.Server.ENTITY_METADATA) {
						List<WrappedWatchableObject> previousMetadata = packet.getWatchableCollectionModifier().read(0);
						List<WrappedWatchableObject> updatedMetadata = null;

						for (int i = 0; i < previousMetadata.size(); i++) {
							WrappedWatchableObject object = previousMetadata.get(i);
							if (object.getIndex() == 10) {
								updatedMetadata = new ArrayList<>(previousMetadata);

								WrappedWatchableObject newObject = null;
								try {
									newObject = (WrappedWatchableObject) object.getClass().getMethod("deepClone").invoke(object);
								} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException exception) {
									exception.printStackTrace();
								}

								newObject.setValue((byte) ((byte) object.getValue() | 16));
								updatedMetadata.set(i, newObject);
								break;
							}
						}

						if (updatedMetadata != null) {
							event.setCancelled(true);

							PacketContainer updatedPacket = packet.shallowClone();
							updatedPacket.getWatchableCollectionModifier().write(0, updatedMetadata);
							ProtocolLibHookImpl.this.sendProcessedPacket(player, updatedPacket);
						}
						return;
					}

					// Correct offsets for 1.9.x clients
					double y = packet.getIntegers().read(2);
					y /= 32;
					if (line instanceof TextLine) {
						y -= Offsets.ARMOR_STAND_ALONE;
						y += Offsets.ARMOR_STAND_ALONE_1_9;
					}
					else if (line instanceof ItemLine) {
						y -= Offsets.ARMOR_STAND_WITH_ITEM;
						y += Offsets.ARMOR_STAND_WITH_ITEM_1_9;
					}
					else if (line instanceof TouchableLine) {
						y -= Offsets.ARMOR_STAND_WITH_SLIME;
						y += Offsets.ARMOR_STAND_WITH_SLIME_1_9;
					}
					else {
						throw new UnsupportedOperationException();
					}
					y *= 32;

					event.setCancelled(true);

					PacketContainer updatedPacket = packet.shallowClone();
					updatedPacket.getIntegers().write(2, (int) y);
					ProtocolLibHookImpl.this.sendProcessedPacket(player, updatedPacket);
				}
			});
		return true;
	}

	@Override
	public void sendDestroyEntitiesPacket(Player player, CraftHologram hologram) {
	}

	@Override
	public void sendCreateEntitiesPacket(Player player, CraftHologram hologram) {
	}

	private CraftHologramLine getHologramLine(Entity bukkitEntity) {
		if (bukkitEntity == null) {
			return null;
		}

		NMSEntityBase entity = this.nmsManager.getNMSEntityBase(bukkitEntity);
		if (entity != null) {
			return entity.getHologramLine();
		}

		return null;
	}

	private void sendProcessedPacket(Player player, PacketContainer packet) {
		ProtocolLibHookImpl.this.processedPackets.add(System.identityHashCode(packet.getHandle()));
		try {
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
		} catch (InvocationTargetException exception) {
			exception.printStackTrace();
		}
	}
}
