/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.benchmark;

/**
 * Enums for DRT benchmark configuration.
 * <p>
 * The benchmark has two orthogonal dimensions:
 * <ul>
 *   <li>{@link RequestInserterType} – HOW requests are dispatched (Default sequential vs. Parallel partitioned)</li>
 *   <li>{@link InsertionSearchStrategy} – WHICH algorithm finds the best insertion for a request</li>
 * </ul>
 * The routing algorithm type for detour path computation is configured via
 * {@link org.matsim.core.config.groups.ControllerConfigGroup.RoutingAlgorithmType} directly
 * (e.g. {@code SpeedyALT}, {@code CHRouter}).
 *
 * @author Steffen Axer
 */
public final class InsertionStrategy {
	private InsertionStrategy() {} // utility class

	/**
	 * The request inserter type controls how requests are dispatched to vehicles.
	 * <ul>
	 *   <li><b>Default</b> – sequential processing via {@code DefaultUnplannedRequestInserter}</li>
	 *   <li><b>Parallel</b> – partitioned parallel processing via {@code ParallelUnplannedRequestInserter}
	 *       (requires partitioner and collection period configuration)</li>
	 * </ul>
	 */
	public enum RequestInserterType {
		Default, Parallel
	}

	/**
	 * The insertion search strategy used to find the best insertion of a request into a vehicle schedule.
	 * <ul>
	 *   <li><b>Selective</b> – fast heuristic, finds a single best insertion using beeline-distance pre-filtering</li>
	 *   <li><b>Extensive</b> – evaluates all feasible insertions for each request</li>
	 *   <li><b>RepeatedSelective</b> – retries selective search multiple times for better quality</li>
	 * </ul>
	 */
	public enum InsertionSearchStrategy {
		Selective, Extensive, RepeatedSelective
	}
}

