package io.github.anon10w1z.bwss.commands;

import io.github.anon10w1z.bwss.BedWarsGame;
import io.github.anon10w1z.bwss.Bwss;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GameStatusCommand extends CommandBase {
	private Bwss bwss = Bwss.getInstance();

	@Override
	public String getCommandName() {
		return "gamestatus";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "gamestatus";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		BedWarsGame game = bwss.getGame();
		if (game == null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No game is in progress"));
			return;
		}
		if (args.length > 0) {
			throw new CommandException("Invalid args length");
		}
		long seconds = (System.currentTimeMillis() - game.getStartTime()) / 1000;
		long minutes = seconds / 60;
		seconds -= minutes * 60;
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.BOLD + "-----------------------"));
		sender.addChatMessage(new ChatComponentText("" + EnumChatFormatting.BOLD + "Game time: " + EnumChatFormatting.RESET + minutes + " minutes " + seconds + " seconds"));
		bwss.getOrderedColors().forEach(color -> {
			List<BedWarsGame.Player> players = game.getPlayersByColor(color);
			players.sort(Comparator.comparingDouble(BedWarsGame.Player::getNetPoints).reversed());
			players.forEach(player -> {
				String colorString = EnumChatFormatting.BOLD + String.valueOf('\u00a7') + bwss.getDyeChatMap().get(color);
				String playerInfo = colorString + player.getUsername()  + ": " + EnumChatFormatting.RESET + player.getNetPoints() + " net points (" + player.getPointsScored() + " points, " + player.getPointsLost() + " points lost)";
				if (player.getFinalDeath() > 0)
					playerInfo += ", final death";
				sender.addChatMessage(new ChatComponentText(playerInfo));
			});
		});
		sender.addChatMessage(new ChatComponentText(""));
		BedWarsGame.Player player = game.getPlayerByUsername(Minecraft.getMinecraft().thePlayer.getName());
		if (player != null) {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.BOLD + "Your stats:"));
			sender.addChatMessage(new ChatComponentText("" + EnumChatFormatting.GREEN + player.getOffensiveString()));
			sender.addChatMessage(new ChatComponentText("" + EnumChatFormatting.RED + player.getDefensiveString()));
		} else {
			sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Could not find client player! Are you nicked?"));
		}
		sender.addChatMessage(new ChatComponentText(EnumChatFormatting.BOLD + "-----------------------"));
	}

	@Override
	public List<String> getCommandAliases() {
		return Collections.singletonList("gs");
	}

	public int getRequiredPermissionLevel()
	{
		return 0;
	}
}
