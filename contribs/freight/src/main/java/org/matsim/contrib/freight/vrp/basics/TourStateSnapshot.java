package org.matsim.contrib.freight.vrp.basics;

import java.util.HashMap;
import java.util.Map;

public class TourStateSnapshot {

	public static String LOAD = "load";

	public static String COST = "cost";

	public static String TIME = "time";

	private final Map<String, Double> stateVars;

	public TourStateSnapshot() {
		stateVars = new HashMap<String, Double>();
	}

	public TourStateSnapshot(TourStateSnapshot tourStateSnapshot) {
		stateVars = new HashMap<String, Double>();
		for (String key : tourStateSnapshot.getStateVars().keySet()) {
			stateVars.put(key, tourStateSnapshot.getStateVar(key));
		}
	}

	Map<String, Double> getStateVars() {
		return this.stateVars;
	}

	public void addStateVar(final String name, final Double val) {
		stateVars.put(name, val);
	}

	public Double getStateVar(String name) {
		return stateVars.get(name);
	}

}
