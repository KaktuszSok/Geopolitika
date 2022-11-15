package kaktusz.geopolitika.util;

import net.minecraft.entity.player.EntityPlayer;

public interface IHasGUI {
	Object getServerGuiContainer(EntityPlayer player);
	Object getClientGuiContainer(EntityPlayer player);
}
