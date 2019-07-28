package app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.simple.parser.ParseException;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import sf.io.HARImporter;
import sf.io.SFPFileIO;
import sf.struct.Player;

public enum DataManager {

	/*
	 * Singleton
	 */
	INSTANCE;

	/*
	 * Code for handling synchronization between current loaded data and archive file syncRequested is set to true when a change is made to set map
	 */
	private final Timer syncTimer = new Timer();
	private final TimerTask syncTask = new TimerTask() {
		@Override
		public void run() {
			if (syncRequested.compareAndSet(true, false)) {
				try {
					SFPFileIO.exportSFP(archive, playerSets, playerSetOrder);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};
	private final AtomicBoolean syncRequested = new AtomicBoolean(false);

	/*
	 * Loaded player sets, where playerSetOrder keeps the order in which individual sets were inserted
	 */
	private final ObservableMap<String, List<Player>> playerSets = FXCollections.observableMap(new ConcurrentHashMap<>());
	private final List<String> playerSetOrder = new ArrayList<>();

	/*
	 * Player set listeners
	 */
	List<MapChangeListener<String, List<Player>>> listeners = new ArrayList<>();

	public void addListener(MapChangeListener<String, List<Player>> listener) {
		listeners.add(listener);
		playerSets.addListener(listener);
	}

	public void removeListeners() {
		for (MapChangeListener<String, List<Player>> listener : listeners) {
			playerSets.removeListener(listener);
		}
	}

	/*
	 * Archive file for already loaded data
	 */
	private final File archive = new File("cache.sfp");

	/*
	 * Constructor, sets execution of sync calls every 5 seconds
	 */
	private DataManager() {
		syncTimer.schedule(syncTask, 0, 5000);
	}

	/*
	 * Returns list based on key
	 */
	public List<Player> getSet(String key) {
		return this.playerSets.getOrDefault(key, null);
	}

	/*
	 * Returns key at specified position
	 */
	public String getLastKey() {
		return this.playerSetOrder.isEmpty() ? null : this.playerSetOrder.get(this.playerSetOrder.size() - 1);
	}

	public String getFirstKey() {
		return this.playerSetOrder.isEmpty() ? null : this.playerSetOrder.get(0);
	}

	/*
	 * Returns amount of sets currently loaded
	 */
	public int getSize() {
		return this.playerSetOrder.size();
	}

	/*
	 * Returns key order
	 */
	public List<String> getOrder() {
		return this.playerSetOrder;
	}

	/*
	 * Loads sets from archive file
	 */
	public boolean loadArchive() {
		if (archive.exists()) {
			try {
				playerSets.putAll(SFPFileIO.importSFP(archive, playerSetOrder));
			} catch (ClassNotFoundException | IOException e) {
				return false;
			}

			return true;
		} else {
			return false;
		}
	}

	/*
	 * Loads set from HTTP archive file
	 */
	public boolean loadHAR(File file) {
		try {
			String name = file.getName().substring(0, file.getName().lastIndexOf("."));
			String extension = file.getName().toLowerCase();

			if (!playerSets.containsKey(name) && extension.endsWith(".har")) {
				playerSetOrder.add(name);
				playerSets.put(name, HARImporter.importHAR(file));

				syncRequested.set(true);
			}

			return true;
		} catch (IOException | ParseException e) {
			return false;
		}
	}

	/*
	 * Permanently removes set
	 */
	public void removeSet(String key) {
		playerSets.remove(key);
		playerSetOrder.remove(key);

		syncRequested.set(true);
	}

	/*
	 * Request immediate sync manually
	 */
	public void requestSync() {
		syncTask.run();
		syncTimer.cancel();
	}

}
