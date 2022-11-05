package kaktusz.geopolitika.integration;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.util.GTUtility;
import gregtech.api.worldgen.config.FillerConfigUtils;
import gregtech.api.worldgen.config.OreDepositDefinition;
import gregtech.api.worldgen.config.WorldGenRegistry;
import gregtech.api.worldgen.filler.BlockFiller;
import gregtech.api.worldgen.filler.FillerEntry;
import gregtech.common.pipelike.cable.tile.TileEntityCable;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import kaktusz.geopolitika.permaloaded.tileentities.ChunkResourcesMarker;
import kaktusz.geopolitika.permaloaded.tileentities.LabourMachineGT;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.Loader;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

public class GTCEuIntegration {
	private static boolean gtceuLoaded = false;

	/**
	 * Stolen and adapted from https://github.com/GregTechCEu/GregTech/blob/master/src/main/java/gregtech/integration/jei/GTOreInfo.java
	 */
	private static class GTOreInfo {
		private final OreDepositDefinition definition;
		private final String name;
		private final int spawnWeight;
		private final List<ItemStack> ores = new ArrayList<>();
		private final List<Integer> oreWeights = new ArrayList<>();

		public GTOreInfo(OreDepositDefinition definition) {
			this.definition = definition;

			//Get the Name and trim unneeded information
			if (definition.getAssignedName() == null) {
				this.name = makePrettyName(definition.getDepositName());
			} else {
				this.name = definition.getAssignedName();
			}

			this.spawnWeight = definition.getWeight();

			BlockFiller blockFiller = definition.getBlockFiller();

			FillerEntry entry = blockFiller.getAllPossibleStates().get(0);
			if (entry instanceof FillerConfigUtils.LayeredFillerEntry) {
				FillerConfigUtils.LayeredFillerEntry layeredEntry = (FillerConfigUtils.LayeredFillerEntry)entry;

				FillerEntry subEntry = layeredEntry.getPrimary();
				ores.add(getDefaultOre(subEntry));
				oreWeights.add(35);

				subEntry = layeredEntry.getSecondary();
				ores.add(getDefaultOre(subEntry));
				oreWeights.add(30);

				subEntry = layeredEntry.getBetween();
				ores.add(getDefaultOre(subEntry));
				oreWeights.add(25);

				subEntry = layeredEntry.getSporadic();
				ores.add(getDefaultOre(subEntry));
				oreWeights.add(10);
			}
		}

		private ItemStack getDefaultOre(FillerEntry entry) {
			IBlockState oreState = getOreState(entry);
			ItemStack itemStack = GTUtility.toItem(oreState);
			itemStack.setItemDamage(0);
			return itemStack;
		}

		private IBlockState getOreState(FillerEntry entry) {
			Optional<IBlockState> first = entry.getPossibleResults().stream().findFirst();
			return first.orElse(null);
		}

		public String makePrettyName(String name) {
			FileSystem fs = FileSystems.getDefault();
			String separator = fs.getSeparator();

			//Remove the leading "folderName\"
			String[] tempName = name.split(Matcher.quoteReplacement(separator));
			//Take the last entry in case of nested folders
			String newName = tempName[tempName.length - 1];
			//Remove the ".json"
			tempName = newName.split("\\.");
			//Take the first entry
			newName = tempName[0];
			//Replace all "_" with a space
			newName = newName.replaceAll("_", " ");
			//Capitalize the first letter
			newName = newName.substring(0, 1).toUpperCase() + newName.substring(1);

			return newName;
		}
	}

	public static void postInit() {
		gtceuLoaded = Loader.isModLoaded("gregtech");

		if (!gtceuLoaded)
			return;

		//make GT machines require labour
		PermaloadedSavedData.entityFactory.put(
				LabourMachineGT.ID, LabourMachineGT::new
		);

		//create resource deposits for each GT ore
		Geopolitika.logger.info("Num deposits: " + WorldGenRegistry.getOreDeposits().size());
		for (OreDepositDefinition oreDeposit : WorldGenRegistry.getOreDeposits()) {
			if(!oreDeposit.isVein())
				continue;

			GTOreInfo oreInfo = new GTOreInfo(oreDeposit);
			if(oreInfo.ores.size() == 0)
				continue;

			String veinName = oreInfo.name;
			if(veinName.endsWith(" vein"))
				veinName = veinName.replace(" vein", "");
			ChunkResourcesMarker.ResourcePreset preset = new ChunkResourcesMarker.ResourcePreset(veinName);
			for (int i = 0; i < oreInfo.ores.size(); i++) {
				preset.add(oreInfo.ores.get(i), oreInfo.oreWeights.get(i));
			}
			Geopolitika.logger.info("Added resource preset for GT vein " + veinName + ": " + preset.toString());
			ChunkResourcesMarker.addResourcePreset(oreInfo.spawnWeight, preset);
		}
	}

	public static boolean isGregtechLoaded() {
		return gtceuLoaded;
	}

	public static boolean isTileEntityGregtechMachine(TileEntity te) {
		return isGregtechLoaded()
				&& te.hasCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, null)
				&& !(te instanceof TileEntityCable);
	}
}
