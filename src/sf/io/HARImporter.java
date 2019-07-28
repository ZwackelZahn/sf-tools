package sf.io;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import sf.struct.Item;
import sf.struct.Player;
import util.Constants;

public class HARImporter {

	public static List<Player> importHAR(File file) throws IOException, ParseException {
		// JSON object from .HAR file
		JSONObject json = (JSONObject) new JSONParser().parse(new FileReader(file));

		// Collects all game related strings from JSON and throws away extra
		List<String> texts = new ArrayList<>();
		collect(texts, (key, value) -> key.toLowerCase().equals("text") && (String.valueOf(value).contains("otherplayername") || String.valueOf(value).contains("ownplayername")), "", json);

		// Imports guild data if available
		Map<String, Long[]> guildData = new HashMap<String, Long[]>();
		Optional<String> guildString = texts.stream().filter(S -> S.contains("owngroupsave")).findFirst();

		if (guildString.isPresent()) {
			Map<String, String> playerData = parse(guildString.get());
			List<Long> groupSave = splitl(playerData.get("owngroupsave.groupSave").toString(), "/");
			List<String> groupNames = splits(playerData.get("owngroupmember.r").toString(), ",");
			List<Long> groupKnights = splitl(playerData.get("owngroupknights.r").toString(), ",");

			for (int i = 0; i < groupNames.size(); i++) {
				guildData.put(groupNames.get(i), new Long[] { groupSave.get(314 + i), groupSave.get(214 + i), groupSave.get(264 + i), groupSave.get(390 + i), groupKnights.get(i) });
			}
		}

		// Imports all players
		Set<Player> players = new HashSet<>();

		for (String player : texts) {
			players.add(buildPlayer(player, guildData));
		}

		// Sorts player by guild position and level
		List<Player> sorted = new ArrayList<>(players);
		Collections.sort(sorted, Comparator.comparing((Player P) -> Objects.isNull(P.GuildRole) ? 5 : P.GuildRole).thenComparing(P -> -P.Level));

		return sorted;
	}

	private static Map<String, String> parse(String str) {
		return Arrays.asList(str.split("&")).stream().map(S -> S.split(":")).filter(KV -> Arrays.stream(Constants.FILTER).anyMatch(S -> KV[0].contains(S))).collect(Collectors.toMap(E -> E[0], E -> E[1]));
	}

	private static List<String> splits(String str, String delim) {
		return Arrays.asList(str.split(delim));
	}

	private static List<Long> splitl(String str, String delim) {
		return splits(str, delim).stream().mapToLong(S -> Long.parseLong(S)).boxed().collect(Collectors.toList());
	}

	private static void collect(List<String> collection, BiPredicate<String, Object> filter, Object key, Object value) {
		if (value instanceof JSONArray) {
			collectArray(collection, filter, (JSONArray) value);
		} else if (value instanceof JSONObject) {
			collectObject(collection, filter, (JSONObject) value);
		} else if (filter.test((String) key, value)) {
			collection.add((String) value);
		}
	}

	@SuppressWarnings("unchecked")
	private static void collectObject(List<String> collection, BiPredicate<String, Object> filter, JSONObject root) {
		root.keySet().forEach(key -> collect(collection, filter, key, root.get(key)));
	}

	@SuppressWarnings("unchecked")
	private static void collectArray(List<String> collection, BiPredicate<String, Object> filter, JSONArray root) {
		root.iterator().forEachRemaining(E -> collect(collection, filter, "", E));
	}

	private static Item buildItem(List<Long> data) {
		Item item = new Item();

		item.SlotID = data.get(0);
		item.ItemID = data.get(1);
		item.Rating1 = data.get(2);
		item.Rating2 = data.get(3);
		item.Attribute1Type = data.get(4);
		item.Attribute2Type = data.get(5);
		item.Attribute3Type = data.get(6);
		item.Attribute1 = data.get(7);
		item.Attribute2 = data.get(8);
		item.Attribute3 = data.get(9);
		item.Value = data.get(10) / 100L;
		item.Gem = data.get(11);

		return item;
	}

