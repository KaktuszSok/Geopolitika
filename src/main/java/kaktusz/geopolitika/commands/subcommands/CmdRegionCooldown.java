package kaktusz.geopolitika.commands.subcommands;

import kaktusz.geopolitika.commands.CommandAssertions;
import kaktusz.geopolitika.commands.CommandPermissions;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;

public class CmdRegionCooldown extends Subcommand {
	public CmdRegionCooldown(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandTranslationKey, boolean adminMode, String[] args) throws CommandException {
		if(args.length != 1)
			throw new WrongUsageException("");

		int cooldown = CommandBase.parseInt(args[0], 0);
		Entity entity = CommandAssertions.senderMustBeEntity(sender);
		TileEntityControlPoint controlPoint = CommandAssertions.entityMustBeInClaimedRegion(entity);

		controlPoint.setOccupationCooldown(cooldown);
		sendSuccessMessage(sender, rootCommandTranslationKey, cooldown);
	}
}
