package app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import sf.DataManager;
import sf.Player;
import ui.TabFile;
import ui.TabDetail;
import ui.TabSettings;

public class TabManager {

	// Tab pane
	private static TabPane TAB_PANE;
	private static TabFile TAB_FILE;
	private static TabSettings TAB_SETTINGS;

	// Tab info
	private static Map<String, Boolean> TABS_OPEN = new HashMap<>();
	private static Map<String, Tab> TABS = new HashMap<>();

	// Constructor
	public static TabPane build() {
		TAB_PANE = new TabPane();
		TAB_PANE.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);

		TAB_FILE = new TabFile();
		TAB_SETTINGS = new TabSettings();

		TAB_PANE.getTabs().addAll(TAB_FILE, TAB_SETTINGS);

		DataManager.getInstance().get().addListener(new MapChangeListener<String, List<Player>>() {
			@Override
			public void onChanged(Change<? extends String, ? extends List<Player>> c) {
				Platform.runLater(() -> {
					if (c.wasAdded()) {
						TabDetail tab = new TabDetail(c.getKey(), c.getValueAdded());
						tab.setOnClosed(E -> {
							TABS_OPEN.put(c.getKey(), false);
							TAB_FILE.updateContent();
						});

						TABS.put(c.getKey(), tab);
						TABS_OPEN.put(c.getKey(), false);
					} else if (c.wasRemoved()) {
						TABS.remove(c.getKey());
						TABS_OPEN.remove(c.getKey());
					}

					update();
					TAB_FILE.updateContent();
				});
			}
		});

		update();
		TAB_FILE.updateContent();

		return TAB_PANE;
	}

	public static void update() {
		for (Map.Entry<String, Tab> tab : TABS.entrySet()) {
			if (TABS_OPEN.getOrDefault(tab.getKey(), false)) {
				if (!TAB_PANE.getTabs().contains(tab.getValue())) {
					TAB_PANE.getTabs().add(tab.getValue());
				}
			} else {
				if (TAB_PANE.getTabs().contains(tab.getValue())) {
					TAB_PANE.getTabs().remove(tab.getValue());
				}
			}
		}

		TAB_PANE.getTabs().subList(2, TAB_PANE.getTabs().size()).removeIf(T -> !TABS.values().contains(T));
	}

	public static boolean isOpen(String name) {
		return TABS_OPEN.getOrDefault(name, false);
	}

	public static void toggle(String name) {
		TABS_OPEN.put(name, !TABS_OPEN.getOrDefault(name, false));

		Platform.runLater(() -> {
			update();
			TAB_FILE.updateContent();
		});
	}

}
