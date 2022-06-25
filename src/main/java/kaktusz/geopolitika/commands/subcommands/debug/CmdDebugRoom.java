package kaktusz.geopolitika.commands.subcommands.debug;

import kaktusz.geopolitika.buildings.RoomInfo;
import kaktusz.geopolitika.commands.CommandAssertions;
import kaktusz.geopolitika.commands.CommandPermissions;
import kaktusz.geopolitika.commands.subcommands.Subcommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.util.HashSet;

public class CmdDebugRoom extends Subcommand {
	public CmdDebugRoom(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandTranslationKey, boolean adminMode, String[] args) throws CommandException {
		if(args.length != 0)
			throw new WrongUsageException("");
		CommandAssertions.senderMustBeEntity(sender);

		RoomInfo room = RoomInfo.calculateRoom(sender.getEntityWorld(), sender.getPosition(), new HashSet<>(), new HashSet<>());
		if(room == null)
			sender.sendMessage(new TextComponentString("null"));
		else
			sender.sendMessage(new TextComponentString(room.toString()));
	}
}
