/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.santiago;

public final class SantiagoScenarioConstants {

	// additional modes
	public enum Modes{
							bus,
							metro,
							train,
							colectivo,
							taxi,
							other,
//							school_bus,
//							motorcycle,
							truck
						};
					
	//Santiago greater area population according to "Informe de Difusión", page 9, tabla 1			
	public final static int N = 6651700;
	
	public final static String toCRS = "EPSG:32719";
	
	public final class SubpopulationName {
		public final static String carUsers = "carUsers";
	}
	
	public final class SubpopulationValues {
		public final static String carAvail = "carAvail";
	}
	
}
