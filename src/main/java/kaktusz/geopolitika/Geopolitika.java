package kaktusz.geopolitika;

import kaktusz.geopolitika.handlers.ModSyncHandler;
import kaktusz.geopolitika.handlers.RegistryHandler;
import kaktusz.geopolitika.init.ModBlocks;
import kaktusz.geopolitika.init.ModCommands;
import kaktusz.geopolitika.integration.GTCEuIntegration;
import kaktusz.geopolitika.integration.XaeroIntegrationCommon;
import kaktusz.geopolitika.permaloaded.tileentities.ChunkDepositMarker;
import kaktusz.geopolitika.proxy.CommonProxy;
import kaktusz.geopolitika.states.StatesManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

@Mod(modid = Geopolitika.MODID, name = Geopolitika.NAME, version = Geopolitika.VERSION,
        dependencies = "required-after:ftblib;required-after:ftbutilities;after:xaerominimapfair;after:xaeroworldmap;after:gregtechce")
public class Geopolitika
{
    public static final String MODID = "geopolitika";
    public static final String NAME = "Geopolitika";
    public static final String VERSION = "1.0";

    @Mod.Instance
    public static Geopolitika INSTANCE;
    public static Logger logger;

    public static final String CLIENT_PROXY_CLASS = "kaktusz.geopolitika.proxy.ClientProxy";
    public static final String COMMON_PROXY_CLASS = "kaktusz.geopolitika.proxy.CommonProxy";
    @SidedProxy(clientSide = CLIENT_PROXY_CLASS, serverSide = COMMON_PROXY_CLASS)
    public static CommonProxy PROXY;

    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs("tabGeopolitika") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModBlocks.CONTROL_POINT);
        }
    };

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        RegistryHandler.init(event);
        ModSyncHandler.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if(event.getSide() == Side.CLIENT) {
            XaeroIntegrationCommon.postInit();
        }
        GTCEuIntegration.postInit();
        if(!GTCEuIntegration.isGregtechLoaded()) {
            ChunkDepositMarker.addVanillaOres();
        }
    }

    @Mod.EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent event)
    {
        StatesManager.restart();
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        ModCommands.onServerStart(event);
    }
}
