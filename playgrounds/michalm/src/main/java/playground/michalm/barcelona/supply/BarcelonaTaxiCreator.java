/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.barcelona.supply;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.zone.util.RandomPointUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;

import playground.michalm.barcelona.BarcelonaZones;

public class BarcelonaTaxiCreator implements VehicleGenerator.VehicleCreator {
	private static final int PAXPERCAR = 4;

	private final Network network;
	private final PreparedPolygon preparedPolygon;

	private int currentVehicleId = 0;

	public BarcelonaTaxiCreator(Scenario scenario) {
		network = (Network)scenario.getNetwork();
		preparedPolygon = new PreparedPolygon(BarcelonaZones.readAgglomerationArea());
	}

	@Override
	public VehicleImpl createVehicle(double t0, double t1) {
		Id<Vehicle> vehId = Id.create("taxi" + currentVehicleId++, Vehicle.class);
		Point p = RandomPointUtils.getRandomPointInGeometry(preparedPolygon);
		Link link = NetworkUtils.getNearestLinkExactly(network, MGC.point2Coord(p));
		return new VehicleImpl(vehId, link, PAXPERCAR, Math.round(t0), Math.round(t1));
	}
}