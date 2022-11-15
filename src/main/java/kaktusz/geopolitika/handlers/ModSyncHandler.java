package kaktusz.geopolitika.handlers;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.integration.MinimapIntegrationHelper;
import kaktusz.geopolitika.networking.ChunksSavedDataSyncPacket;
import kaktusz.geopolitika.networking.PTEDisplaysSyncPacket;
import kaktusz.geopolitika.states.ChunksSavedData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber
public class ModSyncHandler {
	public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Geopolitika.MODID);
	private static int highestPacketId = 0;

	private static final Set<World> unsyncedStatesDataWorlds = new HashSet<>();
	private static int ticksUntilResendPTEDisplays = 200;

	public static void init(FMLInitializationEvent e) {
		INSTANCE.registerMessage(
				ChunksSavedDataSyncPacket.ChunksSavedDataSyncHandler.class,
				ChunksSavedDataSyncPacket.class,
				highestPacketId++,
				Side.CLIENT
		);
		INSTANCE.registerMessage(
				PTEDisplaysSyncPacket.PTEDisplaysSyncHandler.class,
				PTEDisplaysSyncPacket.class,
				highestPacketId++,
				Side.CLIENT
		);
	}

	public static void onChunksDataMarkedDirty(ChunksSavedData data) {
		unsyncedStatesDataWorlds.add(data.getWorld());
	}

	@SubscribeEvent
	public static void onWorldTick(TickEvent.WorldTickEvent e) {
		if(e.side != Side.SERVER || e.phase != TickEvent.Phase.END)
			return;

		if(unsyncedStatesDataWorlds.contains(e.world)) {
			unsyncedStatesDataWorlds.remove(e.world);
			ChunksSavedDataSyncPacket syncPacket = new ChunksSavedDataSyncPacket(ChunksSavedData.get(e.world));
			INSTANCE.sendToDimension(syncPacket, e.world.provider.getDimension());
		}

		if(ticksUntilResendPTEDisplays == 0) {
			List<EntityPlayerMP> players = e.world.getPlayers(EntityPlayerMP.class, p -> true);
			for (EntityPlayerMP player : players) {
				PTEDisplaysSyncPacket packet = new PTEDisplaysSyncPacket(player.getPosition(), player.world);
				INSTANCE.sendTo(packet, player);
			}
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent e) {
		if(e.side != Side.SERVER || e.phase != TickEvent.Phase.END)
			return;

		if(ticksUntilResendPTEDisplays > 0) {
			ticksUntilResendPTEDisplays--;
		} else {
			ticksUntilResendPTEDisplays = 200;
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
		ChunksSavedDataSyncPacket syncPacket = new ChunksSavedDataSyncPacket(ChunksSavedData.get(e.player.world));
		INSTANCE.sendTo(syncPacket, (EntityPlayerMP) e.player);

		PTEDisplaysSyncPacket packet = new PTEDisplaysSyncPacket(e.player.getPosition(), e.player.world);
		INSTANCE.sendTo(packet, (EntityPlayerMP) e.player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
		ChunksSavedDataSyncPacket syncPacket = new ChunksSavedDataSyncPacket(ChunksSavedData.get(e.player.world));
		INSTANCE.sendTo(syncPacket, (EntityPlayerMP) e.player);

		PTEDisplaysSyncPacket packet = new PTEDisplaysSyncPacket(e.player.getPosition(), e.player.world);
		INSTANCE.sendTo(packet, (EntityPlayerMP) e.player);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent e) {
		ChunksSavedDataSyncPacket syncPacket = new ChunksSavedDataSyncPacket(ChunksSavedData.get(e.player.world));
		INSTANCE.sendTo(syncPacket, (EntityPlayerMP) e.player);

		PTEDisplaysSyncPacket packet = new PTEDisplaysSyncPacket(e.player.getPosition(), e.player.world);
		INSTANCE.sendTo(packet, (EntityPlayerMP) e.player);
	}

	@SubscribeEvent
	public static void onWorldLoad(WorldEvent.Load e) {
		if(e.getWorld().isRemote) { //client-side
			MinimapIntegrationHelper.clearCachedPTEDisplays();
		}
	}

	@SubscribeEvent
	public static void onWorldUnload(WorldEvent.Unload e) {
		if(e.getWorld().isRemote) { //client-side
			MinimapIntegrationHelper.clearCachedPTEDisplays();
		}
	}
}
