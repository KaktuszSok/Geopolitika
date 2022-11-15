package kaktusz.geopolitika.permaloaded.tileentities;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import kaktusz.geopolitika.blocks.BlockCollector;
import kaktusz.geopolitika.capabilities.CollectorItemHandler;
import kaktusz.geopolitika.integration.PTEDisplay;
import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.util.MiscUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

//TODO filter functionality
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ResourceCollector extends PermaloadedTileEntity implements DisplayablePTE {
	public static final int ID = 3000;
	public static final int INV_SIZE = 9*6;

	private final CollectorItemHandler inventory = new CollectorItemHandler(INV_SIZE);

	public ResourceCollector(BlockPos position) {
		super(position);
	}

	@Override
	public int getID() {
		return ID;
	}

	@Override
	public boolean verify() {
		return getWorld().getBlockState(getPosition()).getBlock() instanceof BlockCollector;
	}

	public CollectorItemHandler getInventory() {
		return inventory;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setTag("inventory", inventory.serializeNBT());

		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		inventory.deserializeNBT(nbt.getCompoundTag("inventory"));
	}

	@Nullable
	@Override
	public PTEDisplay getDisplay() {
		PTEDisplay display = new PTEDisplay(new ItemStack(Blocks.CHEST));
		display.hoverText = "Resource Collector";
		float fillRatio = 0;
		int totalSlots = inventory.getSlots();
		for (int i = 0; i < totalSlots; i++) {
			int slotMax = inventory.getSlotLimit(i);
			ItemStack stack = inventory.getStackInSlot(i);
			float slotFill = stack.getCount() / (float)Math.min(slotMax, stack.getMaxStackSize());
			fillRatio += slotFill;
		}
		fillRatio /= totalSlots;
		display.hoverText += "\n - Filled " + Math.round(fillRatio*1000)/10.0 + "%";
		return display;
	}

	@Override
	public PermaloadedTileEntity getPermaTileEntity() {
		return this;
	}

	public static ItemStack insertIntoNearby(ItemStack stack, PermaloadedSavedData save, BlockPos where, int chunkRadius, boolean respectState) {
		ChunkPos centre = new ChunkPos(where);

		ForgeTeam state = null;
		if(respectState) {
			state = StatesManager.getChunkOwner(where, save.getWorld());
		}
		List<ResourceCollector> collectors = save.findTileEntitiesByTypeOrInterface(ResourceCollector.class, centre, chunkRadius, state, false);

		while (!stack.isEmpty() && collectors.size() > 0) {
			ResourceCollector chosenCollector = MiscUtils.chooseRandom(collectors);
			collectors.remove(chosenCollector);
			IItemHandler inv = chosenCollector.getInventory();

			int slots = inv.getSlots();

			for (int i = 0; i < slots && !stack.isEmpty(); i++)
			{
				stack = inv.insertItem(i, stack, false);
			}
		}

		return stack;
	}
}
