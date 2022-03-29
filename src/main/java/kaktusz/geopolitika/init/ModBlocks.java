package kaktusz.geopolitika.init;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.blocks.BlockControlPoint;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

import java.util.HashSet;
import java.util.Set;

public class ModBlocks {
	public static final Set<Block> BLOCKS = new HashSet<>();
	public static final Set<Runnable> BLOCK_REGISTER_CALLBACKS = new HashSet<>();

	public static final BlockControlPoint CONTROL_POINT = new BlockControlPoint("control_point", (new Material(MapColor.IRON)), Geopolitika.CREATIVE_TAB);
}
