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

package org.matsim.withinday.trafficmonitoring;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.router.util.TravelTime;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Inject;

/**
* @author ikaddoura
*/

public class TtmobsimListener implements MobsimAfterSimStepListener {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Inject
	private TravelTime travelTime;

	private boolean case1 = false;
	private boolean case2 = false;

	private Link link;
	private double networkChangeEventTime;
	private double reducedFreespeed;

	public TtmobsimListener(NetworkChangeEvent nce) {

		if (nce.getLinks().size() > 1) {
			throw new RuntimeException("Expecting only one network change event for a single link. Aborting...");
		} else {
			for (Link link : nce.getLinks()) {
				this.link = link;
				this.networkChangeEventTime = nce.getStartTime();
				this.reducedFreespeed = nce.getFreespeedChange().getValue();

				Assertions.assertEquals(true, this.reducedFreespeed < this.link.getFreespeed());
			}
		}
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {

		if (e.getSimulationTime() <= networkChangeEventTime) {

			Assertions.assertEquals(Math.ceil(link.getLength()/link.getFreespeed()),
					Math.ceil(travelTime.getLinkTravelTime(link, e.getSimulationTime(), null, null)),
					testUtils.EPSILON,
					"Wrong travel time at time step " + e.getSimulationTime() + ". Should be the freespeed travel time.");

			case1 = true;

		} else {
			Assertions.assertEquals(Math.ceil(link.getLength() / reducedFreespeed),
					Math.ceil(travelTime.getLinkTravelTime(link, e.getSimulationTime(), null, null)),
					testUtils.EPSILON,
					"Wrong travel time at time step " + e.getSimulationTime() + ". Should be the travel time resulting from the network change event (reduced freespeed).");

			case2 = true;
		}

	}

	public boolean isCase1() {
		return case1;
	}

	public boolean isCase2() {
		return case2;
	}

}

