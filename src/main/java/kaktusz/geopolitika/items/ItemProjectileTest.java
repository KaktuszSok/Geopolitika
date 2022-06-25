package kaktusz.geopolitika.items;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.init.ModItems;
import kaktusz.geopolitika.permaloaded.projectiles.AimHelper;
import kaktusz.geopolitika.permaloaded.projectiles.ProjectileManager;
import kaktusz.geopolitika.permaloaded.projectiles.ShellProjectile;
import kaktusz.geopolitika.util.IHasModel;
import kaktusz.geopolitika.util.MessageUtils;
import kaktusz.geopolitika.util.NoPossibleSolutionsException;
import kaktusz.geopolitika.util.WorldUtils;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class ItemProjectileTest extends Item implements IHasModel {

	private double shootVelocity = 5d;

	public ItemProjectileTest(String name, CreativeTabs tab) {
		setTranslationKey(name);
		setRegistryName(name);
		setCreativeTab(tab);

		ModItems.ITEMS.add(this);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		if(worldIn.isRemote)
			return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));

		if(playerIn.isSneaking()) {
			return shiftRightClick(worldIn, playerIn, handIn);
		}

		return rightClick(worldIn, playerIn, handIn);
	}

	private ActionResult<ItemStack> rightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		ItemStack itemStack = playerIn.getHeldItem(handIn);
		if(!itemStack.hasTagCompound())
			itemStack.setTagCompound(new NBTTagCompound());

		@SuppressWarnings("ConstantConditions")
		BlockPos target = NBTUtil.getPosFromTag(itemStack.getTagCompound().getCompoundTag("target"));
		Vec3d vel;
		try {
			vel = AimHelper.calculateVelocity(
					playerIn.getPositionEyes(0),
					new Vec3d(target).add(0.5d, 1.0d, 0.5d),
					shootVelocity,
					0.05d,
					false
			);
		} catch (NoPossibleSolutionsException e) {
			MessageUtils.sendErrorMessage(playerIn,"target_too_far");
			return new ActionResult<>(EnumActionResult.FAIL, playerIn.getHeldItem(handIn));
		}
		Geopolitika.logger.info(vel);
		shootProjectile((WorldServer) worldIn, playerIn.getPositionEyes(0), vel, playerIn);

		return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
	}

	private ActionResult<ItemStack> shiftRightClick (World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		RayTraceResult trace = WorldUtils.rayTraceBlocksDontLoad(
				worldIn,
				playerIn.getPositionEyes(0),
				playerIn.getPositionEyes(0).add(playerIn.getLookVec().scale(512d)),
				false,
				true,
				false
		);
		if(trace == null)
			return new ActionResult<>(EnumActionResult.FAIL, playerIn.getHeldItem(handIn));
		BlockPos target = trace.getBlockPos();

		ItemStack itemStack = playerIn.getHeldItem(handIn);
		if(!itemStack.hasTagCompound())
			itemStack.setTagCompound(new NBTTagCompound());
		//noinspection ConstantConditions
		itemStack.getTagCompound().setTag("target", NBTUtil.createPosTag(target));
		return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
	}

	public void shootProjectile(WorldServer world, Vec3d pos, Vec3d vel, Entity shootingEntity) {
		ShellProjectile proj = new ShellProjectile(world, pos, vel);
		proj.setShootingEntity(shootingEntity);
		ProjectileManager.get(world).addProjectile(proj);
	}

	@Override
	public void registerModels() {
		Geopolitika.PROXY.registerItemRenderer(this, 0, "inventory");
	}
}
