package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jfxtras.styles.jmetro8.JMetro;
import sf.DataManager;
import ui.util.FX;

public class Main extends Application {

	public static Stage STAGE;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		try {
			STAGE = stage;

			FX.setTooltipDuration(5);

			Scene scene = new Scene(TabManager.build(), 1024, 576);

			// Styles
			new JMetro(JMetro.Style.DARK).applyTheme(scene);
			scene.getStylesheets().add(ClassLoader.getSystemResource("app.css").toExternalForm());

			stage.setTitle("SF Tools 1.0");
			stage.setResizable(false);
			stage.setScene(scene);
			stage.centerOnScreen();
			stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		DataManager.INSTANCE.requestSync();
	}
}
