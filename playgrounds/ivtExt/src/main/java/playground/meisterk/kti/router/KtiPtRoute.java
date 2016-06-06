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

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.routes.AbstractRoute;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.matrices.Entry;

public class KtiPtRoute extends AbstractRoute {

	public static final char SEPARATOR = '=';
	public static final String IDENTIFIER = "kti";

	public static final double CROW_FLY_FACTOR = 1.5;

	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
	private SwissHaltestelle fromStop = null;
	private BasicLocation fromMunicipality = null;
	private BasicLocation toMunicipality = null;
	private SwissHaltestelle toStop = null;
	private Double inVehicleTime = null;
	private String routeDescription;

	public KtiPtRoute(Id startLinkId, Id endLinkId, PlansCalcRouteKtiInfo plansCalcRouteKtiInfo) {
		super(startLinkId, endLinkId);
		this.plansCalcRouteKtiInfo = plansCalcRouteKtiInfo;
	}

	public KtiPtRoute(
			Id startLinkId,
			Id endLinkId,
			PlansCalcRouteKtiInfo plansCalcRouteKtiInfo,
			SwissHaltestelle fromStop,
			BasicLocation fromMunicipality,
			BasicLocation toMunicipality,
			SwissHaltestelle toStop) {
		this(startLinkId, endLinkId, plansCalcRouteKtiInfo);
		this.fromStop = fromStop;
		this.fromMunicipality = fromMunicipality;
		this.toMunicipality = toMunicipality;
		this.toStop = toStop;
		this.inVehicleTime = this.calcInVehicleTime();
	}

	@Override
	public String getRouteDescription() {

		if (this.fromStop == null) {
//			return super.getRouteDescription();
			return this.routeDescription ;
		}
		String routeDescription =
			IDENTIFIER + SEPARATOR +
			this.fromStop.getId() + SEPARATOR +
			this.fromMunicipality.getId() + SEPARATOR +
			Double.toString(this.inVehicleTime) + SEPARATOR +
			this.toMunicipality.getId() + SEPARATOR +
			this.toStop.getId();

		return routeDescription;

	}

	@Override
	public String getRouteType() {
		return "ktiPtRoute";
	}

	@Override
	public void setRouteDescription(
			String routeDescription) {

//		super.setRouteDescription(routeDescription);
		this.routeDescription = routeDescription ;
		
		if (routeDescription.startsWith(IDENTIFIER)) {
			throw new RuntimeException("Reference to balmermi removed! the rest of the code is not working");
			//String[] routeDescriptionArray = StringUtils.explode(routeDescription, SEPARATOR);
			//this.fromStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(routeDescriptionArray[1]);
			//this.fromMunicipality = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(Id.create(routeDescriptionArray[2], Zone.class));
			//this.inVehicleTime = Double.parseDouble(routeDescriptionArray[3]);
			//this.toMunicipality = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(Id.create(routeDescriptionArray[4], Zone.class));
			//this.toStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(routeDescriptionArray[5]);
		} else {
			this.fromStop = null;
			this.fromMunicipality = null;
			this.toMunicipality = null;
			this.toStop = null;
		}

	}

	public double calcInVehicleDistance() {
		return CoordUtils.calcEuclideanDistance(this.getFromStop().getCoord(), this.getToStop().getCoord()) * CROW_FLY_FACTOR;
	}

	protected double calcInVehicleTime() {

		Entry matrixEntry = this.plansCalcRouteKtiInfo.getPtTravelTimes().getEntry(this.fromMunicipality.getId().toString(), this.toMunicipality.getId().toString());
		if (matrixEntry == null) {
			throw new RuntimeException("No entry found for " + this.fromMunicipality.getId() + " --> " + this.toMunicipality.getId());
		}

		double travelTime = matrixEntry.getValue() * 60.0;
		/*
		 * A value of NaN in the travel time matrix indicates that the matrix contains no valid value for this entry.
		 * In this case, the travel time is calculated with the distance of the relation and an average speed.
		 */
		if (Double.isNaN(travelTime)) {
			travelTime = this.calcInVehicleDistance() / this.plansCalcRouteKtiInfo.getKtiConfigGroup().getIntrazonalPtSpeed();
		}

		return travelTime;

	}

	public double calcAccessEgressDistance(final Activity fromAct, final Activity toAct) {

		return
		(CoordUtils.calcEuclideanDistance(fromAct.getCoord(), this.getFromStop().getCoord()) +
		CoordUtils.calcEuclideanDistance(toAct.getCoord(), this.getToStop().getCoord()))
		* CROW_FLY_FACTOR;

	}

	public SwissHaltestelle getFromStop() {
		return fromStop;
	}

	public BasicLocation getFromMunicipality() {
		return fromMunicipality;
	}

	public BasicLocation getToMunicipality() {
		return toMunicipality;
	}

	public SwissHaltestelle getToStop() {
		return toStop;
	}

	public Double getInVehicleTime() {
		return this.inVehicleTime;
	}

}
