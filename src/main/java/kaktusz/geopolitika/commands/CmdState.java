package kaktusz.geopolitika.commands;

import kaktusz.geopolitika.commands.subcommands.state.CmdStateBalance;
import kaktusz.geopolitika.commands.subcommands.state.CmdStateCooldown;
import kaktusz.geopolitika.commands.subcommands.state.CmdStateDeposit;
import kaktusz.geopolitika.commands.subcommands.state.CmdStateStats;

public class CmdState extends CommandRoot {
	public CmdState() {
		super("state", CommandPermissions.ANYONE);
		addSubcommand(new CmdStateBalance("balance", CommandPermissions.ANYONE));
		addSubcommand(new CmdStateDeposit("deposit", CommandPermissions.ANYONE));
		addSubcommand(new CmdStateStats("stats", CommandPermissions.ANYONE));
		addSubcommand(new CmdStateCooldown("cooldown", CommandPermissions.OP));
	}
}
