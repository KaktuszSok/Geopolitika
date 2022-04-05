package kaktusz.geopolitika.commands;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import java.util.function.BiFunction;

public enum CommandPermissions {
	ANYONE(0, (s,c) -> true),
	ENTITY(0, (s,c) -> s.getCommandSenderEntity() != null),
	OP(2, (s,c) -> s.canUseCommand(2, c.getName()));

	public final int level;
	public final BiFunction<ICommandSender, ICommand, Boolean> predicate;
	CommandPermissions(int level, BiFunction<ICommandSender, ICommand, Boolean> predicate) {
		this.level = level;
		this.predicate = predicate;
	}

	public boolean hasPermissions(ICommandSender sender, ICommand command) {
		return predicate.apply(sender, command);
	}

	public int getPermissionLevel(int level) {
		return level;
	}
}
