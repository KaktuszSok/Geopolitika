package kaktusz.geopolitika.integration;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.states.ClientStatesManager;
import kaktusz.geopolitika.states.CommonStateInfo;
import kaktusz.geopolitika.util.ReflectionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import xaero.common.IXaeroMinimap;
import xaero.common.XaeroMinimapSession;
import xaero.common.graphics.ImprovedFramebuffer;
import xaero.common.interfaces.render.InterfaceRenderer;
import xaero.common.minimap.MinimapInterface;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.radar.MinimapRadar;
import xaero.common.minimap.radar.MinimapRadarList;
import xaero.common.minimap.radar.category.EntityRadarCategory;
import xaero.common.minimap.radar.category.setting.EntityRadarCategorySettings;
import xaero.common.minimap.region.MinimapChunk;
import xaero.common.minimap.region.MinimapTile;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.radar.EntityIconManager;
import xaero.common.minimap.waypoints.render.WaypointsGuiRenderer;
import xaero.common.misc.Misc;
import xaero.common.misc.OptimizedMath;
import xaero.common.settings.ModSettings;
import xaero.minimap.XaeroMinimap;

import java.lang.reflect.Field;
import java.util.Iterator;

@SideOnly(Side.CLIENT)
public class XaeroMinimapIntegration {

	public static boolean enabled = false;

