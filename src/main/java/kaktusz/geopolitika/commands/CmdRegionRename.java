package kaktusz.geopolitika.commands;

import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.MessageUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.util.StringJoiner;

public class CmdRegionRename extends Subcommand {
	public CmdRegionRename(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandName, String[] args) throws CommandException {
		if(args.length < 1)
			throw new WrongUsageException("");
		if(!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
			throw new PlayerNotFoundException(MessageUtils.getCommandErrorKey("must_be_called_by_player"));

		TileEntityControlPoint controlPoint = StatesManager.getChunkControlPoint(sender.getPosition(), sender.getEntityWorld());
		if(controlPoint == null) {
			throw new CommandException(MessageUtils.getCommandErrorKey("must_be_in_claimed_region"));
		}


		if(!StatesManager.hasPlayerRegionRenameAuthority((EntityPlayerMP) sender.getCommandSenderEntity(), controlPoint)) {
			throw new CommandException(MessageUtils.getCommandErrorKey("region_insufficient_authority"));
		}

		StringJoiner nameJoiner = new StringJoiner(" ");
		for (String arg : args) {
			nameJoiner.add(arg);
		}

		if(nameJoiner.toString().isEmpty())
			throw new WrongUsageException("");

		boolean success = controlPoint.setRegionName(nameJoiner.toString());
		if(!success)
			throw new CommandException(MessageUtils.getCommandErrorKey("region_name_too_long"));

		sendSuccessMessage(sender, rootCommandName, nameJoiner.toString());
	}
}
