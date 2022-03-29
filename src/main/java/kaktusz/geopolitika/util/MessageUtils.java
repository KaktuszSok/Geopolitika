package kaktusz.geopolitika.util;

import kaktusz.geopolitika.Geopolitika;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;

public class MessageUtils {
	public static final Style errorStyle = new net.minecraft.util.text.Style().setColor(TextFormatting.RED);

	public static void sendErrorMessage(ICommandSender target, String errorTranslationKey, @Nonnull Object... args) {
		target.sendMessage(
				new TextComponentTranslation(Geopolitika.MODID + ".error." + errorTranslationKey, args)
						.setStyle(errorStyle)
		);
	}
}
