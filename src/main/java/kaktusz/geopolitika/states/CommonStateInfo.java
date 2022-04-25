package kaktusz.geopolitika.states;

import com.feed_the_beast.ftblib.lib.EnumTeamColor;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;

public class CommonStateInfo {
	public static CommonStateInfo NONE = new CommonStateInfo((short) 0, (short)EnumTeamColor.BLUE.ordinal());

	public short id;
	public EnumTeamColor colour;

	public CommonStateInfo(short id, short colourIdx) {
		this.id = id;
		this.colour = EnumTeamColor.values()[colourIdx];
	}

	public CommonStateInfo(ForgeTeam state) {
		id = state.getUID();
		colour = state.getColor();
	}

	public boolean isValid() {
		return id != 0;
	}
}
