package ui.util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class FXLabel extends Label {

	public FXLabel(String format, Object... args) {
		super(String.format(format, args));
	}

	public FXLabel(Number text) {
		super(text == null ? "" : text.toString());
	}

	public FXLabel(String text) {
		super(text == null ? "" : text);
	}

	public FXLabel style(String style) {
		setStyle(style);

		return this;
	}

	public FXLabel font(Number size) {
		setFont(Font.font(size.doubleValue()));

		return this;
	}

	public FXLabel font(Number size, String family) {
		setFont(Font.font(family, size.doubleValue()));

		return this;
	}

	public FXLabel font(Number size, FontWeight weight) {
		setFont(Font.font(null, weight, size.doubleValue()));

		return this;
	}

	public FXLabel font(FontWeight weight) {
		setFont(Font.font(null, weight, -1));

		return this;
	}

	public FXLabel font(Number size, String family, FontWeight weight) {
		setFont(Font.font(family, weight, size.doubleValue()));

		return this;
	}

	public FXLabel align(Pos align) {
		setAlignment(align);

		return this;
	}

	public FXLabel align(TextAlignment align) {
		setTextAlignment(align);

		return this;
	}

	public FXLabel wrap(boolean wrap) {
		setWrapText(wrap);

		return this;
	}

}
