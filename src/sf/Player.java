package sf;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import util.Constants;

public class Player implements Serializable {

	private static final long serialVersionUID = 40L;
	
	private final Map<String, Object> data;
	
	public String Name;
	
	public final Long Book;
	public final Long XP;
	public final Long XPNext;
	public final Long Level;
	
	public final Long Mount;
	
	public final String Race;
	public final String Sex;
	public final String Class;
	
	public final Long HonorPlayer;
	public final Long HonorFortress;
	public final Long RankPlayer;
	public final Long RankFortress;
	
	public final String Potion1;
	public final String Potion2;
	public final String Potion3;
	
	public final Long PotionDuration1;
	public final Long PotionDuration2;
	public final Long PotionDuration3;
	
	public final Long Armor;
	public final Long Strength;
	public final Long Dexterity;
	public final Long Intelligence;
	public final Long Constitution;
	public final Long Luck;
	
	public final Long FortressWall;
	public final Long FortressWarriors;
	public final Long FortressArchers;
	public final Long FortressMages;
	public final Long FortressKnights;
	public final Long FortressUpgrades;
	
	public final Long Achievements;
	public final Long AchievementsTotal;
	
	public String Guild;
	public final String GuildRole;
	public final Long GuildRoleId;
	public final Long GuildTreasure;
	public final Long GuildInstructor;
	public final Long GuildPet;
	
	public Player(String name, String group, Map<String, Object> data) {
		this(data);
		this.Name =  name;
		this.Guild = group;
	}
	
	@SuppressWarnings("unchecked")
	public Player(Map<String, Object> data) {
		this.data = data;
		
		this.Name =  (String) data.get("name");
		this.Guild = (String) data.get("guild");
		
		this.Book = Get(data, "xp_book");
		this.XP = Get(data, "xp_current");
		this.XPNext = Get(data, "xp_required");
		this.Level = Get(data, "xp_level");
		
		this.Mount = Constants.MOUNTS[(int) Get(data, "mount").intValue()];
		
		this.Race = Constants.RACES[(int) Get(data, "player_race").intValue()];
		this.Sex = Constants.SEXES[(int) Get(data, "player_sex").intValue()];
		this.Class = Constants.CLASSES[(int) Get(data, "player_class").intValue()];
		
		this.HonorPlayer = Get(data, "rank_player_honor");
		this.HonorFortress = Get(data, "rank_fortress_honor");
		this.RankPlayer = Get(data, "rank_player");
		this.RankFortress = Get(data, "rank_fortress");
		
		this.Potion1 = Constants.POTIONS[(int) Get(data, "potion_1").intValue()];
		this.Potion2 = Constants.POTIONS[(int) Get(data, "potion_2").intValue()];
		this.Potion3 = Constants.POTIONS[(int) Get(data, "potion_3").intValue()];
		
		this.PotionDuration1 = Get(data, "potion_dur_1");
		this.PotionDuration2 = Get(data, "potion_dur_2");
		this.PotionDuration3 = Get(data, "potion_dur_3");
		
		this.Armor = Get(data, "attr_armor");
		this.Strength = Get(data, "attr_strength");
		this.Dexterity = Get(data, "attr_dexterity");
		this.Intelligence = Get(data, "attr_intelligence");
		this.Constitution = Get(data, "attr_constitution");
		this.Luck = Get(data, "attr_luck");
		
		List<Long> levels = (List<Long>) data.get("fortress_units");
		this.FortressWall = levels.get(0);
		this.FortressWarriors = levels.get(1);
		this.FortressArchers = levels.get(2);
		this.FortressMages = levels.get(3);
		this.FortressKnights = Get(data, "fortress_knights");
		this.FortressUpgrades = Get(data, "fortress_upgrades");
		
		List<Long> achievements = (List<Long>) data.get("achievements_data");
		List<Long> stripped = achievements.subList(0, achievements.size() / 2);
		
		this.Achievements = stripped.stream().filter(L -> L > 0).count();
		this.AchievementsTotal = (long) stripped.size();
		
		if (data.containsKey("guild_role")) {
			this.GuildRole = Constants.GROUP_ROLES[(int) Get(data, "guild_role").intValue()];
			this.GuildRoleId = Get(data, "guild_role");
			this.GuildTreasure = Get(data, "guild_treasure");
			this.GuildInstructor = Get(data, "guild_instructor");
			this.GuildPet = Get(data, "guild_pet");
		} else {
			this.GuildRole = null;
			this.GuildRoleId = 5L;
			this.GuildTreasure = null;
			this.GuildInstructor = null;
			this.GuildPet = null;
		}
	}	
	
	public Map<String, Object> GetData() {
		return this.data;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		
		Player p = (Player) o;
		if (p == this) {
			return true;
		} else {
			return Name.equals(p.Name);
		}
	}
	
	@Override
	public int hashCode() {
		return Name.hashCode();
	}
	
	private static Long Get(Map<String, Object> d, Object key) {
		return Long.parseLong(String.valueOf(d.get(key)));
	}

}
