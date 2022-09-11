package kaktusz.geopolitika.commands.subcommands.state;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import kaktusz.geopolitika.commands.CommandAssertions;
import kaktusz.geopolitika.commands.CommandPermissions;
import kaktusz.geopolitika.commands.subcommands.Subcommand;
import kaktusz.geopolitika.states.StatesSavedData;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandHelp;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.text.NumberFormat;

public class CmdStateDeposit extends Subcommand {
	private static final NumberFormat BALANCE_FORMAT = NumberFormat.getNumberInstance();

	public CmdStateDeposit(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandTranslationKey, boolean adminMode, String[] args) throws CommandException {
		if(args.length != 1)
			throw new WrongUsageException("");

		EntityPlayerMP player = CommandAssertions.senderMustBePlayer(sender);
		ForgeTeam state = CommandAssertions.playerMustBeInState(player);
		long trueDeposit = CommandHelp.parseLong(args[0]);
		if(!adminMode && trueDeposit < 0)
			trueDeposit = 0;

		StatesSavedData statesSavedData = StatesSavedData.get(player.world);
		statesSavedData.addBalance(state.getUID(), trueDeposit);
		sendSuccessMessage(sender, rootCommandTranslationKey,
				BALANCE_FORMAT.format(trueDeposit),
				state.getCommandTitle(),
				BALANCE_FORMAT.format(statesSavedData.getBalance(state.getUID())));
	}
}
