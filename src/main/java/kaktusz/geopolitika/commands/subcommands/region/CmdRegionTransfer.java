package kaktusz.geopolitika.commands.subcommands.region;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.util.FinalIDObject;
import kaktusz.geopolitika.commands.CommandAssertions;
import kaktusz.geopolitika.commands.CommandPermissions;
import kaktusz.geopolitika.commands.subcommands.Subcommand;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.MessageUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class CmdRegionTransfer extends Subcommand {
	public CmdRegionTransfer(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandTranslationKey, boolean adminMode, String[] args) throws CommandException {
		if(args.length != 1)
			throw new WrongUsageException("");

		EntityPlayerMP player = CommandAssertions.senderMustBePlayer(sender);
		ForgeTeam state = CommandAssertions.playerMustBeInState(player);
		TileEntityControlPoint controlPoint = CommandAssertions.entityMustBeInClaimedRegion(player);

		if(!adminMode &&
				(!state.equalsTeam(controlPoint.getOwner()) ||
				!StatesManager.hasPlayerModifyClaimsAuthority(player))) {
			throw new CommandException(MessageUtils.getCommandErrorKey("region_insufficient_authority"));
		}

		if(controlPoint.isConflictOngoing()) {
			throw new CommandException(MessageUtils.getCommandErrorKey("region_conflict_ongoing"));
		}

		ForgeTeam targetState = CommandAssertions.specifiedStateMustBeValid(args[0]);

		controlPoint.setOwner(targetState);
		controlPoint.claimChunks(false);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
		if(args.length != 1)
			return super.getTabCompletions(server, sender, args, targetPos);

		return StatesManager.getAllStates().map(FinalIDObject::getId).collect(Collectors.toList());
	}
}
