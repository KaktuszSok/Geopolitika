package kaktusz.geopolitika.init;

import kaktusz.geopolitika.Geopolitika;
import net.minecraftforge.common.config.Config;

@Config(modid = Geopolitika.MODID)
public class ModConfig {
	@Config.Comment({"How many chunks do control points claim in each direction?",
					 "The area of chunks claimed is (2N+1)x(2N+1). e.g. 1x1 for N=0, 3x3 for N=1, 9x9 for N=4.",
					 "Only applies to newly placed control points."})
	@Config.RangeInt(min=0, max=32)
	public static byte controlPointClaimRadius = 4;
}
