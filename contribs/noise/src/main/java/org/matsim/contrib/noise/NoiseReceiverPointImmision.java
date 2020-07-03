package org.matsim.contrib.noise;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * A class which holds some information needed for noise-computation during a timestep
 * 
 * @author droeder / Senozon Deutschland GmbH
 *
 */
final class NoiseReceiverPointImmision {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(NoiseReceiverPointImmision.class);
	
	private Map<Id<Link>, Double> linkId2IsolatedImmission = new HashMap<>(0);
	private Map<Id<NoiseVehicleType>, Map<Id<Link>, Double>> linkId2IsolatedImmissionPlusOneVehicle = new HashMap<>(0);

	NoiseReceiverPointImmision() {
	}

	public Map<Id<Link>, Double> getLinkId2IsolatedImmission() {
		return Collections.unmodifiableMap(linkId2IsolatedImmission);
	}

	public void setLinkId2IsolatedImmission(Id<Link> linkId, Double isolatedImmission) {
		this.linkId2IsolatedImmission.put(linkId, isolatedImmission);
	}
	
	public Map<Id<Link>, Double> getLinkId2IsolatedImmissionPlusOneVehicle(Id<NoiseVehicleType> vehicleTypeId) {
		return Collections.unmodifiableMap(linkId2IsolatedImmissionPlusOneVehicle.computeIfAbsent(vehicleTypeId, type -> new HashMap<>(0)));
	}

	public void setLinkId2IsolatedImmissionPlusOneVehicle(Id<NoiseVehicleType> vehicleType,
			Map<Id<Link>, Double> linkId2IsolatedImmissionPlusOneVehicle) {
		this.linkId2IsolatedImmissionPlusOneVehicle.put(vehicleType, linkId2IsolatedImmissionPlusOneVehicle);
	}
	
	////////////////////////////////////////////////////////////////////////////////
}

