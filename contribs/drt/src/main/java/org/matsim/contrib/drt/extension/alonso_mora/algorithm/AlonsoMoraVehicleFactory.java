package org.matsim.contrib.drt.extension.alonso_mora.algorithm;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * Wraps a DVRP vehicle to provide additional information that is needed for the
 * algorithm by Alonso-Mora et al.
 * 
 * @author sebhoerl
 */
public interface AlonsoMoraVehicleFactory {
	AlonsoMoraVehicle createVehicle(DvrpVehicle vehicle);
}
