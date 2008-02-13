/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioParsing.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi.mz;

import java.util.Date;

import org.matsim.plans.Plans;

import playground.balmermi.Scenario;

public class MZ2Plans {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void dilZhFilter() {

		System.out.println("running dilZhFilter... " + (new Date()));
		
		Scenario.setUpScenarioConfig();

		//////////////////////////////////////////////////////////////////////

		System.out.println("  creating plans object... ");
		Plans plans = new Plans(false);
		System.out.println("  done.");

		System.out.println("  running plans modules... ");
		new PlansCreateFromMZ("input/Wegeketten_tabelle.dat").run(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////
		
		Scenario.writePlans(plans);
		Scenario.writeConfig();

		System.out.println("done. " + (new Date()));
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		dilZhFilter();
	}
}
