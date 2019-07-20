package app;

import java.io.File;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jfxtras.styles.jmetro8.JMetro;
import ui.util.FX;

public class Main extends Application {

	/*
	 * Stage variable for file dialogs
	 */
	private static Stage STAGE = null;

	/*
	 * Helper function for creating file dialogs
	 */
	public static File createFileDialog(String title, boolean isSaveDialog, String initialFileName, FileChooser.ExtensionFilter... extensionFilters) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle(title);
		chooser.setInitialFileName(initialFileName);
		chooser.getExtensionFilters().addAll(extensionFilters);

		if (isSaveDialog) {
			return chooser.showSaveDialog(STAGE);
		} else {
			return chooser.showOpenDialog(STAGE);
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		STAGE = stage;

		/*
		 * Fix for long pop-up delays, shortens initial time to 5 milliseconds
		 */
		FX.setTooltipShowDelay(5);

		/*
		 * Create scene and set styles
		 */
		Scene scene = new Scene(TabManager.INSTANCE.getRoot(), 1024, 576);
		new JMetro(JMetro.Style.DARK).applyTheme(scene);
		scene.getStylesheets().add(ClassLoader.getSystemResource("app.css").toExternalForm());

		/*
		 * Configure and show stage
		 */
		stage.setTitle("SF Tools 1.0.3");
		stage.setResizable(false);
		stage.setScene(scene);
		stage.centerOnScreen();
		stage.show();
	}

	@Override
	public void stop() {
		DataManager.INSTANCE.requestSync();
	}
}
