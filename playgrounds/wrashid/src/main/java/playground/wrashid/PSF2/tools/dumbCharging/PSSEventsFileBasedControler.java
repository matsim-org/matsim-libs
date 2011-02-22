/* *********************************************************************** *
 * project: org.matsim.*
 * PSSEventsFileBasedControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.tools.dumbCharging;

import playground.wrashid.PSF2.chargingSchemes.dumbCharging.EventsBasedDumbChargingMain;

/**
 *  As a template config file, use:
	test/input/playground/wrashid/PSF2/chargingSchemes/dumbCharging/config-event-file-based.xml
	
	adapt that file as follows:
	- adapt the plans, facilities and network file. facilities file only needed, if used for scenario.
	- also set the property: main.inputEventsForSimulationPath (path to events file)
	- actually the rest of the content of this file is ignored (should be just run 1 iteraion! => else need to adapt it)
	
 * @author wrashid
 *
 */
public class PSSEventsFileBasedControler {

	
	
	public static void main(String[] args) {
		EventsBasedDumbChargingMain.runEventsBasedPSSControler("test/input/playground/wrashid/PSF2/chargingSchemes/dumbCharging/config-event-file-based.xml");
	}
	
	
}
