package kaktusz.geopolitika.commands.subcommands;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.util.FinalIDObject;
import kaktusz.geopolitika.commands.CommandAssertions;
import kaktusz.geopolitika.commands.CommandPermissions;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.states.StatesSavedData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class CmdStateCooldown extends Subcommand {
	public CmdStateCooldown(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandTranslationKey, boolean adminMode, String[] args) throws CommandException {
		if(args.length != 3)
			throw new WrongUsageException("");

		ForgeTeam attackingState = CommandAssertions.specifiedStateMustBeValid(args[0]);
		ForgeTeam defendingState = CommandAssertions.specifiedStateMustBeValid(args[1]);
		int cooldown = CommandBase.parseInt(args[2], 0);

		StatesSavedData.get(server.getWorld(0)).setOccupationCooldown(attackingState.getUID(), defendingState.getUID(), cooldown);
		sendSuccessMessage(sender, rootCommandTranslationKey, attackingState.getCommandTitle(), defendingState.getCommandTitle(), cooldown);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
		if(args.length > 2)
			return super.getTabCompletions(server, sender, args, targetPos);

		return StatesManager.getAllStates().map(FinalIDObject::getId).collect(Collectors.toList());
	}
}
