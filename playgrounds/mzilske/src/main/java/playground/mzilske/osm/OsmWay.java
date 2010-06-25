package playground.mzilske.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmWay {
	public final long id;
	public final List<String> nodes = new ArrayList<String>();
	public final Map<String, String> tags = new HashMap<String, String>();
	public int hierarchy = -1;

	public OsmWay(final long id) {
		this.id = id;
	}
}
