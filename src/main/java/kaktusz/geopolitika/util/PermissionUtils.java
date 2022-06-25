package kaktusz.geopolitika.util;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent;

public class PermissionUtils {
	private static FakePlayer fakePlayerCache = null;

	public static boolean canBreakBlock(BlockPos pos, World world) {
		FakePlayer fakePlayer = getFakePlayer(world);
		BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(world, pos, world.getBlockState(pos), fakePlayer);
		MinecraftForge.EVENT_BUS.post(breakEvent);
		return !breakEvent.isCanceled();
	}

	public static boolean canPlaceBlock(BlockPos pos, World world, EnumFacing placedAgainstDir) {
		FakePlayer fakePlayer = getFakePlayer(world);
		BlockEvent.EntityPlaceEvent placeEvent = new BlockEvent.EntityPlaceEvent(
				new BlockSnapshot(world, pos, world.getBlockState(pos)), //am I giving the right argument for state? no clue. probably not.
				world.getBlockState(pos.add(placedAgainstDir.getDirectionVec())),
				fakePlayer);
		MinecraftForge.EVENT_BUS.post(placeEvent);
		return !placeEvent.isCanceled();
	}

	public static FakePlayer getFakePlayer(World world) {
		if((fakePlayerCache == null || fakePlayerCache.world != world) && !world.isRemote) {
			fakePlayerCache = FakePlayerFactory.getMinecraft((WorldServer)world);
		}

		return fakePlayerCache;
	}
}
