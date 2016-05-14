/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.workbench.santiago;

import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.polettif.boescpa.lib.tools.PopulationUtils;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

/**
 * Adapts the provided population for the new network.
 */
public class PreparePopulation {

	public static void main(String[] args) {
		String base = "C:/Users/Flavio/Desktop/data/santiago/";

		PopulationUtils.readPopulation(base + "input_original/plans_final.xml");
	}
}
