package org.matsim.contrib.pseudosimulation;

import org.apache.logging.log4j.LogManager;
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
	/** @deprecated superseded by {@link #TRANSIT_EMULATION}, which also names which emulator. */
	@Deprecated
	public static final String FULL_TRANSIT_PERFORMANCE_TRANSMISSION = "fullTransitPerformanceTransmission";
	private boolean fullTransitPerformanceTransmission = true;
	public static final String TRANSIT_EMULATION = "transitEmulation";
	private TransitEmulation transitEmulation = TransitEmulation.fullTransitPerformance;

	// Which of the two were named explicitly, so a config written against the old flag keeps its
	// meaning instead of silently picking up the new default.
	private boolean fullTransitPerformanceTransmissionSet = false;
	private boolean transitEmulationSet = false;

	public PSimConfigGroup() {
		super(GROUP_NAME);
	}

	/**
	 * The emulation this config group asks for, honouring the superseded
	 * {@value #FULL_TRANSIT_PERFORMANCE_TRANSMISSION} flag where it is the only one given.
	 *
	 * <p>
	 * The old flag only said whether to emulate transit at all, so it maps onto the new setting as
	 * {@code false -> none} and {@code true -> fullTransitPerformance}. Naming both is accepted
	 * while they agree on that question and rejected when they do not, rather than letting one
	 * quietly win.
	 */
	public TransitEmulation resolveTransitEmulation() {
		if (!fullTransitPerformanceTransmissionSet) {
			return transitEmulation;
		}
		TransitEmulation implied = fullTransitPerformanceTransmission
				? TransitEmulation.fullTransitPerformance
				: TransitEmulation.none;
		if (!transitEmulationSet) {
			LogManager.getLogger(PSimConfigGroup.class).warn(
					"{}.{} is superseded by {}.{}. Reading it as {}={}; set {} instead, since the old flag "
							+ "cannot express which transit emulator to use.",
					GROUP_NAME, FULL_TRANSIT_PERFORMANCE_TRANSMISSION, GROUP_NAME, TRANSIT_EMULATION,
					TRANSIT_EMULATION, implied, TRANSIT_EMULATION);
			return implied;
		}
		boolean bothEmulate = (implied != TransitEmulation.none) == (transitEmulation != TransitEmulation.none);
		if (!bothEmulate) {
			throw new IllegalStateException(String.format(
					"%s.%s=%s contradicts %s.%s=%s. The first says transit emulation is %s, the second says "
							+ "it is %s. Remove the superseded %s.",
					GROUP_NAME, FULL_TRANSIT_PERFORMANCE_TRANSMISSION, fullTransitPerformanceTransmission,
					GROUP_NAME, TRANSIT_EMULATION, transitEmulation,
					fullTransitPerformanceTransmission ? "on" : "off",
					transitEmulation != TransitEmulation.none ? "on" : "off",
					FULL_TRANSIT_PERFORMANCE_TRANSMISSION));
		}
		return transitEmulation;
	}

	@StringGetter(TRANSIT_EMULATION)
	public TransitEmulation getTransitEmulation() {
		return transitEmulation;
	}

	@StringSetter(TRANSIT_EMULATION)
	public void setTransitEmulation(TransitEmulation transitEmulation) {
		this.transitEmulation = transitEmulation;
		this.transitEmulationSet = true;
	}

	@StringGetter(ITERATIONS_PER_CYCLE)
	public int getIterationsPerCycle() {
		return iterationsPerCycle;
	}

	@StringSetter(ITERATIONS_PER_CYCLE)
	public  void setIterationsPerCycle(int iterationsPerCycle) {
		this.iterationsPerCycle = iterationsPerCycle;
	}

	/** @deprecated use {@link #getTransitEmulation()}; this cannot name which emulator to use. */
	@Deprecated
	@StringGetter(FULL_TRANSIT_PERFORMANCE_TRANSMISSION)
	public  boolean isFullTransitPerformanceTransmission() {
		return fullTransitPerformanceTransmission;
	}

	/** @deprecated use {@link #setTransitEmulation}; this cannot name which emulator to use. */
	@Deprecated
	@StringSetter(FULL_TRANSIT_PERFORMANCE_TRANSMISSION)
	public  void setFullTransitPerformanceTransmission(boolean fullTransitPerformanceTransmission) {
		this.fullTransitPerformanceTransmission = fullTransitPerformanceTransmission;
		this.fullTransitPerformanceTransmissionSet = true;
	}

}
