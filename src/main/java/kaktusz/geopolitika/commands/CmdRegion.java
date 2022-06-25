package kaktusz.geopolitika.commands;

import kaktusz.geopolitika.commands.subcommands.region.*;
import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

public class CmdRegion extends CommandRoot {

	public CmdRegion() {
		super("region", CommandPermissions.ANYONE, "controlpoint", "cp");
		addSubcommand(new CmdRegionInfo("info", CommandPermissions.ENTITY));
		addSubcommand(new CmdRegionRename("rename", CommandPermissions.ENTITY));
		addSubcommand(new CmdRegionOccupy("occupy", CommandPermissions.ENTITY));
		addSubcommand(new CmdRegionTransfer("transfer", CommandPermissions.ENTITY));
		addSubcommand(new CmdRegionCooldown("cooldown", CommandPermissions.OP));
		addSubcommand(new CmdRegionWarScore("warscore", CommandPermissions.OP));
	}
}
