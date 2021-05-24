package org.matsim.contrib.pseudosimulation;

import org.matsim.core.config.ReflectiveConfigGroup;

public class PSimConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "psim";

	public static final String ITERATIONS_PER_CYCLE = "iterationsPerCycle";
	private int iterationsPerCycle = 5;
	public static final String FULL_TRANSIT_PERFORMANCE_TRANSMISSION = "fullTransitPerformanceTransmission";
	private boolean fullTransitPerformanceTransmission = true;

	public PSimConfigGroup() {
		super(GROUP_NAME);
	}


	@StringGetter(ITERATIONS_PER_CYCLE)
	public int getIterationsPerCycle() {
		return iterationsPerCycle;
	}

	@StringSetter(ITERATIONS_PER_CYCLE)
	public  void setIterationsPerCycle(int iterationsPerCycle) {
		this.iterationsPerCycle = iterationsPerCycle;
	}

	@StringGetter(FULL_TRANSIT_PERFORMANCE_TRANSMISSION)
	public  boolean isFullTransitPerformanceTransmission() {
		return fullTransitPerformanceTransmission;
	}

	@StringSetter(FULL_TRANSIT_PERFORMANCE_TRANSMISSION)
	public  void setFullTransitPerformanceTransmission(boolean fullTransitPerformanceTransmission) {
		this.fullTransitPerformanceTransmission = fullTransitPerformanceTransmission;
	}



}
