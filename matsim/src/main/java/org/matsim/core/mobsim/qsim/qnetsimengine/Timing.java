package org.matsim.core.mobsim.qsim.qnetsimengine;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

import java.util.ArrayList;
import java.util.List;

public class Timing {

	private final String name;
	private final LongList durations = new LongArrayList();

	public Timing(String name) {
		this.name = name;
	}

	void addDuration(long duration) {
		durations.add(duration);
	}

	public String getName() {
		return name;
	}

	public LongList getDurations() {
		return this.durations;
	}
}
