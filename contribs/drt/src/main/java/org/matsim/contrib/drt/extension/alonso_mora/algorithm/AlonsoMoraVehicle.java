package org.matsim.contrib.drt.extension.alonso_mora.algorithm;

import java.util.List;
import java.util.Set;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.util.LinkTimePair;

/**
 * Represents a vehicle in the context of the algorithm by Alonso-Mora et al.
 * 
 * @author sebhoerl
 */
public interface AlonsoMoraVehicle {
	public DvrpVehicle getVehicle();

	public LinkTimePair getNextDiversion(double now);

	void addOnboardRequest(AlonsoMoraRequest request);

	void removeOnboardRequest(AlonsoMoraRequest request);

	Set<AlonsoMoraRequest> getOnboardRequests();

	public void setRoute(List<AlonsoMoraStop> route);

	public List<AlonsoMoraStop> getRoute();
}
