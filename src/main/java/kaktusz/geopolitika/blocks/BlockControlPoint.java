package kaktusz.geopolitika.blocks;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.init.ModBlocks;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.MessageUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockControlPoint extends BlockBase implements ITileEntityProvider {
	public BlockControlPoint(String name, Material material, CreativeTabs tab) {
		super(name, material, tab);
		ModBlocks.BLOCK_REGISTER_CALLBACKS.add(() -> GameRegistry.registerTileEntity(
				TileEntityControlPoint.class,
				new ResourceLocation(Geopolitika.MODID, "road_builder")
		));

		setResistance(6000000.0F);
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityControlPoint();
	}

	/**
	 * Handles the event fired when this block is placed by an entity.
	 * @param simulate If true, will only check if the placement would be valid or not. Otherwise, sets up the placed tile entity.
	 * @return Whether to allow the event or not. If false, the event should be cancelled.
	 */
	public boolean handleBlockPlacementEvent(BlockEvent.EntityPlaceEvent e, boolean sendMessageIfDisallowed, boolean simulate) {
		Entity placerEntity = e.getEntity();
		if(!(placerEntity instanceof EntityPlayerMP))
			return false;

		EntityPlayerMP player = ((EntityPlayerMP) placerEntity);
		if(!StatesManager.getPlayerState(player).isValid()) { //ensure has state
			if(sendMessageIfDisallowed) {
				MessageUtils.sendErrorMessage(placerEntity, "cp_placement_no_state");
			}
			return false;
		}

		if(!StatesManager.hasPlayerModifyClaimsAuthority(player)) { //ensure has authority
			if(sendMessageIfDisallowed) {
				MessageUtils.sendErrorMessage(placerEntity, "cp_insufficient_state_authority");
			}
			return false;
		}

		if(StatesManager.getChunkOwner(e.getPos(), e.getWorld()).isValid()) { //ensure chunk not claimed
			if(sendMessageIfDisallowed) {
				MessageUtils.sendErrorMessage(placerEntity, "cp_placement_already_claimed");
			}
			return false;
		}

		if(!simulate) {
			TileEntity te = e.getWorld().getTileEntity(e.getPos());
			if (!(te instanceof TileEntityControlPoint))
				return false;
			TileEntityControlPoint cp = (TileEntityControlPoint) te;
			cp.setOwner(StatesManager.getPlayerState(player));
			cp.claimChunks(false);
		}
		return true;
	}

	/**
	 * Handles the event fired when this block is broken by a player.
	 * @param sendMessageIfDisallowed If true, will send a message to the breaker explaining why the action is not allowed.
	 * @return Whether to allow the event or not. If false, the event should be cancelled.
	 */
	public boolean handleBlockBreakEvent(BlockEvent.BreakEvent e, boolean sendMessageIfDisallowed) {
		if(e.getWorld().isRemote)
			return true;

		TileEntity te = e.getWorld().getTileEntity(e.getPos());
		if(!(te instanceof TileEntityControlPoint))
			return true;
		TileEntityControlPoint cp = (TileEntityControlPoint) te;

		if(!cp.getOwner().isValid()) { //no owner - allow breaking
			return true;
		}

		EntityPlayerMP player = (EntityPlayerMP) e.getPlayer();
		if(!StatesManager.isPlayerInState(player, cp.getOwner())) { //ensure same state
			if(sendMessageIfDisallowed) {
				MessageUtils.sendErrorMessage(player, "cp_break_other_state");
			}
			return false;
		}

		if(!StatesManager.hasPlayerModifyClaimsAuthority(player)) { //ensure has authority
			if(sendMessageIfDisallowed) {
				MessageUtils.sendErrorMessage(player, "cp_insufficient_state_authority");
			}
			return false;
		}

		if(cp.isConflictOngoing()) { //ensure no conflict
			if(sendMessageIfDisallowed) {
				MessageUtils.sendErrorMessage(player, "cp_break_conflict_ongoing");
			}
			return false;
		}

		return true;
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
	{
		TileEntity tileentity = worldIn.getTileEntity(pos);
		if (tileentity instanceof TileEntityControlPoint)
		{
			TileEntityControlPoint cp = (TileEntityControlPoint)tileentity;
			//InventoryUtils.dropInventoryItems(worldIn, pos, cp.inventory);
			cp.onRemoveFromWorld();
		}

		super.breakBlock(worldIn, pos, state);
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(worldIn.isRemote)
			return true;

		TileEntity te = worldIn.getTileEntity(pos);
		if(!(te instanceof TileEntityControlPoint))
			return false;
		TileEntityControlPoint cp = (TileEntityControlPoint) te;
		EntityPlayerMP player = (EntityPlayerMP) playerIn;

		ForgeTeam playerState = StatesManager.getPlayerState(player);
		if(!playerState.isValid())
			return true;


		if(!cp.getOwner().isValid() //unowned control point
				&& StatesManager.hasPlayerModifyClaimsAuthority(player)) //player can modify claims of their state
		{
			cp.setOwner(playerState);
			cp.claimChunks(false);
		}
		else if(cp.getOwner().equalsTeam(playerState) //control point owned by player's state
				&& !cp.isConflictOngoing() //no conflict
				&& StatesManager.hasPlayerModifyClaimsAuthority(player)) //player can modify claims of their state
		{
			cp.claimChunks(false);
		}
		else if(cp.getOwner().equalsTeam(playerState) //control point owned by player's state
				&& cp.isConflictOngoing()) //yes conflict
		{
			cp.tryHighlightOccupiers(player);
		}
		else if(cp.isBeingOccupiedBy(playerState)) { //player is the attacker
			cp.tryActivelyOccupy(player);
		}

		return true;
	}
}
