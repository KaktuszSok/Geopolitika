package kaktusz.geopolitika.commands;

import kaktusz.geopolitika.commands.subcommands.state.CmdStateBalance;
import kaktusz.geopolitika.commands.subcommands.state.CmdStateCooldown;
import kaktusz.geopolitika.commands.subcommands.state.CmdStateDeposit;

public class CmdState extends CommandRoot {
	public CmdState() {
		super("state", CommandPermissions.ANYONE);
		addSubcommand(new CmdStateBalance("bal", CommandPermissions.ANYONE));
		addSubcommand(new CmdStateDeposit("dep", CommandPermissions.ANYONE));
		addSubcommand(new CmdStateCooldown("cooldown", CommandPermissions.OP));
	}
}
