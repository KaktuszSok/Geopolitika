package kaktusz.geopolitika.capabilities;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

public class CollectorItemHandler extends ItemStackHandler {

	public CollectorItemHandler(int size) {
		super(size);
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		return !stack.isEmpty();
	}
}
