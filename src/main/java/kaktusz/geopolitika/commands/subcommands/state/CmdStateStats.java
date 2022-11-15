package kaktusz.geopolitika.commands.subcommands.state;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.util.FinalIDObject;
import kaktusz.geopolitika.commands.CommandAssertions;
import kaktusz.geopolitika.commands.CommandPermissions;
import kaktusz.geopolitika.commands.subcommands.Subcommand;
import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import kaktusz.geopolitika.permaloaded.tileentities.LabourConsumer;
import kaktusz.geopolitika.permaloaded.tileentities.LabourSupplier;
import kaktusz.geopolitika.states.ChunksSavedData;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.states.StatesSavedData;
import kaktusz.geopolitika.util.MessageUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CmdStateStats extends Subcommand {

	public CmdStateStats(String name, CommandPermissions permissionLevel) {
		super(name, permissionLevel);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String rootCommandTranslationKey, boolean adminMode, String[] args) throws CommandException {
		if(args.length > 1)
			throw new WrongUsageException("");

		EntityPlayerMP player = CommandAssertions.senderMustBePlayer(sender);
		ForgeTeam targetState;
		if(args.length == 0) {
			targetState = CommandAssertions.playerMustBeInState(player);
		}
		else {
			targetState = CommandAssertions.specifiedStateMustBeValid(args[0]);
		}

		StatesSavedData stateData = StatesSavedData.get(player.world);
		ChunksSavedData chunksData = ChunksSavedData.get(player.world);
		PermaloadedSavedData permaData = PermaloadedSavedData.get(player.world);

		long balance = stateData.getBalance(targetState.getUID());
		String balanceStr = CmdStateBalance.BALANCE_FORMAT.format(balance);
		Collection<ChunkPos> stateChunks = chunksData.getOwnedChunks(targetState.getUID());
		Iterator<LabourSupplier> labourSuppliers = permaData.getLabourSuppliersInChunks(stateChunks).iterator();
		Iterator<LabourConsumer> labourConsumers = permaData.getLabourConsumersInChunks(stateChunks).iterator();

		double population = 0;
		double maxIndustrialOutput = 0;
		double industrialOutput = 0;

		while (labourSuppliers.hasNext()) {
			LabourSupplier next = labourSuppliers.next();
			population += next.getIdealLabourPerTick();
		}
		while (labourConsumers.hasNext()) {
			LabourConsumer next = labourConsumers.next();
			maxIndustrialOutput += next.getLabourPerTick();
			industrialOutput += next.getLabourReceived();
		}

		//TODO translation compat
		ITextComponent message = new TextComponentString("Statistics for state ");
		message.appendSibling(targetState.getCommandTitle()).appendText(":")
				.appendSibling(new TextComponentString("\n - Balance: ").setStyle(BASE_MESSAGE_STYLE))
				.appendText(balanceStr + "cr")
				.appendSibling(new TextComponentString("\n - Claimed Territory: ").setStyle(BASE_MESSAGE_STYLE))
				.appendText(stateChunks.size() + " Chunks")
				.appendSibling(new TextComponentString("\n - Population (Max. Labour): ").setStyle(BASE_MESSAGE_STYLE))
				.appendText(String.valueOf(population))
				.appendSibling(new TextComponentString("\n - Industrial Output (Fulfilled/Max): ").setStyle(BASE_MESSAGE_STYLE))
				.appendText(industrialOutput + "/" + maxIndustrialOutput);
		player.sendMessage(message);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
		if(args.length != 1)
			return super.getTabCompletions(server, sender, args, targetPos);

		return StatesManager.getAllStates().map(FinalIDObject::getId).collect(Collectors.toList());
	}
}
