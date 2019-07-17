package ui.util;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import sf.DataManager;
import sf.Player;
import sf.io.PlayerSetExporter;

public class EntryContextMenu extends ContextMenu {

	public EntryContextMenu(String label, List<Player> players) {
		ComboBox<String> box = new ComboBox<>();
		box.setItems(FXCollections.observableArrayList(DataManager.getInstance().get().keySet()));
		box.getItems().remove(label);
		box.setPrefWidth(200);

		CustomMenuItem c0 = new CustomMenuItem();
		c0.setContent(box);
		c0.setHideOnClick(false);

		MenuItem b0 = new MenuItem("Any");
		MenuItem b1 = new MenuItem("Group members");
		MenuItem b4 = new MenuItem("Any");
		MenuItem b5 = new MenuItem("Group members");
		MenuItem b3 = new MenuItem("Remove");

		Menu b01 = new Menu("Save as image");
		b01.getItems().addAll(c0, new SeparatorMenuItem(), b0, b1);
		Menu b45 = new Menu("Save as CSV");
		b45.getItems().addAll(b4, b5);

		b0.setOnAction(E -> PlayerSetExporter.asImage(label, box.getSelectionModel().getSelectedItem(), false));
		b1.setOnAction(E -> PlayerSetExporter.asImage(label, box.getSelectionModel().getSelectedItem(), true));
		b3.setOnAction(E -> DataManager.getInstance().remove(label));
		b4.setOnAction(E -> PlayerSetExporter.asCSV(label, false));
		b5.setOnAction(E -> PlayerSetExporter.asCSV(label, true));

		getItems().addAll(b01, b45, new SeparatorMenuItem(), b3);

		this.setOnHidden(E -> box.getSelectionModel().clearSelection());
	}

}
