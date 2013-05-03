/* *********************************************************************** *
 * project: org.matsim.*
 * TransitTravelDisutilityWrapper.java
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

package org.matsim.pt.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

/**
 * Wraps a TransitTravelDisutility object into a TravelDisutility object.
 * 
 * @author cdobler
 */
public class TransitTravelDisutilityWrapper implements TravelDisutility {
	
	private final TransitTravelDisutility transitTravelDisutilty;
	private CustomDataManager customDataManager; 
	
	public TransitTravelDisutilityWrapper(TransitTravelDisutility transitTravelDisutilty) {
		this.transitTravelDisutilty = transitTravelDisutilty;
	}

	public void setCustomDataManager(CustomDataManager customDataManager) {
		this.customDataManager = customDataManager;
	}
	
	public double getLinkTravelDisutility(Link link, double time,
			Person person, Vehicle vehicle, CustomDataManager dataManager) {
		return this.transitTravelDisutilty.getLinkTravelDisutility(link, time, person, vehicle, dataManager);
	}

	public double getTravelTime(Person person, Coord coord, Coord toCoord) {
		return this.transitTravelDisutilty.getTravelTime(person, coord, toCoord);
	}

	public double getTravelDisutility(Person person, Coord coord, Coord toCoord) {
		return this.transitTravelDisutilty.getTravelDisutility(person, coord, toCoord);
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		return this.transitTravelDisutilty.getLinkTravelDisutility(link, time, person, vehicle, this.customDataManager);
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}

}
