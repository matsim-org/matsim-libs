/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.genericUtils;

import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author gleich
 *
 */
public class TerminusStopFinderTest {

	TransitSchedule schedule;
	TransitScheduleFactory stopFactory;

	@Test
	void testFindSecondTerminusStop() {
		/*
		 * straight line
		 *
		 * /----------------------\
		 * X------->--------------{>}
		 */
		ArrayList<TransitStopFacility> stops = new ArrayList<>();
		stops.add(getOrCreateStopAtCoord(0, 0));
		stops.add(getOrCreateStopAtCoord(10, 0));
		stops.add(getOrCreateStopAtCoord(40, 0));

		int indexSecondTerminusStop = TerminusStopFinder.findSecondTerminusStop(stops);
		Assertions.assertEquals(2, indexSecondTerminusStop);

		/*
		 * rectangular line
		 *
		 * <------{^}
		 * |       |
		 * |       |
		 * |       |
		 * X------->
		 */
		stops = new ArrayList<>();
		stops.add(getOrCreateStopAtCoord(0, 0));
		stops.add(getOrCreateStopAtCoord(10, 0));
		stops.add(getOrCreateStopAtCoord(10, 10));
		stops.add(getOrCreateStopAtCoord(0, 10));

		indexSecondTerminusStop = TerminusStopFinder.findSecondTerminusStop(stops);
		Assertions.assertEquals(2, indexSecondTerminusStop);

		/*
		 * triangular line both candidate stops at same distance from first terminus
		 *
		 * <---\
		 * |    \
		 * |     \
		 * |      \
		 * X------{>}
		 */
		stops = new ArrayList<>();
		stops.add(getOrCreateStopAtCoord(0, 0));
		stops.add(getOrCreateStopAtCoord(10, 0));
		stops.add(getOrCreateStopAtCoord(0, 10));

		indexSecondTerminusStop = TerminusStopFinder.findSecondTerminusStop(stops);
		Assertions.assertEquals(1, indexSecondTerminusStop);

		/*
		 * triangular line many stops
		 *
		 * <-\
		 * |  \
		 * |   \--{<}----\
		 * |              \
		 * X--->--->--->--->
		 */
		stops = new ArrayList<>();
		stops.add(getOrCreateStopAtCoord(0, 0));
		stops.add(getOrCreateStopAtCoord(10, 0));
		stops.add(getOrCreateStopAtCoord(20, 0));
		stops.add(getOrCreateStopAtCoord(30, 0));
		stops.add(getOrCreateStopAtCoord(40, 0));
		stops.add(getOrCreateStopAtCoord(20, 10));
		stops.add(getOrCreateStopAtCoord(0, 20));

		indexSecondTerminusStop = TerminusStopFinder.findSecondTerminusStop(stops);
		Assertions.assertEquals(5, indexSecondTerminusStop);

		/*
		 * TODO: Currently failing, would require a more elaborate algorithm to determine the terminus stop
		 * More complex example:
		 *
		 * Back- and forth directions have different lengths. For the human eye the second terminus is obvious
		 *
		 * {<}------<---<---<---^
		 *   \------>--->       |
		 *              |       ^
		 *   /--<---<---<       |
		 *  X--->--->--->--->--->
		 */
		stops = new ArrayList<>();
		// first terminus, going east (lower part)
		stops.add(getOrCreateStopAtCoord(0, 0));
		stops.add(getOrCreateStopAtCoord(10, 0));
		stops.add(getOrCreateStopAtCoord(20, 0));
		stops.add(getOrCreateStopAtCoord(30, 0));
		stops.add(getOrCreateStopAtCoord(40, 0));
		stops.add(getOrCreateStopAtCoord(50, 0));
		// going north
		stops.add(getOrCreateStopAtCoord(50, 20));
		stops.add(getOrCreateStopAtCoord(50, 40));
		// going west (upper part)
		stops.add(getOrCreateStopAtCoord(40, 40));
		// stop mistaken as second terminus
		stops.add(getOrCreateStopAtCoord(30, 40));
		stops.add(getOrCreateStopAtCoord(20, 40));
		// real second terminus
		stops.add(getOrCreateStopAtCoord(0, 40));
		// going back (upper part)
		stops.add(getOrCreateStopAtCoord(20, 30));
		stops.add(getOrCreateStopAtCoord(30, 30));
		// going back (lower part)
		stops.add(getOrCreateStopAtCoord(30, 10));
		stops.add(getOrCreateStopAtCoord(20, 10));
		stops.add(getOrCreateStopAtCoord(10, 10));

		indexSecondTerminusStop = TerminusStopFinder.findSecondTerminusStop(stops);
//		Assert.assertEquals(11, indexSecondTerminusStop);
	}

	@BeforeEach
	public void setUp() {
		schedule = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getTransitSchedule();
		stopFactory = schedule.getFactory();
	}

	private TransitStopFacility getOrCreateStopAtCoord(int x, int y) {
		Id<TransitStopFacility> stopId = getStopId(x, y);
		if (schedule.getFacilities().containsKey(stopId)) {
			return schedule.getFacilities().get(stopId);
		} else {
			return stopFactory.createTransitStopFacility(
					stopId, CoordUtils.createCoord(x, y), false);
		}
	}

	private Id<TransitStopFacility> getStopId(int x, int y) {
		return Id.create(x + "," + y, TransitStopFacility.class);
	}

}
