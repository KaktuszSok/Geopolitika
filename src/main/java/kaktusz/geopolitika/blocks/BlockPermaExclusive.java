package kaktusz.geopolitika.blocks;

import kaktusz.geopolitika.permaloaded.ExclusiveZoneTE;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class BlockPermaExclusive<T extends ExclusiveZoneTE> extends BlockPermaBase<T> {
	public BlockPermaExclusive(String name, Material material, CreativeTabs tab) {
		super(name, material, tab);
	}

	@Override
	public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
		T test = createPermaTE(pos);
		return test.canPlaceHere(worldIn);
	}
}
