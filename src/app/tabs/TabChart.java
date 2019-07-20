package app.tabs;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;
import ui.util.FXLabel;

public class TabChart extends Tab {

	public TabChart() {
		setText("Chart");
		setClosable(false);
		setContent(createContent());
	}

	private Node createContent() {
		VBox root = new VBox();
		root.setAlignment(Pos.CENTER);
		
		root.getChildren().add(new FXLabel("Nothing here yet :(").font(25));
		
		return root;
	}

}
