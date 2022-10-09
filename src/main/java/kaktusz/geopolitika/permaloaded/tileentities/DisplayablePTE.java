package kaktusz.geopolitika.permaloaded.tileentities;

/**
 * A PermaloadedTileEntity which will show up on players' maps.
 */
public interface DisplayablePTE extends PTEInterface {
	PTEDisplay getDisplay();
}
