package sf.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;

import sf.Player;

public class SFPExporter {

	public static void exportFastSFP(File file, Map<String, List<Player>> players) throws IOException {
		new PrintWriter(file).close();
		;

		try (RandomAccessFile raf = new RandomAccessFile(file, "rw"); FileOutputStream fos = new FileOutputStream(raf.getFD()); ObjectOutputStream oos = new ObjectOutputStream(fos);) {
			oos.writeInt(players.size());

			for (Map.Entry<String, List<Player>> keyval : players.entrySet()) {
				oos.writeInt(keyval.getValue().size());
				oos.writeObject(keyval.getKey());

				for (Player p : keyval.getValue()) {
					oos.writeObject(p);
					oos.reset();
				}
			}
		}
	}
}
