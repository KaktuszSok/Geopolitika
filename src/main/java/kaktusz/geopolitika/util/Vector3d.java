package kaktusz.geopolitika.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Vector3d {
	public double x, y, z;

	public Vector3d(Vec3i vec3i) {
		this(vec3i.getX(), vec3i.getY(), vec3i.getZ());
	}
	public Vector3d(double xIn, double yIn, double zIn) {
		x = xIn;
		y = yIn;
		z = zIn;
	}

	public void set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void add(Vector3d vec) {
		x += vec.x;
		y += vec.y;
		z += vec.z;
	}

	public void scale(double factor) {
		x *= factor;
		y *= factor;
		z *= factor;
	}

	public BlockPos toBlockPos() {
		return new BlockPos(x, y, z);
	}

	public Vec3d toVec3d() {
		return new Vec3d(x,y,z);
	}

	public static Vector3d sum(Vector3d a, Vector3d b) {
		return new Vector3d(a.x + b.x, a.y + b.y, a.z + b.z);
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + "," + z + ")";
	}
}
