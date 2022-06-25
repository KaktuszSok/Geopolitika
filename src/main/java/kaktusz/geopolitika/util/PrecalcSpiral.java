package kaktusz.geopolitika.util;

import net.minecraft.util.math.ChunkPos;

public class PrecalcSpiral {

	public final int length;
	public final ChunkPos[] positions;

	//adapted from https://stackoverflow.com/a/3706260
	public PrecalcSpiral(int length, ChunkPos centre) {
		this.length = length;
		positions = new ChunkPos[length];

		int dx = 1, dz = 0;
		int segmentLength = 1;

		int x = 0, z = 0;
		int segmentPassed = 0;
		for (int i = 0; i < length; i++) {
			positions[i] = new ChunkPos(centre.x + x, centre.z + z);

			x += dx;
			z += dz;
			segmentPassed++;
			if(segmentPassed == segmentLength) {
				segmentPassed = 0;
				int temp = dx;
				dx = -dz;
				dz = temp;

				if(dz == 0) {
					segmentLength++;
				}
			}
		}
	}
}
