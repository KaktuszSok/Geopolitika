package kaktusz.geopolitika.commands.subcommands;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.util.FinalIDObject;
import kaktusz.geopolitika.commands.CommandAssertions;
import kaktusz.geopolitika.commands.CommandPermissions;
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
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandTranslationKey, boolean adminMode, String[] args) throws CommandException {
		if(args.length != 2)
			throw new WrongUsageException("");
		EntityPlayerMP player = CommandAssertions.senderMustBePlayer(sender);
		TileEntityControlPoint controlPoint = CommandAssertions.entityMustBeInClaimedRegion(player);

		ForgeTeam targetState = CommandAssertions.specifiedStateMustBeValid(args[0]);
		int amountToAdd = CommandBase.parseInt(args[1]);

		if(!controlPoint.isBeingOccupiedBy(targetState)) {
			controlPoint.tryBeginOccupation(targetState, adminMode);
			if (!controlPoint.isBeingOccupiedBy(targetState)) {
				throw new CommandException(MessageUtils.getCommandErrorKey("region_must_be_occupiable_by_specified_state"));
			}
		}

		sendSuccessMessage(sender, rootCommandTranslationKey, amountToAdd, targetState.getCommandTitle(), controlPoint.getRegionName(true));
		controlPoint.addWarScore(targetState, amountToAdd);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
		if(args.length != 1)
			return super.getTabCompletions(server, sender, args, targetPos);

		return StatesManager.getAllStates().map(FinalIDObject::getId).collect(Collectors.toList());
	}
}
