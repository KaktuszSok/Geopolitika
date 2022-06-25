package kaktusz.geopolitika.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

public class ParticleUtils {
	public static void spawnParticleForAll(WorldServer world, EnumParticleTypes particle, boolean longDistance, double x, double y, double z, int count, double xOffset, double yOffset, double zOffset, double speed, int... params) {
		for (EntityPlayerMP player : world.getPlayers(EntityPlayerMP.class, input -> true)) {
			world.spawnParticle(
					player,
					particle,
					longDistance,
					x, y, z,
					count,
					xOffset, yOffset, zOffset,
					speed);
		}
	}
}
