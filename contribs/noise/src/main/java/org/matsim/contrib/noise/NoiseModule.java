/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.noise;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.AbstractModule;
import org.matsim.facilities.ActivityFacility;

/**
* @author ikaddoura
*/
public final class NoiseModule extends AbstractModule {

	@Override
	public void install() {
		install(new NoiseComputationModule());
		install(new NoiseDefaultCarTravelDisutilityModule());
	}

	interface NoiseListener{
		void putNoiseRecord( NoiseRecord record ) ;
	}

	static class NoiseRecord {
		double startTime ;
		double endTime ;
		Id<ActivityFacility> facilityId ;
		Coord coord ;
		double immissions ;
		@Override
		public String toString() {
			return "NoiseRecord=[ startTime=" + startTime + " | endTime=" + endTime + " | facilityId=" + facilityId
				  + " | coord=" + coord + " | immissions=" + immissions + " ]";
		}
	}
}

