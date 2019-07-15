package util;

import java.util.prefs.Preferences;

public enum Data {

	BOOK0(1512), BOOK1(1944),

	MOUNT0(10), MOUNT1(50),

	PET0(0), PET1(270),

	KNIGHTS0(13), KNIGHTS1(15),

	;

	// Preferences
	private static final Preferences PREFERENCES = Preferences.userRoot().node("eu/mar21/sftools");
	static {
		for (Data s : Data.values()) {
			s.val = s.def;
			PREFERENCES.putLong(s.name(), s.val);
		}
	}

	// Parameters
	private Long val;
	private Long def;

	// Constructor
	Data(Number value) {
		val = value.longValue();
		def = value.longValue();
	}

	// Setters & Getters
	public Long val() {
		return this.val;
	}

	public void val(Number value) {
		this.val = value.longValue();

		PREFERENCES.putLong(name(), value.longValue());
	}
}
