package kaktusz.geopolitika.commands;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

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
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("controlpoint", "cp");
	}
}