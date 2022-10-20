package kaktusz.geopolitika.permaloaded.tileentities;

import com.feed_the_beast.ftblib.lib.icon.Color4I;
import kaktusz.geopolitika.blocks.BlockFarm;
import kaktusz.geopolitika.integration.PTEDisplay;
import kaktusz.geopolitika.util.PermissionUtils;
import kaktusz.geopolitika.util.PrecalcSpiral;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;

public class Farm extends ExclusiveZoneTE implements LabourConsumer, DisplayablePTE {
	public static final int ID = 1000;
	public static final int CHUNK_RADIUS = 1;
	private static final int BORDER_WIDTH = 1;
	private static final int REACH_UP = 1;
	private static final int REACH_DOWN = 2;
	private static final Color4I WORK_RADIUS_COLOUR = Color4I.rgba(58, 125, 31, 222);
	private static final short WORK_RADIUS_FILL_OPACITY = 80;

	private final PrecalcSpiral spiral;
	private int spiralIdx = 0;
	private IPlantable plantable = null;
	private int tickCooldown = 0;

	private PrecalcSpiral labourSpiral;
	private double labourReceived = 0;

	public Farm(BlockPos position) {
		super(position);
		int diameter = CHUNK_RADIUS*2 + 1;
		spiral = new PrecalcSpiral(diameter*diameter, new ChunkPos(position));
	}

	public boolean setPlantable(Item source) {
		return setPlantable(source, true);
	}
	private boolean setPlantable(Item source, boolean allowMarkDirty) {
		if(isPlantable(source)) {
			plantable = (IPlantable) source;
			if(allowMarkDirty)
				markDirty();
			return true;
		}
		plantable = null;
		if(allowMarkDirty)
			markDirty();
		return false;
	}

	public static boolean isPlantable(Item source) {
		return source instanceof IPlantable;
	}

	@Override
	public int getID() {
		return ID;
	}

	@Override
	public int getRadius() {
		return CHUNK_RADIUS;
	}

	@Override
	public boolean verify() {
		return getWorld().getBlockState(getPosition()).getBlock() instanceof BlockFarm;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if(nbt.hasKey("plant")) {
			String name = nbt.getString("plant");
			Item item = Item.getByNameOrId(name);
			if(item != null) {
				setPlantable(item, false);
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		if(plantable != null) {
			//noinspection ConstantConditions
			compound.setString("plant", ((Item)plantable).getRegistryName().toString());
		}

		return compound;
	}

	@Override
	public void onTick() {
		if (plantable == null)
			return;

		if (getLabourReceived() < getLabourPerTick()) { //insufficient labour
			if(getWorld().isBlockLoaded(getPosition(), false)) {
				spawnLabourNotReceivedParticles();
			}
			return;
		}

		if(tickCooldown != 0) {
			tickCooldown--;
			return;
		}
		tickCooldown = 20;

		long startTime = System.nanoTime();
		tillChunk(spiral.positions[spiralIdx]);
		spiralIdx = (spiralIdx+1) % spiral.length;
		long elapsed = System.nanoTime() - startTime;
	}

	private void tillChunk(ChunkPos chunk) {
		World world = getWorld();
		if(!world.isBlockLoaded(chunk.getBlock(0,0,0), false)) {
			return; //chunk not loaded
		}

		boolean doFarmland = Blocks.FARMLAND.canSustainPlant(Blocks.FARMLAND.getDefaultState(), world, getPosition(), EnumFacing.DOWN, plantable);
		ChunkPos centreChunk = spiral.positions[0];
		int deltaX = chunk.x - centreChunk.x;
		int deltaZ = chunk.z - centreChunk.z;

		int minX = 0, maxX = 16, minZ = 0, maxZ = 16;
		if(deltaX == CHUNK_RADIUS)
			maxX -= BORDER_WIDTH;
		if(deltaX == -CHUNK_RADIUS)
			minX += BORDER_WIDTH;
		if(deltaZ == CHUNK_RADIUS)
			maxZ -= BORDER_WIDTH;
		if(deltaZ == -CHUNK_RADIUS)
			minZ += BORDER_WIDTH;

		int maxY = getPosition().getY() + REACH_UP;
		int minY = getPosition().getY() - REACH_DOWN;
		if(minY < 0) minY = 0;
		for (int x = minX; x < maxX; x++) {
			for (int z = minZ; z < maxZ; z++) {
				for (int y = maxY; y >= minY; y--) {
					BlockPos pos = chunk.getBlock(x, y, z);
					if(world.isAirBlock(pos)) {
						continue;
					}
					BlockPos plantPos = pos.up();
					if(!world.getBlockState(plantPos).getBlock().isReplaceable(world, plantPos)) { //can't place crop on top of this block
						break;
					}

					IBlockState state = world.getBlockState(pos);
					Block block = state.getBlock();
					if(block.isReplaceable(world, pos)) {
						if(state.getMaterial().isLiquid()) {
							break;
						}
						if(!PermissionUtils.canBreakBlock(pos, world))
							break;
						world.setBlockToAir(pos);
						continue;
					}

					if(doFarmland && (block == Blocks.GRASS || block == Blocks.DIRT)) {
						if(x % 8 == 4 && z % 8 == 4) {
							if(!PermissionUtils.canBreakBlock(pos, world) || !PermissionUtils.canPlaceBlock(pos, world, EnumFacing.DOWN))
								break;
							world.setBlockState(pos, Blocks.WATER.getDefaultState(), 2);
							break;
						} else {
							state = Blocks.FARMLAND.getDefaultState();
							block = state.getBlock();
							if(!PermissionUtils.canBreakBlock(pos, world) || !PermissionUtils.canPlaceBlock(pos, world, EnumFacing.DOWN))
								break;
							world.setBlockState(pos, state, 2);
						}
					}

					if(block.canSustainPlant(state, world, pos, EnumFacing.UP, plantable)) { //try plant a plant
						if(!PermissionUtils.canPlaceBlock(pos, world, EnumFacing.DOWN))
							break;
						world.setBlockState(plantPos, plantable.getPlant(world, plantPos), 2);
					}
					break;
				}
			}
		}
	}

	public double getLabourPerTick() {
		return plantable == null ? 0 : 3.0D;
	}

	@Override
	public int getLabourTier() {
		return 1;
	}

	@Override
	public double getLabourReceived() {
		return labourReceived;
	}

	@Override
	public void addLabourReceived(double amount) {
		labourReceived += amount;
	}

	@Override
	public int getSearchRadius() {
		return 3 + CHUNK_RADIUS;
	}

	@Override
	public PrecalcSpiral getCachedSpiral() {
		return labourSpiral;
	}

	@Override
	public PrecalcSpiral setCachedSpiral(PrecalcSpiral spiral) {
		return labourSpiral = spiral;
	}

	@Override
	public PermaloadedTileEntity getPermaTileEntity() {
		return this;
	}

	@Override
	public PTEDisplay getDisplay() {
		Item iconItem = plantable == null ? Items.WHEAT : ((Item) plantable);
		PTEDisplay disp = createBasicPTEDisplay(new ItemStack(iconItem), "Farm");

		disp.addRadiusHighlight(getSearchRadius());
		disp.addRadiusHighlight(getRadius(), WORK_RADIUS_COLOUR.rgba(), WORK_RADIUS_FILL_OPACITY);

		if(plantable == null) {
			disp.tint = 0x55000000;
			disp.zOrder = 1;
		}

		return disp;
	}
}
