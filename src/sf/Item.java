package sf;

import java.io.Serializable;
import java.util.List;

public class Item implements Serializable {

	private static final long serialVersionUID = 41L;

	public final Long SlotID;
	public final Long ItemID;

	public final Long Rating1;
	public final Long Rating2;

	public final Long Attribute1Type;
	public final Long Attribute2Type;
	public final Long Attribute3Type;

	public final Long Attribute1;
	public final Long Attribute2;
	public final Long Attribute3;

	public final Long Value;

	public final Long Gem;

	public Item(List<Long> data) {
		this.SlotID = data.get(0);
		this.ItemID = data.get(1);
		this.Rating1 = data.get(2);
		this.Rating2 = data.get(3);
		this.Attribute1Type = data.get(4);
		this.Attribute2Type = data.get(5);
		this.Attribute3Type = data.get(6);
		this.Attribute1 = data.get(7);
		this.Attribute2 = data.get(8);
		this.Attribute3 = data.get(9);
		this.Value = data.get(10) / 100L;
		this.Gem = data.get(11);
	}

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
