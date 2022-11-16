package kaktusz.geopolitika.states;

import kaktusz.geopolitika.Geopolitika;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StatesSavedData extends WorldSavedData {
	private static final String DATA_NAME = Geopolitika.MODID + "_statesData";

	private static class StateData {
		public final short stateId;
		public long balance = 0;
		public final Map<Short, Long> cooldowns = new HashMap<>();

		public StateData(short stateId) {
			this.stateId = stateId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			StateData stateData = (StateData) o;
			return stateId == stateData.stateId;
		}

		@Override
		public int hashCode() {
			return Objects.hash(stateId);
		}
	}

	private final Map<Short, StateData> stateDatas = new HashMap<>();
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

	private StateData getStateData(short stateId) {
		return stateDatas.computeIfAbsent(stateId, StateData::new);
	}

	@Nullable
	private StateData getStateDataOrNull(short stateId) {
		return stateDatas.get(stateId);
	}

	public long getBalance(short stateId) {
		StateData stateData = getStateDataOrNull(stateId);
		if(stateData == null)
			return 0;
		return stateData.balance;
	}

	public void addBalance(short stateId, long amount) {
		getStateData(stateId).balance += amount;
		markDirty();
	}

	public void setOccupationCooldown(short attackersId, short defendersId, int cooldownMinutes) {
		setOccupationCooldownExpiry(attackersId, defendersId,
				System.currentTimeMillis() + ((long) cooldownMinutes*60*1000));
	}

	private void setOccupationCooldownExpiry(short attackersId, short defendersId, long expiryTime) {
		getStateData(defendersId).cooldowns.put(attackersId, expiryTime);
		markDirty();
	}

	/**
	 * Gets how long the attackers must wait until starting a new occupation of the defenders
	 */
	public long getOccupationCooldown(short attackersId, short defendersId) {
		StateData stateData = getStateDataOrNull(defendersId);
		if(stateData == null)
			return 0;
		Long cooldown_ms = stateData.cooldowns.get(attackersId);
		if(cooldown_ms == null)
			return 0;

		return cooldown_ms - System.currentTimeMillis();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		NBTTagList statesNBT = nbt.getTagList("states", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < statesNBT.tagCount(); i++) {
			NBTTagCompound stateTag = statesNBT.getCompoundTagAt(i);
			short stateId = stateTag.getShort("id");
			getStateData(stateId).balance = stateTag.getLong("balance");
			NBTTagList cooldownsList = stateTag.getTagList("cooldowns", 10);
			for (int j = 0; j < cooldownsList.tagCount(); j++) {
				NBTTagCompound cooldownNbt = cooldownsList.getCompoundTagAt(j);
				long expiry = cooldownNbt.getLong("expiry");
				if(expiry <= System.currentTimeMillis())
					continue;
				short attacker = cooldownNbt.getShort("attacker");
				setOccupationCooldownExpiry(attacker, stateId, expiry);
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTTagCompound nbt = new NBTTagCompound();
		NBTTagList statesNbt = new NBTTagList();
		stateDatas.forEach(
				(stateId, stateData) -> {
					NBTTagCompound stateTag = new NBTTagCompound();
					stateTag.setShort("id", stateId);
					stateTag.setLong("balance", stateData.balance);
					NBTTagList cooldownsListNbt = new NBTTagList();
					stateData.cooldowns.forEach(
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
					stateTag.setTag("cooldowns", cooldownsListNbt);

					statesNbt.appendTag(stateTag);
				}
		);
		nbt.setTag("states", statesNbt);
		return nbt;
	}
}
