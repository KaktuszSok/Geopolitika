package kaktusz.geopolitika.integration;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SortedSetMultimap;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.permaloaded.tileentities.PTEDisplay;
import kaktusz.geopolitika.states.ClientStatesManager;
import kaktusz.geopolitika.states.CommonStateInfo;
import kaktusz.geopolitika.util.ReflectionUtils;
import kaktusz.geopolitika.util.Vec2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import xaero.common.IXaeroMinimap;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.region.MinimapTile;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaero.minimap.XaeroMinimap;

import java.util.*;
import java.util.stream.Stream;

@Mod.EventBusSubscriber({Side.CLIENT})
@SideOnly(Side.CLIENT)
public class XaeroWorldmapIntegration {

	public static boolean enabled = false;

	public static void postInit() {
		enabled = Loader.isModLoaded("xaeroworldmap");

		//set up minimap+worldmap support
		if(!enabled || !Loader.isModLoaded("xaerominimapfair"))
			return;

		XaeroMinimap.instance.getSupportMods().worldmapSupport = new SupportXaeroWorldmapWithClaims(XaeroMinimap.instance);
	}

	@SubscribeEvent
	public static void onGuiOpen(GuiOpenEvent e) {
		if(!enabled)
			return;

		if(!(e.getGui() instanceof GuiMap) || e.getGui() instanceof XaeroWorldmapIntegration.GuiMapWithClaims)
			return; //not opening a map or opening our patched version - ignore.

		XaeroWorldmapIntegration.GuiMapWithClaims patchedGui = new XaeroWorldmapIntegration.GuiMapWithClaims(null, null, WorldMapSession.getCurrentSession().getMapProcessor(), Minecraft.getMinecraft().player);
		e.setGui(patchedGui);
	}

	public static class GuiMapWithClaims extends GuiMap {
		@SuppressWarnings("FieldMayBeFinal")
		private MapProcessor mapProcessor;
		private final Map<ChunkPos, CommonStateInfo> claimedChunksCache = new HashMap<>();
		private long nextCacheRefreshTimeMillis = 0;

		public GuiMapWithClaims(GuiScreen parent, GuiScreen escape, MapProcessor mapProcessor, Entity player) {
			super(parent, escape, mapProcessor, player);
			this.mapProcessor = mapProcessor;
		}

