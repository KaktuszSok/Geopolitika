package kaktusz.geopolitika.commands;

import kaktusz.geopolitika.commands.subcommands.state.CmdStateCooldown;

public class CmdState extends CommandRoot {
	public CmdState() {
		super("state", CommandPermissions.ANYONE);
		addSubcommand(new CmdStateCooldown("cooldown", CommandPermissions.OP));
	}
}
