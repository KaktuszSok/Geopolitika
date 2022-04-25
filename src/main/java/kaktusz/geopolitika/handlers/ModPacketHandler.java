package kaktusz.geopolitika.handlers;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.networking.ChunksSavedDataSyncPacket;
import kaktusz.geopolitika.states.ChunksSavedData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber
public class ModPacketHandler {
	public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Geopolitika.MODID);
	private static int highestPacketId = 0;

	private static final Set<World> unsyncedStatesDataWorlds = new HashSet<>();

	public static void init(FMLInitializationEvent e) {
//		if(e.getSide().isClient()) {
//			INSTANCE.registerMessage(
//					ChunksSavedDataSyncPacket.ChunksSavedDataSyncHandler.class,
//					ChunksSavedDataSyncPacket.class,
//					highestPacketId++,
//					Side.CLIENT
//			);
//		}
		INSTANCE.registerMessage(
				ChunksSavedDataSyncPacket.ChunksSavedDataSyncHandler.class,
				ChunksSavedDataSyncPacket.class,
				highestPacketId++,
				Side.CLIENT
		);
	}

	public static void onChunksDataMarkedDirty(ChunksSavedData data) {
		unsyncedStatesDataWorlds.add(data.getWorld());
	}

	@SubscribeEvent
	public static void onWorldTick(TickEvent.WorldTickEvent e) {
		if(e.side != Side.SERVER)
			return;

		if(unsyncedStatesDataWorlds.contains(e.world)) {
			unsyncedStatesDataWorlds.remove(e.world);
			ChunksSavedDataSyncPacket syncPacket = new ChunksSavedDataSyncPacket(ChunksSavedData.get(e.world));
			INSTANCE.sendToDimension(syncPacket, e.world.provider.getDimension());
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
		ChunksSavedDataSyncPacket syncPacket = new ChunksSavedDataSyncPacket(ChunksSavedData.get(e.player.world));
		INSTANCE.sendTo(syncPacket, (EntityPlayerMP) e.player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
		ChunksSavedDataSyncPacket syncPacket = new ChunksSavedDataSyncPacket(ChunksSavedData.get(e.player.world));
		INSTANCE.sendTo(syncPacket, (EntityPlayerMP) e.player);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent e) {
		ChunksSavedDataSyncPacket syncPacket = new ChunksSavedDataSyncPacket(ChunksSavedData.get(e.player.world));
		INSTANCE.sendTo(syncPacket, (EntityPlayerMP) e.player);
	}
}
