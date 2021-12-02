package org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs;

import java.util.stream.Stream;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraTrip;

/**
 * Represents a trip-vehicle graph
 * 
 * @author sebhoerl
 */
public interface VehicleGraph {

	void addRequest(AlonsoMoraRequest request, double now);

	void removeRequest(AlonsoMoraRequest request);

	Stream<AlonsoMoraTrip> stream();

	int getSize();

	void preserveVehicleAssignment(double now);

}