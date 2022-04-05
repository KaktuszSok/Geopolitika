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

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommandRoot extends CommandBase {

	private final String name;
	public final CommandPermissions permissionLevel;
	private final LinkedHashMap<String, Subcommand> subcommands = new LinkedHashMap<>();
	private final List<String> tabCompletions = new ArrayList<>();

	public CommandRoot(String name, CommandPermissions permissionLevel) {
		this.name = name;
		this.permissionLevel = permissionLevel;
	}

	public void addSubcommand(Subcommand sub) {
		subcommands.put(sub.name, sub);
		tabCompletions.add(sub.name);
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
		if(sub == null)
			throw new WrongUsageException(getUsage(sender), getUsageParameter(sender));

		try {
			sub.execute(server, sender, getName(), Arrays.copyOfRange(args, 1, args.length));
		} catch (WrongUsageException e) {
			throw new WrongUsageException(getSubcommandUsage(sender, sub));
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
		if(args.length > 1) {
			Subcommand sub = subcommands.get(args[0]);
			if(sub == null)
				return Collections.emptyList();

			return sub.getTabCompletions(server, sender, Arrays.copyOfRange(args, 1, args.length), targetPos);
		}

		return tabCompletions;
	}
}
