package ui;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.json.simple.parser.ParseException;

import app.Main;
import app.TabManager;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
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
import javafx.util.Pair;
import sf.io.HARImporter;
import sf.io.SFPExporter;
import sf.io.SFPImporter;
import ui.util.FX;

public class TabFile extends Tab {

	private ScrollPane pane;

	public TabFile() {
		setText("Files");
		setClosable(false);
		setContent(createContent());
	}

	private Node createContent() {
		pane = new ScrollPane();
		pane.setPannable(true);
		pane.setHbarPolicy(ScrollBarPolicy.NEVER);
		pane.setVbarPolicy(ScrollBarPolicy.NEVER);
		pane.setPadding(new Insets(0, 50, 0, 50));

		GridPane root = new GridPane();

		FX.col(root, null, 20);
		FX.col(root, HPos.CENTER, 60);
		FX.col(root, null, 20);

		FX.row(root, null, 40);
		FX.row(root, null, 10);
		FX.row(root, null, 10);
		FX.row(root, null, 40);

		root.add(FX.label("Loading cached data", 20), 1, 1);
		root.add(new ProgressBar(-1), 1, 2);

		new Thread(() -> {
			File file = new File("sftools.sfp");
			if (file.exists()) {
				try {
					Main.PLAYERS.addAll(SFPImporter.importSFP(file));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			Platform.runLater(() -> {
				root.getChildren().clear();

				createSecondaryContent(root);
			});
		}).start();

		return root;
	}

	private void createSecondaryContent(GridPane root) {
		FX.clean(root);

		FX.col(root, null, 20);
		FX.col(root, HPos.CENTER, 60);
		FX.col(root, null, 20);

		FX.row(root, null, 10);
		FX.row(root, null, 40);
		FX.row(root, null, 15);
		FX.row(root, null, 15);
		FX.row(root, null, 5);
		FX.row(root, null, 20);

		ProgressBar bar = new ProgressBar(0);
		bar.setVisible(false);

		root.setOnDragOver(E -> {
			Dragboard db = E.getDragboard();

			if (db.hasFiles()) {
				E.acceptTransferModes(TransferMode.COPY);
			} else {
				E.consume();
			}
		});

		root.setOnDragDropped(E -> {
			List<File> files = E.getDragboard().getFiles();

			new Thread(() -> {
				bar.setVisible(true);

				double size = files.size();
				double done = 0;

				for (File f : files) {
					String name = f.getName().substring(0, f.getName().indexOf("."));
					String extr = f.getName().toLowerCase();

					if (!Main.PLAYERS.stream().anyMatch(KV -> KV.getKey().equals(name)) && extr.endsWith(".har")) {
						try {
							Main.PLAYERS.add(new Pair<>(name, HARImporter.importHAR(f)));
						} catch (IOException | ParseException e) {
							e.printStackTrace();
						}
					}

					done++;

					bar.setProgress(done / size);
				}

				try {
					SFPExporter.exportSFP(new File("sftools.sfp"), Main.PLAYERS);
				} catch (IOException e) {
					e.printStackTrace();
				}

				bar.setVisible(false);

				Platform.runLater(() -> {
					updateContent();
				});

			}).start();

			E.setDropCompleted(true);
			E.consume();
		});

		this.setOnSelectionChanged(E -> {
			Platform.runLater(() -> {
				updateContent();
			});
		});

		updateContent();

		root.add(pane, 0, 1, 3, 1);
		root.add(FX.label("Drop files here", 20), 1, 3);
		root.add(bar, 1, 4);
	}

	public void updateContent() {
		if (Main.PLAYERS.isEmpty()) {
			pane.setContent(null);
		} else {
			HBox box = new HBox();
			box.setAlignment(Pos.CENTER_LEFT);
			box.setSpacing(5);

			for (int i = 0; i < Main.PLAYERS.size(); i++) {
				createSub(box, i);
			}

			pane.setContent(box);
		}
	}

	private void createSub(HBox box, int i) {
		BorderPane root = new BorderPane();

		root.setOnMouseClicked(E -> {
			if (E.isStillSincePress() && E.isControlDown() && E.getButton().equals(MouseButton.PRIMARY)) {
				E.consume();

				Main.PLAYERS.remove(i);

				try {
					SFPExporter.exportSFP(new File("sftools.sfp"), Main.PLAYERS);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (E.isStillSincePress() && E.getButton().equals(MouseButton.PRIMARY)) {
				E.consume();
				TabManager.toggle(TabManager.getLabel(i));

				Platform.runLater(() -> {
					updateContent();
				});
			}
		});

		if (TabManager.isOpen(TabManager.getLabel(i))) {
			root.setStyle("-fx-background-color: accent_color;");
		} else {
			root.setStyle("-fx-background-color: tab_pane_background_color;");
		}

		Label label = FX.label(Main.PLAYERS.get(i).getKey(), 18);
		label.setPadding(new Insets(0, 10, 0, 10));
		label.setWrapText(true);
		label.setTextAlignment(TextAlignment.CENTER);

		root.setCenter(label);

		root.setPrefSize(200, 200);
		root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

		box.getChildren().add(root);
	}
}
