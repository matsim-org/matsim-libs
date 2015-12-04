/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideInvalidateStartTimes.java
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
package playground.thibautd.parknride.replanning;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.thibautd.parknride.ParkAndRideConstants;

/**
 * @author thibautd
 */
public class ParkAndRideInvalidateStartTimes extends AbstractMultithreadedModule {
	public ParkAndRideInvalidateStartTimes(final Controler controler) {
		super( controler.getConfig().global() );
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlanAlgorithm() {
			@Override
			public void run(final Plan plan) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity && ((Activity) pe).getType().equals( ParkAndRideConstants.PARKING_ACT ) ) {
						((Activity) pe).setStartTime( Time.UNDEFINED_TIME );
						((Activity) pe).setEndTime( Time.UNDEFINED_TIME );
					}
				}
			}
		};
	}
}

