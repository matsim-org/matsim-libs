/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkCost.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.toronto.router.calculators;

import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.vehicles.Vehicle;


/**
 * Cost calculator with mode-based differential boarding penalties. These boarding penalties are standard
 * as required by GTAModel V2 and up.
 * 
 * @author pkucirek
 */
public class TorontoTransitRouterNetworkTravelTimeAndDisutility extends IterativeTransitTimeAndDisutility {

	private final double busWeight;
	private final double subwayWeight;
	private final double streetcarWeight;
	
	//private final HashMap<K, V>
	
	public TorontoTransitRouterNetworkTravelTimeAndDisutility(
			TransitRouterConfig config, TransitDataCache cache, double busWeight, double subwayWeight, double streetcarWeight) {
		super(cache, config);
		
		this.busWeight = busWeight;
		this.subwayWeight = subwayWeight;
		this.streetcarWeight = streetcarWeight; 
	}

	public double getLinkTravelDisutility(final TransitRouterNetworkLink link, final double time, final Person person, final Vehicle vehicle) {
		
		double cost = super.getLinkTravelDisutility(link, time, person, vehicle, /* dataManager */ null); 
			
		if (link instanceof TransitRouterNetworkLink){			
			if (((TransitRouterNetworkLink) link).getRoute() == null){
				//transfer link
				
				TransitRouterNetworkNode toStop = (TransitRouterNetworkNode) link.getToNode();
				String mode = toStop.route.getTransportMode();
				
				if (mode.toUpperCase().equals("BUS")){
					return cost - this.busWeight;
				}else if (mode.toUpperCase().equals("TRAM") || mode.toUpperCase().equals("STREETCAR")){
					return cost - this.streetcarWeight;
				}else if (mode.toUpperCase().equals("SUBWAY")){
					return cost - this.subwayWeight;
				}else{
					return cost;
				}
			}
		}
		return cost;
	}

}
	

