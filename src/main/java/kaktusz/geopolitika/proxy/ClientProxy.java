package kaktusz.geopolitika.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;

import java.util.Objects;

public class ClientProxy extends CommonProxy {
	public void registerItemRenderer(Item item, int meta, String id)
	{
		Objects.requireNonNull(item.getRegistryName());
		ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(item.getRegistryName(), id));
	}

	@Override
	public World getClientWorld() {
		return Minecraft.getMinecraft().world;
	}
}
