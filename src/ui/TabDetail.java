package ui;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import sf.DataManager;
import sf.Player;
import ui.util.FX;
import ui.util.PlayerSnapshot;
import util.Data;

public class TabDetail extends Tab {

	public TabDetail(String name, List<Player> players) {
		setText(String.format("List %s", name));
		setClosable(true);
		setContent(createContent(name, players));
	}

	private Node createContent(String name, List<Player> players) {
		BorderPane root = new BorderPane();
		GridPane grid = new GridPane();

		ListView<String> playerList = new ListView<>();
		playerList.setItems(FXCollections.observableArrayList(players.stream().map(P -> P.Name).collect(Collectors.toList())));
		playerList.setOrientation(Orientation.VERTICAL);
		playerList.getSelectionModel().selectedIndexProperty().addListener((property, o, n) -> {
			createSecondaryContent(grid, players.get(n.intValue()));
		});
		playerList.getSelectionModel().selectFirst();

		root.setLeft(playerList);
		root.setCenter(grid);

		setOnSelectionChanged(E -> {
			createSecondaryContent(grid, players.get(playerList.getSelectionModel().getSelectedIndex()));
		});

		{
			MenuItem b0 = new MenuItem("Any");
			MenuItem b1 = new MenuItem("Group members");
			MenuItem b2 = new SeparatorMenuItem();
			MenuItem b3 = new MenuItem("Remove");

			Menu b01 = new Menu("Save as image");
			b01.getItems().addAll(b0, b1);
		
			b0.setOnAction(E -> PlayerSnapshot.save(name, players, false));
			b1.setOnAction(E -> PlayerSnapshot.save(name, players, true));
			b3.setOnAction(E -> DataManager.getInstance().remove(name));

			setContextMenu(new ContextMenu(b01, b2, b3));
		}

		return root;
	}

