package kaktusz.geopolitika.commands;

import kaktusz.geopolitika.buildings.BuildingGeneric;
import kaktusz.geopolitika.buildings.BuildingHouse;
import kaktusz.geopolitika.commands.subcommands.debug.CmdDebugBuilding;
import kaktusz.geopolitika.commands.subcommands.debug.CmdDebugRoom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CmdDebug extends CommandRoot {
	public CmdDebug() {
		super("gpdebug", CommandPermissions.OP);
		addSubcommand(new CmdDebugRoom("room", CommandPermissions.OP));
		addSubcommand(new CmdDebugBuilding<BuildingGeneric>("building", CommandPermissions.OP) {
			@Override
			protected BuildingGeneric createBuildingInfo(World world, BlockPos pos) {
				return new BuildingGeneric(world, pos);
			}
		});
		addSubcommand(new CmdDebugBuilding<BuildingHouse>("house", CommandPermissions.OP) {
			@Override
			protected BuildingHouse createBuildingInfo(World world, BlockPos pos) {
				return new BuildingHouse(world, pos);
			}
		});
	}
}
