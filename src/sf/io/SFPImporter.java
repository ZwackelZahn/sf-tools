package sf.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javafx.util.Pair;
import sf.Player;

public class SFPImporter {

	public static List<Pair<String, List<Player>>> importSFP(File file) throws IOException, ClassNotFoundException {
		Stack<Pair<String, List<Player>>> players = new Stack<>();

		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
			Long length = ois.readLong();

			for (int i = 0; i < length; i++) {
				Long len = ois.readLong();
				String label = (String) ois.readObject();

				players.push(new Pair<>(label, new ArrayList<>()));

				for (int j = 0; j < len; j++) {
					players.peek().getValue().add((Player) ois.readObject());
				}
			}
		}

		return new ArrayList<>(players);
	}

}