	public static void postInit() {
		enabled = Loader.isModLoaded("xaerominimapfair");

		if(!enabled)
			return;

		MinimapInterface minimapInterface = XaeroMinimap.instance.getInterfaces().getMinimapInterface();
		try {
			Field fboRendererField = minimapInterface.getClass().getDeclaredField("minimapFBORenderer");
			fboRendererField.setAccessible(true);
			MinimapFBORendererWithClaims rendererWithClaims = new MinimapFBORendererWithClaims(
					XaeroMinimap.instance, Minecraft.getMinecraft(), minimapInterface.getWaypointsGuiRenderer(), minimapInterface
			);
			fboRendererField.set(minimapInterface, rendererWithClaims);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static class MinimapFBORendererWithClaims extends MinimapFBORenderer {

		protected ImprovedFramebuffer scalingFramebuffer;
		protected ImprovedFramebuffer rotationFramebuffer;
		protected EntityIconManager entityIconManager;

		public MinimapFBORendererWithClaims(IXaeroMinimap modMain, Minecraft mc, WaypointsGuiRenderer waypointsGuiRenderer, MinimapInterface minimapInterface) {
			super(modMain, mc, waypointsGuiRenderer, minimapInterface);
		}

		@Override
		public void loadFrameBuffer(MinimapProcessor minimapProcessor) {
			super.loadFrameBuffer(minimapProcessor);
			//get references to private buffers
			try {
				rotationFramebuffer = ReflectionUtils.getPrivateField(this, MinimapFBORenderer.class, "rotationFramebuffer");
				scalingFramebuffer = ReflectionUtils.getPrivateField(this, MinimapFBORenderer.class, "scalingFramebuffer");
				entityIconManager = ReflectionUtils.getPrivateField(this, MinimapFBORenderer.class, "entityIconManager");

				Geopolitika.logger.info("Private buffers retrieved!");
			} catch (NoSuchFieldException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void renderChunksToFBO(XaeroMinimapSession minimapSession, MinimapProcessor minimap, EntityPlayer player, Entity renderEntity, int bufferSize, int viewW, float sizeFix, float partial, int level, boolean retryIfError, boolean useWorldMap, boolean lockedNorth, int shape, double ps, double pc, boolean cave, boolean circle) {
			ScaledResolution scaledRes = new ScaledResolution(this.mc);
			double maxVisibleLength = !lockedNorth && shape != 1 ? (double)viewW * Math.sqrt(2.0D) : (double)viewW;
			double halfMaxVisibleLength = maxVisibleLength / 2.0D;
			double radiusBlocks = maxVisibleLength / 2.0D / this.zoom;
			double playerX = minimap.getEntityRadar().getEntityX(renderEntity, partial);
			double playerZ = minimap.getEntityRadar().getEntityZ(renderEntity, partial);
			int xFloored = OptimizedMath.myFloor(playerX);
			int zFloored = OptimizedMath.myFloor(playerZ);
			int playerChunkX = xFloored >> 6;
			int playerChunkZ = zFloored >> 6;
			int offsetX = xFloored & 63;
			int offsetZ = zFloored & 63;
			boolean zooming = (double)((int)this.zoom) != this.zoom;
			this.scalingFramebuffer.bindFramebuffer(true);
			GL11.glClear(16640);
			GlStateManager.enableTexture2D();
			RenderHelper.disableStandardItemLighting();
			long before = System.currentTimeMillis();
			GlStateManager.clear(256);
			GlStateManager.matrixMode(5889);
			this.helper.defaultOrtho();
			GlStateManager.matrixMode(5888);
			GL11.glPushMatrix();
			GlStateManager.loadIdentity();
			before = System.currentTimeMillis();
			double xInsidePixel = minimap.getEntityRadar().getEntityX(renderEntity, partial) - (double)xFloored;
			if (xInsidePixel < 0.0D) {
				++xInsidePixel;
			}

			double zInsidePixel = minimap.getEntityRadar().getEntityZ(renderEntity, partial) - (double)zFloored;
			if (zInsidePixel < 0.0D) {
				++zInsidePixel;
			}

			float halfWView = (float)viewW / 2.0F;
			float angle = (float)(90.0D - this.getRenderAngle(lockedNorth));
			GlStateManager.enableBlend();
			GlStateManager.translate(256.0F, 256.0F, -2000.0F);
			GlStateManager.scale(this.zoom, this.zoom, 1.0D);
			Gui.drawRect(-256, -256, 256, 256, -16777216);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			float chunkGridAlphaMultiplier = 1.0F;
			int minX = playerChunkX + (int)Math.floor(((double)offsetX - radiusBlocks) / 64.0D);
			int minZ = playerChunkZ + (int)Math.floor(((double)offsetZ - radiusBlocks) / 64.0D);
			int maxX = playerChunkX + (int)Math.floor(((double)(offsetX + 1) + radiusBlocks) / 64.0D);
			int maxZ = playerChunkZ + (int)Math.floor(((double)(offsetZ + 1) + radiusBlocks) / 64.0D);
			int grid;
			int r;
			int g;
			int b;
			boolean slimeChunks;
			int X;
			int drawZ;
			int t;
			int slimeDrawX;
			int slimeDrawZ;
			if (useWorldMap) {
				chunkGridAlphaMultiplier = this.modMain.getSupportMods().worldmapSupport.getMinimapBrightness();
				this.modMain.getSupportMods().worldmapSupport.drawMinimap(minimapSession, this.helper, xFloored, zFloored, minX, minZ, maxX, maxZ, zooming, this.zoom);
			} else if (minimap.getMinimapWriter().getLoadedBlocks() != null && level >= 0) {
				grid = minimap.getMinimapWriter().getLoadedLevels();
				chunkGridAlphaMultiplier = grid <= 1 ? 1.0F : 0.375F + 0.625F * (1.0F - (float)level / (float)(grid - 1));
				r = minimap.getMinimapWriter().getLoadedMapChunkX();
				g = minimap.getMinimapWriter().getLoadedMapChunkZ();
				b = minimap.getMinimapWriter().getLoadedBlocks().length;
				slimeChunks = this.modMain.getSettings().getSlimeChunks(minimapSession.getWaypointsManager());
				minX = Math.max(minX, r);
				minZ = Math.max(minZ, g);
				maxX = Math.min(maxX, r + b - 1);
				maxZ = Math.min(maxZ, g + b - 1);

				for(X = minX; X <= maxX; ++X) {
					int canvasX = X - minimap.getMinimapWriter().getLoadedMapChunkX();

					for(int Z = minZ; Z <= maxZ; ++Z) {
						int canvasZ = Z - minimap.getMinimapWriter().getLoadedMapChunkZ();
						MinimapChunk mchunk = minimap.getMinimapWriter().getLoadedBlocks()[canvasX][canvasZ];
						if (mchunk != null) {
							mchunk.bindTexture(level);
							if (mchunk.isHasSomething() && level < mchunk.getLevelsBuffered() && mchunk.getGlTexture(level) != 0) {
								if (!zooming) {
									GL11.glTexParameteri(3553, 10240, 9728);
								} else {
									GL11.glTexParameteri(3553, 10240, 9729);
								}

								int drawX = (X - playerChunkX) * 64 - offsetX;
								drawZ = (Z - playerChunkZ) * 64 - offsetZ;
								GlStateManager.enableBlend();
								GL14.glBlendFuncSeparate(770, 771, 1, 771);
								this.helper.drawMyTexturedModalRect((float)drawX, (float)drawZ, 0, 64, 64.0F, 64.0F, -64.0F, 64.0F);
								GL11.glTexParameteri(3553, 10240, 9728);
								if (slimeChunks) {
									for(t = 0; t < 16; ++t) {
										if (mchunk.getTile(t % 4, t / 4) != null && mchunk.getTile(t % 4, t / 4).isSlimeChunk()) {
											slimeDrawX = drawX + 16 * (t % 4);
											slimeDrawZ = drawZ + 16 * (t / 4);
											Gui.drawRect(slimeDrawX, slimeDrawZ, slimeDrawX + 16, slimeDrawZ + 16, -2142047936);
										}
									}
								}
								renderClaimsOverlay(minimapSession, mchunk, drawX, drawZ);

								GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
							}
						}
					}
				}

				GL14.glBlendFuncSeparate(770, 771, 1, 0);
			}

			if (this.modMain.getSettings().chunkGrid > -1) {
				GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
				grid = ModSettings.COLORS[this.modMain.getSettings().chunkGrid];
				r = grid >> 16 & 255;
				g = grid >> 8 & 255;
				b = grid & 255;
				Tessellator tessellator = Tessellator.getInstance();
				BufferBuilder vertexBuffer = tessellator.getBuffer();
				vertexBuffer.begin(1, DefaultVertexFormats.POSITION_COLOR);
				GlStateManager.disableTexture2D();
				GlStateManager.enableBlend();
				float red = (float)r / 255.0F;
				float green = (float)g / 255.0F;
				float blue = (float)b / 255.0F;
				float alpha = 0.8F;
				red *= chunkGridAlphaMultiplier;
				green *= chunkGridAlphaMultiplier;
				blue *= chunkGridAlphaMultiplier;
				GlStateManager.glLineWidth(1.0F);
				drawZ = (int)Math.ceil(this.zoom);

				float lineZ;
				for(t = minX; t <= maxX; ++t) {
					slimeDrawX = (t - playerChunkX + 1) * 64 - offsetX;

					for(slimeDrawZ = 0; slimeDrawZ < 4; ++slimeDrawZ) {
						lineZ = (float)slimeDrawX + (float)(-16 * slimeDrawZ);
						this.helper.addColoredLineToExistingBuffer(vertexBuffer, lineZ, -((float)halfMaxVisibleLength), lineZ, (float)halfMaxVisibleLength + (float)drawZ, red, green, blue, alpha);
					}
				}

				for(t = minZ; t <= maxZ; ++t) {
					slimeDrawX = (t - playerChunkZ + 1) * 64 - offsetZ;

					for(slimeDrawZ = 0; slimeDrawZ < 4; ++slimeDrawZ) {
						lineZ = (float)slimeDrawX + (float)((double)(-16 * slimeDrawZ) - 1.0D / this.zoom);
						this.helper.addColoredLineToExistingBuffer(vertexBuffer, -((float)halfMaxVisibleLength), lineZ, (float)halfMaxVisibleLength + (float)drawZ, lineZ, red, green, blue, alpha);
					}
				}

				tessellator.draw();
				GlStateManager.disableBlend();
				GlStateManager.enableTexture2D();
				GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
			}

			this.scalingFramebuffer.unbindFramebuffer();
			this.rotationFramebuffer.bindFramebuffer(false);
			GL11.glClear(16640);
			this.scalingFramebuffer.bindFramebufferTexture();
			GlStateManager.loadIdentity();
			if (this.modMain.getSettings().getAntiAliasing()) {
				GL11.glTexParameteri(3553, 10240, 9729);
				GL11.glTexParameteri(3553, 10241, 9729);
			} else {
				GL11.glTexParameteri(3553, 10240, 9728);
				GL11.glTexParameteri(3553, 10241, 9728);
			}

			GlStateManager.translate(halfWView, halfWView, -2980.0F);
			GL11.glPushMatrix();
			if (!lockedNorth) {
				GL11.glRotatef(-angle, 0.0F, 0.0F, 1.0F);
			}

			GlStateManager.translate(-xInsidePixel * this.zoom, -zInsidePixel * this.zoom, 0.0D);
			GlStateManager.disableBlend();
			GlStateManager.color(1.0F, 1.0F, 1.0F, (float)(this.modMain.getSettings().minimapOpacity / 100.0D));
			this.helper.drawMyTexturedModalRect(-256.0F, -256.0F, 0, 0, 512.0F, 512.0F, 512.0F);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GL11.glPopMatrix();
			before = System.currentTimeMillis();
			GlStateManager.disableAlpha();
			GlStateManager.alphaFunc(516, 1.0F);
			GlStateManager.disableBlend();
			GlStateManager.tryBlendFuncSeparate(770, 771, 1, 1);
			GlStateManager.depthFunc(519);
			GlStateManager.depthFunc(515);
			GlStateManager.depthMask(false);
			GlStateManager.depthMask(true);
			GlStateManager.bindTexture(1);
			GlStateManager.bindTexture(0);
			this.mc.getTextureManager().bindTexture(InterfaceRenderer.guiTextures);
			if (this.modMain.getSettings().getSmoothDots()) {
				GL11.glTexParameteri(3553, 10240, 9729);
				GL11.glTexParameteri(3553, 10241, 9729);
			} else {
				GL11.glTexParameteri(3553, 10240, 9728);
				GL11.glTexParameteri(3553, 10241, 9728);
			}

			GlStateManager.enableAlpha();
			GlStateManager.alphaFunc(516, 0.0F);
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
			EntityPlayer p = player;
			ModSettings settings = this.modMain.getSettings();
			boolean smoothDots = settings.getSmoothDots();
			boolean debugEntityIcons = settings.debugEntityIcons;
			slimeChunks = settings.debugEntityVariantIds;
			X = settings.getDotsStyle();
			MinimapRadar minimapRadar = minimap.getEntityRadar();
			boolean reversedOrder = ModSettings.keyReverseEntityRadar.isKeyDown();
			this.entityIconManager.allowPrerender();
			Iterator entityLists = minimapRadar.getRadarListsIterator();

			while(entityLists.hasNext()) {
				MinimapRadarList entityList = (MinimapRadarList)entityLists.next();
				EntityRadarCategory entityCategory = entityList.getCategory();
				drawZ = ((Double)entityCategory.getSettingValue(EntityRadarCategorySettings.ICONS)).intValue();
				t = ((Double)entityCategory.getSettingValue(EntityRadarCategorySettings.NAMES)).intValue();
				double nameScale = settings.getDotNameScale();
				double iconScale = (Double)entityCategory.getSettingValue(EntityRadarCategorySettings.ICON_SCALE);
				int dotSize = ((Double)entityCategory.getSettingValue(EntityRadarCategorySettings.DOT_SIZE)).intValue();
				int heightLimit = ((Double)entityCategory.getSettingValue(EntityRadarCategorySettings.HEIGHT_LIMIT)).intValue();
				boolean heightBasedFade = (Boolean)entityCategory.getSettingValue(EntityRadarCategorySettings.HEIGHT_FADE);
				int startFadingAt = ((Double)entityCategory.getSettingValue(EntityRadarCategorySettings.START_FADING_AT)).intValue();
				boolean displayNameWhenIconFails = (Boolean)entityCategory.getSettingValue(EntityRadarCategorySettings.ICON_NAME_FALLBACK);
				boolean alwaysNameTags = (Boolean)entityCategory.getSettingValue(EntityRadarCategorySettings.ALWAYS_NAMETAGS);
				int colorIndex = ((Double)entityCategory.getSettingValue(EntityRadarCategorySettings.COLOR)).intValue();
				this.renderEntityListToFBO(minimap, p, renderEntity, entityList.getEntities(), ps, pc, playerX, playerZ, partial, t, drawZ, alwaysNameTags, minimapRadar, lockedNorth, X, smoothDots, debugEntityIcons, slimeChunks, cave, nameScale, displayNameWhenIconFails, heightLimit, heightBasedFade, startFadingAt, iconScale, dotSize, colorIndex, entityCategory, reversedOrder);
			}

			if (settings.compassLocation == 1) {
				int compassScale = settings.getCompassScale();
				this.waypointsGuiRenderer.drawCompass(this.helper, viewW / 2 - 7 - 3 * compassScale, viewW / 2 - 7 - 3 * compassScale, ps, pc, 1.0D, circle, (float)compassScale, true);
			}

			this.mc.getTextureManager().bindTexture(InterfaceRenderer.guiTextures);
			GL11.glTexParameteri(3553, 10240, 9728);
			GL11.glTexParameteri(3553, 10241, 9728);
			GlStateManager.disableAlpha();
			GlStateManager.alphaFunc(516, 0.1F);
			this.rotationFramebuffer.unbindFramebuffer();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.disableBlend();
			GlStateManager.matrixMode(5889);
			Misc.minecraftOrtho(scaledRes);
			GlStateManager.matrixMode(5888);
			GL11.glPopMatrix();
		}

		protected void renderClaimsOverlay(XaeroMinimapSession session, MinimapChunk mchunk, int drawX, int drawZ) {
			World world = session.getMinimapProcessor().mainWorld;
			for(int t = 0; t < 16; ++t) {
				MinimapTile tile = mchunk.getTile(t % 4, t / 4);
				if(tile == null)
					continue;
				CommonStateInfo owner = ClientStatesManager.getChunkOwner(tile.getX(), tile.getZ(), world);
				if (owner.isValid()) {
					int claimDrawX = drawX + 16 * (t % 4);
					int claimDrawZ = drawZ + 16 * (t / 4);
					MinimapIntegrationHelper.drawChunkClaim(claimDrawX, claimDrawZ, owner, tile.getX(), tile.getZ(), world);
				}
			}
		}
	}
}
