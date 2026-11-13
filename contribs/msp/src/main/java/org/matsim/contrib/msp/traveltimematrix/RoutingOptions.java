package org.matsim.contrib.msp.traveltimematrix;

import picocli.CommandLine;

/**
 * The two options that decide how anything in this package <b>routes</b>, as opposed to what it routes
 * over: the time step the free-speed travel times are rounded to, and how many threads to use.
 * <p>
 * Separate from {@link MatrixOptions} because an experiment that sweeps {@code cellSize} and
 * {@code maxNeighborDistance} still needs these two and must not offer the swept ones as options - a
 * {@code --cell-size} flag that is silently ignored is worse than no flag.
 */
public final class RoutingOptions {

	@CommandLine.Option(names = "--qsim-time-step-size", description = "Time step size of the run [s]. A free-speed "
		+ "link travel time is rounded up to a multiple of it, so this has to match the run that anything is "
		+ "compared against.", defaultValue = "1")
	private double qSimTimeStepSize;

	@CommandLine.Option(names = "--threads", description = "Threads the matrix and the routing use. Defaults to "
		+ "the number of processors.")
	private Integer numberOfThreads;

	public double getQSimTimeStepSize() {
		return this.qSimTimeStepSize;
	}

	public int getNumberOfThreads() {
		return this.numberOfThreads == null ? Runtime.getRuntime().availableProcessors() : this.numberOfThreads;
	}
}
