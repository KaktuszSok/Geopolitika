package kaktusz.geopolitika.handlers;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.client.rendering.RenderCustomVehicle;
import kaktusz.geopolitika.entities.EntityCustomVehicle;
import kaktusz.geopolitika.entities.SolidEntityPart;
import kaktusz.geopolitika.init.ModBlocks;
import kaktusz.geopolitika.init.ModItems;
import kaktusz.geopolitika.util.IHasModel;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.EntityRegistry;

@Mod.EventBusSubscriber
public class RegistryHandler {

	private static int entityId = 0;

	public static void init(FMLInitializationEvent e) {
		NetworkRegistry.INSTANCE.registerGuiHandler(Geopolitika.INSTANCE, new GuiProxy());
	}

	@SubscribeEvent
	public static void onItemRegister(RegistryEvent.Register<Item> event) {
		event.getRegistry().registerAll(ModItems.ITEMS.toArray(new Item[0]));
	}

	@SubscribeEvent
	public static void onBlockRegister(RegistryEvent.Register<Block> event) {
		event.getRegistry().registerAll(ModBlocks.BLOCKS.toArray(new Block[0]));

		//special registration callbacks (e.g. tile entities)
		ModBlocks.BLOCK_REGISTER_CALLBACKS.forEach(Runnable::run);
	}

	@SubscribeEvent
	public static void onModelRegister(ModelRegistryEvent event) {
		for(Item item : ModItems.ITEMS) {
			if(item instanceof IHasModel) {
				((IHasModel)item).registerModels();
			}
		}

		for(Block block : ModBlocks.BLOCKS) {
			if(block instanceof IHasModel) {
				((IHasModel)block).registerModels();
			}
		}

		registerEntityRenderers();
	}

	@SubscribeEvent
	public static void onEntityRegister(RegistryEvent.Register<EntityEntry> event) {
		ResourceLocation customVehicleRL = new ResourceLocation(Geopolitika.MODID, "custom_vehicle");
		ResourceLocation solidPartRL = new ResourceLocation(Geopolitika.MODID, "solid_part");

		event.getRegistry().registerAll(
				EntityEntryBuilder.create()
				.entity(EntityCustomVehicle.class)
				.id(customVehicleRL, entityId++)
				.name(customVehicleRL.getPath())
				.tracker(256, 3, true)
				.build(),

				EntityEntryBuilder.create()
				.entity(SolidEntityPart.class)
				.id(solidPartRL, entityId++)
				.name(solidPartRL.getPath())
				.tracker(256, 3, true)
				.build()
		);
	}

	private static void registerEntityRenderers() {
		RenderingRegistry.registerEntityRenderingHandler(EntityCustomVehicle.class, RenderCustomVehicle::new);
	}
}
