package playground.mzilske.osm;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.collections.StoredSortedMap;

public class NodeView {
	
	private StoredSortedMap<String, OsmNode> timeStepMap;

	public NodeView(NodeDatabase db) {
		ClassCatalog classCatalog = db.getJavaCatalog();
		EntryBinding<String> timestepKeyBinding = new SerialBinding<String>(classCatalog, String.class);
		EntryBinding<OsmNode> timestepValueBindung = new SerialBinding<OsmNode>(classCatalog, OsmNode.class);
		timeStepMap = new StoredSortedMap<String, OsmNode>(db.getTimestepDb(), timestepKeyBinding, timestepValueBindung, true);
	}

	public StoredSortedMap<String, OsmNode> getTimeStepMap() {
		return timeStepMap;
	}

}
