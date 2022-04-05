package kaktusz.geopolitika.commands;

import kaktusz.geopolitika.Geopolitika;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public abstract class Subcommand {
	protected static final Style BASE_MESSAGE_STYLE = new Style().setColor(TextFormatting.GRAY);

	public final String name;
	public final CommandPermissions permissionLevel;

	public Subcommand(String name, CommandPermissions permissionLevel) {
		this.name = name;
		this.permissionLevel = permissionLevel;
	}

	public abstract void execute(MinecraftServer server, ICommandSender sender, String rootCommandName, String[] args) throws CommandException;

	protected void sendSuccessMessage(ICommandSender target, String rootCommandName, @Nonnull Object... args) {
		target.sendMessage(new TextComponentTranslation(
				Geopolitika.MODID + ".commands." + rootCommandName + "." + this.name + ".success",
				args
		).setStyle(BASE_MESSAGE_STYLE));
	}

	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
		return Collections.emptyList();
	}
}
