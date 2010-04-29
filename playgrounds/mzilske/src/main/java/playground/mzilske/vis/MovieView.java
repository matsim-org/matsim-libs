package playground.mzilske.vis;

import playground.mzilske.vis.EventsCollectingServer.TimeStep;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.collections.StoredSortedMap;

public class MovieView {
	
	private StoredSortedMap<Double, TimeStep> timeStepMap;

	public MovieView(MovieDatabase db) {
		ClassCatalog classCatalog = db.getJavaCatalog();
		EntryBinding<Double> timestepKeyBinding = new SerialBinding<Double>(classCatalog, Double.class);
		EntryBinding<TimeStep> timestepValueBindung = new SerialBinding<TimeStep>(classCatalog, TimeStep.class);
		timeStepMap = new StoredSortedMap<Double, TimeStep>(db.getTimestepDb(), timestepKeyBinding, timestepValueBindung, true);
	}

	public StoredSortedMap<Double, TimeStep> getTimeStepMap() {
		return timeStepMap;
	}

}
