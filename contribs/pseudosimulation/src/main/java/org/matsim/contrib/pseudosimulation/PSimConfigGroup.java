package org.matsim.contrib.pseudosimulation;

import org.matsim.core.config.ReflectiveConfigGroup;

public class PSimConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "psim";

	/** How a PSim iteration works out what a transit leg costs the agent. */
	public enum TransitEmulation {
		/** Transit legs take no time at all. Only sensible when the scenario has no transit. */
		none,
		/**
		 * Samples a random one of the last few observed departures and applies a stochastic
		 * boarding model, so repeated lookups of the same leg disagree.
		 */
		fullTransitPerformance,
		/**
		 * Deterministic wait time at the access stop plus deterministic stop-to-stop times along
		 * the route, both measured in the preceding QSim iteration, falling back to the timetable
		 * where nothing was measured.
		 */
		waitAndStopStopTimes
	}

	public static final String ITERATIONS_PER_CYCLE = "iterationsPerCycle";
	private int iterationsPerCycle = 5;
	public static final String FULL_TRANSIT_PERFORMANCE_TRANSMISSION = "fullTransitPerformanceTransmission";
	private boolean fullTransitPerformanceTransmission = true;
	public static final String TRANSIT_EMULATION = "transitEmulation";
	private TransitEmulation transitEmulation = TransitEmulation.fullTransitPerformance;

	public PSimConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(TRANSIT_EMULATION)
	public TransitEmulation getTransitEmulation() {
		return transitEmulation;
	}

	@StringSetter(TRANSIT_EMULATION)
	public void setTransitEmulation(TransitEmulation transitEmulation) {
		this.transitEmulation = transitEmulation;
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
