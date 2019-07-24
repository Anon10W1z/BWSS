package io.github.anon10w1z.bwss.commands;

import io.github.anon10w1z.bwss.BedWarsGame;
import io.github.anon10w1z.bwss.Bwss;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;
import java.util.List;

public class EndGameCommand extends CommandBase {
	private Bwss bwss = Bwss.getInstance();

	@Override
	public String getCommandName() {
		return "endgame";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "endgame <save>";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		BedWarsGame game = bwss.getGame();
		if (game == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No game is in progress"));
			return;
		}
		if (args.length > 1) {
			throw new CommandException("Invalid args length");
		}
		if (args.length == 0 || !CommandBase.parseBoolean(args[0])) {
			sender.addChatMessage(new ChatComponentText("Discarding game without saving"));
			bwss.nullifyGame();
			return;
		}
		sender.addChatMessage(new ChatComponentText("Ending and saving game"));
		game.save();
		bwss.nullifyGame();
	}

	@Override
	public List<String> getCommandAliases() {
		return Collections.singletonList("eg");
	}

	public int getRequiredPermissionLevel()
	{
		return 0;
	}
}
