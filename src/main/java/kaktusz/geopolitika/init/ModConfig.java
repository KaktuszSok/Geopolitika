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

	@Config.Comment("War score gained every second when unopposed inside the occupied territory.")
	@Config.RangeInt(min=0, max=1000)
	public static int warScoreOccupationGain = 1;
	@Config.Comment("War score gained when right-clicking the occupied territory's control point. May be done once per second.")
	@Config.RangeInt(min=0, max=1000)
	public static int warScoreActiveOccupationGain = 2;
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
	@Config.Comment("How many minutes between when a control point is successfully defended and when the state can be attacked by the occupiers again.")
	@Config.RangeInt(min=0)
	public static int occupationCooldownOnDefend = 120;
	@Config.Comment("How many seconds do defenders have to wait between revealing the location of occupiers by right-clicking a control point?")
	@Config.RangeInt(min=0)
	public static int highlightOccupiersCooldown = 3*60;
	@Config.Comment("Duration of the glow effect given to revealed occupiers.")
	@Config.RangeInt(min=0)
	public static int highlightOccupiersDuration = 15;

	@Config.Comment({
			"The higher this value, the less likely it is that a chunk will generate resources for a mine.",
			"Specifically, the chance for a chunk to contain a deposit is 1/N where N is the rarity specified here."
	})
	@Config.RangeDouble(min=1.0)
	public static double chunkResourcesRarity = 100.0;

	@Config.Comment({"The hourly upkeep cost for a state's claimed chunks."})
	public static int upkeepCostPerChunk = 3;

	@Config.Comment("How many display icons can be kept up on the world map after leaving their vicinity. Reduce if the map screen is lagging.")
	@Config.RangeInt(min=0)
	public static int maxMapDisplaysCached = 1000;
}
