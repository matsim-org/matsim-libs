package org.matsim.contrib.drt.extension.alonso_mora.algorithm.relocation;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;

/**
 * General interface for the relocation solver of the algorithm by Alonso-Mora
 * et al.
 * 
 * @author sebhoerl
 */
public interface RelocationSolver {
	Collection<Relocation> solve(Collection<Relocation> candidates);

	static public class Relocation {
		public final AlonsoMoraVehicle vehicle;
		public final Link destination;
		public final double cost;

		public Relocation(AlonsoMoraVehicle vehicle, Link destination, double cost) {
			this.vehicle = vehicle;
			this.destination = destination;
			this.cost = cost;
		}
	}
}
