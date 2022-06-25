package kaktusz.geopolitika.permaloaded.projectiles;

import kaktusz.geopolitika.util.ParticleUtils;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

public class ShellProjectile extends VirtualProjectile {

	public float explosionStrength = 4f;
	public boolean damageTerrain = true;
	private double trailOffset = 0d;

	public ShellProjectile(WorldServer world, Vec3d position, Vec3d velocity, float explosionStrength) {
		this(world, position, velocity);
		this.explosionStrength = explosionStrength;
	}
	public ShellProjectile(WorldServer world, Vec3d position, Vec3d velocity) {
		super(world, position, velocity);
	}

	@Override
	public void drawTrail(Vec3d from, Vec3d to) {
		double distance = from.distanceTo(to);
		Vec3d dir = to.subtract(from).normalize();
		for (; trailOffset < distance; trailOffset += 0.5f) {
			Vec3d curr = from.add(dir.scale(trailOffset));
			ParticleUtils.spawnParticleForAll(getWorld(),
					EnumParticleTypes.SMOKE_LARGE,
					true,
					curr.x, curr.y, curr.z,
					1,
					0, 0, 0,
					0);
		}
		trailOffset -= distance;
	}

	@Override
	public boolean onHitEntity(RayTraceResult trace) {
		return true;
	}

	@Override
	public boolean onHitBlock(RayTraceResult trace) {
		return true;
	}

	@Override
	public void destroy() {
		Vec3d pos = getPosition();
		getWorld().createExplosion(null, pos.x, pos.y, pos.z, explosionStrength, damageTerrain);
		super.destroy();
	}
}