	private void createSecondaryContent(GridPane root, Player p) {
		root.getChildren().clear();

		FX.clean(root);

		FX.pad(root, 5, 0, 0, 0, 0, 0);

		FX.col(root, HPos.CENTER, 2);
		FX.col(root, HPos.LEFT, 15);
		FX.col(root, HPos.CENTER, 8);
		FX.col(root, HPos.LEFT, 15);
		FX.col(root, HPos.CENTER, 8);
		FX.col(root, HPos.CENTER, 4);
		FX.col(root, HPos.LEFT, 15);
		FX.col(root, HPos.CENTER, 8);
		FX.col(root, HPos.LEFT, 15);
		FX.col(root, HPos.CENTER, 8);
		FX.col(root, HPos.CENTER, 2);

		FX.row(root, VPos.BOTTOM, 8);
		FX.row(root, VPos.CENTER, 6);
		FX.row(root, VPos.TOP, 6);
		FX.row(root, VPos.BOTTOM, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 1);
		FX.row(root, VPos.BOTTOM, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 1);
		FX.row(root, VPos.BOTTOM, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);
		FX.row(root, null, 5);

		root.add(FX.label(String.format("%s (%d)", p.Name, p.Level), 25), 0, 0, 11, 1);
		root.add(FX.label(p.Guild != null ? p.Guild : "", 16), 0, 1, 11, 1);
		root.add(FX.bar(1.0 * p.XP / p.XPNext, String.format("%s out of %s XP left to next level", FX.formatl(p.XPNext - p.XP), FX.formatl(p.XPNext))), 1, 2, 9, 1);

		Label l1 = FX.labelh("Mount & Potions");
		Label l2 = FX.labelh("Rankings");
		Label l3 = FX.labelh("Group");
		Label l4 = FX.labelh("Collectibles");
		Label l5 = FX.labelh("Attributes");
		Label l6 = FX.labelh("Fortress");

		root.add(l1, 1, 3, 4, 1);
		root.add(FX.label("Mount:"), 1, 4);
		root.add(FX.label("Potions:"), 1, 6);

		root.add(l2, 1, 10, 4, 1);
		root.add(FX.label("Player:"), 1, 12);
		root.add(FX.label("Fortress:"), 1, 13);
		root.add(FX.label("Rank"), 2, 11);
		root.add(FX.label("Honor"), 3, 11);

		if (p.GuildRole != null) {
			root.add(l3, 1, 15, 4, 1);
			root.add(FX.label("Position"), 1, 16);
			root.add(FX.label("Treasure"), 1, 18);
			root.add(FX.label("Instructor"), 1, 19);
			root.add(FX.label("Pet"), 3, 18);
			root.add(FX.label("Knights"), 3, 19);
		}

		root.add(l4, 6, 3, 4, 1);
		root.add(FX.label("Scrapbook"), 6, 4);
		root.add(FX.label("Achievements"), 6, 5);

		root.add(l5, 6, 10, 4, 1);
		root.add(FX.label("Strength"), 6, 11);
		root.add(FX.label("Dexterity"), 6, 12);
		root.add(FX.label("Intelligence"), 6, 13);
		root.add(FX.label("Constitution"), 8, 11);
		root.add(FX.label("Luck"), 8, 12);
		root.add(FX.label("Armor"), 8, 13);

		root.add(l6, 6, 15, 4, 1);
		root.add(FX.label("Upgrades"), 6, 16);
		root.add(FX.label("Wall"), 8, 16);
		root.add(FX.label("Warriors"), 8, 17);
		root.add(FX.label("Archers"), 8, 18);
		root.add(FX.label("Mages"), 8, 19);

		if (p.GuildRole != null) {
			root.add(FX.labelc(p.Mount, Data.MOUNT0.val(), Data.MOUNT1.val()), 2, 4);
		} else {
			root.add(FX.label(p.Mount.toString()), 2, 4);
		}

		int pot = 0;
		if (p.PotionDuration1 != 0) {
			root.add(FX.label(MessageFormat.format("{0}", p.Potion1)), 3, 6 + pot);
			root.add(FX.label(MessageFormat.format("+{0}%", p.PotionDuration1)), 2, 6 + pot);
			pot++;
		}

		if (p.PotionDuration2 != 0) {
			root.add(FX.label(MessageFormat.format("{0}", p.Potion2)), 3, 6 + pot);
			root.add(FX.label(MessageFormat.format("+{0}%", p.PotionDuration2)), 2, 6 + pot);
			pot++;
		}

		if (p.PotionDuration3 != 0) {
			root.add(FX.label(MessageFormat.format("{0}", p.Potion3)), 3, 6 + pot);
			root.add(FX.label(MessageFormat.format("+{0}%", p.PotionDuration3)), 2, 6 + pot);
			pot++;
		}

		root.add(FX.label(p.RankPlayer.toString()), 2, 12);
		root.add(FX.label(p.RankFortress.toString()), 2, 13);
		root.add(FX.label(p.HonorPlayer.toString()), 3, 12);
		root.add(FX.label(p.HonorFortress.toString()), 3, 13);

		if (p.GuildRole != null) {
			root.add(FX.label(p.GuildRole), 2, 16);
			root.add(FX.label(p.GuildTreasure.toString()), 2, 18);
			root.add(FX.label(p.GuildInstructor.toString()), 2, 19);
			root.add(FX.labelc(p.GuildPet, Data.PET0.val(), Data.PET1.val()), 4, 18);
			root.add(FX.labelc(p.FortressKnights, Data.KNIGHTS0.val(), Data.KNIGHTS1.val()), 4, 19);
		}

		ProgressBar bookBar;

		if (p.GuildRole != null) {
			bookBar = FX.barc(p.Book.doubleValue() / 2160D, Data.BOOK0.val().doubleValue() / 2160D, Data.BOOK1.val().doubleValue() / 2160D);
		} else {
			bookBar = new ProgressBar(p.Book.doubleValue() / 2160D);
		}

		FX.tip(bookBar, MessageFormat.format("{0} ({1}%) out of {2} items collected", p.Book, (int) (100D * p.Book.doubleValue() / 2160D), 2160));

		ProgressBar achievementBar = FX.bar(p.Achievements.doubleValue() / p.AchievementsTotal.doubleValue(), String.format("%d out of %d achievements collected", p.Achievements, p.AchievementsTotal));

		root.add(bookBar, 7, 4, 3, 1);
		root.add(achievementBar, 7, 5, 3, 1);

		root.add(FX.label(p.Strength.toString()), 7, 11);
		root.add(FX.label(p.Dexterity.toString()), 7, 12);
		root.add(FX.label(p.Intelligence.toString()), 7, 13);
		root.add(FX.label(p.Constitution.toString()), 9, 11);
		root.add(FX.label(p.Luck.toString()), 9, 12);
		root.add(FX.label(p.Armor.toString()), 9, 13);

		root.add(FX.label(p.FortressUpgrades.toString()), 7, 16);
		root.add(FX.label(p.FortressWall.toString()), 9, 16);
		root.add(FX.label(p.FortressWarriors.toString()), 9, 17);
		root.add(FX.label(p.FortressArchers.toString()), 9, 18);
		root.add(FX.label(p.FortressMages.toString()), 9, 19);
	}

}