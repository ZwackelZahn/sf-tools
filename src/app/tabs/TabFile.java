package app.tabs;

import java.io.File;
import java.util.List;

import app.DataManager;
import app.TabManager;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Tab;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;
import ui.util.EntryContextMenu;
import ui.util.FX;
import ui.util.FXLabel;

public class TabFile extends Tab {

	/*
	 * Variables
	 */
	private GridPane root;
	private HBox scrollBox;

	/*
	 * Constructor
	 */
	public TabFile() {
		this.scrollBox = new HBox();
		this.scrollBox.setAlignment(Pos.CENTER_LEFT);
		this.scrollBox.setSpacing(5);

		this.root = new GridPane();
		FX.col(this.root, null, 20);
		FX.col(this.root, HPos.CENTER, 60);
		FX.col(this.root, null, 20);
		FX.row(this.root, null, 40);
		FX.row(this.root, null, 10);
		FX.row(this.root, null, 10);
		FX.row(this.root, null, 40);

		this.root.add(new FXLabel("Loading cached data").font(20), 1, 1);
		this.root.add(new ProgressBar(-1), 1, 2);

		new Thread(() -> {
			DataManager.INSTANCE.loadArchive();
			
			Platform.runLater(() -> {
				root.getChildren().clear();

				createContent();
			});
		}).start();

		setText("Files");
		setContent(this.root);
	}

	/*
	 * Content
	 */
	private void createContent() {
		FX.clean(this.root);
		FX.col(this.root, null, 20);
		FX.col(this.root, HPos.CENTER, 60);
		FX.col(this.root, null, 20);
		FX.row(this.root, null, 10);
		FX.row(this.root, null, 40);
		FX.row(this.root, null, 15);
		FX.row(this.root, null, 15);
		FX.row(this.root, null, 5);
		FX.row(this.root, null, 20);

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setPannable(true);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setVbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setPadding(new Insets(0, 50, 0, 50));
		scrollPane.setContent(this.scrollBox);

		ProgressBar bar = new ProgressBar(0);
		bar.setVisible(false);

		this.root.add(scrollPane, 0, 1, 3, 1);
		this.root.add(new FXLabel("Drop files here").font(20), 1, 3);
		this.root.add(bar, 1, 4);

		this.root.setOnDragOver(E -> {
			Dragboard db = E.getDragboard();

			if (db.hasFiles()) {
				E.acceptTransferModes(TransferMode.COPY);
			} else {
				E.consume();
			}
		});

		this.root.setOnDragDropped(E -> {
			List<File> files = E.getDragboard().getFiles();

			new Thread(() -> {
				bar.setVisible(true);

				double size = files.size();
				double done = 0;

				for (File f : files) {
					DataManager.INSTANCE.loadHAR(f);

					bar.setProgress(++done / size);
				}

				bar.setVisible(false);
			}).start();

			E.setDropCompleted(true);
			E.consume();
		});

		update();
	}

	/*
	 * Update fills the scroll pane with entries
	 */
	public void update() {
		this.scrollBox.getChildren().clear();

		for (String key : DataManager.INSTANCE.getOrder()) {
			createBox(key);
		}
	}

	public void createBox(String key) {
		boolean selected = key.equals(TabManager.INSTANCE.getDetailsTab().getSelectedKey());

		BorderPane box = new BorderPane();
		box.setPrefSize(200, 200);
		box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		box.setStyle(String.format("-fx-background-color: %s_color;", selected ? "accent" : "tab_pane_background"));
		box.setCenter(new FXLabel(key).font(18).wrap(true).align(TextAlignment.CENTER));

		ContextMenu menu = new EntryContextMenu(key);
		box.setOnContextMenuRequested(E -> {
			if (menu.isShowing()) {
				menu.hide();
			}

			menu.show(box, E.getScreenX(), E.getScreenY());
		});

		box.setOnMouseClicked(E -> {
			if (E.isStillSincePress() && E.getButton().equals(MouseButton.PRIMARY)) {
				E.consume();

				if (selected) {
					TabManager.INSTANCE.getDetailsTab().clearKey();
				} else {
					TabManager.INSTANCE.getDetailsTab().selectKey(key);
				}

				update();
			}
		});

		this.scrollBox.getChildren().add(box);
	}

}
