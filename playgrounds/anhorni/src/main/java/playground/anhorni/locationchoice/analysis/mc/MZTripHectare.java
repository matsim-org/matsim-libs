/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.analysis.mc;

import playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources.Hectare;

public class MZTripHectare {
	
	MZTrip mzTrip;
	Hectare hectare;
	
	public MZTripHectare(MZTrip mzTrip, Hectare hectare) {
		this.mzTrip = mzTrip;
		this.hectare = hectare;
	}
	
	public MZTrip getMzTrip() {
		return mzTrip;
	}
	public void setMzTrip(MZTrip mzTrip) {
		this.mzTrip = mzTrip;
	}
	public Hectare getHectare() {
		return hectare;
	}
	public void setHectare(Hectare hectare) {
		this.hectare = hectare;
	}
}
