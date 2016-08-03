/* *********************************************************************** *
 * project: org.matsim.*
 * ElevationScoringFunction.java
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

/**
 * 
 */
package playground.jjoubert.coord3D;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.ScoringFunction;

/**
 *
 * @author jwjoubert
 */
public class ElevationScoringFunction implements ScoringFunction {
	private double score;
	private Network network;
	private String vehicleType;
	
	private Map<Id<Link>, Map<String, Double>> experienceMap = new TreeMap<Id<Link>, Map<String,Double>>();
	
	public ElevationScoringFunction(Network network, String vehicleType) {
		this.network = network;
		this.vehicleType = vehicleType;
		reset();
	}

	@Override
	public void handleActivity(Activity activity) {
	}

	@Override
	public void handleLeg(Leg leg) {
		List<Link> links = RouteUtils.getLinksFromNodes(RouteUtils.getNodes((NetworkRoute) leg.getRoute(), network));
		for(Link link : links){
			score -= getExperiencedDistance(link);
		}
	}

	@Override
	public void agentStuck(double time) {
	}

	@Override
	public void addMoney(double amount) {
	}

	@Override
	public void finish() {
	}

	@Override
	public double getScore() {
		return this.score;
	}
	
	public void reset(){
		this.score = 0.0;
	}

	@Override
	public void handleEvent(Event event) {
	}
	
	/**
	 * Inflate the experienced distance based on the fitted function of Scora (2012).
	 * 
	 * @param link
	 * @return
	 */
	private double getExperiencedDistance(Link link){
		double angle = Utils3D.calculateAngle(link);
		
		double factor = 1.0;
		if(this.vehicleType.equalsIgnoreCase("A")){
			/* Keep factor 1.0 */
		} else if(this.vehicleType.equalsIgnoreCase("B")){
			factor = Math.max(0.0, 2.644847*Math.pow(angle, 2.0) + 380.740361*angle + 490.162370)/490.162370;
		} else{
			throw new RuntimeException("Don't know how to interpret the elevation factor for mode type '" + this.vehicleType + "'");
		}
		
		double experiencedDistance = factor*link.getLength();
		
		return experiencedDistance;
	}


}
