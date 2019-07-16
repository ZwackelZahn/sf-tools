package sf;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.simple.parser.ParseException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import sf.io.HARImporter;
import sf.io.SFPExporter;
import sf.io.SFPImporter;

public class DataManager {

	// Singleton header
	private static DataManager INSTANCE = new DataManager();

	public static DataManager getInstance() {
		if (Objects.isNull(INSTANCE)) {
			INSTANCE = new DataManager();
		}

		return INSTANCE;
	}

	// Class
	public final Timer TIMER;
	public final TimerTask TIMER_TASK;

	private final ObservableMap<String, List<Player>> players = FXCollections.observableMap(new ConcurrentHashMap<>());
	private final File cache = new File("cache.sfp");

	private final AtomicBoolean stateChanged = new AtomicBoolean(false);

	// Constructor
	public DataManager() {
		TIMER_TASK = new TimerTask() {
			@Override
			public void run() {
				if (stateChanged.compareAndSet(true, false)) {
					try {
						SFPExporter.exportFastSFP(cache, players);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};

		TIMER = new Timer();
		TIMER.schedule(TIMER_TASK, 0, 5000);
	}

	// Functions
	public ObservableMap<String, List<Player>> get() {
		return players;
	}

	public List<Player> get(String key) {
		return players.get(key);
	}

	public String get(int i) {
		return players.keySet().toArray()[i].toString();
	}

	public void loadCachedFiles() {
		try {
			if (cache.exists()) {
				players.putAll(SFPImporter.importSFP(cache));
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void loadFile(File file) {
		try {
			String filename = file.getName().substring(0, file.getName().lastIndexOf("."));
			String extended = file.getName().toLowerCase();

			if (!players.containsKey(filename) && extended.endsWith(".har")) {
				players.put(filename, HARImporter.importHAR(file));

				stateChanged.set(true);
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	public void remove(String key) {
		players.remove(key);

		stateChanged.set(true);
	}

}
