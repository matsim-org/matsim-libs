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

/**
 * 
 */
package playground.johannes.gsv.demand.loader;

import java.io.IOException;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;

import playground.johannes.gsv.demand.AbstractTaskWrapper;
import playground.johannes.gsv.demand.tasks.PlanPrimaryActivity2;

/**
 * @author johannes
 *
 */
public class PlanPrimaryActivityLoader2 extends AbstractTaskWrapper {

	public PlanPrimaryActivityLoader2(Scenario scenario, Random random) throws IOException {
		delegate = new PlanPrimaryActivity2(scenario.getTransitSchedule(), random);
	}
}
