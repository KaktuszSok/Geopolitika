package kaktusz.geopolitika.handlers;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import kaktusz.geopolitika.init.ModConfig;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.MessageUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.*;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Handles events regarding the occupation of control points.
 */
@Mod.EventBusSubscriber
public class OccupationEventHandler {

	//TODO make these translatable
	private static final Style LOSE_WAR_SCORE_MESSAGE_STYLE = new Style().setColor(TextFormatting.DARK_RED);
	private static final Style GAIN_WAR_SCORE_MESSAGE_STYLE = new Style().setColor(TextFormatting.DARK_GREEN);

	private static final Queue<Runnable> queuedActions = new LinkedList<>();


	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent e) {
		if(!(e.getEntityLiving() instanceof EntityPlayerMP))
			return;

		queuedActions.add(() -> tryLoseWarScoreForDying(e));
		queuedActions.add(() -> tryGainWarScoreForKill(e));
	}

	private static void tryLoseWarScoreForDying(LivingDeathEvent e) {
		EntityPlayerMP deadPlayer = (EntityPlayerMP) e.getEntityLiving();
		ForgeTeam deadPlayerState = StatesManager.getPlayerState(deadPlayer);
		TileEntityControlPoint deathControlPoint = StatesManager.getChunkControlPoint(deadPlayer.getPosition(), deadPlayer.world);
		if(deathControlPoint != null) {
			if(deathControlPoint.isBeingOccupiedBy(deadPlayerState)) { //occupier died in region being occupied
				int warScoreLost = ModConfig.warScoreDeathLoss;
				ITextComponent message = new TextComponentTranslation("geopolitika.info.lose_war_score_death",
						deadPlayer.getDisplayName().getFormattedText(),
						warScoreLost,
						deathControlPoint.getRegionName(true).getFormattedText())
						.setStyle(LOSE_WAR_SCORE_MESSAGE_STYLE);
				MessageUtils.sendMessageToState(deadPlayerState, message);
				deathControlPoint.addWarScore(deadPlayerState, -warScoreLost);
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
			int warScoreLost = ModConfig.warScoreDeathLoss;
			ITextComponent message = new TextComponentTranslation("geopolitika.info.lose_war_score_death",
					deadPlayer.getDisplayName().getFormattedText(),
					warScoreLost,
					killerControlPoint.getRegionName(true).getFormattedText())
					.setStyle(LOSE_WAR_SCORE_MESSAGE_STYLE);
			MessageUtils.sendMessageToState(deadPlayerState, message);
			killerControlPoint.addWarScore(deadPlayerState, -warScoreLost);
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
			int warScoreGained = ModConfig.warScoreKillGain;
			ITextComponent message = new TextComponentTranslation("geopolitika.info.gain_war_score_kill",
					killer.getDisplayName().getFormattedText(),
					warScoreGained,
					killerControlPoint.getRegionName(true).getFormattedText())
					.setStyle(GAIN_WAR_SCORE_MESSAGE_STYLE);
			MessageUtils.sendMessageToState(killerState, message);
			killerControlPoint.addWarScore(killerState, warScoreGained);
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent e) {
		while (!queuedActions.isEmpty()) {
			queuedActions.poll().run();
		}
	}
}
