/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.pt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class DistancebasedVariableAccessModule implements VariableAccessEgressTravelDisutility {

	
	private Map<String,Boolean> teleportedModes = new HashMap<>();
	private Map<Integer,String> distanceMode = new TreeMap<>();
	private Map<String, LeastCostPathCalculator> lcpPerNonTeleportedMode = new HashMap<>();
	
	private final Network carnetwork;
	private final Config config;
	
	/**
	 * 
	 */
	public DistancebasedVariableAccessModule(Network carnetwork, Config config) {
		this.config = config;
		this.carnetwork = carnetwork;
	}
	/**
	 * 
	 * @param mode the mode to register
	 * @param maximumAccessDistance maximum beeline Distance for using this mode
	 * @param isTeleported defines whether this is a teleported mode
	 * @param lcp for non teleported modes, some travel time assumption is required
	 */
	public void registerMode(String mode, int maximumAccessDistance, boolean isTeleported, LeastCostPathCalculator lcp){
		if (this.distanceMode.containsKey(maximumAccessDistance)){
			throw new RuntimeException("Maximum distance of "+maximumAccessDistance+" is already registered to mode "+distanceMode.get(maximumAccessDistance)+" and cannot be re-registered to mode: "+mode);
		}
		if (isTeleported){
			teleportedModes.put(mode, true);
			distanceMode.put(maximumAccessDistance, mode);
		} else {
			teleportedModes.put(mode, false);
			distanceMode.put(maximumAccessDistance,mode);
			if (lcp!= null){
			lcpPerNonTeleportedMode.put(mode, lcp);}
			else {throw new NullPointerException("LCP cannot be null for non-teleported mode");
			}
			
		}
	}
	
	
	/* (non-Javadoc)
	 * @see playground.jbischoff.pt.VariableAccessEgressTravelDisutility#getAccessEgressModeAndTraveltime(org.matsim.api.core.v01.population.Person, org.matsim.api.core.v01.Coord, org.matsim.api.core.v01.Coord)
	 */
	@Override
	public Leg getAccessEgressModeAndTraveltime(Person person, Coord coord, Coord toCoord) {
		double egressDistance = CoordUtils.calcEuclideanDistance(coord, toCoord);
		String mode = getModeForDistance(egressDistance);
		Leg leg = PopulationUtils.createLeg(mode);
		Link startLink = NetworkUtils.getNearestLink(carnetwork, coord);
		Link endLink = NetworkUtils.getNearestLink(carnetwork, toCoord);
		Route route = new GenericRouteImpl(startLink.getId(),endLink.getId());
		leg.setRoute(route);
		if (this.teleportedModes.get(mode)){
			double distf = config.plansCalcRoute().getModeRoutingParams().get(mode).getBeelineDistanceFactor();
			double speed = config.plansCalcRoute().getModeRoutingParams().get(mode).getTeleportedModeSpeed();
			double distance = egressDistance*distf;
			double travelTime = distance / speed;
			leg.setTravelTime(travelTime);
			route.setDistance(distance);
			
						
		} else {
			double distance = egressDistance*1.3;
			double travelTime = distance / 7.25;
			leg.setTravelTime(travelTime);
			route.setDistance(distance);
			
//			too expensive
//			Path path = this.lcpPerNonTeleportedMode.get(mode).calcLeastCostPath(startLink.getFromNode(), endLink.getToNode(), 0, person, null);
//			route.setDistance(path.travelCost);
//			route.setTravelTime(path.travelTime);
		}
		return leg;
	}

	/**
	 * @param egressDistance
	 * @return
	 */
	private String getModeForDistance(double egressDistance) {
		for (Entry<Integer, String> e : this.distanceMode.entrySet()){
			if (e.getKey()>=egressDistance){
//				System.out.println("Mode" + e.getValue()+" "+egressDistance);
				return e.getValue();
			}
		}
		throw new RuntimeException(egressDistance + " m is not covered by any egress / access mode.");
		
	}


	/* (non-Javadoc)
	 * @see playground.jbischoff.pt.VariableAccessEgressTravelDisutility#isTeleportedAccessEgressMode(java.lang.String)
	 */
	@Override
	public boolean isTeleportedAccessEgressMode(String mode) {
		return this.teleportedModes.get(mode);
	}
	
	
	

}
