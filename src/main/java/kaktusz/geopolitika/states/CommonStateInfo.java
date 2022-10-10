package kaktusz.geopolitika.states;

import com.feed_the_beast.ftblib.lib.EnumTeamColor;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import net.minecraft.util.text.ITextComponent;

public class CommonStateInfo {
	public static CommonStateInfo NONE = new CommonStateInfo(StatesManager.getNoneState());

	public final short id;
	public final ITextComponent name;
	public final EnumTeamColor colour;

	public CommonStateInfo(short id, ITextComponent name, short colourIdx) {
		this.id = id;
		this.name = name;
		this.colour = EnumTeamColor.values()[colourIdx];
	}

	public CommonStateInfo(ForgeTeam state) {
		id = state.getUID();
		name = state.getCommandTitle();
		colour = state.getColor();
	}

	public boolean isValid() {
		return id != 0;
	}
}
