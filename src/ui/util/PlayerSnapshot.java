package ui.util;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import app.Main;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import sf.Player;
import util.Data;

public class PlayerSnapshot {

	public static void save(String name, List<Player> players, boolean onlyMembers) {
		List<WritableImage> images = new ArrayList<>();

		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save as");
		chooser.setInitialFileName(name);
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG file (*.png)", "*.png"));
		File file = chooser.showSaveDialog(Main.STAGE);

		if (file != null) {
			for (int i = 0; i < players.size() / 50 + 1; i++) {
				images.add(savePartial(players.subList(i * 50, Math.min(50 + i * 50, players.size())), onlyMembers));

				if (onlyMembers) {
					break;
				}
			}

			try {
				String filepath = file.getPath();
				String path = filepath.substring(0, filepath.lastIndexOf("."));

				for (int i = 0; i < images.size(); i++) {
					File f = new File(String.format("%s%s.png", path, i > 0 ? String.valueOf(i) : ""));

					ImageIO.write(SwingFXUtils.fromFXImage(images.get(i), null), "png", f);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static WritableImage savePartial(List<Player> players, boolean onlyMembers) {
		GridPane root = new GridPane();

		FX.col(root, HPos.CENTER, 22);
		FX.cola(root, HPos.CENTER, 1);
		FX.col(root, HPos.CENTER, 8);
		FX.col(root, HPos.CENTER, 8);
		FX.col(root, HPos.CENTER, 8);
		FX.cola(root, HPos.CENTER, 1);
		FX.col(root, HPos.CENTER, 8);
		FX.cola(root, HPos.CENTER, 1);
		FX.col(root, HPos.CENTER, 2.5);
		FX.col(root, HPos.CENTER, 2.5);
		FX.col(root, HPos.CENTER, 2.5);
		FX.cola(root, HPos.CENTER, 1);
		FX.col(root, HPos.CENTER, 8);
		FX.col(root, HPos.CENTER, 8);
		FX.col(root, HPos.CENTER, 8);
		FX.col(root, HPos.CENTER, 8);

		FX.row(root, null, 2);
		FX.row(root, null, 2);
		FX.rowa(root, null, 2);

		for (int i = 0; i < 25; i++) {
			FX.row(root, null, -1);
			FX.row(root, null, -1);
			FX.rowa(root, null, 1);
		}

		createCell(root, "Name", 0, 0, 1, 2);
		createCell(root, "General", 2, 0, 6, 1);
		createCell(root, "Level", 2, 1, 1, 1);
		createCell(root, "Album", 3, 1, 1, 1);
		createCell(root, "Mount", 4, 1, 1, 1);
		createCell(root, "Awards", 6, 1, 1, 1);
		createCell(root, "Potions", 8, 0, 3, 2);
		createCell(root, "Guild", 12, 0, 4, 1);
		createCell(root, "Treasure", 12, 1, 1, 1);
		createCell(root, "Instructor", 13, 1, 1, 1);
		createCell(root, "Pet", 14, 1, 1, 1);
		createCell(root, "Knights", 15, 1, 1, 1);

		createBorder(root, 0, 2, 16, 1);
		createBorder(root, 1, 0, 1, 78);
		createBorder(root, 5, 2, 1, 76);
		createBorder(root, 7, 0, 1, 78);
		createBorder(root, 11, 0, 1, 78);

		int row = 3;
		for (int i = 0; i < players.size(); i++) {
			Player p = players.get(i);

			if (onlyMembers && p.GuildRole == null) {
				continue;
			} else {
				createCell(root, 0, row, p.Name, null);
				createCell(root, 2, row, p.Level.toString(), null);

				if (p.GuildRole != null) {
					createCell(root, 3, row, MessageFormat.format("{0,number,##.#}%", 100D * p.Book.doubleValue() / 2160D), getColor(p.Book, Data.BOOK0.val(), Data.BOOK1.val()));
					createCell(root, 4, row, p.Mount.toString(), getColor(p.Mount, Data.MOUNT0.val(), Data.MOUNT1.val()));

					createCell(root, 12, row, p.GuildTreasure.toString(), null);
					createCell(root, 13, row, p.GuildInstructor.toString(), null);
					createCell(root, 14, row, p.GuildPet.toString(), getColor(p.GuildPet, Data.PET0.val(), Data.PET1.val()));
					createCell(root, 15, row, p.FortressKnights.toString(), getColor(p.FortressKnights, Data.KNIGHTS0.val(), Data.KNIGHTS1.val()));
				} else {
					createCell(root, 3, row, MessageFormat.format("{0,number,##.#}%", 100D * p.Book.doubleValue() / 2160D), null);
					createCell(root, 4, row, p.Mount.toString(), null);
				}

				createCell(root, 6, row, p.Achievements.toString(), null);

				createCell(root, 8, row, "", getColor(p.PotionDuration1, 5, 25));
				createCell(root, 9, row, "", getColor(p.PotionDuration2, 5, 25));
				createCell(root, 10, row, "", getColor(p.PotionDuration3, 5, 25));

				row++;

				if (i % 2 == 1) {
					createBorder(root, 0, row, 16, 1);

					row++;
				}
			}
		}

		WritableImage image = new WritableImage(752, 912);
		new Scene(root, 800, 912);
		root.snapshot(null, image);

		return image;
	}

	private static void createBorder(GridPane root, int c, int r, int cc, int rr) {
		StackPane stack = new StackPane();
		stack.getChildren().add(FX.label(" ", 0.2));
		GridPane.setFillHeight(stack, true);
		GridPane.setFillWidth(stack, true);
		stack.setStyle("-fx-background-color: black;");
		root.add(stack, c, r, cc, rr);
	}

	private static void createCell(GridPane root, String text, int c, int r, int cc, int rr) {
		StackPane stack = new StackPane();

		stack.setAlignment(Pos.CENTER);
		stack.getChildren().add(FX.labelb(text));

		GridPane.setFillHeight(stack, true);
		GridPane.setFillWidth(stack, true);

		root.add(stack, c, r, cc, rr);
	}

	private static void createCell(GridPane root, int col, int row, String text, String color) {
		StackPane stack = new StackPane();
		stack.setAlignment(Pos.CENTER);

		if (color != null) {
			stack.setStyle(String.format("-fx-background-color: %s", color));
		}

		stack.getChildren().add(new Label(text));
		GridPane.setFillHeight(stack, true);
		GridPane.setFillWidth(stack, true);
		root.add(stack, col, row);
	}

	private static String getColor(Long number, Number a, Number b) {
		if (number < a.longValue()) {
			return "#FB4A2D";
		} else if (number < b.longValue()) {
			return "#FFE943";
		} else {
			return "#70AD47";
		}
	}

}
