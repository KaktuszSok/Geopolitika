package kaktusz.geopolitika.permaloaded.projectiles;

import com.google.common.base.Predicates;
import kaktusz.geopolitika.util.WorldUtils;
import net.minecraft.entity.Entity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public abstract class VirtualProjectile {
	@SuppressWarnings({"Guava", "unchecked"})
	private static final com.google.common.base.Predicate<Entity> PROJECTILE_TARGETS = Predicates.and(
			EntitySelectors.NOT_SPECTATING,
			EntitySelectors.IS_ALIVE,
			Entity::canBeCollidedWith
	);
	private static final boolean ALLOW_LOADING_CHUNKS = true;

	private final WorldServer world;
	private Vec3d position;
	private Vec3d velocity;
	private double gravity = 0.05d;
	protected int lifetime = 1000;
	private int ticksInAir = 0;
	private Entity shootingEntity = null;
	private final Set<Consumer<VirtualProjectile>> destroyedCallbacks = new HashSet<>();
	private boolean destroyed = false;

	public VirtualProjectile(WorldServer world, Vec3d position, Vec3d velocity) {
		this.world = world;
		this.position = position;
		this.velocity = velocity;
	}

	public Vec3d getPosition() {
		return position;
	}

	public Vec3d getVelocity() {
		return velocity;
	}

	public WorldServer getWorld() {
		return world;
	}

	public void setGravity(double gravity) {
		this.gravity = gravity;
	}

	public double getGravity() {
		return gravity;
	}

	public void setShootingEntity(Entity shootingEntity) {
		this.shootingEntity = shootingEntity;
	}

	@Nullable
	public Entity getShootingEntity() {
		return shootingEntity;
	}

	public void addDestroyedCallback(Consumer<VirtualProjectile> callback) {
		destroyedCallbacks.add(callback);
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	public void tick() {
		if(position.y < -64) {
			lifetime = 0;
			return;
		}
		lifetime--;

		Vec3d rayEnd = position.add(velocity);
		RayTraceResult trace = ALLOW_LOADING_CHUNKS ?
				world.rayTraceBlocks(position, rayEnd, false, true, false) :
				WorldUtils.rayTraceBlocksDontLoad(world, position, rayEnd, false, true, false);
		if(trace != null) {
			rayEnd = trace.hitVec;
		}
		RayTraceResult entityTrace = findEntityOnPath(position, rayEnd);
		if(entityTrace != null) {
			if(onHitEntity(entityTrace)) {
				move(entityTrace.hitVec);
				destroy();
				return;
			}
		}
		if(trace != null && onHitBlock(trace)) {
			move(trace.hitVec);
			destroy();
			return;
		}

		move(position.add(velocity));
		velocity = velocity.add(0, -gravity, 0);
		ticksInAir++;
	}

	protected void move(Vec3d to) {
		drawTrail(position, to);
		position = to;
	}

	public void drawTrail(Vec3d from, Vec3d to) {

	}

	/**
	 * @return True if the projectile should be destroyed, false otherwise
	 */
	public abstract boolean onHitEntity(RayTraceResult trace);

	/**
	 * @return True if the projectile should be destroyed, false otherwise
	 */
	public abstract boolean onHitBlock(RayTraceResult trace);

	public void destroy() {
		destroyed = true;
		for (Consumer<VirtualProjectile> destroyedCallback : destroyedCallbacks) {
			destroyedCallback.accept(this);
		}
	}

	@Nullable
	protected RayTraceResult findEntityOnPath(Vec3d start, Vec3d end)
	{
		AxisAlignedBB aabb = new AxisAlignedBB(start, end).grow(1.0d);
		RayTraceResult result = null;
		List<Entity> list = this.world.getEntitiesInAABBexcluding(ticksInAir < 5 ? shootingEntity : null, aabb, PROJECTILE_TARGETS);
		double d0 = 0.0D;

		for (int i = 0; i < list.size(); ++i)
		{
			Entity entity1 = list.get(i);

			AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(0.1);
			RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(start, end);

			if (raytraceresult != null)
			{
				double d1 = start.squareDistanceTo(raytraceresult.hitVec);

				if (d1 < d0 || d0 == 0.0D)
				{
					result = raytraceresult;
					d0 = d1;
				}
			}
		}

		return result;
	}
}
