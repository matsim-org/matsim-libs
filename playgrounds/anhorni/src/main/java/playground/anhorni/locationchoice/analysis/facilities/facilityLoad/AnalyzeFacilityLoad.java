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

package playground.anhorni.locationchoice.analysis.facilities.facilityLoad;

import org.matsim.core.gbl.Gbl;

public class AnalyzeFacilityLoad {
		
	public static void main(final String[] args) {
		Gbl.startMeasurement();
		final AnalyzeFacilityLoad analyzer = new AnalyzeFacilityLoad();
		analyzer.run();
		Gbl.printElapsedTime();
	}	
		
	public void run() {
		FacilityLoadReader reader = new FacilityLoadReader();
		reader.readFiles();
		
		FacilityLoadsWriter writer = new FacilityLoadsWriter();
		writer.write(reader.getFacilityLoads());		
	}

}
