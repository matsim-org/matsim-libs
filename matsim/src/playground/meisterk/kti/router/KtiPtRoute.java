/* *********************************************************************** *
 * project: org.matsim.*
 * KtiPtRoute.java
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

package playground.meisterk.kti.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.matrices.Entry;
import org.matsim.world.Location;

public class KtiPtRoute extends GenericRouteImpl {

	public static final char SEPARATOR = '=';
	public static final String IDENTIFIER = "kti";
	
	public static final double CROW_FLY_FACTOR = 1.5;
	
	private final static Logger log = Logger.getLogger(KtiPtRoute.class);

	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
	private SwissHaltestelle fromStop = null;
	private Location fromMunicipality = null;
	private Location toMunicipality = null;
	private SwissHaltestelle toStop = null;
	private Double ptMatrixInvehicleTime = null;
	
	public KtiPtRoute(Link startLink, Link endLink, PlansCalcRouteKtiInfo plansCalcRouteKtiInfo) {
		super(startLink, endLink);
		this.plansCalcRouteKtiInfo = plansCalcRouteKtiInfo;
	}

	public KtiPtRoute(
			Link startLink, 
			Link endLink,
			PlansCalcRouteKtiInfo plansCalcRouteKtiInfo,
			SwissHaltestelle fromStop,
			Location fromMunicipality, 
			Location toMunicipality,
			SwissHaltestelle toStop) {
		this(startLink, endLink, plansCalcRouteKtiInfo);
		this.fromStop = fromStop;
		this.fromMunicipality = fromMunicipality;
		this.toMunicipality = toMunicipality;
		this.toStop = toStop;
		this.ptMatrixInvehicleTime = this.calcInVehicleTime();
	}
	
	@Override
	public String getRouteDescription() {
		
		if (this.fromStop == null) {
			return super.getRouteDescription();
		}
		String routeDescription = 
			IDENTIFIER + SEPARATOR + 
			this.fromStop.getId() + SEPARATOR + 
			this.fromMunicipality.getId() + SEPARATOR +
			Double.toString(this.ptMatrixInvehicleTime) + SEPARATOR +
			this.toMunicipality.getId() + SEPARATOR +
			this.toStop.getId(); 
		
		return routeDescription;
		
	}

	@Override
	public void setRouteDescription(
			Link startLink, 
			String routeDescription,
			Link endLink) {
		
		super.setRouteDescription(startLink, routeDescription, endLink);
		if (routeDescription.startsWith(IDENTIFIER)) {
			String[] routeDescriptionArray = StringUtils.explode(routeDescription, SEPARATOR);
			this.fromStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(new IdImpl(routeDescriptionArray[1]));
			this.fromMunicipality = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(new IdImpl(routeDescriptionArray[2]));			
			this.ptMatrixInvehicleTime = Double.parseDouble(routeDescriptionArray[3]);
			this.toMunicipality = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(new IdImpl(routeDescriptionArray[4]));
			this.toStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(new IdImpl(routeDescriptionArray[5]));
		} else {
			this.fromStop = null;
			this.fromMunicipality = null;			
			this.toMunicipality = null;
			this.toStop = null;
		}
		
	}

	public double calcInVehicleDistance() {
		return CoordUtils.calcDistance(this.getFromStop().getCoord(), this.getToStop().getCoord()) * CROW_FLY_FACTOR;
	}
	
	protected double calcInVehicleTime() {

		Entry traveltime = this.plansCalcRouteKtiInfo.getPtTravelTimes().getEntry(this.fromMunicipality, this.toMunicipality);
		if (traveltime == null) {
			throw new RuntimeException("No entry found for " + this.fromMunicipality.getId() + " --> " + this.toMunicipality.getId());
		}
		return traveltime.getValue() * 60.0;
		
	}

	public double calcAccessEgressDistance(final ActivityImpl fromAct, final ActivityImpl toAct) {
		
		return 
		(CoordUtils.calcDistance(fromAct.getCoord(), this.getFromStop().getCoord()) + 
		CoordUtils.calcDistance(toAct.getCoord(), this.getToStop().getCoord()))
		* CROW_FLY_FACTOR;
		
	}
	
	public SwissHaltestelle getFromStop() {
		return fromStop;
	}

	public Location getFromMunicipality() {
		return fromMunicipality;
	}

	public Location getToMunicipality() {
		return toMunicipality;
	}

	public SwissHaltestelle getToStop() {
		return toStop;
	}

	public Double getPtMatrixInVehicleTime() {
		return this.ptMatrixInvehicleTime;
	}
	
}
