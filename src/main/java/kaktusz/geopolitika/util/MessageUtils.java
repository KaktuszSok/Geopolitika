package kaktusz.geopolitika.util;

import kaktusz.geopolitika.Geopolitika;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.util.text.*;

import javax.annotation.Nonnull;

public class MessageUtils {
	public static final Style ERROR_STYLE = new net.minecraft.util.text.Style().setColor(TextFormatting.RED);
	private static final TextComponentString EMPTY_STRING = new TextComponentString("");

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
}
