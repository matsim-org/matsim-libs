package org.matsim.contrib.cadyts.car;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Aggregates flow volumes in PCU (Passenger Car Units) instead of raw vehicle counts.
 */
class PcuVolumesAnalyzer implements LinkLeaveEventHandler {

	private final Vehicles vehicles;
	private final int timeBinSize;
	private final int maxTime;

	// Map<LinkId, double[]> - stores PCU sum per time bin
	private final Map<Id<Link>, double[]> volumes = new HashMap<>();

	PcuVolumesAnalyzer(Vehicles vehicles, int timeBinSize, int maxTime) {
		this.vehicles = vehicles;
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
	}

	@Override
	public void reset(int iteration) {
		this.volumes.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		int bin = getIndex(event.getTime());
		if (bin < 0) return;

		double pcu = 1.0;
		if (this.vehicles != null) {
			Vehicle v = this.vehicles.getVehicles().get(event.getVehicleId());
			if (v != null && v.getType() != null) {
				pcu = v.getType().getPcuEquivalents();
			}
		}

		// Lazy initialization of the array for this link
		double[] linkVolumes = this.volumes.computeIfAbsent(event.getLinkId(), k -> new double[getIndex(maxTime) + 1]);

		if (bin < linkVolumes.length) {
			linkVolumes[bin] += pcu;
		}
	}

	public double[] getPcuVolumesForLink(Id<Link> linkId) {
		return this.volumes.get(linkId);
	}

	public Set<Id<Link>> getLinkIds() {
		return this.volumes.keySet();
	}

	private int getIndex(double time) {
		if (time > maxTime) return -1;
		return (int) (time / timeBinSize);
	}
}
