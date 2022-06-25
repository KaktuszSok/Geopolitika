package kaktusz.geopolitika.handlers;

import com.feed_the_beast.ftblib.events.team.ForgeTeamDeletedEvent;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.math.ChunkDimPos;
import com.feed_the_beast.ftbutilities.data.ClaimedChunk;
import com.feed_the_beast.ftbutilities.data.ClaimedChunks;
import com.feed_the_beast.ftbutilities.events.chunks.ChunkModifiedEvent;
import kaktusz.geopolitika.blocks.BlockControlPoint;
import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import kaktusz.geopolitika.permaloaded.projectiles.ProjectileManager;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.states.ChunksSavedData;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.MessageUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;

/**
 * Event handler dealing with general gameplay logic
 */
@Mod.EventBusSubscriber
public class GameplayEventHandler {
	private static final ITextComponent WILDERNESS_TEXT = new TextComponentString("Wilderness").setStyle(
			new Style().setColor(TextFormatting.WHITE)
	);

	public static final Set<TileEntityControlPoint> loadedControlPoints = new HashSet<>();
	private static final Map<EntityPlayerMP, String> playerLastRegions = new HashMap<>();
	public static final Map<ChunkDimPos, ClaimedChunk> pendingChunkClaims = new HashMap<>();

	@SubscribeEvent
	public static void blockPlaced(BlockEvent.EntityPlaceEvent e) {
		Block block = e.getPlacedBlock().getBlock();
		if(block instanceof BlockControlPoint) {
			if(!((BlockControlPoint)block).handleBlockPlacementEvent(e, true,false)) { //if placement is disallowed, cancel the event
				//e.setCanceled(true);
				e.getWorld().destroyBlock(e.getPos(), !(e.getEntity() instanceof EntityPlayer && ((EntityPlayer)e.getEntity()).isCreative()));
				return;
			}
		}
	}

	@SubscribeEvent
	public static void blockBroken(BlockEvent.BreakEvent e) {
		Block block = e.getState().getBlock();
		if(block instanceof BlockControlPoint) {
			if(!((BlockControlPoint)block).handleBlockBreakEvent(e, true)) { //if placement is disallowed, cancel the event
				e.setCanceled(true);
				return;
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void onChunkChanged(EntityEvent.EnteringChunk event)
	{
		if (event.getEntity().world.isRemote || !(event.getEntity() instanceof EntityPlayerMP) || !Universe.loaded())
		{
			return;
		}

		EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
		ForgePlayer p = Universe.get().getPlayer(player.getGameProfile());

		if (p == null || p.isFake())
		{
			return;
		}

		ITextComponent regionName = StatesManager.getRegionName(
				event.getNewChunkX(), event.getNewChunkZ(),
				event.getEntity().world,
				false
		);
		if(regionName.getUnformattedText().equals(playerLastRegions.get(player))) {
			return;
		}
		playerLastRegions.put(player, regionName.getUnformattedText());

		ForgeTeam owner = StatesManager.getChunkOwner(event.getNewChunkX(), event.getNewChunkZ(), event.getEntity().world);
		regionName.setStyle(new Style()
				.setColor(owner.getColor().getTextFormatting()));

		MessageUtils.setTitleTimings(player, 10, 10, 30);

		if(owner.isValid()) {
			MessageUtils.displaySubtitle(player, regionName);
		} else {
			MessageUtils.displaySubtitle(player, WILDERNESS_TEXT);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onTeamDeleted(ForgeTeamDeletedEvent e) {
		for (TileEntityControlPoint loadedControlPoint : loadedControlPoints) {
			if(!loadedControlPoint.isInvalid() && loadedControlPoint.getOwner().equalsTeam(e.getTeam())) {
				TileEntityControlPoint.OccupationProgress winner = loadedControlPoint.getWinningOccupier();
				if(winner != null) {
					loadedControlPoint.capitulate(winner.getOccupiers().getUID()); //capitulate to state with highest war score
				}
				else {
					loadedControlPoint.setOwner(null); //remove owner and unclaim chunks
				}
			}
		}

		ChunksSavedData savedDataReference = null;
		OptionalInt savedDataReferenceDim = OptionalInt.empty();
		for (ClaimedChunk chunk : ClaimedChunks.instance.getTeamChunks(e.getTeam(), OptionalInt.empty()))
		{
			ChunkDimPos pos = chunk.getPos();

			if (chunk.isLoaded())
			{
				new ChunkModifiedEvent.Unloaded(chunk, e.getTeam().getOwner()).post();
			}

			chunk.setLoaded(false);
			new ChunkModifiedEvent.Unclaimed(chunk, e.getTeam().getOwner()).post();
			ClaimedChunks.instance.removeChunk(pos);

			if(!savedDataReferenceDim.isPresent() || savedDataReferenceDim.getAsInt() != pos.dim) {
				savedDataReference = ChunksSavedData.get(e.getUniverse().server.getWorld(pos.dim));
				savedDataReferenceDim = OptionalInt.of(pos.dim);
			}
			savedDataReference.removeChunkControlPoint(pos.getChunkPos());
		}
	}

	@SubscribeEvent
	public static void onChunkLoaded(ChunkEvent.Load e) {
		if(e.getWorld().isRemote)
			return;

		PermaloadedSavedData.get(e.getWorld()).onChunkLoaded(e.getChunk());
	}

	@SubscribeEvent
	public static void onWorldTick(TickEvent.WorldTickEvent e) {
		if(e.side != Side.SERVER)
			return;

		if(e.phase == TickEvent.Phase.START) {
			PermaloadedSavedData.get(e.world).onWorldTick();
			ProjectileManager.get(e.world).tick();
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onServerTick(TickEvent.ServerTickEvent e) {
		if (e.phase == TickEvent.Phase.START)
		{
			if (ClaimedChunks.isActive())
			{
				for (Map.Entry<ChunkDimPos, ClaimedChunk> pending : pendingChunkClaims.entrySet()) {
					ClaimedChunks.instance.addChunk(pending.getValue());
				}
				pendingChunkClaims.clear();
			}
		}
	}

	@SubscribeEvent
	public static void onWorldUnloaded(WorldEvent.Unload e) {
		if(e.getWorld().isRemote || !(e.getWorld() instanceof WorldServer))
			return;

		ProjectileManager.onWorldUnloaded((WorldServer) e.getWorld());
	}

	@SubscribeEvent
	public static void onChatMessage(ServerChatEvent e) {
		EntityPlayerMP player = e.getPlayer();
		if(player == null)
			return;

		ForgeTeam state = StatesManager.getPlayerState(player);
		if(!state.isValid()) {
			ITextComponent message = new TextComponentString("")
					.appendSibling(player.getDisplayName().createCopy().appendText(": ").setStyle(new Style().setColor(TextFormatting.GRAY)))
					.appendSibling(ForgeHooks.newChatWithLinks(e.getMessage()));
			e.setComponent(message);
			return;
		}

		ITextComponent message = new TextComponentString("")
				.appendSibling(MessageUtils.getStateMessagePrefix(state))
				.appendSibling(player.getDisplayName().createCopy().appendText(": ").setStyle(new Style().setColor(TextFormatting.GRAY)))
				.appendSibling(ForgeHooks.newChatWithLinks(e.getMessage()));
		e.setComponent(message);
	}
}