		@Override
		public void drawScreen(int scaledMouseX, int scaledMouseY, float partialTicks) {
			super.drawScreen(scaledMouseX, scaledMouseY, partialTicks);

			if(GuiScreen.isAltKeyDown()) { //don't draw Geopolitica overlays when holding alt
				return;
			}

			long nowTime = System.currentTimeMillis();
			double scale = getFinalScale();
			double cameraX = 0, cameraZ = 0;
			try {
				cameraX = ReflectionUtils.getPrivateField(this, GuiMap.class, "cameraX");
				cameraZ = ReflectionUtils.getPrivateField(this, GuiMap.class, "cameraZ");
			} catch (IllegalAccessException | NoSuchFieldException e) {
				e.printStackTrace();
			}
			GlStateManager.pushMatrix();
			GlStateManager.translate(width/2.0 - cameraX*scale, (double) height/mc.displayHeight/2 + height/2.0 - cameraZ*scale, 0);
			GlStateManager.scale(scale, scale, 1);
			//1. get coordinates of top-left and bottom-right blocks visible on the map
			int left = screenToBlockPos(0, cameraX, scale, width);
			int top = screenToBlockPos(0, cameraZ, scale, height);
			int right = screenToBlockPos(width, cameraX, scale, width);
			int bottom = screenToBlockPos(height, cameraZ, scale, height);
			if(nowTime >= nextCacheRefreshTimeMillis) {
				clearClaimedChunksCache();
				for (int x = left; x < right + 16; x += 16) {
					for (int z = top; z < bottom + 16; z += 16) {
						int cx = x >> 4;
						int cz = z >> 4;
						updateClaimedChunksCache(cx, cz);
					}
				}
			}
			//2. draw chunk claims
			claimedChunksCache.forEach((cpos, owner) -> {
				MinimapIntegrationHelper.drawChunkClaim(cpos.x << 4, cpos.z << 4, owner, cpos.x, cpos.z, mapProcessor.getWorld());
			});

			//3. draw PermaloadedTileEntities
			RenderHelper.enableGUIStandardItemLighting();

			boolean drawRegionTooltip = GuiScreen.isShiftKeyDown() || getUserScale() < 1.0d; //if shift is held or we are zoomed out, we want to draw region info instead of hovered PTEs tooltip.
			Vec2i closestHoveredCoord = null;
			double closestHoveredDist = Double.POSITIVE_INFINITY;

			int mouseBlockPosX = screenToBlockPos(scaledMouseX, cameraX, scale, width);
			int mouseBlockPosZ = screenToBlockPos(scaledMouseY, cameraZ, scale, height);

			//get all PTEDisplays sorted per-coordinate
			ListMultimap<Vec2i, PTEDisplay> displaysByCoord = MinimapIntegrationHelper.getSortedPTEDisplaysIncludingCached();
			for (Vec2i coord : displaysByCoord.keySet())
			{
				//only care about highest ordered display with same icon
				Set<ItemStack> rendered = new HashSet<>();
				List<PTEDisplay> displaysAtCoord = displaysByCoord.get(coord);
				for (int i = displaysAtCoord.size() - 1; i >= 0; i--) {
					PTEDisplay disp = displaysAtCoord.get(i);
					if(rendered.contains(disp.displayStack))
						continue;
					rendered.add(disp.displayStack);

					MinimapIntegrationHelper.drawPTEDisplay(disp, coord.getX(), coord.getY(), getUserScale());
					if(drawRegionTooltip)
						continue; //ignore PTE tooltips if we are drawing the region tooltip instead

					//finding closest tooltip:
					double dx = Math.abs(mouseBlockPosX - coord.getX());
					double dz = Math.abs(mouseBlockPosZ - coord.getY());
					double maxHoverDist = 16d/getUserScale();
					if(dx < maxHoverDist && dz < maxHoverDist && dx+dz <= closestHoveredDist) {
						closestHoveredCoord = coord;
						closestHoveredDist = dx+dz;
					}
				}
			}
			RenderHelper.disableStandardItemLighting();
			GlStateManager.popMatrix();

			//3.5 draw hovered tooltip
			if(drawRegionTooltip) { //draw hovered region tooltip
				ChunkPos hoveredChunk = new ChunkPos(mouseBlockPosX >> 4, mouseBlockPosZ >> 4);
				CommonStateInfo hoveredOwner = claimedChunksCache.getOrDefault(hoveredChunk, CommonStateInfo.NONE);
				MinimapIntegrationHelper.drawRegionTooltip(
						mouseBlockPosX,
						mouseBlockPosZ,
						hoveredOwner,
						mapProcessor.getWorld(),
						scaledMouseX,
						scaledMouseY,
						this
				);
			} else if(closestHoveredCoord  != null) { //draw hovered PTEs tooltip
				MinimapIntegrationHelper.drawPTEHoverText(displaysByCoord.get(closestHoveredCoord), scaledMouseX, scaledMouseY, this);
			}

			if(nowTime >= nextCacheRefreshTimeMillis) {
				nextCacheRefreshTimeMillis = nowTime + 150;
			}
		}

		protected double getFinalScale() {
			double scaledresolutionFactor = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
			return getUserScale()/getScaleMultiplier(Math.min(mc.displayWidth, mc.displayHeight))/scaledresolutionFactor;
		}

		protected double getScaleMultiplier(int screenShortSide) {
			return screenShortSide <= 1080 ? 1.0D : (double)screenShortSide / 1080.0D;
		}

		protected int screenToBlockPos(double screenPos, double camPos, double scale, int screenSize) {
			return (int)Math.floor((camPos + (screenPos - screenSize/2.0)/scale));
		}

		protected void clearClaimedChunksCache() {
			claimedChunksCache.clear();
		}

		protected void updateClaimedChunksCache(int cx, int cz) {
			CommonStateInfo owner = ClientStatesManager.getChunkOwner(cx, cz, mapProcessor.getWorld());
			if(!owner.isValid())
				return;
			claimedChunksCache.put(new ChunkPos(cx, cz), owner);
		}
	}

	public static class SupportXaeroWorldmapWithClaims extends SupportXaeroWorldmap {

		private static HashMap<MapTileChunk, Long> seedsUsed;
		@SuppressWarnings("FieldMayBeFinal")
		private IXaeroMinimap modMain;

