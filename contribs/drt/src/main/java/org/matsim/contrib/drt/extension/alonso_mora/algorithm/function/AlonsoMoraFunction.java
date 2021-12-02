package org.matsim.contrib.drt.extension.alonso_mora.algorithm.function;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;

/**
 * This class represents one of the core components of the algorithm described
 * by Alonso-Mora et al. It replicates the "travel" function as defined in the
 * paper. Its task is to take a vehicle with its current position and a list of
 * stops to be inserted into the vehicle schedule. The function is supposed to
 * return an optimal assignment that does not violate pickup and dropoff
 * constraints of the requests, as well as capacity constraints of the vehicle.
 * 
 * For convenience the class combines the routing activities for both
 * constructing the request graph and the trip-vehicle graph as described in the
 * paper. See the individual methods for more information.
 * 
 * As we operate the algorithm in a slightly more general context than in the
 * initial version by Alonso-Mora et al., additional constraints may be added
 * (vehicle shift start/end times, charging requirements, ...).
 * 
 * @author sebhoerl
 */
public interface AlonsoMoraFunction {
	/**
	 * Check whether two requests can potentially be shared. See
	 * {@link DefaultAlonsoMoraFunction#checkShareability(AlonsoMoraRequest, AlonsoMoraRequest, double)}.
	 */
	boolean checkShareability(AlonsoMoraRequest firstRequest, AlonsoMoraRequest secondRequest, double now);

	/**
	 * Find a minimum cost stop sequence for a vehicle and the given list of
	 * requests. See
	 * {@link DefaultAlonsoMoraFunction#calculateRoute(Collection, AlonsoMoraVehicle, double)}.
	 */
	Optional<Result> calculateRoute(Collection<AlonsoMoraRequest> requests, AlonsoMoraVehicle vehicle, double now);

	/**
	 * Check whether a relocation trip can be assigned to the vehicle, given the
	 * destination link. See
	 * {@link DefaultAlonsoMoraFunction#checkRelocation(AlonsoMoraVehicle, Link, double)}.
	 */
	Optional<Double> checkRelocation(AlonsoMoraVehicle vehicle, Link destination, double now);

	static public class Result {
		private final double cost;
		private final List<AlonsoMoraStop> stops;

		public Result(double cost, List<AlonsoMoraStop> stops) {
			this.cost = cost;
			this.stops = stops;
		}

		public double getCost() {
			return cost;
		}

		public List<AlonsoMoraStop> getStops() {
			return stops;
		}
	}
}
