/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.vsp.congestion.handlers;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * @author nagel
 *
 */
public final class CongestionUtils {
	private CongestionUtils(){} // do not instantiate

		public static final LinkCongestionInfo getOrCreateLinkInfo( Id<Link> linkId, Map<Id<Link>, LinkCongestionInfo> linkId2congestionInfo, Scenario scenario) {
			// a bit awkward to pass the scenario, but allows to make it static.  kai, sep'15
			
			LinkCongestionInfo linkInfo = linkId2congestionInfo.get( linkId ) ;
			if (linkInfo != null){ 
				return linkInfo ;
			}
			LinkCongestionInfo.Builder builder = new LinkCongestionInfo.Builder();
			Network network = scenario.getNetwork();
			Link link = network.getLinks().get(linkId);
			builder.setLinkId(link.getId());
		
			builder.setFreeTravelTime(Math.floor(link.getLength() / link.getFreespeed()));
		
			double flowCapacity_capPeriod = link.getCapacity() * scenario.getConfig().qsim().getFlowCapFactor();
			double marginalDelay_sec = ((1 / (flowCapacity_capPeriod / scenario.getNetwork().getCapacityPeriod()) ) );
			builder.setMarginalDelayPerLeavingVehicle_sec(marginalDelay_sec);
		
			double storageCapacity_cars = (int) (Math.ceil((link.getLength() * link.getNumberOfLanes()) 
					/ ((Network)network).getEffectiveCellSize()) * scenario.getConfig().qsim().getStorageCapFactor() );
			builder.setStorageCapacityCars(storageCapacity_cars);
			
			linkInfo = builder.build() ;
		
			linkId2congestionInfo.put(link.getId(), linkInfo);
			
			return linkInfo ;
		}

}
