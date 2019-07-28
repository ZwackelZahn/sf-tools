package sf.struct;

import java.io.Serializable;

public class Player implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Values
	 */
	public String Name;

	public Long Book;
	public Long XP;
	public Long XPNext;
	public Long Level;

	public Long Mount;

	public Long Race;
	public Long Sex;
	public Long Class;

	public Long HonorPlayer;
	public Long HonorFortress;
	public Long RankPlayer;
	public Long RankFortress;

	public Long Potion1;
	public Long Potion2;
	public Long Potion3;

	public Long PotionDuration1;
	public Long PotionDuration2;
	public Long PotionDuration3;

	public Long Armor;
	public Long Strength;
	public Long Dexterity;
	public Long Intelligence;
	public Long Constitution;
	public Long Luck;

	public Long FortressWall;
	public Long FortressWarriors;
	public Long FortressArchers;
	public Long FortressMages;
	public Long FortressKnights;
	public Long FortressUpgrades;

	public Item Head;
	public Item Body;
	public Item Hands;
	public Item Feet;
	public Item Neck;
	public Item Belt;
	public Item Ring;
	public Item Misc;
	public Item Weapon;
	public Item WeaponSecondary;

	public Long Achievements;

	public String Guild;
	public Long GuildRole;
	public Long GuildTreasure;
	public Long GuildInstructor;
	public Long GuildPet;

	/*
	 * Player gear score calculation
	 */
	public Long getGearScore() {
		Double score = 0D;

		score += getItemScore(this.Head);
		score += getItemScore(this.Body);
		score += getItemScore(this.Hands);
		score += getItemScore(this.Feet);

		score += getItemScore(this.Neck);
		score += getItemScore(this.Belt);
		score += getItemScore(this.Ring);
		score += getItemScore(this.Misc);

		score += getWeaponScore(this.Weapon);

		if (this.Class == 1 || this.Class == 4) {
			score += getWeaponScore(this.WeaponSecondary);

			score /= 10D;
		} else {
			score /= 9D;
		}

		return score.longValue();
	}

	private Double getBaseScore(Item i) {
		Double attr = i.Attribute1.doubleValue();
		Double amod = 1.0;
		Double cmod = 1.0;

		if (i.Attribute1Type == 6) {
			amod = 1.15;
		} else if (i.Attribute3Type == 5) {
			amod = 1.35;
		} else {
			amod = 2.35;
		}

		if (i.Attribute1Type != 6) {
			if (this.Class == 1 || this.Class >= 5) {
				cmod = i.Attribute1Type == 1 ? 1.0 : 0.8;
			} else if (this.Class >= 3) {
				cmod = i.Attribute1Type == 2 ? 1.0 : 0.8;
			} else {
				cmod = i.Attribute1Type == 3 ? 1.0 : 0.8;
			}
		}

		return (attr / (this.Level.doubleValue() * amod)) * cmod;
	}

	private Double getItemScore(Item i) {
		if (i.ItemID == 0) {
			return 0.0;
		}

		Double attrScore = getBaseScore(i);
		Double armorScore = 1.0;

		return 100.0 * armorScore * attrScore;
	}

	private Double getWeaponScore(Item i) {
		if (i.ItemID == 0) {
			return 0.0;
		}

		Double attrScore = getBaseScore(i);
		Double damageScore = 1.0;

		return 100.0 * damageScore * attrScore;
	}

	/*
	 * Comparison overrides
	 */
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

}
