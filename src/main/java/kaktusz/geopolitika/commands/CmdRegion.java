package kaktusz.geopolitika.commands;

import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CmdRegion extends CommandRoot {

	public CmdRegion() {
		super("region", CommandPermissions.ANYONE);
		addSubcommand(new CmdRegionInfo("info", CommandPermissions.ENTITY));
		addSubcommand(new CmdRegionRename("rename", CommandPermissions.ENTITY));
		addSubcommand(new CmdRegionOccupy("occupy", CommandPermissions.ENTITY));
		addSubcommand(new CmdRegionTransfer("transfer", CommandPermissions.ENTITY));
		addSubcommand(new CmdRegionCooldown("cooldown", CommandPermissions.OP));
		addSubcommand(new CmdRegionWarScore("warscore", CommandPermissions.OP));
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("controlpoint", "cp");
	}
}