		public SupportXaeroWorldmapWithClaims(IXaeroMinimap modMain) {
			super(modMain);
			this.modMain = modMain;
			try {
				seedsUsed = ReflectionUtils.getPrivateField(null, SupportXaeroWorldmap.class, "seedsUsed");
			} catch (IllegalAccessException | NoSuchFieldException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void drawMinimap(XaeroMinimapSession minimapSession, MinimapRendererHelper helper, int xFloored, int zFloored, int minViewX, int minViewZ, int maxViewX, int maxViewZ, boolean zooming, double zoom) {
			WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
			if (worldmapSession != null) {
				MapProcessor mapProcessor = worldmapSession.getMapProcessor();
				synchronized(mapProcessor.renderThreadPauseSync) {
					if (!mapProcessor.isRenderingPaused()) {
						if (mapProcessor.getCurrentDimension() == null) {
							return;
						}

						String worldString = this.compatibilityVersion >= 7 ? mapProcessor.getCurrentWorldId() : mapProcessor.getCurrentWorldString();
						if (worldString == null) {
							return;
						}

						int mapX = xFloored >> 4;
						int mapZ = zFloored >> 4;
						int chunkX = mapX >> 2;
						int chunkZ = mapZ >> 2;
						int tileX = mapX & 3;
						int tileZ = mapZ & 3;
						int insideX = xFloored & 15;
						int insideZ = zFloored & 15;
						GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
						GlStateManager.enableBlend();
						int minX = (mapX >> 2) - 4;
						int maxX = (mapX >> 2) + 4;
						int minZ = (mapZ >> 2) - 4;
						int maxZ = (mapZ >> 2) + 4;
						float brightness = this.getMinimapBrightness();
						boolean wmUsesHashcodes = this.compatibilityVersion >= 5;
						int globalRegionCacheHashCode = wmUsesHashcodes ? WorldMap.settings.getRegionCacheHashCode() : 0;
						boolean reloadEverything = wmUsesHashcodes ? WorldMap.settings.reloadEverything : false;
						int globalReloadVersion = wmUsesHashcodes ? WorldMap.settings.reloadVersion : 0;
						boolean slimeChunks = this.modMain.getSettings().getSlimeChunks(minimapSession.getWaypointsManager());
						if (this.compatibilityVersion >= 7) {
							GL14.glBlendFuncSeparate(1, 0, 0, 1);
						}

						if (this.compatibilityVersion >= 6) {
							GuiMap.setupTextureMatricesAndTextures(brightness);
						}

						for(int i = minX; i <= maxX; ++i) {
							for(int j = minZ; j <= maxZ; ++j) {
								MapRegion region = mapProcessor.getMapRegion(i >> 3, j >> 3, mapProcessor.regionExists(i >> 3, j >> 3));
								if (region != null) {
									int drawX;
									int drawZ;
									synchronized(region) {
										drawX = wmUsesHashcodes ? region.getCacheHashCode() : 0;
										drawZ = wmUsesHashcodes ? region.getReloadVersion() : 0;
										if (!region.recacheHasBeenRequested() && !region.reloadHasBeenRequested() && (region.getLoadState() == 0 || (region.getLoadState() == 4 || region.getLoadState() == 2 && region.isBeingWritten()) && (reloadEverything && drawZ != globalReloadVersion || drawX != globalRegionCacheHashCode || region.getVersion() != mapProcessor.getGlobalVersion() || region.getLoadState() != 2 && region.shouldCache()))) {
											if (region.getLoadState() == 2) {
												region.requestRefresh(mapProcessor);
											} else {
												mapProcessor.getMapSaveLoad().requestLoad(region, "Minimap", false);
												mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
											}
										}
									}

									if (!mapProcessor.isUploadingPaused()) {
										if (this.compatibilityVersion >= 7) {
											if (region.isLoaded()) {
												mapProcessor.getMapWorld().getCurrentDimension().getMapRegions().bumpLoadedRegion(region);
											}
										} else {
											List<MapRegion> regions = mapProcessor.getMapWorld().getCurrentDimension().getMapRegionsList();
											regions.remove(region);
											regions.add(region);
										}
									}

									if (i >= minViewX && i <= maxViewX && j >= minViewZ && j <= maxViewZ) {
										MapTileChunk chunk = region.getChunk(i & 7, j & 7);
										if (chunk != null && chunk.getGlColorTexture() != -1) {
											this.bindMapTextureWithLighting(brightness, chunk, zooming);
											GL11.glTexParameterf(3553, 33082, 0.0F);
											drawX = 64 * (chunk.getX() - chunkX) - 16 * tileX - insideX;
											drawZ = 64 * (chunk.getZ() - chunkZ) - 16 * tileZ - insideZ;
											if (this.compatibilityVersion < 7) {
												GL14.glBlendFuncSeparate(770, 771, 1, 771);
												GuiMap.renderTexturedModalRectWithLighting((float)drawX, (float)drawZ, 0, 0, 64.0F, 64.0F);
											} else {
												GuiMap.renderTexturedModalRectWithLighting((float)drawX, (float)drawZ, 64.0F, 64.0F);
											}

											if (slimeChunks) {
												GuiMap.restoreTextureStates();
												if (this.compatibilityVersion >= 7) {
													GL14.glBlendFuncSeparate(770, 771, 1, 771);
												}

												Long seed = this.modMain.getSettings().getSlimeChunksSeed(minimapSession.getWaypointsManager().getCurrentContainerAndWorldID());
												Long savedSeed = (Long)seedsUsed.get(chunk);
												boolean newSeed = seed == null && savedSeed != null || seed != null && !seed.equals(savedSeed);
												if (newSeed) {
													seedsUsed.put(chunk, seed);
												}

												for(int t = 0; t < 16; ++t) {
													if (newSeed || (chunk.getTileGridsCache()[t % 4][t / 4] & 1) == 0) {
														chunk.getTileGridsCache()[t % 4][t / 4] = (byte)(1 | (MinimapTile.isSlimeChunk(this.modMain.getSettings(), chunk.getX() * 4 + t % 4, chunk.getZ() * 4 + t / 4, seed) ? 2 : 0));
													}

													if ((chunk.getTileGridsCache()[t % 4][t / 4] & 2) != 0) {
														int slimeDrawX = drawX + 16 * (t % 4);
														int slimeDrawZ = drawZ + 16 * (t / 4);
														Gui.drawRect(slimeDrawX, slimeDrawZ, slimeDrawX + 16, slimeDrawZ + 16, -2142047936);
													}
												}

												if (this.compatibilityVersion >= 6) {
													GuiMap.setupTextures(brightness);
												}

												if (this.compatibilityVersion >= 7) {
													GL14.glBlendFuncSeparate(1, 0, 0, 1);
													GlStateManager.enableBlend();
												}
											}
											renderClaimsOverlay(worldmapSession, brightness, drawX, drawZ, chunk);

											if (this.compatibilityVersion < 7) {
												GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
												GlStateManager.enableBlend();
											}
										}
									}
								}
							}
						}

						GuiMap.restoreTextureStates();
						GL14.glBlendFuncSeparate(770, 771, 1, 0);
						GlStateManager.disableBlend();
					}

				}
			}
		}

		private void renderClaimsOverlay(WorldMapSession session, float brightness, int drawX, int drawZ, MapTileChunk chunk) {
			GuiMap.restoreTextureStates();
			if (this.compatibilityVersion >= 7) {
				GL14.glBlendFuncSeparate(770, 771, 1, 771);
			}

			World world = session.getMapProcessor().getWorld();
			for(int t = 0; t < 16; ++t) {
				MapTile tile = chunk.getTile(t%4, t/4);
				if(tile == null)
					continue;
				CommonStateInfo owner = ClientStatesManager.getChunkOwner(tile.getChunkX(), tile.getChunkZ(), world);
				if (owner.isValid()) {
					int claimDrawX = drawX + 16 * (t % 4);
					int claimDrawZ = drawZ + 16 * (t / 4);
					MinimapIntegrationHelper.drawChunkClaim(claimDrawX, claimDrawZ, owner, tile.getChunkX(), tile.getChunkZ(), world);
				}
			}

			if (this.compatibilityVersion >= 6) {
				GuiMap.setupTextures(brightness);
			}

			if (this.compatibilityVersion >= 7) {
				GL14.glBlendFuncSeparate(1, 0, 0, 1);
				GlStateManager.enableBlend();
			}
		}

		private void bindMapTextureWithLighting(float brightness, MapTileChunk chunk, boolean zooming) {
			if (this.compatibilityVersion >= 7) {
				GuiMap.bindMapTextureWithLighting3(chunk, zooming ? 9729 : 9728, 0);
			} else if (this.compatibilityVersion >= 6) {
				GuiMap.bindMapTextureWithLighting3(brightness, chunk, zooming ? 9729 : 9728, 0);
			} else {
				GuiMap.bindMapTextureWithLighting(brightness, chunk, zooming ? 9729 : 9728, 0);
			}

		}
	}
}
