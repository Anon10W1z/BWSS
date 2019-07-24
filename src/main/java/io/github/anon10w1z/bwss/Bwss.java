package io.github.anon10w1z.bwss;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.anon10w1z.bwss.commands.EndGameCommand;
import io.github.anon10w1z.bwss.commands.GameStatusCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandException;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"ResultOfMethodCallIgnored", "WeakerAccess"})
@Mod(modid = BwssConstants.MODID, name = BwssConstants.NAME, version = BwssConstants.VERSION, updateJSON = BwssConstants.UPDATE_JSON)
public class Bwss {
    @Mod.Instance
    private static Bwss instance;
    private Logger logger;

    private BedWarsGame game;
    private File gamesFolder;
    private Map<Character, EnumDyeColor> teamDyeMap = Maps.newHashMap();
    private Map<EnumDyeColor, Character> dyeChatMap = Maps.newHashMap();
    private List<EnumDyeColor> orderedColors = ImmutableList.of(EnumDyeColor.RED, EnumDyeColor.BLUE, EnumDyeColor.LIME, EnumDyeColor.YELLOW, EnumDyeColor.CYAN, EnumDyeColor.WHITE, EnumDyeColor.PINK, EnumDyeColor.GRAY);
    private String lastMap = "";
    private String lastMode = "";
    private int earlyDcCount = 0;

    private static Minecraft minecraft = Minecraft.getMinecraft();

    private String getLine(Scoreboard scoreboard, String member) {
        for (ScorePlayerTeam team : scoreboard.getTeams())
            if (team.getMembershipCollection().contains(member))
                return team.getColorPrefix() + team.getColorSuffix();
        return "";
    }

