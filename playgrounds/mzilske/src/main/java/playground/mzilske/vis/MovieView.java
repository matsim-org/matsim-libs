package playground.mzilske.vis;

import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.collections.StoredSortedMap;

public class MovieView {
	
	private StoredSortedMap<Double, AgentSnapshotInfo> timeStepMap;

	public MovieView(MovieDatabase db) {
		ClassCatalog classCatalog = db.getJavaCatalog();
		EntryBinding<Double> timestepKeyBinding = new SerialBinding<Double>(classCatalog, Double.class);
		EntryBinding<AgentSnapshotInfo> timestepValueBindung = new SerialBinding<AgentSnapshotInfo>(classCatalog, AgentSnapshotInfo.class);
		timeStepMap = new StoredSortedMap<Double, AgentSnapshotInfo>(db.getTimestepDb(), timestepKeyBinding, timestepValueBindung, true);
	}

	public StoredSortedMap<Double, AgentSnapshotInfo> getTimeStepMap() {
		return timeStepMap;
	}

}
