package kaktusz.geopolitika.commands;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.util.FinalIDObject;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.MessageUtils;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class CmdRegionWarScore extends Subcommand {
	public CmdRegionWarScore(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandName, String[] args) throws CommandException {
		if(args.length != 2)
			throw new WrongUsageException("");
		if(!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
			throw new PlayerNotFoundException(MessageUtils.getCommandErrorKey("must_be_called_by_player"));

		TileEntityControlPoint controlPoint = StatesManager.getChunkControlPoint(sender.getPosition(), sender.getEntityWorld());
		if(controlPoint == null) {
			throw new CommandException(MessageUtils.getCommandErrorKey("must_be_in_claimed_region"));
		}

		String stateName = args[0];
		ForgeTeam targetState = Universe.get().getTeam(stateName);
		if(!targetState.isValid()) {
			throw new CommandException(MessageUtils.getCommandErrorKey("specified_invalid_state"));
		}
		int amountToAdd = CommandBase.parseInt(args[1]);

		controlPoint.beginOccupation(targetState);
		if(!controlPoint.isBeingOccupiedBy(targetState)) {
			throw new CommandException(MessageUtils.getCommandErrorKey("region_must_be_occupiable_by_specified_state"));
		}

		sendSuccessMessage(sender, rootCommandName, amountToAdd, targetState.getCommandTitle(), controlPoint.getRegionName(true));
		controlPoint.addWarScore(targetState, amountToAdd);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
		if(args.length != 1)
			return super.getTabCompletions(server, sender, args, targetPos);

		return StatesManager.getAllStates().map(FinalIDObject::getId).collect(Collectors.toList());
	}
}
