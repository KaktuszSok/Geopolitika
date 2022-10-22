package kaktusz.geopolitika.util;

import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.Universe;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.states.StatesManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PermissionUtils {
	public static boolean canBreakBlock(BlockPos pos, World world, EntityPlayerMP player) {

		ForgeTeam state = StatesManager.getChunkOwner(pos, world);
		if(state.isValid() && state.isMember(Universe.get().getPlayer(player.getUniqueID())))
			return true;

		BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(world, pos, world.getBlockState(pos), player);
		MinecraftForge.EVENT_BUS.post(breakEvent);
		return !breakEvent.isCanceled();
	}

	public static boolean canPlaceBlock(BlockPos pos, World world, EnumFacing placedAgainstDir, EntityPlayerMP player) {

		ForgeTeam state = StatesManager.getChunkOwner(pos, world);
		if(state.isValid() && state.isMember(Universe.get().getPlayer(player.getUniqueID())))
			return true;

		BlockEvent.EntityPlaceEvent placeEvent = new BlockEvent.EntityPlaceEvent(
				new BlockSnapshot(world, pos, world.getBlockState(pos)), //am I giving the right argument for state? no clue. probably not.
				world.getBlockState(pos.add(placedAgainstDir.getDirectionVec())),
				player);
		MinecraftForge.EVENT_BUS.post(placeEvent);
		return !placeEvent.isCanceled();
	}

	public static FakePlayer getFakePlayer(World world) {
		return FakePlayerFactory.getMinecraft((WorldServer)world);
	}

	/**
	 * Gets a fake player representing the owner of the territory at the given block position.
	 * If no owner is found for the block's chunk, gives a generic fake player.
	 */
	@Nonnull
	public static FakePlayer getFakePlayerFromOwner(World world, BlockPos pos) {
		ForgeTeam state = StatesManager.getChunkOwner(pos, world);
		ForgePlayer owner = state.getOwner();
		if(owner == null) {
			owner = state.getMembers().size() > 0 ? state.getMembers().get(0) : null;
		}

		FakePlayer permsPlayer = null;
		if(owner != null) {
			permsPlayer = FakePlayerFactory.get((WorldServer) world, owner.getProfile());
		}
		if(permsPlayer == null) {
			permsPlayer = PermissionUtils.getFakePlayer(world);
		}
		return permsPlayer;
	}
}
