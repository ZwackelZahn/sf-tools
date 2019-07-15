package app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.util.Pair;
import sf.Player;
import ui.TabFile;
import ui.TabPlayerList;
import ui.TabSettings;

public class TabManager {

	// Tab pane
	private static TabPane TAB_PANE;
	private static TabFile TAB_FILE;
	private static TabSettings TAB_SETTINGS;

	// Tab info
	private static Map<String, Pair<Boolean, Tab>> TABS = new HashMap<>();

	// Constructor
	public static TabPane build() {
		TAB_PANE = new TabPane();
		TAB_PANE.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);

		TAB_FILE = new TabFile();
		TAB_SETTINGS = new TabSettings();

		TAB_PANE.getTabs().addAll(TAB_FILE, TAB_SETTINGS);

		Main.PLAYERS.addListener(new ListChangeListener<Pair<String, List<Player>>>() {
			@Override
			public void onChanged(Change<? extends Pair<String, List<Player>>> c) {
				Platform.runLater(() -> {
					while (c.next()) {
						for (Pair<String, List<Player>> item : c.getRemoved()) {
							TABS.remove(item.getKey());
						}

						for (Pair<String, List<Player>> item : c.getAddedSubList()) {
							TABS.put(item.getKey(), new Pair<>(false, new TabPlayerList(item)));
						}
					}

					update();
					
					TAB_FILE.updateContent();
				});
			}
		});

		return TAB_PANE;
	}

	public static String getLabel(int i) {
		return (String) TABS.keySet().toArray()[i];
	}

	public static void update() {
		for (Pair<Boolean, Tab> tab : TABS.values()) {
			if (tab.getKey()) {
				if (!TAB_PANE.getTabs().contains(tab.getValue())) {
					TAB_PANE.getTabs().add(tab.getValue());
				}
			} else {
				if (TAB_PANE.getTabs().contains(tab.getValue())) {
					TAB_PANE.getTabs().remove(tab.getValue());
				}
			}
		}

		TAB_PANE.getTabs().subList(2, TAB_PANE.getTabs().size()).removeIf(T -> !TABS.values().stream().map(KV -> KV.getValue()).collect(Collectors.toList()).contains(T));
	}

	public static boolean isOpen(String name) {
		return TABS.getOrDefault(name, new Pair<>(false, null)).getKey();
	}

	public static void toggle(String name) {
		TABS.put(name, new Pair<>(!TABS.get(name).getKey(), TABS.get(name).getValue()));
		Platform.runLater(() -> {
			update();
		});
	}

}
