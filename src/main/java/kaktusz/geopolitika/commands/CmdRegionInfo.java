package kaktusz.geopolitika.commands;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.util.MessageUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class CmdRegionInfo extends Subcommand {
	protected static final Style HEADING_STYLE = new Style().setColor(TextFormatting.WHITE);
	protected static final Style CONTROL_POINT_POS_STYLE = new Style().setColor(TextFormatting.WHITE);

	public CmdRegionInfo(String n, CommandPermissions permissionLevel) {
		super(n, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandName, String[] args) throws CommandException {
		if(args.length != 0)
			throw new WrongUsageException("");
		if(sender.getCommandSenderEntity() == null)
			throw new PlayerNotFoundException(MessageUtils.getCommandErrorKey("must_be_called_by_player"));

		BlockPos controlPointPos = StatesManager.getChunkControlPointPos(sender.getPosition(), sender.getEntityWorld());
		if(controlPointPos == null) {
			throw new CommandException(MessageUtils.getCommandErrorKey("must_be_in_claimed_region"));
		}
		ForgeTeam state = StatesManager.getChunkOwner(sender.getPosition(), sender.getEntityWorld());

		ITextComponent message = new TextComponentString("")
				.appendSibling(new TextComponentString("Region ").setStyle(HEADING_STYLE))
				.appendSibling(StatesManager.getRegionName(sender.getPosition(), sender.getEntityWorld(), false))
				.appendSibling(new TextComponentString(" in ").setStyle(HEADING_STYLE))
				.appendSibling(state.getCommandTitle())
				.appendSibling(new TextComponentString("\nControl Point position: ").setStyle(BASE_MESSAGE_STYLE))
				.appendSibling(new TextComponentString(
						controlPointPos.getX() + ", " + controlPointPos.getY() + ", " + controlPointPos.getZ()
				).setStyle(CONTROL_POINT_POS_STYLE));

		sender.sendMessage(message);
	}
}
