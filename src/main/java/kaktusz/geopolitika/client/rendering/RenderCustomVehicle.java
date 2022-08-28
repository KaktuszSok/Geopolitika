package kaktusz.geopolitika.client.rendering;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.entities.EntityCustomVehicle;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@SideOnly(Side.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RenderCustomVehicle extends Render<EntityCustomVehicle> {

	public RenderCustomVehicle(RenderManager renderManager) {
		super(renderManager);
	}

	@Nullable
	@Override
	protected ResourceLocation getEntityTexture(EntityCustomVehicle entity) {
		return TextureMap.LOCATION_BLOCKS_TEXTURE;
	}

	@Override
	public void doRender(EntityCustomVehicle entity, double x, double y, double z, float entityYaw, float partialTicks) {
		for (Map.Entry<Vec3i, IBlockState> kvp : entity.getStates().entrySet()) {
			Vec3i localPos = kvp.getKey();
			renderBlock(
					entity,
					kvp.getValue(),
					x, y, z,
					localPos,
					entityYaw, 0f, 0f,
					partialTicks
			);
		}
	}

	public void renderBlock(EntityCustomVehicle entity, IBlockState iblockstate, double x, double y, double z, Vec3i offset, float entityYaw, float entityPitch, float entityRoll, float partialTicks)
	{
		if (iblockstate.getRenderType() == EnumBlockRenderType.MODEL)
		{
			World world = entity.world;

			if (iblockstate != world.getBlockState(new BlockPos(entity)) && iblockstate.getRenderType() != EnumBlockRenderType.INVISIBLE)
			{
				this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
				GlStateManager.pushMatrix();
				GlStateManager.disableLighting();
				Tessellator tessellator = Tessellator.getInstance();
				BufferBuilder bufferbuilder = tessellator.getBuffer();

				if (this.renderOutlines)
				{
					GlStateManager.enableColorMaterial();
					GlStateManager.enableOutlineMode(this.getTeamColor(entity));
				}

				bufferbuilder.begin(7, DefaultVertexFormats.BLOCK);
				BlockPos blockpos = new BlockPos(
						entity.posX + offset.getX(),
						entity.posY + offset.getY(),
						entity.posZ + offset.getZ());
				GlStateManager.translate(
						(float)(x - (double)(blockpos.getX() - offset.getX()) - 0.5D),
						(float)(y - (double)(blockpos.getY() - offset.getY())),
						(float)(z - (double)(blockpos.getZ() - offset.getZ()) - 0.5D));
				BlockRendererDispatcher blockrendererdispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
				blockrendererdispatcher.getBlockModelRenderer().renderModel(world, blockrendererdispatcher.getModelForState(iblockstate), iblockstate, blockpos, bufferbuilder, false, MathHelper.getPositionRandom(offset));
				tessellator.draw();

				if (this.renderOutlines)
				{
					GlStateManager.disableOutlineMode();
					GlStateManager.disableColorMaterial();
				}

				GlStateManager.enableLighting();
				GlStateManager.popMatrix();
			}
		}
	}
}
