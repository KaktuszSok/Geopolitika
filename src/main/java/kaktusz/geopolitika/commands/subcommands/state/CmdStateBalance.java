package kaktusz.geopolitika.commands.subcommands.state;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import kaktusz.geopolitika.commands.CommandAssertions;
import kaktusz.geopolitika.commands.CommandPermissions;
import kaktusz.geopolitika.commands.subcommands.Subcommand;
import kaktusz.geopolitika.states.StatesSavedData;
import kaktusz.geopolitika.util.MessageUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import java.text.NumberFormat;

public class CmdStateBalance extends Subcommand {
	private static final NumberFormat BALANCE_FORMAT = NumberFormat.getNumberInstance();

	public CmdStateBalance(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandTranslationKey, boolean adminMode, String[] args) throws CommandException {
		if(args.length != 0)
			throw new WrongUsageException("");

		EntityPlayerMP player = CommandAssertions.senderMustBePlayer(sender);
		ForgeTeam state = CommandAssertions.playerMustBeInState(player);

		long balance = StatesSavedData.get(player.world).getBalance(state.getUID());
		String balanceStr = BALANCE_FORMAT.format(balance);
		MessageUtils.sendStateMessage(player, new TextComponentTranslation("geopolitika.commands.state.balance.response", balanceStr));
	}
}
