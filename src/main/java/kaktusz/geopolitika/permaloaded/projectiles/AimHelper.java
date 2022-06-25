package kaktusz.geopolitika.permaloaded.projectiles;

import kaktusz.geopolitika.util.MathsUtils;
import kaktusz.geopolitika.util.NoPossibleSolutionsException;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.Random;

public class AimHelper {

	private static Random rng = new Random();

	@Nullable
	public static Vec3d calculateVelocity(Vec3d start, Vec3d target, double projectileSpeed, double gravity, boolean directFire, double inaccuracy) throws NoPossibleSolutionsException {
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
		double yAngle = Math.atan2(delta.z, delta.x);

		double horizontalFactor = Math.cos(angle);
		double normX = Math.cos(yAngle)*horizontalFactor + rng.nextGaussian()*inaccuracy;
		double normY = Math.sin(angle) + rng.nextGaussian()*inaccuracy;
		double normZ = Math.sin(yAngle)*horizontalFactor + rng.nextGaussian()*inaccuracy;
		double magnitude = Math.sqrt(normX*normX + normY*normY + normZ*normZ);
		double factor = projectileSpeed/magnitude;
		return new Vec3d(normX*factor, normY*factor, normZ*factor);
	}
}
