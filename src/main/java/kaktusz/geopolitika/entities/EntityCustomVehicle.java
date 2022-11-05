package kaktusz.geopolitika.entities;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.util.Vector3d;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.MultiPartEntityPart;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EntityCustomVehicle extends Entity implements IEntityMultiPart {

	private static final DataParameter<NBTTagCompound> statesNBT = EntityDataManager.createKey(EntityCustomVehicle.class, DataSerializers.COMPOUND_TAG);

	private final Map<Vec3i, IBlockState> states = new HashMap<>();
	private final Vector3d originOffset = new Vector3d(0, 0 ,0); //final but modified in updateAABB
	private MultiPartEntityPart[] parts;
	private final Map<MultiPartEntityPart, Vec3d> partOffsets = new HashMap<>();

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
		partOffsets.forEach((p, o) -> {
			p.onUpdate();
			p.setLocationAndAngles(
					posX + o.x - originOffset.x,
					posY + o.y - originOffset.y,
					posZ + o.z - originOffset.z,
					0f, 0f);
		});

		if (motionX*motionY*motionZ != 0)
		{
			for (MultiPartEntityPart part : parts) {
				List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, part.getEntityBoundingBox());

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
		}

		this.motionX *= 0.9800000190734863D;
		this.motionY *= 0.9800000190734863D;
		this.motionZ *= 0.9800000190734863D;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		setDead();
		return true;
	}

	public boolean canBePushed()
	{
		return false;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox() {
		return null;
		//return getEntityBoundingBox();
	}

	@Nullable
	public AxisAlignedBB getCollisionBox(Entity entityIn)
	{
		return entityIn.canBePushed() ? entityIn.getEntityBoundingBox() : null;
	}

	@Override
	public boolean canBeCollidedWith() {
		//return false;
		return !isDead;
	}

	private void setStates(Map<Vec3i, IBlockState> states) {
		NBTTagCompound nbt = new NBTTagCompound();
		writeStatesNBT(nbt, states);
		dataManager.set(statesNBT, nbt);
	}

	public Map<Vec3i, IBlockState> getStates() {
		return states;
	}

	public Vector3d getOriginOffset() {
		return originOffset;
	}

	@Override
	public void notifyDataManagerChange(DataParameter<?> key) {
		if(key == statesNBT) {
			readStatesNBT(dataManager.get(statesNBT));
		}
	}

	private void updateAABB() {
		//compute aabb
		int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;
		for (Vec3i p : states.keySet()) {
			if(minX > p.getX())
				minX = p.getX();
			if(maxX < p.getX())
				maxX = p.getX();

			if(minY > p.getY())
				minY = p.getY();
			if(maxY < p.getY())
				maxY = p.getY();

			if(minZ > p.getZ())
				minZ = p.getZ();
			if(maxZ < p.getZ())
				maxZ = p.getZ();
		}
		int width = 1 + Math.max(maxX - minX, maxZ - minZ);
		//compute offset
		originOffset.x = (minX + maxX)/2d;
		originOffset.y = minY;
		originOffset.z = (minZ + maxZ)/2d;
		setSize(width, 1 + maxY - minY);
	}

	private void readStatesNBT(NBTTagCompound compound) {
		states.clear();
		partOffsets.clear();
		NBTTagList statesList = compound.getTagList("states", Constants.NBT.TAG_COMPOUND);
		List<MultiPartEntityPart> parts = new ArrayList<>(statesList.tagCount());
		for (NBTBase nbtBase : statesList) {
			NBTTagCompound stateTag = (NBTTagCompound) nbtBase;
			Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(stateTag.getString("b")));
			if(block == null)
				continue;
			Vec3i pos = new Vec3i(stateTag.getByte("x"), stateTag.getByte("y"), stateTag.getByte("z"));
			@SuppressWarnings("deprecation")
			IBlockState state = block.getStateFromMeta(stateTag.getByte("m"));
			states.put(pos, state);
			AxisAlignedBB bb = state.getCollisionBoundingBox(world, this.getPosition().add(pos));
			float blockWidth = bb == null ? 1.0F : (float)Math.max(bb.maxX - bb.minX, bb.maxZ - bb.minZ);
			double blockFloor = bb == null ? 0.0D : bb.minY;
			float blockHeight = bb == null ? 1.0F : (float)(bb.maxY - bb.minY);
			MultiPartEntityPart part = new SolidEntityPart(this, "block", blockWidth, blockHeight);
			part.setLocationAndAngles(this.posX + pos.getX(), this.posY + pos.getY() + blockFloor, this.posZ + pos.getZ(), 0f, 0f);
			parts.add(part);
			partOffsets.put(part, new Vec3d(pos.getX(), pos.getY() + blockFloor, pos.getZ()));
		}
		Geopolitika.logger.info("Read states NBT - n=" + states.size());
		this.parts = parts.toArray(new MultiPartEntityPart[0]);

		updateAABB();
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

	@Override
	public World getWorld() {
		return world;
	}

	@Nullable
	@Override
	public MultiPartEntityPart[] getParts() {
		return parts;
	}

	@Override
	public boolean attackEntityFromPart(MultiPartEntityPart part, DamageSource source, float damage) {
		attackEntityFrom(source, damage);
		return true;
	}
}
