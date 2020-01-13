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
package org.matsim.contrib.av.intermodal.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class FlexibleDistanceBasedVariableAccessModule implements VariableAccessEgressTravelDisutility {

	
	private Map<String,Boolean> teleportedModes = new HashMap<>();
	private Map<String,Integer> distanceMode = new TreeMap<>();
	private Map<String, LeastCostPathCalculator> lcpPerNonTeleportedMode = new HashMap<>();
	
	private final Network carnetwork;
	private final Config config;
	
	private boolean checkCarAvail = true;
	private final Random rand = MatsimRandom.getRandom();
	/**
	 * 
	 */
	public FlexibleDistanceBasedVariableAccessModule(Network carnetwork, Config config) {
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
	public void registerMode(String mode, int maximumAccessDistance, boolean isTeleported){
		if (this.distanceMode.containsKey(mode)){
			throw new RuntimeException("mode "+mode+"is already registered. Check your config four double entries..");
		}
		if (isTeleported){
			teleportedModes.put(mode, true);
			distanceMode.put(mode, maximumAccessDistance);
		}
		else {
			teleportedModes.put(mode, false);
			distanceMode.put(mode, maximumAccessDistance);
			
			
		}
		
	}
	
	@Override
	public Leg getAccessEgressModeAndTraveltime(Person person, Coord coord, Coord toCoord, double time) {
		double egressDistance = CoordUtils.calcEuclideanDistance(coord, toCoord);
		
		String mode = getModeForDistance(egressDistance, person);
		Leg leg = PopulationUtils.createLeg(mode);
		Link startLink = NetworkUtils.getNearestLink(carnetwork, coord);
		Link endLink = NetworkUtils.getNearestLink(carnetwork, toCoord);
		Route route = RouteUtils.createGenericRouteImpl(startLink.getId(), endLink.getId());
		leg.setRoute(route);
		if (this.teleportedModes.get(mode)){
			double distf = config.plansCalcRoute().getModeRoutingParams().get(mode).getBeelineDistanceFactor();
			double speed = config.plansCalcRoute().getModeRoutingParams().get(mode).getTeleportedModeSpeed();
			double distance = egressDistance*distf;
			double travelTime = distance / speed;
			leg.setTravelTime(travelTime);
			route.setDistance(distance);
			leg.setDepartureTime(time);
			
						
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
	private String getModeForDistance(double egressDistance, Person p) {
		if (distanceMode.containsKey(TransportMode.walk))
		if (egressDistance<=distanceMode.get(TransportMode.walk)) return TransportMode.walk;
		//TODO: MAke Config switch
		List<String> possibleModes = new ArrayList<>();
		for (Entry<String,Integer> e : this.distanceMode.entrySet()){
			if (e.getValue()>=egressDistance){
				if (e.getKey().equals(TransportMode.car)){
					if ((checkCarAvail)&&(p!=null)&&(p.getCustomAttributes()!=null)){
						
						String carA  = PersonUtils.getCarAvail(p);
						if (carA!=null){
						if (carA.equals("always")||carA.equals("sometimes")){
							possibleModes.add(e.getKey());
						
						}
						}
						else {				
							possibleModes.add(e.getKey());
						}
					}
					else {
						possibleModes.add(e.getKey());
					}
				}
				else {
					possibleModes.add(e.getKey());
				}
			}
		}
		if (possibleModes.size()<1){
//			Logger.getLogger(getClass()).warn("Egress distance "+egressDistance+ " is not covered by any available mode mode for person "+p.getId()+". Assuming walk.");
			return (TransportMode.walk);
		}
		return possibleModes.get(rand.nextInt(possibleModes.size()));
	}


	@Override
	public boolean isTeleportedAccessEgressMode(String mode) {
		return this.teleportedModes.get(mode);
	}
	
	
	

}
