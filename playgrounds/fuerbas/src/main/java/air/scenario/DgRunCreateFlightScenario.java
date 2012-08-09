/* *********************************************************************** *
 * project: org.matsim.*
 * DgRunCreateFlightScenario
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
package air.scenario;


/**
 * @author dgrether
 *
 */
public class DgRunCreateFlightScenario {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//WORLD WIDE AIR TRAFFIC
		new DgCreateDgFlightScenario().createWorldFlightScenario(DgCreateDgFlightScenario.inputAirportsCoordinatesFilename, DgCreateDgFlightScenario.inputOagFilename);

	//EUROPEAN AIR TRAFFIC
		new DgCreateDgFlightScenario().createEuropeanFlightScenario(DgCreateDgFlightScenario.inputAirportsCoordinatesFilename, DgCreateDgFlightScenario.inputOagFilename);

	// GERMAN AIR TRAFFIC
		new DgCreateDgFlightScenario().createGermanFlightScenario(DgCreateDgFlightScenario.inputAirportsCoordinatesFilename, DgCreateDgFlightScenario.inputOagFilename);
	
	}

}
