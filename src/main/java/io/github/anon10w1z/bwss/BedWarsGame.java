package io.github.anon10w1z.bwss;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.item.EnumDyeColor;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class BedWarsGame {
	private static final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy_MM-dd_HH-mm-ss_Z");
	private static final SimpleDateFormat prettyDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private long startTime = System.currentTimeMillis();
	private List<Player> players = Lists.newArrayList();
	private List<String> logMessages = Lists.newArrayList();

	private String map;
	private String mode;

	private String logFileName;

	public BedWarsGame(String map, String mode) {
		this.map = map;
		this.mode = mode;
	}

	public void addPlayer(Player player) {
		players.add(player);
	}

	public long getStartTime() {
		return startTime;
	}

	public String getMap() {
		return map;
	}

	public String getMode() {
		return mode;
	}

	public Player getPlayerByUsername(String username) {
		return players.stream().filter(player -> player.getUsername().equals(username)).findFirst().orElse(null);
	}

	public List<Player> getPlayersByColor(EnumDyeColor color) {
		return players.stream().filter(player -> player.getColor() == color).collect(Collectors.toList());
	}

	public List<String> getAllUsernames() {
		return players.stream().map(Player::getUsername).collect(Collectors.toList());
	}

	public String getPrettyDateString() {
		return prettyDateFormat.format(new Date(startTime));
	}

	public List<String> getLogMessages() {
		return logMessages;
	}

	public void logMessage(String message) {
		long seconds = (System.currentTimeMillis() - startTime) / 1000;
		long minutes = seconds / 60;
		seconds -= minutes * 60;
		message = "[" + minutes + ":" + (seconds < 10 ? "0" + seconds : seconds) + "] " + message;
		Bwss.getInstance().logInfo(message);
		logMessages.add(message);
	}

	public void save() {
		try {
			String fileDateString = fileDateFormat.format(new Date(startTime));
			logFileName = "game_" + fileDateString + ".bwg.log";
			File gameLogFile = new File(Bwss.getInstance().getGamesFolder(), logFileName);
			if (gameLogFile.exists())
				gameLogFile.delete();
			gameLogFile.createNewFile();
			PrintWriter out = new PrintWriter(gameLogFile);
			String separator = System.lineSeparator();
			out.write("Local start time: " + getPrettyDateString() + separator);
			out.write("UTC offset: " + ZonedDateTime.now().getOffset().getTotalSeconds() / 3600.0 + " hours" + separator);
			out.write("Map: " + map + separator);
			out.write("Mode: " + mode + separator);
			out.write(separator);
			for (String message : logMessages)
				out.write(message + separator);
			out.write(separator);

			players.forEach(player -> {
				if (player.endTime == 0)
					player.endTime = System.currentTimeMillis();
			});
			Bwss.getInstance().getOrderedColors().forEach(color -> { //verify elimination for the whole team
				List<Player> players = getPlayersByColor(color);
				for (Player player : players)
					if (!player.getEliminated()) {
						players.forEach(player1 -> player1.eliminated = false);
						break;
					}
			});
			players.stream().sorted(Comparator.comparingDouble(Player::getNetEfficiency).reversed()).forEach(player -> {
				out.write(player.getUsername() + separator);
				out.write(" - color: " + player.getColor() + separator);
				out.write(" - " + player.getPointsScored() + " points scored: " + player.getOffensiveString() + separator);
				out.write(" - " + player.getPointsLost() + " points lost: " + player.getDefensiveString() + separator);
				out.write(" - " + player.getMinutesPlayed() + " minutes played" + separator);
				out.write(" - " + player.getNetEfficiency() + " net efficiency (" + player.getOffensiveEfficiency() + " offensive, " + player.getDefensiveEfficiency() + " defensive)" + separator);
				out.write(" - eliminated: " + player.getEliminated() + separator);
				out.write(separator);
			});
			out.close();

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			File gsonFile = new File(Bwss.getInstance().getGamesFolder(), "game_" + fileDateString + ".bwg.json");
			if (gsonFile.exists())
				gsonFile.delete();
			gsonFile.createNewFile();
			FileWriter fileWriter = new FileWriter(gsonFile);
			gson.toJson(this, fileWriter);
			fileWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class Player {
		private long startTime, endTime = 0;
		private String username;
		private EnumDyeColor color;

		public Player(long startTime, String username, EnumDyeColor color) {
			this.startTime = startTime;
			this.username = username;
			this.color = color;
		}

		private int kills = 0, bedBreaks = 0, finalKills = 0;
		private int deaths = 0, lostBed = 0, finalDeath = 0;
		private boolean eliminated = false; //keeps track of whether or not team was eliminated

		public void handleEvent(Event event) {
			switch (event) {
				case KILL:
					kills += 1;
					break;
				case BED_BREAK:
					bedBreaks += 1;
					break;
				case FINAL_KILL:
					finalKills += 1;
					break;
				case DEATH:
					deaths += 1;
					break;
				case BED_BROKEN:
					lostBed = 1;
					break;
				case FINAL_DEATH:
					finalDeath = 1;
					endTime = System.currentTimeMillis();
				case ELIMINATED:
					eliminated = true;
			}
		}

		public String getUsername() {
			return username;
		}

		public EnumDyeColor getColor() {
			return color;
		}

		public double getMinutesPlayed() {
			return (double) (endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime) / 1000D / 60D;
		}

		public double getPointsScored() {
			return 0.5 * kills + 2 * bedBreaks + 2 * finalKills;
		}

		public double getPointsLost() {
			return 0.5 * deaths + 2 * lostBed + 2 * finalDeath;
		}

		public double getNetPoints() {
			return getPointsScored() - getPointsLost();
		}

		public double getOffensiveEfficiency() {
			return getPointsScored() / getMinutesPlayed();
		}

		public double getDefensiveEfficiency() {
			return getPointsLost() / getMinutesPlayed();
		}

		public double getNetEfficiency() {
			return getNetPoints() / getMinutesPlayed();
		}

		public boolean getEliminated() {
			return eliminated;
		}

		public int getKills() {
			return kills;
		}

		public int getBedBreaks() {
			return bedBreaks;
		}

		public int getFinalKills() {
			return finalKills;
		}

		public int getDeaths() {
			return deaths;
		}

		public int getLostBed() {
			return lostBed;
		}

		public int getFinalDeath() {
			return finalDeath;
		}

		public String getOffensiveString() {
			return formatStatString(kills, "kill") + ", " + formatStatString(bedBreaks, "bed break") + ", " + formatStatString(finalKills, "final kill");
		}

		public String getDefensiveString() {
			return formatStatString(deaths, "death") + ", " + formatStatString(lostBed, "lost bed") + ", " + formatStatString(finalDeath, "final death");
		}

		private String formatStatString(int i, String name) {
			return i + " " + name + (i == 1 ? "" : "s");
		}
	}

	public enum Event {
		KILL,
		BED_BREAK,
		FINAL_KILL,
		DEATH,
		BED_BROKEN,
		FINAL_DEATH,
		ELIMINATED
	}
}
