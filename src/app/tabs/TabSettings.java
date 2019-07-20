package app.tabs;

import java.text.MessageFormat;

import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import ui.util.FX;
import ui.util.FXLabel;
import util.Prefs;

public class TabSettings extends Tab {

	public TabSettings() {
		setText("Settings");
		setClosable(false);
		setContent(createContent());
	}

	private Node createContent() {
		GridPane root = new GridPane();

		FX.col(root, null, 5);
		FX.col(root, HPos.LEFT, 15);
		FX.col(root, HPos.CENTER, 20);
		FX.col(root, HPos.CENTER, 5);
		FX.col(root, null, 10);
		FX.col(root, HPos.LEFT, 15);
		FX.col(root, HPos.CENTER, 20);
		FX.col(root, HPos.CENTER, 5);
		FX.col(root, null, 5);

		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 10);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 10);
		FX.row(root, null, 5);

		createControlBlock(root, 0, 0, "Scrapbook highlighting", Prefs.BOOK0, Prefs.BOOK1, 0L, 2160L, true);
		createControlBlock(root, 1, 0, "Mount highlighting", Prefs.MOUNT0, Prefs.MOUNT1, 0L, 50L, false);
		createControlBlock(root, 0, 1, "Pet highlighting", Prefs.PET0, Prefs.PET1, 0L, 1000L, false);
		createControlBlock(root, 1, 1, "Knight highlighting", Prefs.KNIGHTS0, Prefs.KNIGHTS1, 0L, 20L, false);
		createControlBlock(root, 0, 2, "Highlighting", "Apply to all players", Prefs.HIGHLIGHT_ALL);

		return root;
	}

	private void createControlBlock(GridPane root, int col, int row, String text, String sub, Prefs a) {
		int c = 1 + col * 4;
		int r = 1 + row * 4;

		Label label = new FXLabel(text).font(15, FontWeight.BOLD);
		CheckBox box = new CheckBox(sub);

		box.setSelected(a.val() == 1);
		box.setOnAction(E -> {
			a.val(box.isSelected() ? 1 : 0);
		});
		
		root.add(label, c, r, 2, 1);
		root.add(box, c, r + 1);
	}

	private void createControlBlock(GridPane root, int col, int row, String text, Prefs a, Prefs b, Long min, Long max, boolean percent) {
		int c = 1 + col * 4;
		int r = 1 + row * 4;

		Label label = new Label(text);
		label.setFont(Font.font(null, FontWeight.BOLD, 15));

		Label la = new Label("");
		Label lb = new Label("");

		Slider sa = new Slider();
		sa.setOrientation(Orientation.HORIZONTAL);

		Slider sb = new Slider();
		sb.setOrientation(Orientation.HORIZONTAL);

		if (percent) {
			sa.setMin(0);
			sa.setMax(100);
			sa.setValue(100D * a.val().doubleValue() / max.doubleValue());
			sb.setMin(0);
			sb.setMax(100);
			sb.setValue(100D * b.val().doubleValue() / max.doubleValue());
			la.setText(MessageFormat.format("{0}%", 100D * a.val().doubleValue() / max.doubleValue()));
			lb.setText(MessageFormat.format("{0}%", 100D * b.val().doubleValue() / max.doubleValue()));
		} else {
			sa.setMin(min);
			sa.setMax(max);
			sa.setValue(a.val());
			sb.setMin(min);
			sb.setMax(max);
			sb.setValue(b.val());
			la.setText(a.val().toString());
			lb.setText(b.val().toString());
		}

		sa.valueProperty().addListener((P, O, N) -> {
			if (N.doubleValue() > sb.getValue()) {
				sa.setValue(O.doubleValue());
			} else {
				if (percent) {
					a.val(N.doubleValue() * max.doubleValue() / 100D);
					la.setText(MessageFormat.format("{0}%", N.intValue()));
				} else {
					a.val(N.longValue());
					la.setText(String.valueOf(N.intValue()));
				}
			}
		});

		sb.valueProperty().addListener((P, O, N) -> {
			if (N.doubleValue() < sa.getValue()) {
				sb.setValue(O.doubleValue());
			} else {
				if (percent) {
					b.val(N.doubleValue() * max.doubleValue() / 100D);
					lb.setText(MessageFormat.format("{0}%", N.intValue()));
				} else {
					b.val(N.longValue());
					lb.setText(String.valueOf(N.intValue()));
				}
			}
		});

		root.add(label, c, r, 2, 1);
		root.add(new Label("Minimal"), c, r + 1);
		root.add(new Label("Requested"), c, r + 2);
		root.add(sa, c + 1, r + 1);
		root.add(sb, c + 1, r + 2);
		root.add(la, c + 2, r + 1);
		root.add(lb, c + 2, r + 2);
	}
}
