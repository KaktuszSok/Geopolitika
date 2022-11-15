package kaktusz.geopolitika.util;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.states.StatesManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.*;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MessageUtils {
	public static final Style ERROR_STYLE = new Style().setColor(TextFormatting.RED);
	public static final Style BOLD_STYLE = new Style().setBold(true);
	public static final Style DARK_GREY_STYLE = new Style().setColor(TextFormatting.DARK_GRAY);
	public static final Style SHINY_STYLE = new Style().setColor(TextFormatting.AQUA);
	private static final TextComponentString EMPTY_STRING = new TextComponentString("");
	private static final ITextComponent MESSAGE_PREFIX = new TextComponentString("[")
			.setStyle(new Style().setColor(TextFormatting.GRAY))
			.appendSibling(new TextComponentString(Geopolitika.NAME).setStyle(new Style().setColor(TextFormatting.DARK_AQUA)))
			.appendText("] ");
	private static final ITextComponent IMPORTANT_PREFIX = new TextComponentString("-------- [")
			.setStyle(new Style().setColor(TextFormatting.GRAY))
			.appendSibling(new TextComponentString(Geopolitika.NAME).setStyle(new Style().setColor(TextFormatting.DARK_AQUA)))
			.appendText("] --------\n");
	private static final ITextComponent IMPORTANT_SUFFIX = new TextComponentString(
			"\n"
	).setStyle(new Style().setColor(TextFormatting.GRAY));

	public static void sendErrorMessage(ICommandSender target, String errorTranslationKey, @Nonnull Object... args) {
		target.sendMessage(
				new TextComponentTranslation(Geopolitika.MODID + ".error." + errorTranslationKey, args)
						.setStyle(ERROR_STYLE)
		);
	}

	public static String getCommandErrorKey(String suffix) {
		return Geopolitika.MODID + ".error.command." + suffix;
	}

	public static void setTitleTimings(EntityPlayerMP target, int fadeIn, int sustain, int fadeOut) {
		SPacketTitle timesPacket = new SPacketTitle(SPacketTitle.Type.TIMES, null, fadeIn, sustain, fadeOut);
		target.connection.sendPacket(timesPacket);
	}

	public static void displayTitle(EntityPlayerMP target, ITextComponent message) {
		SPacketTitle subtitlePacket = new SPacketTitle(SPacketTitle.Type.SUBTITLE, EMPTY_STRING);
		target.connection.sendPacket(subtitlePacket);
		SPacketTitle titlePacket = new SPacketTitle(SPacketTitle.Type.TITLE, message);
		target.connection.sendPacket(titlePacket);
	}

	public static void displaySubtitle(EntityPlayerMP target, ITextComponent message) {
		SPacketTitle subtitlePacket = new SPacketTitle(SPacketTitle.Type.SUBTITLE, message);
		target.connection.sendPacket(subtitlePacket);
		SPacketTitle titlePacket = new SPacketTitle(SPacketTitle.Type.TITLE, EMPTY_STRING);
		target.connection.sendPacket(titlePacket);
	}

	public static void displayActionbar(EntityPlayerMP target, ITextComponent message) {
		SPacketTitle actionbarPacket = new SPacketTitle(SPacketTitle.Type.ACTIONBAR, message);
		target.connection.sendPacket(actionbarPacket);
	}

	public static void broadcastInfoMessage(MinecraftServer server, ITextComponent messageContent) {
		ITextComponent message = new TextComponentString("")
				.appendSibling(MESSAGE_PREFIX)
				.appendSibling(messageContent);
		server.getPlayerList().sendMessage(message);
	}

	public static void broadcastImportantMessage(MinecraftServer server, ITextComponent messageContent) {
		ITextComponent message = new TextComponentString("")
				.appendSibling(IMPORTANT_PREFIX)
				.appendSibling(messageContent)
				.appendSibling(IMPORTANT_SUFFIX);
		server.getPlayerList().sendMessage(message);
	}

	public static void sendMessageToState(ForgeTeam state, ITextComponent messageContent) {
		for (EntityPlayerMP member : state.getOnlineMembers()) {
			sendStateMessage(member, messageContent);
		}
	}

	public static void sendStateMessage(EntityPlayerMP target, ITextComponent messageContent) {
		ITextComponent stateMessagePrefix = getStateMessagePrefix(StatesManager.getPlayerState(target));
		ITextComponent message = new TextComponentString("")
				.appendSibling(stateMessagePrefix)
				.appendSibling(messageContent);
		target.sendMessage(message);
	}

	public static ITextComponent getStateMessagePrefix(ForgeTeam state) {
		return new TextComponentString("[")
				.setStyle(new Style().setColor(TextFormatting.GRAY))
				.appendSibling(state.getCommandTitle())
				.appendText("] ");
	}

	public static void sendInfoMessage(ICommandSender target, ITextComponent messageContent) {
		ITextComponent message = new TextComponentString("")
				.appendSibling(MESSAGE_PREFIX)
				.appendSibling(messageContent);
		target.sendMessage(message);
	}
}
