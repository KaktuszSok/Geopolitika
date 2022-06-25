package kaktusz.geopolitika.permaloaded.projectiles;

import kaktusz.geopolitika.util.NoPossibleSolutionsException;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

public class AimHelper {

	@Nullable
	public static Vec3d calculateVelocity(Vec3d start, Vec3d target, double projectileSpeed, double gravity, boolean directFire) throws NoPossibleSolutionsException {
		Vec3d delta = target.subtract(start);
		double dh = Math.sqrt(delta.x*delta.x + delta.z*delta.z);

		//https://physics.stackexchange.com/a/70480
		double v2 = projectileSpeed*projectileSpeed;
		double w = (v2*(v2 - 2*gravity*delta.y)/(gravity*gravity*dh*dh)) - 1;
		if(w < 0)
			throw new NoPossibleSolutionsException("No possible trajectory from " + start + " to " + target + " exists for the specified gravity and speed.");
		double tanAngle = (v2/(gravity*dh)) +
				Math.sqrt(w) * (directFire ? -1 : 1);
		double angle = Math.atan(tanAngle);
		double velY = Math.sin(angle)*projectileSpeed;
		double velH = Math.cos(angle)*projectileSpeed;
		double yAngle = Math.atan2(delta.z, delta.x);
		return new Vec3d(Math.cos(yAngle)*velH, velY, Math.sin(yAngle)*velH);
	}
}
