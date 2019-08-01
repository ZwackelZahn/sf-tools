package sf.struct;

import java.io.Serializable;

import sf.struct.defs.Attribute;

public class Item implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Double MULT = Math.pow(2, 16);

	/*
	 * Values
	 */
	public Long SlotID;
	public Long ItemID;

	public Long Rating1;
	public Long Rating2;

	public Long Attribute1Type;
	public Long Attribute2Type;
	public Long Attribute3Type;

	public Long Attribute1;
	public Long Attribute2;
	public Long Attribute3;

	public Long Value;

	public Long Mods;

	/*
	 * Helper functions
	 */
	public Long getMinDamage() {
		return this.Rating1;
	}

	public Long getMaxDamage() {
		return this.Rating2;
	}

	public Long getAverageDamage() {
		return (this.Rating1 + this.Rating2) / 2;
	}

	public Long getArmor() {
		return this.Rating1;
	}

	public Long getGemValue() {
		return (long) ((this.Mods.doubleValue() - this.Mods.doubleValue() % MULT) / MULT);
	}

	public Long getUpgradeCount() {
		return (long) ((this.Mods.doubleValue() % MULT) / 256.0);
	}

	public Double getUpgradeValue() {
		return 0.032 * getUpgradeCount().doubleValue();
	}

	public Boolean isEnchanted() {
		return ItemID != (ItemID % Math.pow(2, 16));
	}

	public Long getGemAttribute() {
		Long gem = (long) (((this.SlotID.doubleValue() - (this.SlotID.doubleValue() % MULT)) / MULT) % 64.0);

		if (gem <= 1) {
			return null;
		} else if (gem % 10 == 0) {
			return Attribute.STRENGTH;
		} else if (gem % 10 == 1) {
			return Attribute.INTELLIGENCE;
		} else if (gem % 10 == 2) {
			return Attribute.DEXTERITY;
		} else if (gem % 10 == 3) {
			return Attribute.CONSTITUTION;
		} else if (gem % 10 == 4) {
			return Attribute.LUCK;
		} else if (gem % 10 == 5) {
			return Attribute.ALL;
		} else {
			return null;
		}
	}

	public Long getApproximateLevel() {
		Double attributes = this.Attribute1.doubleValue() + this.Attribute2.doubleValue() + this.Attribute3.doubleValue();

		attributes /= (1 + getUpgradeValue());

		Double itemLevel = 0.0;

		if (this.Attribute1Type.equals(Attribute.ALL)) {
			itemLevel = 5.0 * attributes / 17.2;
		} else if (this.Attribute3Type.equals(Attribute.LUCK)) {
			itemLevel = attributes / 4.3;
		} else {
			itemLevel = attributes / 2.15;
		}

		return itemLevel.longValue();
	}

}
