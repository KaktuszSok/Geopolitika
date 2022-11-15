package kaktusz.geopolitika.blocks;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.containers.GenericContainer;
import kaktusz.geopolitika.containers.GenericContainerGUI;
import kaktusz.geopolitika.init.ModBlocks;
import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import kaktusz.geopolitika.permaloaded.tileentities.PermaloadedTileEntity;
import kaktusz.geopolitika.permaloaded.tileentities.ResourceCollector;
import kaktusz.geopolitika.tileentities.TileEntityCollector;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.IHasGUI;
import kaktusz.geopolitika.util.InventoryUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlockCollector extends BlockPermaBase<ResourceCollector> implements ITileEntityProvider {
	public BlockCollector(String name, Material material, CreativeTabs tab) {
		super(name, material, tab);
		ModBlocks.BLOCK_REGISTER_CALLBACKS.add(() -> GameRegistry.registerTileEntity(
				TileEntityCollector.class,
				new ResourceLocation(Geopolitika.MODID, "resource_collector")
		));
	}

	@Override
	protected ResourceCollector createPermaTE(BlockPos pos) {
		return new ResourceCollector(pos);
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityCollector();
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(worldIn.isRemote)
			return true;

		PermaloadedTileEntity pte = PermaloadedSavedData.get(worldIn).getTileEntityAt(pos);
		if(!(pte instanceof ResourceCollector))
			return false;

		playerIn.openGui(Geopolitika.INSTANCE, 1, worldIn, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
	{
		PermaloadedTileEntity pte = PermaloadedSavedData.get(worldIn).getTileEntityAt(pos);

		if (pte instanceof ResourceCollector)
		{
			InventoryUtils.dropInventoryItems(worldIn, pos, ((ResourceCollector)pte).getInventory());
			worldIn.updateComparatorOutputLevel(pos, this);
		}

		super.breakBlock(worldIn, pos, state);
	}
}
