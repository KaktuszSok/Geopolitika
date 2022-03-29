package kaktusz.geopolitika.handlers;

import kaktusz.geopolitika.blocks.BlockControlPoint;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class EventHandlers {

	@SubscribeEvent
	public static void blockPlaced(BlockEvent.EntityPlaceEvent e) {
		Block block = e.getPlacedBlock().getBlock();
		if(block instanceof BlockControlPoint) {
			if(!((BlockControlPoint)block).handleBlockPlacementEvent(e, true,false)) { //if placement is disallowed, cancel the event
				//e.setCanceled(true);
				e.getWorld().destroyBlock(e.getPos(), !(e.getEntity() instanceof EntityPlayer && ((EntityPlayer)e.getEntity()).isCreative()));
				return;
			}
		}
	}

	@SubscribeEvent
	public static void blockBroken(BlockEvent.BreakEvent e) {
		Block block = e.getState().getBlock();
		if(block instanceof BlockControlPoint) {
			if(!((BlockControlPoint)block).handleBlockBreakEvent(e, true)) { //if placement is disallowed, cancel the event
				e.setCanceled(true);
				return;
			}
		}
	}
}
