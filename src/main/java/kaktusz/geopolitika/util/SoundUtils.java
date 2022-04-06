package kaktusz.geopolitika.util;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class SoundUtils {

	public static void playSoundForState(ForgeTeam state, SoundEvent sound, float volume, float pitch) {
		state.getOnlineMembers().forEach(
				member -> member.connection.sendPacket(new SPacketSoundEffect(
						sound, SoundCategory.NEUTRAL,
						member.posX, member.posY, member.posZ,
						volume, pitch))
		);
	}
}
