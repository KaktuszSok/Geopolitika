package kaktusz.geopolitika.entities;

import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class SolidEntityPart extends MultiPartEntityPart {
	public SolidEntityPart(World world) {
		super(null, "ERROR", 1f, 1f);
	}

	public SolidEntityPart(IEntityMultiPart parent, String partName, float width, float height) {
		super(parent, partName, width, height);
		this.noClip = true;
	}

	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox() {
		return getEntityBoundingBox();
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}
}
