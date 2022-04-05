package kaktusz.geopolitika.commands;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.MessageUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.lang3.time.DurationFormatUtils;

public class CmdRegionOccupy extends Subcommand {
	public CmdRegionOccupy(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandName, String[] args) throws CommandException {
		if(args.length != 0)
			throw new WrongUsageException("");
		if(sender.getCommandSenderEntity() == null)
			throw new PlayerNotFoundException(MessageUtils.getCommandErrorKey("must_be_called_by_player"));

		TileEntityControlPoint controlPoint = StatesManager.getChunkControlPoint(sender.getPosition(), sender.getEntityWorld());
		if(controlPoint == null) {
			throw new CommandException(MessageUtils.getCommandErrorKey("must_be_in_claimed_region"));
		}

		ForgeTeam playerState = StatesManager.getPlayerState((EntityPlayerMP)sender);
		if(!playerState.isValid()) {
			throw new CommandException(MessageUtils.getCommandErrorKey("no_state"));
		}

		if(playerState.equalsTeam(controlPoint.getOwner())) {
			throw new CommandException(MessageUtils.getCommandErrorKey("friendly_territory"));
		}

		long cooldownTimeLeft = controlPoint.getOccupyCooldownTimeLeft();
		if(cooldownTimeLeft > 0) {
			throw new CommandException(MessageUtils.getCommandErrorKey("region_in_cooldown"), DurationFormatUtils.formatDurationHMS(cooldownTimeLeft));
		}

		controlPoint.beginOccupation(playerState);
	}
}
