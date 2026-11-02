package org.matsim.contrib.msp.traveltimematrix.congestion;

import org.matsim.contrib.msp.traveltimematrix.RoadClassification;

import java.util.Collection;
import java.util.List;

/**
 * How much longer than free speed a trip takes at a given time of day, as a share: {@code 0.2} means
 * "20% longer than on an empty network".
 * <p>
 * This is the one time-dependent input of the matrix. Everything else about a travel time matrix is a
 * geometry that does not change over the day, which is what makes the time dimension cheap:
 * {@code TT(o,d,t) = TT_ff(o,d) * (1 + CI(t))} needs one lookup here per query.
 *
 * @see CongestionIndexProfile the implementation: one value per time bin, read off a run
 * @see CongestionIndexCalculator how it is measured
 */
public interface CongestionIndex {

	/**
	 * @param time time of day [s]
	 * @param roadClass one of {@link #getRoadClasses()}, or {@link RoadClassification#ALL} for the
	 * network-wide value
	 * @return the excess travel time index, {@code >= 0}. 0 means free-flow conditions.
	 */
	double get(double time, String roadClass);

	/**
	 * @return the network-wide index, i.e. {@code get(time, }{@link RoadClassification#ALL}{@code )}
	 */
	default double get(double time) {
		return get(time, RoadClassification.ALL);
	}

	/**
	 * @return the road classes this index distinguishes, including {@link RoadClassification#ALL}
	 */
	Collection<String> getRoadClasses();

	/**
	 * Free-flow conditions at every time of day: the index is 0 everywhere, so a matrix wrapped with it
	 * returns its free-speed travel times unchanged. Useful as the null case of an experiment - the error
	 * of <em>not</em> modelling congestion at all is the number every variant has to beat.
	 */
	static CongestionIndex freeFlow() {
		return new CongestionIndex() {
			@Override
			public double get(double time, String roadClass) {
				return 0;
			}

			@Override
			public Collection<String> getRoadClasses() {
				return List.of(RoadClassification.ALL);
			}

			@Override
			public String toString() {
				return "CongestionIndex.freeFlow()";
			}
		};
	}

	/**
	 * The same index at every time of day. The matrix is then not time-varying at all but scaled by a
	 * constant, which is what {@code DetourTimeEstimator.createMatrixBasedEstimator(speedFactor, ...)} of
	 * {@code contribs/drt} does today - so this is the second baseline an experiment compares against.
	 *
	 * @param index the excess travel time index, e.g. the daily average
	 */
	static CongestionIndex constant(double index) {
		return new CongestionIndex() {
			@Override
			public double get(double time, String roadClass) {
				return index;
			}

			@Override
			public Collection<String> getRoadClasses() {
				return List.of(RoadClassification.ALL);
			}

			@Override
			public String toString() {
				return "CongestionIndex.constant(" + index + ")";
			}
		};
	}
}
