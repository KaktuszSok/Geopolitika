package kaktusz.geopolitika.commands;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.Universe;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.MessageUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CommandAssertions {

	public static Entity senderMustBeEntity(ICommandSender sender) throws CommandException {
		if(sender.getCommandSenderEntity() == null)
			throw new CommandException(MessageUtils.getCommandErrorKey("must_be_called_by_entity"));
		return (Entity) sender;
	}

	public static EntityPlayerMP senderMustBePlayer(ICommandSender sender) throws PlayerNotFoundException {
		if(!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
			throw new PlayerNotFoundException(MessageUtils.getCommandErrorKey("must_be_called_by_player"));
		return (EntityPlayerMP) sender;
	}

	public static TileEntityControlPoint entityMustBeInClaimedRegion(Entity entity) throws CommandException {
		TileEntityControlPoint controlPoint = StatesManager.getChunkControlPoint(entity.getPosition(), entity.getEntityWorld());
		if(controlPoint == null) {
			throw new CommandException(MessageUtils.getCommandErrorKey("must_be_in_claimed_region"));
		}
		return controlPoint;
	}

	public static ForgeTeam playerMustBeInState(EntityPlayerMP player) throws CommandException {
		ForgeTeam playerState = StatesManager.getPlayerState(player);
		if(!playerState.isValid()) {
			throw new CommandException(MessageUtils.getCommandErrorKey("no_state"));
		}
		return playerState;
	}

	public static ForgeTeam specifiedStateMustBeValid(String stateName) throws CommandException {
		ForgeTeam targetState = Universe.get().getTeam(stateName);
		if(!targetState.isValid()) {
			throw new CommandException(MessageUtils.getCommandErrorKey("specified_invalid_state"));
		}
		return targetState;
	}
}
