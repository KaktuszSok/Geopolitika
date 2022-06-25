package kaktusz.geopolitika.commands.subcommands.region;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import kaktusz.geopolitika.commands.CommandAssertions;
import kaktusz.geopolitika.commands.CommandPermissions;
import kaktusz.geopolitika.commands.subcommands.Subcommand;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class CmdRegionOccupy extends Subcommand {
	public CmdRegionOccupy(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandTranslationKey, boolean adminMode, String[] args) throws CommandException {
		if(args.length != 0)
			throw new WrongUsageException("");

		EntityPlayerMP player = CommandAssertions.senderMustBePlayer(sender);
		TileEntityControlPoint controlPoint = CommandAssertions.entityMustBeInClaimedRegion(player);
		ForgeTeam playerState = CommandAssertions.playerMustBeInState(player);

		controlPoint.tryBeginOccupation(playerState, adminMode);
	}


}
