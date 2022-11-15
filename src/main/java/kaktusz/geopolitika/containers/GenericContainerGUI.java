package kaktusz.geopolitika.containers;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class GenericContainerGUI extends GuiContainer {

	private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");

	private final ITextComponent title;
	private final GenericContainer container;

	public GenericContainerGUI(ITextComponent title, GenericContainer container) {
		super(container);
		int inventoryRows = container.inventory.getSlots()/9;
		this.ySize = 114 + inventoryRows * 18;
		this.title = title;
		this.container = container;

		allowUserInput = false;
	}

	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
		this.fontRenderer.drawString(title.getUnformattedText(), 8, 6, 4210752);
		this.fontRenderer.drawString(container.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
	{
		int inventoryRows = container.inventory.getSlots()/9;
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
		int i = (this.width - this.xSize) / 2;
		int j = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(i, j, 0, 0, this.xSize, inventoryRows * 18 + 17);
		this.drawTexturedModalRect(i, j + inventoryRows * 18 + 17, 0, 126, this.xSize, 96);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		renderHoveredToolTip(mouseX, mouseY);
	}
}
