package sf.struct;

import java.io.Serializable;

public class Item implements Serializable {

	private static final long serialVersionUID = 1L;

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

	public Long Gem;

	/*
	 * Helper functions
	 */
	public Long getMinDamage() {
		return this.Rating1;
	}

	public Long getMaxDamage() {
		return this.Rating2;
	}

	public Long getArmor() {
		return this.Rating1;
	}

}
