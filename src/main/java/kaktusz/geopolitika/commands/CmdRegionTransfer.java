package kaktusz.geopolitika.commands;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.Universe;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.MessageUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class CmdRegionTransfer extends Subcommand {
	public CmdRegionTransfer(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandName, String[] args) throws CommandException {
		if(args.length < 1)
			throw new WrongUsageException("");
		if(!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
			throw new PlayerNotFoundException(MessageUtils.getCommandErrorKey("must_be_called_by_player"));
		EntityPlayerMP player = (EntityPlayerMP) sender.getCommandSenderEntity();

		TileEntityControlPoint controlPoint = StatesManager.getChunkControlPoint(sender.getPosition(), sender.getEntityWorld());
		if(controlPoint == null) {
			throw new CommandException(MessageUtils.getCommandErrorKey("must_be_in_claimed_region"));
		}

		if(!StatesManager.getPlayerState(player).isValid()) {
			throw new CommandException(MessageUtils.getCommandErrorKey("no_state"));
		}

		if(!StatesManager.getPlayerState(player).equalsTeam(controlPoint.getOwner()) ||
				!StatesManager.hasPlayerModifyClaimsAuthority(player)) {
			throw new CommandException(MessageUtils.getCommandErrorKey("region_insufficient_authority"));
		}

		if(controlPoint.isConflictOngoing()) {
			throw new CommandException(MessageUtils.getCommandErrorKey("region_conflict_ongoing"));
		}

		String transferTargetName = args[0];
		ForgeTeam targetState = Universe.get().getTeam(transferTargetName);
		if(!targetState.isValid()) {
			throw new CommandException(MessageUtils.getCommandErrorKey("specified_invalid_state"));
		}

		controlPoint.setOwner(targetState);
		controlPoint.claimChunks(false);
	}
}
