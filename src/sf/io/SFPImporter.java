package sf.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sf.Player;

public class SFPImporter {

	public static Map<String, List<Player>> importSFP(File file, List<String> keyOrder) throws IOException, ClassNotFoundException {
		Map<String, List<Player>> players = new HashMap<>();

		try (FileInputStream fis = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(fis)) {
			int mapSize = ois.readInt();

			for (int i = 0; i < mapSize; i++) {
				int listSize = ois.readInt();
				String listName = ois.readObject().toString();

				List<Player> list = new ArrayList<>(listSize);
				for (int j = 0; j < listSize; j++) {
					list.add((Player) ois.readObject());
				}

				players.put(listName, list);
				keyOrder.add(listName);
			}
		}

		return players;
	}

}
