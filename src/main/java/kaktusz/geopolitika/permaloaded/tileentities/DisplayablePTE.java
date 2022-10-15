package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.integration.PTEDisplay;

import javax.annotation.Nullable;

/**
 * A PermaloadedTileEntity which will show up on players' maps.
 */
public interface DisplayablePTE extends PTEInterface {
	@Nullable
	PTEDisplay getDisplay();
}
