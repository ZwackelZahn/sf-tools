package sf.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sf.struct.Player;

public class SFPFileIO {

	public static void exportSFP(File file, Map<String, List<Player>> players, List<String> keyOrder) throws IOException {
		new PrintWriter(file).close();

		try (RandomAccessFile raf = new RandomAccessFile(file, "rw"); FileOutputStream fos = new FileOutputStream(raf.getFD()); ObjectOutputStream oos = new ObjectOutputStream(fos);) {
			oos.writeInt(players.size());

			for (String key : keyOrder) {
				oos.writeObject(key);
				oos.writeObject(players.get(key));
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, List<Player>> importSFP(File file, List<String> keyOrder) throws FileNotFoundException, IOException, ClassNotFoundException {
		Map<String, List<Player>> players = new HashMap<>();

		try (FileInputStream fis = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(fis)) {
			int mapSize = ois.readInt();

			for (int i = 0; i < mapSize; i++) {
				String listName = ois.readObject().toString();

				keyOrder.add(listName);
				players.put(listName, (ArrayList<Player>) ois.readObject());
			}
		}

		return players;
	}

}
