package kaktusz.geopolitika.commands;

import kaktusz.geopolitika.Geopolitika;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandRoot extends CommandBase {

	private final String name;
	public final CommandPermissions permissionLevel;
	private final LinkedHashMap<String, Subcommand> subcommands = new LinkedHashMap<>();

	public CommandRoot(String name, CommandPermissions permissionLevel) {
		this.name = name;
		this.permissionLevel = permissionLevel;
	}

	public void addSubcommand(Subcommand sub) {
		subcommands.put(sub.name, sub);
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return super.checkPermission(server, sender);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "%s";
	}

	public String getUsageParameter(ICommandSender sender) {
		if(subcommands.size() == 1)
			return "/" + getName() + " " + subcommands.keySet().toArray()[0];

		StringBuilder result = new StringBuilder("/");
		result.append(getName());
		StringJoiner joiner = new StringJoiner(":", " <", ">");
		for (String subcommandName : subcommands.keySet()) { //idk if values() is ordered correctly so sticking to keySet()
			if(subcommands.get(subcommandName).permissionLevel.hasPermissions(sender, this)) {
				joiner.add(subcommandName);
			}
		}
		result.append(joiner.toString());

		return result.toString();
	}

	public String getSubcommandUsage(ICommandSender sender, Subcommand subcommand) {
		return Geopolitika.MODID + ".commands." + name + "." + subcommand.name + ".usage";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if(args.length < 1)
			throw new WrongUsageException(getUsage(sender), getUsageParameter(sender));

		String subcommandName = args[0];
		Subcommand sub = subcommands.get(subcommandName);
		if(sub == null || !sub.permissionLevel.hasPermissions(sender, this))
			throw new WrongUsageException(getUsage(sender), getUsageParameter(sender));

		try {
			sub.execute(server, sender, getName(), Arrays.copyOfRange(args, 1, args.length));
		} catch (WrongUsageException e) {
			throw new WrongUsageException(getSubcommandUsage(sender, sub));
		}
	}

	@Override
	public final List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
		if(args.length == 0)
			return getPossibleTabCompletions(server, sender, args, targetPos);

		return getPossibleTabCompletions(server, sender, args, targetPos)
				.stream().filter(tc -> tc.startsWith(args[args.length-1])).collect(Collectors.toList());
	}

	protected List<String> getPossibleTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
		if(args.length > 1) {
			Subcommand sub = subcommands.get(args[0]);
			if(sub == null || !sub.permissionLevel.hasPermissions(sender, this))
				return Collections.emptyList();

			return sub.getTabCompletions(server, sender, Arrays.copyOfRange(args, 1, args.length), targetPos);
		}

		List<String> tabCompletions = new ArrayList<>();
		for (String subcommandName : subcommands.keySet()) {
			if(subcommands.get(subcommandName).permissionLevel.hasPermissions(sender, this)) {
				tabCompletions.add(subcommandName);
			}
		}
		return tabCompletions;
	}
}
