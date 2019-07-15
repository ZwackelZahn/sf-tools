package sf.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

import javafx.util.Pair;
import sf.Player;

public class SFPExporter {

	public static void exportSFP(File file, List<Pair<String, List<Player>>> players) throws IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
			oos.writeLong(players.size());

			for (Pair<String, List<Player>> list : players) {
				oos.writeLong(list.getValue().size());
				oos.writeObject(list.getKey());

				for (Player p : list.getValue()) {
					oos.writeObject(p);
				}
			}
		}
	}
}
