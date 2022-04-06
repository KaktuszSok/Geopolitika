package kaktusz.geopolitika.init;

import kaktusz.geopolitika.commands.CmdRegion;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public class ModCommands {

	public static void onServerStart(FMLServerStartingEvent e) {
		e.registerServerCommand(new CmdRegion());
		e.registerServerCommand(new CmdRegion().setAdminMode(true));
	}
}
