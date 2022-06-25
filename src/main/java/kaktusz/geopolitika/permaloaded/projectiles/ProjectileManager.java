package kaktusz.geopolitika.permaloaded.projectiles;

import kaktusz.geopolitika.Geopolitika;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.*;

public class ProjectileManager {
	private static final Map<WorldServer, ProjectileManager> managers = new HashMap<>();

	private final WorldServer world;
	private final Set<VirtualProjectile> projectiles = new HashSet<>();
	private final Queue<VirtualProjectile> projectilesToAdd = new LinkedList<>();
	private final Queue<VirtualProjectile> projectilesToRemove = new LinkedList<>();

	public ProjectileManager(WorldServer world) {
		this.world = world;
	}

	public static ProjectileManager get(World world) {
		return managers.computeIfAbsent((WorldServer) world, ProjectileManager::new);
	}

	public WorldServer getWorld() {
		return world;
	}

	public void addProjectile(VirtualProjectile projectile) {
		Geopolitika.logger.info("adding projectile " + projectile);
		projectilesToAdd.add(projectile);
		projectile.addDestroyedCallback(this::removeProjectile);
	}

	public void removeProjectile(VirtualProjectile projectile) {
		projectilesToRemove.add(projectile);
	}

	public static void onWorldUnloaded(WorldServer world) {
		managers.remove(world);
	}

	public void tick() {
		Geopolitika.logger.info("add " + projectilesToAdd.size());
		while (!projectilesToAdd.isEmpty()) {
			projectiles.add(projectilesToAdd.remove());
		}

		Geopolitika.logger.info("remove " + projectilesToRemove.size());
		while (!projectilesToRemove.isEmpty()) {
			projectiles.remove(projectilesToRemove.remove());
		}

		Geopolitika.logger.info("have " + projectiles.size());
		Iterator<VirtualProjectile> iter = projectiles.iterator();
		while (iter.hasNext()) {
			VirtualProjectile proj = iter.next();
			if(proj.lifetime <= 0)
				iter.remove();
			proj.tick();
		}
	}
}