	private static Player buildPlayer(String text, Map<String, Long[]> guild) {
		Player player = new Player();

		Map<String, String> sets = parse(text);

		sets.forEach((K, V) -> {
			if (K.contains("groupname")) {
				player.Guild = V;
			} else if (K.contains("name")) {
				player.Name = V;
			} else if (K.contains("unitlevel")) {
				List<Long> fortressUnits = splitl(V, "/");

				player.FortressWall = fortressUnits.get(0);
				player.FortressWarriors = fortressUnits.get(1);
				player.FortressArchers = fortressUnits.get(2);
				player.FortressMages = fortressUnits.get(3);
			} else if (K.contains("achievement")) {
				List<Long> achievements = splitl(V, "/");
				List<Long> stripped = achievements.subList(0, achievements.size() / 2);

				player.Achievements = stripped.stream().filter(L -> L > 0).count();
			} else if (K.contains("fortressrank")) {
				player.RankFortress = Long.parseLong(V);
			} else if (K.contains("playerlookat")) {
				List<Long> values = splitl(V, "/");

				player.XP = values.get(3);
				player.XPNext = values.get(4);
				player.Book = values.get(163) - 10000;
				player.Level = values.get(173);

				player.HonorPlayer = values.get(5);
				player.RankPlayer = values.get(6);

				player.Race = values.get(18) % 16;
				player.Sex = values.get(19) % 16;
				player.Class = values.get(20) % 16;

				player.Strength = values.get(21) + values.get(26);
				player.Dexterity = values.get(22) + values.get(27);
				player.Intelligence = values.get(23) + values.get(28);
				player.Constitution = values.get(24) + values.get(29);
				player.Luck = values.get(25) + values.get(29);
				player.Armor = values.get(168);

				player.Head = buildItem(values.subList(39, 51));
				player.Body = buildItem(values.subList(51, 63));
				player.Hands = buildItem(values.subList(63, 75));
				player.Feet = buildItem(values.subList(75, 87));
				player.Neck = buildItem(values.subList(87, 99));
				player.Belt = buildItem(values.subList(99, 111));
				player.Ring = buildItem(values.subList(111, 123));
				player.Misc = buildItem(values.subList(123, 135));

				player.Weapon = buildItem(values.subList(135, 147));
				player.WeaponSecondary = buildItem(values.subList(147, 159));

				player.Mount = Constants.MOUNTS[(int) (values.get(159) % 16)];

				player.Potion1 = values.get(194) == 16 ? 6 : values.get(194) == 0 ? 0 : (values.get(194) - 1) % 6;
				player.Potion2 = values.get(195) == 16 ? 6 : values.get(195) == 0 ? 0 : (values.get(195) - 1) % 6;
				player.Potion3 = values.get(196) == 16 ? 6 : values.get(196) == 0 ? 0 : (values.get(196) - 1) % 6;

				player.PotionDuration1 = values.get(200);
				player.PotionDuration2 = values.get(201);
				player.PotionDuration3 = values.get(202);

				player.HonorFortress = values.get(248);
				player.FortressUpgrades = values.get(247);
				player.FortressKnights = values.get(258);
			} else if (K.contains("playerSave")) {
				List<Long> values = splitl(V, "/");

				player.XP = values.get(8);
				player.XPNext = values.get(9);
				player.Book = values.get(438) - 10000;
				player.Level = values.get(465);

				player.HonorPlayer = values.get(10);
				player.RankPlayer = values.get(11);

				player.Race = values.get(27) % 16;
				player.Sex = values.get(28) % 16;
				player.Class = values.get(29) % 16;

				player.Strength = values.get(30) + values.get(35);
				player.Dexterity = values.get(31) + values.get(36);
				player.Intelligence = values.get(32) + values.get(37);
				player.Constitution = values.get(33) + values.get(38);
				player.Luck = values.get(34) + values.get(39);
				player.Armor = values.get(447);

				player.Head = buildItem(values.subList(48, 60));
				player.Body = buildItem(values.subList(60, 72));
				player.Hands = buildItem(values.subList(72, 84));
				player.Feet = buildItem(values.subList(84, 96));
				player.Neck = buildItem(values.subList(96, 108));
				player.Belt = buildItem(values.subList(108, 120));
				player.Ring = buildItem(values.subList(120, 132));
				player.Misc = buildItem(values.subList(132, 144));

				player.Weapon = buildItem(values.subList(144, 156));
				player.WeaponSecondary = buildItem(values.subList(156, 168));

				player.Mount = Constants.MOUNTS[(int) (values.get(286) % 16)];

				player.Potion1 = values.get(493) == 16 ? 6 : values.get(493) == 0 ? 0 : (values.get(493) - 1) % 6;
				player.Potion2 = values.get(494) == 16 ? 6 : values.get(494) == 0 ? 0 : (values.get(494) - 1) % 6;
				player.Potion3 = values.get(495) == 16 ? 6 : values.get(495) == 0 ? 0 : (values.get(495) - 1) % 6;

				player.PotionDuration1 = values.get(499);
				player.PotionDuration2 = values.get(500);
				player.PotionDuration3 = values.get(501);

				player.RankFortress = values.get(583);
				player.HonorFortress = values.get(582);
				player.FortressUpgrades = values.get(581);
				player.FortressKnights = values.get(598);
			}
		});

		if (!guild.isEmpty() && guild.containsKey(player.Name)) {
			Long[] pg = guild.get(player.Name);

			if (pg[0] < 4) {
				player.GuildRole = pg[0];
				player.GuildTreasure = pg[1];
				player.GuildInstructor = pg[2];
				player.GuildPet = pg[3];

				if (Objects.isNull(player.FortressKnights)) {
					player.FortressKnights = pg[4];
				}
			}
		}

		return player;
	}

}
