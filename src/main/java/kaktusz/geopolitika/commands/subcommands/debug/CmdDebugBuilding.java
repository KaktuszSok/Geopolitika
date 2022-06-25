package kaktusz.geopolitika.commands.subcommands.debug;

import kaktusz.geopolitika.buildings.BuildingInfo;
import kaktusz.geopolitika.commands.CommandAssertions;
import kaktusz.geopolitika.commands.CommandPermissions;
import kaktusz.geopolitika.commands.subcommands.Subcommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CmdDebugBuilding extends Subcommand {
	public CmdDebugBuilding(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandTranslationKey, boolean adminMode, String[] args) throws CommandException {
		if(args.length != 0)
			throw new WrongUsageException("");
		CommandAssertions.senderMustBeEntity(sender);

		long startTime = System.nanoTime();
		BuildingInfo building = new BuildingInfo(sender.getEntityWorld(), sender.getPosition());
		long endTime = System.nanoTime();
		long nsElapsed = endTime - startTime;
		sender.sendMessage(new TextComponentString("Done in " + (nsElapsed/1000000d) + "ms"));
		if(!building.isValid())
			sender.sendMessage(new TextComponentString("invalid"));
		else
			sender.sendMessage(new TextComponentString(building.toString()));
	}
}
