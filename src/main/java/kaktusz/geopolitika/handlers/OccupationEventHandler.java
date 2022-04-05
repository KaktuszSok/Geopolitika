package kaktusz.geopolitika.handlers;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Handles events regarding the occupation of control points.
 */
@Mod.EventBusSubscriber
public class OccupationEventHandler {

	//TODO make these translatable
	private static final ITextComponent LOSE_WAR_SCORE_DEATH_MESSAGE_PREFIX = new TextComponentString("You died: -100 war score in ");
	private static final ITextComponent GAIN_WAR_SCORE_KILL_MESSAGE_PREFIX = new TextComponentString("You killed an occupier: +100 war score in ");


	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent e) {
		if(!(e.getEntityLiving() instanceof EntityPlayerMP))
			return;

		tryLoseWarScoreForDying(e);
		tryGainWarScoreForKill(e);
	}

	private static void tryLoseWarScoreForDying(LivingDeathEvent e) {
		EntityPlayerMP deadPlayer = (EntityPlayerMP) e.getEntityLiving();
		ForgeTeam deadPlayerState = StatesManager.getPlayerState(deadPlayer);
		TileEntityControlPoint deathControlPoint = StatesManager.getChunkControlPoint(deadPlayer.getPosition(), deadPlayer.world);
		if(deathControlPoint != null) {
			if(deathControlPoint.isBeingOccupiedBy(deadPlayerState)) { //occupier died in region being occupied
				ITextComponent message = new TextComponentString("")
						.appendSibling(LOSE_WAR_SCORE_DEATH_MESSAGE_PREFIX)
						.appendSibling(deathControlPoint.getRegionName(true));
				deadPlayer.sendMessage(message);
				deathControlPoint.addWarScore(deadPlayerState, -100);
				return;
			}
		}

		Entity killer = e.getSource().getTrueSource();
		if(killer == null)
			return;
		TileEntityControlPoint killerControlPoint = StatesManager.getChunkControlPoint(killer.getPosition(), killer.world);
		if(killerControlPoint == null)
			return;

		if(killerControlPoint.isBeingOccupiedBy(deadPlayerState)) { //defender (killer) killed an occupier of his region
			ITextComponent message = new TextComponentString("")
					.appendSibling(LOSE_WAR_SCORE_DEATH_MESSAGE_PREFIX)
					.appendSibling(killerControlPoint.getRegionName(true));
			deadPlayer.sendMessage(message);
			killerControlPoint.addWarScore(deadPlayerState, -100);
		}
	}

	private static void tryGainWarScoreForKill(LivingDeathEvent e) {
		if(!(e.getSource().getTrueSource() instanceof EntityPlayerMP))
			return;

		EntityPlayerMP killer = (EntityPlayerMP) e.getSource().getTrueSource();
		ForgeTeam killerState = StatesManager.getPlayerState(killer);
		ForgeTeam deadPlayerState = StatesManager.getPlayerState((EntityPlayerMP) e.getEntityLiving());
		if(killerState.equalsTeam(deadPlayerState)) //friendly-fire
			return;

		TileEntityControlPoint killerControlPoint = StatesManager.getChunkControlPoint(killer.getPosition(), killer.world);
		if(killerControlPoint == null)
			return;

		if(killerControlPoint.isBeingOccupiedBy(killerState)
				&& (killerControlPoint.getOwner().equalsTeam(deadPlayerState))) { //killer killed a defender of this state
			ITextComponent message = new TextComponentString("")
					.appendSibling(GAIN_WAR_SCORE_KILL_MESSAGE_PREFIX)
					.appendSibling(killerControlPoint.getRegionName(true));
			killer.sendMessage(message);
			killerControlPoint.addWarScore(killerState, 100);
		}
	}
}
