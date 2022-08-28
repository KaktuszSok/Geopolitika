package kaktusz.geopolitika.entities;

import kaktusz.geopolitika.Geopolitika;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EntityCustomVehicle extends Entity {

	private static final DataParameter<NBTTagCompound> statesNBT = EntityDataManager.createKey(EntityCustomVehicle.class, DataSerializers.COMPOUND_TAG);

	private final Map<Vec3i, IBlockState> states = new HashMap<>();

	public EntityCustomVehicle(World worldIn) {
		super(worldIn);
	}

	public EntityCustomVehicle(World world, Map<Vec3i, IBlockState> states) {
		this(world);
		setStates(states);
	}

	@Override
	protected void entityInit() {
		dataManager.register(statesNBT, new NBTTagCompound());
		this.setSize(1f, 1f);
	}

	@Override
	public void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;

		if (!this.hasNoGravity())
		{
			this.motionY -= 0.03999999910593033D;
		}

		this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
		this.doBlockCollisions();

		if (motionX*motionY*motionZ != 0)
		{
			List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getCollisionBoundingBox());

			if (!list.isEmpty())
			{
				for (Entity entity : list)
				{
					if (!entity.noClip && !(entity instanceof EntityShulker))
					{
						entity.move(MoverType.SHULKER, motionX, motionY, motionZ);
					}
				}
			}
		}

		this.motionX *= 0.9800000190734863D;
		this.motionY *= 0.9800000190734863D;
		this.motionZ *= 0.9800000190734863D;
	}

	public boolean canBePushed()
	{
		return false;
	}

	@Nonnull
	@Override
	public AxisAlignedBB getCollisionBoundingBox() {
		return getEntityBoundingBox();
	}

	@Nullable
	public AxisAlignedBB getCollisionBox(Entity entityIn)
	{
		return entityIn.canBePushed() ? entityIn.getEntityBoundingBox() : null;
	}

	private void setStates(Map<Vec3i, IBlockState> states) {
		NBTTagCompound nbt = new NBTTagCompound();
		writeStatesNBT(nbt, states);
		dataManager.set(statesNBT, nbt);
	}

	public Map<Vec3i, IBlockState> getStates() {
		return states;
	}

	@Override
	public void notifyDataManagerChange(DataParameter<?> key) {
		if(key == statesNBT) {
			readStatesNBT(dataManager.get(statesNBT));
		}
	}

	@Override
	public boolean canBeCollidedWith() {
		return !isDead;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		setDead();
		return true;
	}

	private void readStatesNBT(NBTTagCompound compound) {
		states.clear();
		NBTTagList statesList = compound.getTagList("states", Constants.NBT.TAG_COMPOUND);
		for (NBTBase nbtBase : statesList) {
			NBTTagCompound stateTag = (NBTTagCompound) nbtBase;
			Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(stateTag.getString("b")));
			if(block == null)
				continue;
			Vec3i pos = new Vec3i(stateTag.getByte("x"), stateTag.getByte("y"), stateTag.getByte("z"));
			@SuppressWarnings("deprecation")
			IBlockState state = block.getStateFromMeta(stateTag.getByte("m"));
			states.put(pos, state);
		}
		Geopolitika.logger.info("Read states NBT - n=" + states.size());
	}

	private void writeStatesNBT(NBTTagCompound compound, Map<Vec3i, IBlockState> states) {
		NBTTagList statesList = new NBTTagList();

		for (Map.Entry<Vec3i, IBlockState> kvp : states.entrySet()) {
			NBTTagCompound stateTag = new NBTTagCompound();
			Vec3i pos = kvp.getKey();
			stateTag.setByte("x", (byte)pos.getX());
			stateTag.setByte("y", (byte)pos.getY());
			stateTag.setByte("z", (byte)pos.getZ());
			try {
				stateTag.setString("b", Objects.requireNonNull(kvp.getValue().getBlock().getRegistryName()).toString());
			} catch (NullPointerException e) {
				continue;
			}
			stateTag.setByte("m", (byte)kvp.getValue().getBlock().getMetaFromState(kvp.getValue()));
			statesList.appendTag(stateTag);
		}

		compound.setTag("states", statesList);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
		NBTTagCompound justStates = new NBTTagCompound();
		justStates.setTag("states", compound.getTag("states"));
		Geopolitika.logger.info("Read vehicle from NBT - " + compound.toString());
		dataManager.set(statesNBT, justStates);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
		writeStatesNBT(compound, states);
	}
}
