package app;

import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Pair;
import jfxtras.styles.jmetro8.JMetro;
import sf.Player;
import ui.util.FX;

public class Main extends Application {

	public static ObservableList<Pair<String, List<Player>>> PLAYERS = FXCollections.observableArrayList();

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		try {
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
}
