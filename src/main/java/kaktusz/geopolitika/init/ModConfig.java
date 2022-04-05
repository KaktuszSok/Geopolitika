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

	@Config.RangeInt(min=0, max=1000)
	public static int warScoreOccupationGain = 2;
	@Config.RangeInt(min=0, max=1000)
	public static int warScoreDecay = 3;
	@Config.RangeInt(min=0, max=1000)
	public static int warScoreKillGain = 100;
	@Config.RangeInt(min=0, max=1000)
	public static int warScoreDeathLoss = 100;
	@Config.RangeInt(min=0, max=1000)
	public static int warScoreStartingAmount = 100;

	@Config.Comment("How many minutes between when a control point is taken over and when it can be attacked again.")
	@Config.RangeInt(min=0)
	public static int occupationCooldownOnCapitulate = 120;
}
