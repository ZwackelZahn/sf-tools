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
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import sf.Player;
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

			for (int i = 0; i < groupNames.size(); i++) {
				guildData.put(groupNames.get(i), new Long[] { groupSave.get(314 + i), groupSave.get(214 + i), groupSave.get(264 + i), groupSave.get(390 + i) });
			}
		}

		// Imports all players
		Set<Player> players = new HashSet<>();

		for (String player : texts) {
			players.add(buildPlayer(player, guildData));
		}

		// Sorts player by guild position and level
		List<Player> sorted = new ArrayList<>(players);
		Collections.sort(sorted, Comparator.comparing((Player P) -> P.GuildRoleId).thenComparing(P -> -P.Level));

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

	private static Player buildPlayer(String text, Map<String, Long[]> guild) {
		Map<String, Object> data = new HashMap<>();
		Map<String, String> sets = parse(text);

		String[] names = new String[] { null, null };

		sets.forEach((K, V) -> {
			if (K.contains("groupname")) {
				names[1] = V;
			} else if (K.contains("name")) {
				names[0] = V;
			} else if (K.contains("unitlevel")) {
				data.put("fortress_units", splitl(V, "/"));
			} else if (K.contains("achievement")) {
				data.put("achievements_data", splitl(V, "/"));
			} else if (K.contains("fortressrank")) {
				data.put("rank_fortress", Long.parseLong(V));
			} else if (K.contains("playerlookat")) {
				List<Long> values = splitl(V, "/");

				data.put("xp_current", values.get(3));
				data.put("xp_required", values.get(4));
				data.put("rank_player_honor", values.get(5));
				data.put("rank_player", values.get(6));
				data.put("player_race", values.get(18) % 16);
				data.put("player_sex", values.get(19) % 16);
				data.put("player_class", values.get(20) % 16);
				data.put("attr_strength", values.get(21) + values.get(26));
				data.put("attr_dexterity", values.get(22) + values.get(27));
				data.put("attr_intelligence", values.get(23) + values.get(28));
				data.put("attr_constitution", values.get(24) + values.get(29));
				data.put("attr_luck", values.get(25) + values.get(29));
				data.put("mount", values.get(159) % 16);
				data.put("xp_book", values.get(163) - 10000);
				data.put("attr_armor", values.get(168));
				data.put("xp_level", values.get(173));
				data.put("potion_1", values.get(194));
				data.put("potion_2", values.get(195));
				data.put("potion_3", values.get(196));
				data.put("potion_dur_1", values.get(200));
				data.put("potion_dur_2", values.get(201));
				data.put("potion_dur_3", values.get(202));
				data.put("rank_fortress_honor", values.get(248));
				data.put("fortress_upgrades", values.get(247));
				data.put("fortress_knights", values.get(258));
			} else if (K.contains("playerSave")) {
				List<Long> values = splitl(V, "/");

				data.put("xp_current", values.get(8));
				data.put("xp_required", values.get(9));
				data.put("rank_player_honor", values.get(10));
				data.put("rank_player", values.get(11));
				data.put("player_race", values.get(27) % 16);
				data.put("player_sex", values.get(28) % 16);
				data.put("player_class", values.get(29) % 16);
				data.put("attr_strength", values.get(30) + values.get(35));
				data.put("attr_dexterity", values.get(31) + values.get(36));
				data.put("attr_intelligence", values.get(32) + values.get(37));
				data.put("attr_constitution", values.get(33) + values.get(38));
				data.put("attr_luck", values.get(34) + values.get(39));
				data.put("mount", values.get(286) % 16);
				data.put("xp_book", values.get(438) - 10000);
				data.put("attr_armor", values.get(447));
				data.put("xp_level", values.get(465));
				data.put("potion_1", values.get(493));
				data.put("potion_2", values.get(494));
				data.put("potion_3", values.get(495));
				data.put("potion_dur_1", values.get(499));
				data.put("potion_dur_2", values.get(500));
				data.put("potion_dur_3", values.get(501));
				data.put("rank_fortress", values.get(583));
				data.put("rank_fortress_honor", values.get(582));
				data.put("fortress_upgrades", values.get(581));
				data.put("fortress_knights", values.get(598));
			}
		});

		if (!guild.isEmpty() && guild.containsKey(names[0])) {
			Long[] pg = guild.get(names[0]);

			if (pg[0] < 4) {
				data.put("guild_role", pg[0]);
				data.put("guild_treasure", pg[1]);
				data.put("guild_instructor", pg[2]);
				data.put("guild_pet", pg[3]);
			}
		}

		return new Player(names[0], names[1], data);
	}

}
