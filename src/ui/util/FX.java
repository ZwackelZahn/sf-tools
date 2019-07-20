package ui.util;

import java.lang.reflect.Field;
import java.text.DecimalFormat;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Control;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.util.Duration;

public class FX {

	private static final DecimalFormat LONG_NUM_FORMATTER = new DecimalFormat("###,###,###,###");

	public static void col(GridPane g, HPos a, Number wid) {
		ColumnConstraints col = new ColumnConstraints();
		col.setPercentWidth(wid.doubleValue());

		if (a != null) {
			col.setHalignment(a);
		}

		g.getColumnConstraints().add(col);
	}

	public static void cola(GridPane g, HPos a, Number wid) {
		ColumnConstraints col = new ColumnConstraints();
		col.setPrefWidth(wid.doubleValue());
		col.setMinWidth(wid.doubleValue());
		col.setMaxWidth(wid.doubleValue());

		if (a != null) {
			col.setHalignment(a);
		}

		g.getColumnConstraints().add(col);
	}

	public static void clean(GridPane g) {
		g.getColumnConstraints().clear();
		g.getRowConstraints().clear();
	}

	public static void rowa(GridPane g, VPos a, Number wid) {
		RowConstraints row = new RowConstraints();
		row.setPrefHeight(wid.doubleValue());
		row.setMinHeight(wid.doubleValue());
		row.setMaxHeight(wid.doubleValue());

		if (a != null) {
			row.setValignment(a);
		}

		g.getRowConstraints().add(row);
	}

	public static void row(GridPane g, VPos a, Number wid) {
		RowConstraints row = new RowConstraints();
		row.setPercentHeight(wid.doubleValue());

		if (a != null) {
			row.setValignment(a);
		}

		g.getRowConstraints().add(row);
	}

	public static void pad(GridPane g, Number h, Number v, Number t, Number l, Number d, Number r) {
		g.setHgap(h.doubleValue());
		g.setVgap(v.doubleValue());
		g.setPadding(new Insets(t.doubleValue(), l.doubleValue(), d.doubleValue(), r.doubleValue()));
	}

	public static ProgressBar bar(Number progress, String tooltip) {
		ProgressBar bar = new ProgressBar(progress.doubleValue());
		bar.setTooltip(new Tooltip(tooltip));
		return bar;
	}

	public static void tip(Control c, String tooltip) {
		c.setTooltip(new Tooltip(tooltip));
	}

	public static String formatl(Long l) {
		return LONG_NUM_FORMATTER.format(l);
	}

	public static void setTooltipShowDelay(double duration) throws NoSuchFieldException, IllegalAccessException {
		Tooltip tooltip = new Tooltip();

		Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
		fieldBehavior.setAccessible(true);
		Object objBehavior = fieldBehavior.get(tooltip);

		Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
		fieldTimer.setAccessible(true);
		Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

		objTimer.getKeyFrames().clear();
		objTimer.getKeyFrames().add(new KeyFrame(new Duration(duration)));
	}

}
