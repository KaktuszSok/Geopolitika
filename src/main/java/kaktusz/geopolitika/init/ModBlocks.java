package kaktusz.geopolitika.init;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.blocks.*;
import kaktusz.geopolitika.permaloaded.tileentities.CreativeLabourSupplier;
import kaktusz.geopolitika.permaloaded.tileentities.House;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

import java.util.HashSet;
import java.util.Set;

import static kaktusz.geopolitika.permaloaded.PermaloadedSavedData.registerPTEBlock;

public class ModBlocks {
	public static final Set<Block> BLOCKS = new HashSet<>();
	public static final Set<Runnable> BLOCK_REGISTER_CALLBACKS = new HashSet<>();

	public static final BlockControlPoint CONTROL_POINT = new BlockControlPoint("control_point", (new Material(MapColor.IRON)), Geopolitika.CREATIVE_TAB);

	public static final BlockFarm FARM = registerPTEBlock(new BlockFarm("farm",Material.WOOD, Geopolitika.CREATIVE_TAB));
	public static final BlockMine MINE = registerPTEBlock(new BlockMine("mine", Material.ROCK, Geopolitika.CREATIVE_TAB));

	public static final BlockPermaBase<House> HOUSE = registerPTEBlock(new BlockBuilding<>("house", Material.WOOD, Geopolitika.CREATIVE_TAB, House::new));
	public static final BlockPermaBase<CreativeLabourSupplier> CREATIVE_LABOUR_SUPPLIER = registerPTEBlock(new BlockCreativeLabourSupplier("creative_labour_supplier", Material.PORTAL, Geopolitika.CREATIVE_TAB));

	public static final BlockCollector RESOURCE_COLLECTOR = registerPTEBlock(new BlockCollector("resource_collector", Material.IRON, Geopolitika.CREATIVE_TAB));

	public static final BlockVehicleWorkshop VEHICLE_WORKSHOP = new BlockVehicleWorkshop("vehicle_workshop", Material.IRON, Geopolitika.CREATIVE_TAB,
			2, 3, 7);

}
