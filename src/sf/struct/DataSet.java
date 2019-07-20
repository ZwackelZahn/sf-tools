package sf.struct;

import java.util.ArrayList;
import java.util.List;

public class DataSet {

	private final int id;
	private final String key;

	private final List<Player> players = new ArrayList<>();
	private final List<Group> groups = new ArrayList<>();

	public DataSet(int id, String key) {
		this.id = id;
		this.key = key;
	}

	public int getID() {
		return this.id;
	}

	public String getKey() {
		return this.key;
	}

	public List<Player> getPlayers() {
		return this.players;
	}

	public List<Group> getGroups() {
		return this.groups;
	}

}
