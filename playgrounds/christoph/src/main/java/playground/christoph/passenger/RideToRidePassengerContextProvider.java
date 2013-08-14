/* *********************************************************************** *
 * project: org.matsim.*
 * RideToRidePassengerContextProvider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.christoph.passenger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDeparture;

/**
 * Stores information from the RideToRidePassengerAgentIdentifier which is
 * then used by the RideToRidePassengerAgentReplanners to update the identified
 * agents' plans.
 * 
 * @author cdobler
 */
public class RideToRidePassengerContextProvider {

	private final Map<Id, Collection<RideToRidePassengerContext>> map = new ConcurrentHashMap<Id, Collection<RideToRidePassengerContext>>();

	public boolean addContext(Id agentId, RideToRidePassengerContext context) {
		Collection<RideToRidePassengerContext> collection = this.map.get(agentId);
		if (collection == null) {
			collection = new ArrayList<RideToRidePassengerContext>();
			this.map.put(agentId, collection);
		}
		return collection.add(context);
	}
	
	public Collection<RideToRidePassengerContext> removeContextCollection(Id agentId) {
		return this.map.remove(agentId);
	}
	
	public void reset() {
		this.map.clear();
	}
	
	public RideToRidePassengerContext createAndAddContext(Leg rideLeg) {
		return new RideToRidePassengerContext(rideLeg);
	}
	
	public static class RideToRidePassengerContext {

		/*package*/ final Leg rideLeg;
		/*package*/ Leg carLeg;
		/*package*/ Link pickupLink;
		/*package*/ Link dropOffLink;
		/*package*/ MobsimAgent carLegAgent;
		/*package*/ JointDeparture pickupDeparture;
		
		public RideToRidePassengerContext(Leg rideLeg) {
			this.rideLeg = rideLeg;
		}
	}
}
