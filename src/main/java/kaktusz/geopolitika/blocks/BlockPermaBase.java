package kaktusz.geopolitika.blocks;

import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import kaktusz.geopolitika.permaloaded.tileentities.PermaloadedTileEntity;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BlockPermaBase<T extends PermaloadedTileEntity> extends BlockBase {

	public BlockPermaBase(String name, Material material, CreativeTabs tab) {
		super(name, material, tab);
	}

	/**
	 * Registers the permaloaded tile entity of type T with the permaload entity factory.
	 */
	public void registerPermaloadedTE() {
		T temp = createPermaTE(new BlockPos(0,0,0));
		PermaloadedSavedData.entityFactory.put(temp.getID(), this::createPermaTE);
	}

	protected abstract T createPermaTE(BlockPos pos);

	@Nullable
	public T getPermaTileEntity(World world, BlockPos pos) {
		return PermaloadedSavedData.get(world).getTileEntityAt(pos);
	}

	@Override
	public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
		if(worldIn.isRemote)
			return;

		PermaloadedSavedData save = PermaloadedSavedData.get(worldIn);
		save.addTileEntity(createPermaTE(pos));
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		super.breakBlock(worldIn, pos, state);
		PermaloadedSavedData.get(worldIn).removeTileEntityAt(pos);
	}
}
