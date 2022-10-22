package kaktusz.geopolitika.blocks;

import kaktusz.geopolitika.permaloaded.tileentities.CreativeLabourSupplier;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.math.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlockCreativeLabourSupplier extends BlockPermaBase<CreativeLabourSupplier> {
	public BlockCreativeLabourSupplier(String name, Material material, CreativeTabs tab) {
		super(name, material, tab);
	}

	@Override
	protected CreativeLabourSupplier createPermaTE(BlockPos pos) {
		return new CreativeLabourSupplier(pos);
	}
}
