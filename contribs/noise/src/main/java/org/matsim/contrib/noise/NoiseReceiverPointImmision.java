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
	private Map<Id<Link>, Double> linkId2IsolatedImmissionPlusOneCar = new HashMap<>(0);
	private Map<Id<Link>, Double> linkId2IsolatedImmissionPlusOneHGV = new HashMap<>(0); 

	NoiseReceiverPointImmision() {

	}

	public Map<Id<Link>, Double> getLinkId2IsolatedImmission() {
		return Collections.unmodifiableMap(linkId2IsolatedImmission);
	}

	public void setLinkId2IsolatedImmission(Id<Link> linkId, Double isolatedImmission) {
		this.linkId2IsolatedImmission.put(linkId, isolatedImmission);
	}
	
	public Map<Id<Link>, Double> getLinkId2IsolatedImmissionPlusOneCar() {
		return Collections.unmodifiableMap(linkId2IsolatedImmissionPlusOneCar);
	}

	public void setLinkId2IsolatedImmissionPlusOneCar(
			Map<Id<Link>, Double> linkId2IsolatedImmissionPlusOneCar) {
		this.linkId2IsolatedImmissionPlusOneCar = linkId2IsolatedImmissionPlusOneCar;
	}

	public Map<Id<Link>, Double> getLinkId2IsolatedImmissionPlusOneHGV() {
		return Collections.unmodifiableMap(linkId2IsolatedImmissionPlusOneHGV);
	}

	public void setLinkId2IsolatedImmissionPlusOneHGV(
			Map<Id<Link>, Double> linkId2IsolatedImmissionPlusOneHGV) {
		this.linkId2IsolatedImmissionPlusOneHGV = linkId2IsolatedImmissionPlusOneHGV;
	}


	////////////////////////////////////////////////////////////////////////////////
}

