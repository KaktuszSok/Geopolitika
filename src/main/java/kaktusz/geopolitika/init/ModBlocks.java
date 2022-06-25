package kaktusz.geopolitika.init;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.blocks.BlockControlPoint;
import kaktusz.geopolitika.blocks.BlockFarm;
import kaktusz.geopolitika.blocks.BlockMine;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

import java.util.HashSet;
import java.util.Set;

public class ModBlocks {
	public static final Set<Block> BLOCKS = new HashSet<>();
	public static final Set<Runnable> BLOCK_REGISTER_CALLBACKS = new HashSet<>();

	public static final BlockControlPoint CONTROL_POINT = new BlockControlPoint("control_point", (new Material(MapColor.IRON)), Geopolitika.CREATIVE_TAB);
	public static final BlockFarm FARM = new BlockFarm("farm", Material.WOOD, Geopolitika.CREATIVE_TAB);
	public static final BlockMine MINE = new BlockMine("mine", Material.ROCK, Geopolitika.CREATIVE_TAB);

}
