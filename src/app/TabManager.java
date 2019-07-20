package app;

import java.util.List;

import app.tabs.TabChart;
import app.tabs.TabDetails;
import app.tabs.TabFile;
import app.tabs.TabSettings;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import sf.Player;

public enum TabManager {

	INSTANCE;

	/*
	 * Tabs & TabPane
	 */
	private final TabPane root;

	private final TabFile fileTab;
	private final TabSettings settingsTab;
	private final TabDetails detailsTab;
	private final TabChart chartTab;

	/*
	 * Constructor
	 */
	private TabManager() {
		this.root = new TabPane();
		this.root.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

		this.fileTab = new TabFile();
		this.settingsTab = new TabSettings();
		this.detailsTab = new TabDetails();
		this.chartTab = new TabChart();

		this.root.getTabs().addAll(this.fileTab, this.settingsTab, this.detailsTab, this.chartTab);

		/*
		 * Listener to map changes in order to clear details tab when opened entry is removed
		 */
		DataManager.INSTANCE.addListener(new MapChangeListener<String, List<Player>>() {
			@Override
			public void onChanged(Change<? extends String, ? extends List<Player>> change) {
				Platform.runLater(() -> {
					if (change.wasRemoved()) {
						if (change.getKey().equals(detailsTab.getSelectedKey())) {
							detailsTab.clearKey();
						}
					} else if (change.wasAdded()) {
						TabManager.INSTANCE.getDetailsTab().selectKey(DataManager.INSTANCE.getLastKey());
					}

					fileTab.update();
				});
			}

		});
	}

	/*
	 * Getters
	 */
	public TabPane getRoot() {
		return this.root;
	}

	public TabFile getFileTab() {
		return this.fileTab;
	}

	public TabSettings getSettingsTab() {
		return this.settingsTab;
	}

	public TabDetails getDetailsTab() {
		return this.detailsTab;
	}

	public TabChart getChartTab() {
		return this.chartTab;
	}

}
