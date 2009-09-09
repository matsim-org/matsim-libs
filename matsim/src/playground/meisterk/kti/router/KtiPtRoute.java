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

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.world.Location;

public class KtiPtRoute extends GenericRouteImpl {

	public static final char SEPARATOR = '=';
	
	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
	private SwissHaltestelle fromStop;
	private Location fromMunicipality;
	private Location toMunicipality;
	private SwissHaltestelle toStop;
	
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
	}

	
	
	@Override
	public String getRouteDescription() {
		// TODO Auto-generated method stub
		return super.getRouteDescription();
	}

	@Override
	public void setRouteDescription(
			Link startLink, 
			String routeDescription,
			Link endLink) {
		
		super.setRouteDescription(startLink, routeDescription, endLink);
		String[] routeDescriptionArray = StringUtils.explode(routeDescription, SEPARATOR);
		this.fromStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(new IdImpl(routeDescriptionArray[0]));
		this.fromMunicipality = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(new IdImpl(routeDescriptionArray[1]));
		this.toMunicipality = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(new IdImpl(routeDescriptionArray[2]));
		this.toStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(new IdImpl(routeDescriptionArray[3]));
		
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
	
}
