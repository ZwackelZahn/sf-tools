package ui.util;

public class FXStyle {

	private static final String TEXTCOLOR = "text_color";
	private static final String ACCENTCOLOR = "accent_color";

	public static FXStyle textColor() {
		return new FXStyle(TEXTCOLOR);
	}

	public static FXStyle accentColor() {
		return new FXStyle(ACCENTCOLOR);
	}

	// Private
	private String base;
	private String color;
	private Boolean enable;

	private FXStyle(String base) {
		this.base = base;
		this.enable = true;
	}

	public FXStyle setIf(boolean enable) {
		this.enable = enable;
		return this;
	}

	public FXStyle setIf(FXColor color, boolean enable) {
		if (enable) {
			this.color = color.get();
		}

		return this;
	}

	public String get() {
		if (this.enable) {
			return String.format("%s:%s;", base, color);
		} else {
			return null;
		}
	}

}
