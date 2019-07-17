package ui.util;

public enum FXColor {

	GREEN("#70AD47"), YELLOW("#FFE943"), ORANGE("#FB4A2D");

	private final String color;

	FXColor(String color) {
		this.color = color;
	}

	public String get() {
		return this.color;
	}

}
