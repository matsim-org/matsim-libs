package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.events.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Timing {

	private final String name;
	private final List<Long> durations = new ArrayList<>();

	public Timing(String name) {
		this.name = name;
	}

	void addDuration(long duration) {
		durations.add(duration);
	}

	public String getName() {
		return name;
	}

	public List<Long> getDurations() {
		return this.durations;
	}
}
