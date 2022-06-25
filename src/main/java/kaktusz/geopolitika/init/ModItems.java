package kaktusz.geopolitika.init;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.items.ItemProjectileTest;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
	public static final List<Item> ITEMS = new ArrayList<>();

	public static final ItemProjectileTest PROJECTILE_TEST = new ItemProjectileTest("projectile_test", Geopolitika.CREATIVE_TAB);
}
