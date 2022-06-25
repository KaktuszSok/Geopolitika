package kaktusz.geopolitika.commands;

import kaktusz.geopolitika.commands.subcommands.debug.CmdDebugBuilding;
import kaktusz.geopolitika.commands.subcommands.debug.CmdDebugRoom;

public class CmdDebug extends CommandRoot {
	public CmdDebug() {
		super("gpdebug", CommandPermissions.OP);
		addSubcommand(new CmdDebugRoom("room", CommandPermissions.OP));
		addSubcommand(new CmdDebugBuilding("building", CommandPermissions.OP));
	}
}
