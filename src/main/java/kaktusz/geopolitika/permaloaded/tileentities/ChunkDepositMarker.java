package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.integration.PTEDisplay;
import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import kaktusz.geopolitika.util.MessageUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ChunkDepositMarker extends PermaloadedTileEntity implements DisplayablePTE {
	public static final int ID = 998;

	public static class ResourcePreset {
		public final String name;
		private final NavigableMap<Integer, ItemStack> resourcesMap = new TreeMap<>();
		private int total = 0;
		public ItemStack representative = null;
		private int highestWeight = -1;

		public ResourcePreset(String name) {
			this.name = name;
		}

		public ResourcePreset add(Block resource, int weight) {
			return add(new ItemStack(resource, 1), weight);
		}
		public ResourcePreset add(Item resource, int weight) {
			return add(new ItemStack(resource, 1), weight);
		}
		public ResourcePreset add(ItemStack resource, int weight) {
			if(weight <= 0)
				return this;
			total += weight;
			resourcesMap.put(total, resource);

			if(weight > highestWeight) {
				representative = resource;
				highestWeight = weight;
			}
			return this;
		}

		public ItemStack getRandomResource() {
			int value = ThreadLocalRandom.current().nextInt(total);
			return resourcesMap.higherEntry(value).getValue().copy();
		}

		@Override
		public String toString() {
			return "ResourcePreset{" +
					"name='" + name + '\'' +
					", resourcesMap=" + resourcesMap +
					", total=" + total +
					", representative=" + representative +
					", highestWeight=" + highestWeight +
					'}';
		}
	}

	private static final NavigableMap<Integer, ResourcePreset> RESOURCE_PRESETS = new TreeMap<>();
	private static int presetsWeightTotal = 0;

	public static void addVanillaOres() {
		addResourcePreset(75, new ResourcePreset("Iron/Coal")
				.add(Blocks.IRON_ORE, 20)
				.add(Blocks.COAL_ORE, 10)
		);
		addResourcePreset(150, new ResourcePreset("Coal")
				.add(Blocks.COAL_ORE, 20)
				.add(Blocks.IRON_ORE, 5)
				.add(Blocks.GRAVEL, 5)
		);
		addResourcePreset(20, new ResourcePreset("Diamond")
				.add(Blocks.DIAMOND_ORE, 20)
				.add(Blocks.EMERALD_ORE, 5)
				.add(Blocks.LAPIS_ORE, 15)
		);
		addResourcePreset(40, new ResourcePreset("Gold")
				.add(Blocks.GOLD_ORE, 20)
				.add(Blocks.REDSTONE_ORE, 7)
		);
	}

	public static void addResourcePreset(int weight, ResourcePreset preset) {
		presetsWeightTotal += weight;
		RESOURCE_PRESETS.put(presetsWeightTotal, preset);
	}

	@Nullable
	public static ChunkDepositMarker getDepositAt(ChunkPos chunk, PermaloadedSavedData save) {
		BlockPos blockPos = chunk.getBlock(7, -ChunkDepositMarker.ID, 7);
		PermaloadedTileEntity pte = save.getTileEntityAt(blockPos);
		if(pte instanceof ChunkDepositMarker)
			return (ChunkDepositMarker) pte;
		return null;
	}

	private int random = 0;
	public ResourcePreset resources;

	/**
	 * @param position This MUST be 7,-{@value ID},7 in its chunk-relative coordinates!
	 */
	public ChunkDepositMarker(BlockPos position) {
		super(position);
	}

	/**
	 * For marking a chunk as an exclusive zone.
	 */
	public ChunkDepositMarker(ChunkPos chunkPos) {
		this(chunkPos.getBlock(7,-ID,7));
	}

	@Override
	public int getID() {
		return ID;
	}

	@Override
	public boolean verify() {
		return true;
	}

	@Override
	public boolean persistent() {
		return getSave().hasAnyTileEntitiesOfType(Mine.class, new ChunkPos(getPosition()), Mine.MINING_RADIUS); //only persist resources within a mine's radius
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setInteger("rand", random);
		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		random = nbt.getInteger("rand");
		resources = RESOURCE_PRESETS.higherEntry(random).getValue();
	}

	/**
	 * Initialises the resource weights to that of a randomly selected preset
	 */
	public void initialise(Random rng) {
		random = rng.nextInt(presetsWeightTotal);
		resources = RESOURCE_PRESETS.higherEntry(random).getValue();
	}

	public ItemStack getRandomResource() {
		if(resources == null)
			return ItemStack.EMPTY;
		return resources.getRandomResource();
	}

	@Nullable
	@Override
	public PTEDisplay getDisplay() {
		if(resources == null) {
			PTEDisplay display = new PTEDisplay(new ItemStack(Blocks.BARRIER));
			display.hoverText = "INVALID RESOURCE DEPOSIT";
			return display;
		}

		if(resources.representative == null) {
			PTEDisplay display = new PTEDisplay(new ItemStack(Blocks.BARRIER));
			display.hoverText = "INVALID RESOURCE DEPOSIT";
			return display;
		}

		PTEDisplay display = new PTEDisplay(resources.representative);
		display.hoverText = resources.name + " Deposit";
		StringBuilder sb = new StringBuilder();
		int total = 0;
		for (Map.Entry<Integer, ItemStack> entry : resources.resourcesMap.entrySet()) {
			int percentage = (int)Math.round(100.0*((entry.getKey() - total) / (double)resources.total));
			total = entry.getKey();
			sb.append("\n - ").append(percentage).append("% ").append(entry.getValue().getDisplayName());
		}
		display.hoverText += sb.toString();
		ITextComponent subtext; //TODO translation?
		if(persistent()) {
			subtext = new TextComponentString("Resources are being mined from this deposit.");
		} else {
			subtext = new TextComponentString("Place a Mine nearby to collect resources from this deposit.");
		}
		subtext.setStyle(MessageUtils.DARK_GREY_STYLE);
		display.hoverText += "\n" + subtext.getFormattedText();
		return display;
	}

	@Override
	public PermaloadedTileEntity getPermaTileEntity() {
		return this;
	}
}
