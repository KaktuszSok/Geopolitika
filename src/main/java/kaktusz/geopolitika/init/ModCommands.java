package kaktusz.geopolitika.init;

import kaktusz.geopolitika.commands.CmdDebug;
import kaktusz.geopolitika.commands.CmdRegion;
import kaktusz.geopolitika.commands.CmdState;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public class ModCommands {

	public static void onServerStart(FMLServerStartingEvent e) {
		e.registerServerCommand(new CmdRegion());
		e.registerServerCommand(new CmdRegion().setAdminMode(true));
		e.registerServerCommand(new CmdState());
		e.registerServerCommand(new CmdDebug());
	}
}
