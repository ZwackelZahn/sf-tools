package sf.struct;

import java.io.Serializable;
import java.util.Objects;

import sf.struct.defs.Attribute;

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

		score += getGearScore(this.Head);
		score += getGearScore(this.Body);
		score += getGearScore(this.Hands);
		score += getGearScore(this.Feet);

		score += getGearScore(this.Neck);
		score += getGearScore(this.Belt);
		score += getGearScore(this.Ring);
		score += getGearScore(this.Misc);

		return (long) (score / 8.0);
	}

	private Long getGearScore(Item item) {
		Long level = item.getApproximateLevel();
		Long upgrades = item.getUpgradeCount();

		Long gemType = item.getGemAttribute();

		Double levelScore = level.doubleValue() / this.Level.doubleValue();
		Double upgradeScore = upgrades.doubleValue() / 5.0;

		Double gemScore = 0.0;
		if (Objects.isNull(gemType)) {
			gemScore = 0.0;
		} else if (gemType == Attribute.ALL) {
			gemScore = 1.0;
		} else {
			if (this.Class == sf.struct.defs.Class.WARRIOR || this.Class == sf.struct.defs.Class.BATTLEMAGE || this.Class == sf.struct.defs.Class.BERSERKER) {
				gemScore = gemType == Attribute.STRENGTH ? 1.0 : 0.5;
			} else if (this.Class == sf.struct.defs.Class.SCOUT || this.Class == sf.struct.defs.Class.ASSASIN) {
				gemScore = gemType == Attribute.DEXTERITY ? 1.0 : 0.5;
			} else {
				gemScore = gemType == Attribute.INTELLIGENCE ? 1.0 : 0.5;
			}
		}

		return (long) (80.0 * levelScore + 10.0 * upgradeScore + 10.0 * gemScore);
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
