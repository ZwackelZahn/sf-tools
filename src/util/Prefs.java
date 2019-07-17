package util;

import java.util.prefs.Preferences;

public enum Prefs {

	BOOK0(1512), BOOK1(1944),

	MOUNT0(10), MOUNT1(50),

	PET0(0), PET1(270),

	KNIGHTS0(13), KNIGHTS1(15),

	HIGHLIGHT_ALL(0)

	;

	// Preferences
	private static final Preferences PREFERENCES = Preferences.userRoot().node("eu/mar21/sftools");
	static {
		for (Prefs s : Prefs.values()) {
			s.val = PREFERENCES.getLong(s.name(), s.def);
		}
	}

	// Parameters
	private Long val;
	private Long def;

	// Constructor
	Prefs(Number value) {
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
