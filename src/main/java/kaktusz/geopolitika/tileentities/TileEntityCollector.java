package kaktusz.geopolitika.tileentities;

import kaktusz.geopolitika.capabilities.CollectorItemHandler;
import kaktusz.geopolitika.containers.GenericContainer;
import kaktusz.geopolitika.containers.GenericContainerGUI;
import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import kaktusz.geopolitika.permaloaded.tileentities.ResourceCollector;
import kaktusz.geopolitika.util.IHasGUI;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TileEntityCollector extends TileEntity implements IHasGUI {
	private static final Map<Capability<?>, Function<TileEntityCollector, Object>> CAPABILITY_PROVIDERS = new HashMap<>();
	static {
		CAPABILITY_PROVIDERS.put(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, TileEntityCollector::getInventory);
	}

	private final CollectorItemHandler clientInventory = new CollectorItemHandler(ResourceCollector.INV_SIZE);

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		return CAPABILITY_PROVIDERS.containsKey(capability) || super.hasCapability(capability, facing);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		Function<TileEntityCollector, Object> cap = CAPABILITY_PROVIDERS.get(capability);
		return cap != null ? (T)cap.apply(this) : super.getCapability(capability, facing);
	}

	public CollectorItemHandler getInventory() {
		if(world == null || world.isRemote)
			return clientInventory;

		return ((ResourceCollector) PermaloadedSavedData.get(world).getTileEntityAt(pos)).getInventory();
	}

	@Override
	public GenericContainer getServerGuiContainer(EntityPlayer player) {
		return new GenericContainer(player.inventory, getInventory());
	}

	@Override
	public GenericContainerGUI getClientGuiContainer(EntityPlayer player) {
		ITextComponent title = new TextComponentString("Resource Collector");
		return new GenericContainerGUI(title, getServerGuiContainer(player));
	}
}