    private List<String> getSidebarScores() {
        Scoreboard scoreboard = minecraft.theWorld.getScoreboard();
        List<String> found = Lists.newArrayList();

        ScoreObjective sidebar = scoreboard.getObjectiveInDisplaySlot(1);
        if (sidebar != null) {
            List<Score> scores = new ArrayList<>(scoreboard.getScores());
            scores.sort(Comparator.comparingInt(Score::getScorePoints));
            found = scores.stream().filter(score -> score.getObjective().getName().equals(sidebar.getName())).map(score -> getLine(scoreboard, score.getPlayerName())).map(EnumChatFormatting::getTextWithoutFormattingCodes).collect(Collectors.toList());
        }
        return found;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        this.logger = event.getModLog();
        ClientCommandHandler.instance.registerCommand(new EndGameCommand());
        ClientCommandHandler.instance.registerCommand(new GameStatusCommand());
        MinecraftForge.EVENT_BUS.register(instance);

        //tab codes
        teamDyeMap.put('R', EnumDyeColor.RED);
        teamDyeMap.put('G', EnumDyeColor.LIME);
        teamDyeMap.put('B', EnumDyeColor.BLUE);
        teamDyeMap.put('Y', EnumDyeColor.YELLOW);

        teamDyeMap.put('W', EnumDyeColor.WHITE);
        teamDyeMap.put('A', EnumDyeColor.CYAN);
        teamDyeMap.put('P', EnumDyeColor.PINK);
        teamDyeMap.put('S', EnumDyeColor.GRAY);
        //chat codes
        teamDyeMap.put('c', EnumDyeColor.RED);
        teamDyeMap.put('a', EnumDyeColor.LIME);
        teamDyeMap.put('9', EnumDyeColor.BLUE);
        teamDyeMap.put('e', EnumDyeColor.YELLOW);

        teamDyeMap.put('f', EnumDyeColor.WHITE);
        teamDyeMap.put('b', EnumDyeColor.CYAN);
        teamDyeMap.put('d', EnumDyeColor.PINK);
        teamDyeMap.put('8', EnumDyeColor.GRAY);

        //reverse, reverse
        for (Map.Entry<Character, EnumDyeColor> entry : teamDyeMap.entrySet()) {
            char key = entry.getKey();
            EnumDyeColor value = entry.getValue();
            if (Character.isDigit(key) || Character.isLowerCase(key))
                dyeChatMap.put(value, key);
        }

        gamesFolder = new File(event.getSuggestedConfigurationFile().getParentFile().getParentFile(), "bwgames");
        if (!gamesFolder.exists())
            gamesFolder.mkdir();
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent event) {
        List<String> sidebar = getSidebarScores();
        if (!sidebar.isEmpty())
            sidebar.forEach(line -> {
                if (line.startsWith("Map: "))
                    lastMap = line.replace("Map: ", "").trim();
                if (line.startsWith("Mode: "))
                    lastMode = line.replace("Mode: ", "").trim();
            });
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String message = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getFormattedText());
        if (message.trim().startsWith("Protect your bed and destroy the enemy beds.")) { //signifies the start of a game
            minecraft.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.BOLD + "Starting BW game"));
            game = new BedWarsGame(lastMap, lastMode);
            earlyDcCount = 0;
            new Thread(() -> {
                Collection<NetworkPlayerInfo> playerInfoMap = minecraft.thePlayer.sendQueue.getPlayerInfoMap();
                GuiPlayerTabOverlay tabOverlay = minecraft.ingameGUI.getTabList();
                List<String> players = Lists.newArrayList();
                while (players.isEmpty()) { //wait for all team names to show up in tab
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    players = playerInfoMap.stream().map(tabOverlay::getPlayerName).filter(Objects::nonNull).filter(name -> !EnumChatFormatting.getTextWithoutFormattingCodes(name).equals(name)).map(String::trim).collect(Collectors.toList());
                }
                try { //this doesn't catch all players, wait a bit for all to be added to tab menu
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                players = playerInfoMap.stream().map(tabOverlay::getPlayerName).filter(Objects::nonNull).filter(name -> !EnumChatFormatting.getTextWithoutFormattingCodes(name).equals(name)).filter(name -> !game.getAllUsernames().contains(name)).map(String::trim).collect(Collectors.toList());
                String playerCountMessage = EnumChatFormatting.BOLD + "" + (players.size() + earlyDcCount) + " players detected" + (earlyDcCount > 0 ? " (including " + earlyDcCount + " prior disconnections)" : "");
                minecraft.thePlayer.addChatMessage(new ChatComponentText(playerCountMessage));
                for (String fullName : players) {
                    String cleanName = EnumChatFormatting.getTextWithoutFormattingCodes(fullName).trim();
                    if (cleanName.split(" ").length != 2)
                        continue;
                    EnumDyeColor color = teamDyeMap.get(cleanName.charAt(0));
                    String username = cleanName.substring(2);
                    game.addPlayer(new BedWarsGame.Player(game.getStartTime(), username, color));
                    game.logMessage("GAMESTART " + username + " " + color);
                }
            }).start();
            return;
        }

        if (game == null)
            return;

        String[] messageWords = message.split(" ");
        if (messageWords.length == 0)
            return;

        if (message.startsWith("BED DESTRUCTION > ")) {
            message = message.replace("BED DESTRUCTION > ", "");
            messageWords = message.split(" ");
            if (messageWords[0].equals("Your"))
                messageWords[0] = game.getPlayerByUsername(minecraft.thePlayer.getName()).getColor().getName();
            if (messageWords[0].equals("Aqua"))
                messageWords[0] = "Cyan";
            if (messageWords[0].equals("Green"))
                messageWords[0] = "Lime";
            EnumDyeColor destroyedColor = EnumDyeColor.valueOf(messageWords[0].toUpperCase());
            String bedDestroyerName = messageWords[messageWords.length - 1];
            bedDestroyerName = bedDestroyerName.substring(0, bedDestroyerName.length() - 1);
            BedWarsGame.Player bedDestroyer = game.getPlayerByUsername(bedDestroyerName);
            String finalBedDestroyerName = bedDestroyerName;
            game.getPlayersByColor(destroyedColor).forEach(player -> {
                player.handleEvent(BedWarsGame.Event.BED_BROKEN);
                bedDestroyer.handleEvent(BedWarsGame.Event.BED_BREAK); //considered as breaking the bed of each individual player
                game.logMessage("BEDBREAK " + finalBedDestroyerName + " " + player.getUsername());
            });
            return;
        }

        if (message.equals("All beds have been destroyed!")) {
            for (EnumDyeColor color : EnumDyeColor.values())
                for (BedWarsGame.Player player : game.getPlayersByColor(color)) {
                    player.handleEvent(BedWarsGame.Event.BED_BROKEN);
                    game.logMessage("BEDBROKEN " + player.getUsername());
                }
            return;
        }

        BedWarsGame.Player firstWordPlayer = game.getPlayerByUsername(messageWords[0]);
        if (firstWordPlayer != null) {
            if (message.endsWith("FINAL KILL!")) {
                firstWordPlayer.handleEvent(BedWarsGame.Event.FINAL_DEATH);
                List<String> usernames = game.getAllUsernames();
                for (String word : messageWords) {
                    if (word.equals(messageWords[0]))
                        continue;
                    if (word.endsWith("'s")) //i.e. final kill counter or silverfish kill
                        word = word.substring(0, word.length() - 2);
                    if (word.endsWith("."))
                        word = word.substring(0, word.length() - 1);
                    if (usernames.contains(word)) {
                        game.getPlayerByUsername(word).handleEvent(BedWarsGame.Event.FINAL_KILL);
                        game.logMessage("FINALKILL " + word + " " + messageWords[0]);
                        return;
                    }
                }
                game.logMessage("FINALDEATH " + messageWords[0]);
                return;
            }
            if (message.endsWith("reconnected.")) {
                game.logMessage("RECONNECT " + messageWords[0]);
                if (!messageWords[0].equals(minecraft.thePlayer.getName())) { //Hypixel does not send the "fell into the void" reconnect message to other players, so assume death
                    firstWordPlayer.handleEvent(BedWarsGame.Event.DEATH);
                    game.logMessage("DEATH " + messageWords[0]);
                }
                return;
            }
            if (message.endsWith(".")) { //signifies a regular kill or death
                if (message.endsWith("joined.") || message.endsWith("left.")) //ignore guild / friends list messages
                    return;
                firstWordPlayer.handleEvent(BedWarsGame.Event.DEATH);
                List<String> usernames = game.getAllUsernames();
                for (String word : messageWords) {
                    if (word.equals(messageWords[0]))
                        continue;
                    if (word.endsWith("'s")) //i.e. final kill counter or silverfish kill
                        word = word.substring(0, word.length() - 2);
                    if (word.endsWith("."))
                        word = word.substring(0, word.length() - 1);
                    if (usernames.contains(word)) {
                        game.getPlayerByUsername(word).handleEvent(BedWarsGame.Event.KILL);
                        game.logMessage("KILL " + word + " " + messageWords[0]);
                        return;
                    }
                }
                game.logMessage("DEATH " + messageWords[0]);
                return;
            }
            if (message.endsWith("disconnected")) {
                game.logMessage("DISCONNECT " + messageWords[0]);
                if (firstWordPlayer.getLostBed() > 0) { //if a player disconnects and their bed is broken, count it as a final death
                    firstWordPlayer.handleEvent(BedWarsGame.Event.FINAL_DEATH);
                    game.logMessage("FINALDEATH " + messageWords[0]);
                }
                return;
            }
            return;
        } else if (message.endsWith("disconnected") && messageWords.length == 2) { //player disconnected but wasn't added to the game yet
            String rawMessage = event.message.getFormattedText();
            EnumDyeColor color = teamDyeMap.get(rawMessage.charAt(3));
            if (color == null)
                return;
            String username = messageWords[0];
            game.addPlayer(new BedWarsGame.Player(game.getStartTime(), username, color));
            game.logMessage("GAMESTART " + username + " " + color);
            game.logMessage("DISCONNECT " + username);
            ++earlyDcCount;
            return;
        }

        if (message.startsWith("TEAM ELIMINATED > ")) {
            message = message.replace("TEAM ELIMINATED > ", "");
            messageWords = message.split(" ");
            if (messageWords[0].equals("Aqua"))
                messageWords[0] = "Cyan";
            if (messageWords[0].equals("Green"))
                messageWords[0] = "Lime";
            EnumDyeColor eliminatedColor = EnumDyeColor.valueOf(messageWords[0].toUpperCase());
            game.getPlayersByColor(eliminatedColor).forEach(player -> {
                if (player.getLostBed() == 0) {
                    player.handleEvent(BedWarsGame.Event.BED_BROKEN);
                    game.logMessage("BEDBROKEN " + player.getUsername());
                }
                if (player.getFinalDeath() == 0) {
                    player.handleEvent(BedWarsGame.Event.FINAL_DEATH);
                    game.logMessage("FINALDEATH " + player.getUsername());
                }

                player.handleEvent(BedWarsGame.Event.ELIMINATED);
                game.logMessage("ELIMINATED " + player.getUsername());
            });
            if (game.getPlayerByUsername(minecraft.thePlayer.getName()).getColor() == eliminatedColor) {
                minecraft.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.BOLD + "Ending BW game"));
                game.logMessage("Game finished in defeat, saving");
                game.save();
                try {
                    new GameStatusCommand().processCommand(minecraft.thePlayer, new String[]{});
                } catch (CommandException e) {
                    e.printStackTrace();
                }
                game = null;
            }
            return;
        }

        if (message.startsWith("▬▬▬▬▬▬") && message.contains("Reward Summary")) { //signifies the end of a game in victory (otherwise game would be null)
            minecraft.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.BOLD + "Ending BW game"));
            game.logMessage("Game ended (likely in victory), saving");
            game.save();
            try {
                new GameStatusCommand().processCommand(minecraft.thePlayer, new String[]{});
            } catch (CommandException e) {
                e.printStackTrace();
            }
            game = null;
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (game != null) {
            BedWarsGame.Player player = game.getPlayerByUsername(minecraft.thePlayer.getName());
            if (player == null)
                return;
            game.logMessage("DISCONNECT " + player.getUsername());
            if (player.getLostBed() > 0) {
                player.handleEvent(BedWarsGame.Event.FINAL_DEATH);
                game.logMessage("FINALDEATH " + player.getUsername());

                boolean shouldEliminate = true;
                List<BedWarsGame.Player> players = game.getPlayersByColor(player.getColor());
                for (BedWarsGame.Player player1 : players)
                    if (player1.getFinalDeath() == 0) { //if there is at least one alive teammate at time of disconnection, don't eliminate the client player
                        shouldEliminate = false;
                        break;
                    }
                if (shouldEliminate) {
                    players.forEach(player1 -> {
                        player1.handleEvent(BedWarsGame.Event.ELIMINATED);
                        game.logMessage("ELIMINATED " + player1.getUsername());
                    });
                    game.save();
                    game = null;
                }
            }
        }
    }

    public static Bwss getInstance() {
        return instance;
    }

    public BedWarsGame getGame() {
        return game;
    }

    public void nullifyGame() {
        game = null;
    }

    public File getGamesFolder() {
        return gamesFolder;
    }

    public Map<EnumDyeColor, Character> getDyeChatMap() {
        return dyeChatMap;
    }

    public List<EnumDyeColor> getOrderedColors() {
        return orderedColors;
    }

    public void logInfo(String message) {
        logger.log(Level.INFO, message);
    }
}
