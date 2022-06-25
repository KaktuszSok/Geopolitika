package kaktusz.geopolitika.states;

import kaktusz.geopolitika.Geopolitika;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StatesSavedData extends WorldSavedData {
	private static final String DATA_NAME = Geopolitika.MODID + "_statesData";
	private static final String COOLDOWNS_NBT_TAG = "cooldowns";

	private final Map<Short, Map<Short, Long>> cooldowns = new HashMap<>();
	private World world;

	public StatesSavedData() {
		super(DATA_NAME);
	}
	public StatesSavedData(String name) {
		super(name);
	}

	public static StatesSavedData get(World world) {
		MapStorage storage = world.getMapStorage();
		StatesSavedData instance = (StatesSavedData) storage.getOrLoadData(StatesSavedData.class, DATA_NAME);

		if(instance == null) {
			instance = new StatesSavedData();
			storage.setData(DATA_NAME, instance);
		}

		instance.world = world;
		return instance;
	}

	public void setOccupationCooldown(short attackersId, short defendersId, int cooldownMinutes) {
		setOccupationCooldownExpiry(attackersId, defendersId,
				System.currentTimeMillis() + ((long) cooldownMinutes*60*1000));
	}

	private void setOccupationCooldownExpiry(short attackersId, short defendersId, long expiryTime) {
		Map<Short, Long> defenderCooldowns = cooldowns.computeIfAbsent(defendersId, id -> new HashMap<>());
		defenderCooldowns.put(attackersId, expiryTime);
		markDirty();
	}

	/**
	 * Gets how long the attackers must wait until starting a new occupation of the defenders
	 */
	public long getOccupationCooldown(short attackersId, short defendersId) {
		Map<Short, Long> defenderCooldowns = cooldowns.get(defendersId);
		if(defenderCooldowns == null)
			return 0;
		Long cooldown_ms = defenderCooldowns.get(attackersId);
		if(cooldown_ms == null)
			return 0;

		return cooldown_ms - System.currentTimeMillis();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		NBTTagList cooldownsNbt = nbt.getTagList(COOLDOWNS_NBT_TAG, 10);
		for (int i = 0; i < cooldownsNbt.tagCount(); i++) {
			NBTTagCompound stateCooldowns = cooldownsNbt.getCompoundTagAt(i);
			short state = stateCooldowns.getShort("state");
			NBTTagList cooldownsList = stateCooldowns.getTagList("list", 10);
			for (int j = 0; j < cooldownsList.tagCount(); j++) {
				NBTTagCompound cooldownNbt = cooldownsList.getCompoundTagAt(j);
				long expiry = cooldownNbt.getLong("expiry");
				if(expiry <= System.currentTimeMillis())
					continue;
				short attacker = cooldownNbt.getShort("attacker");
				setOccupationCooldownExpiry(attacker, state, expiry);
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTTagCompound nbt = new NBTTagCompound();
		NBTTagList cooldownsNbt = new NBTTagList();
		cooldowns.forEach(
				(state, cooldowns) -> {
					NBTTagCompound stateCooldowns = new NBTTagCompound();
					stateCooldowns.setShort("state", state);
					NBTTagList cooldownsListNbt = new NBTTagList();
					cooldowns.forEach(
							(attacker, expiry) -> {
								if(expiry < System.currentTimeMillis())
									return;
								NBTTagCompound cooldownNbt = new NBTTagCompound();
								cooldownNbt.setShort("attacker", attacker);
								cooldownNbt.setLong("expiry", expiry);
								cooldownsListNbt.appendTag(cooldownNbt);
							}
					);
					if(cooldownsListNbt.isEmpty())
						return;
					stateCooldowns.setTag("list", cooldownsListNbt);

					cooldownsNbt.appendTag(stateCooldowns);
				}
		);
		nbt.setTag(COOLDOWNS_NBT_TAG, cooldownsNbt);
		return nbt;
	}
}
