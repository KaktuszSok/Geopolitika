package kaktusz.geopolitika.blocks;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.init.ModBlocks;
import kaktusz.geopolitika.init.ModItems;
import kaktusz.geopolitika.util.IHasModel;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

public class BlockBase extends Block implements IHasModel {

	public BlockBase(String name, Material material, CreativeTabs tab) {
		super(material);
		setTranslationKey(name);
		setRegistryName(name);
		setCreativeTab(tab);

		ModBlocks.BLOCKS.add(this);
		//noinspection ConstantConditions
		ModItems.ITEMS.add(new ItemBlock(this).setRegistryName(this.getRegistryName()));
	}

	@Override
	public void registerModels() {
		Geopolitika.PROXY.registerItemRenderer(Item.getItemFromBlock(this), 0, "inventory");
	}
}
