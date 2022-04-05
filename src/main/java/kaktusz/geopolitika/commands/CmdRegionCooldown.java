package kaktusz.geopolitika.commands;

import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.MessageUtils;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class CmdRegionCooldown extends Subcommand {
	public CmdRegionCooldown(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandName, String[] args) throws CommandException {
		if(args.length != 1)
			throw new WrongUsageException("");
		int cooldown = CommandBase.parseInt(args[0], 0);

		if(!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
			throw new PlayerNotFoundException(MessageUtils.getCommandErrorKey("must_be_called_by_player"));

		TileEntityControlPoint controlPoint = StatesManager.getChunkControlPoint(sender.getPosition(), sender.getEntityWorld());
		if(controlPoint == null) {
			throw new CommandException(MessageUtils.getCommandErrorKey("must_be_in_claimed_region"));
		}

		controlPoint.setOccupationCooldown(cooldown);
		sendSuccessMessage(sender, rootCommandName, cooldown);
	}
}
