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

package playground.benjamin.internalization.exposure;

import java.util.Map;
import java.util.SortedMap;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.benjamin.internalization.EquilTestSetUp;
import playground.benjamin.scenarios.munich.exposure.GridTools;
import playground.benjamin.scenarios.munich.exposure.IntervalHandler;

/**
 * @author amit
 */

public class IntervalHandlerTest {

	@Test
	public void activityDurationForActiveAgentTest ( ) {
		EquilTestSetUp equilTestSetUp = new EquilTestSetUp();
		Scenario sc = equilTestSetUp.createConfig();
		equilTestSetUp.createNetwork(sc);
		equilTestSetUp.createActiveAgents(sc);

		EventsManager events = EventsUtils.createEventsManager();

		Double xMin = 0.0;
		Double xMax = 20000.0;
		Double yMin = 0.0;
		Double yMax = 12500.0;

		Integer noOfXCells = 32;
		Integer noOfYCells = 20;

		Integer noOfTimeBins =24;
		Double timeBinSize = sc.getConfig().qsim().getEndTime() / noOfTimeBins;

		GridTools gt = new GridTools(sc.getNetwork().getLinks(), xMin, xMax, yMin, yMax);
		Map<Id<Link>, Integer> links2xCells = gt.mapLinks2Xcells(noOfXCells);
		Map<Id<Link>, Integer> links2yCells = gt.mapLinks2Ycells(noOfYCells);
		IntervalHandler intHandler = new IntervalHandler( timeBinSize, sc.getConfig().qsim().getEndTime(), 32, 20, links2xCells, links2yCells);
		events.addHandler(intHandler);

		Controler controler = new Controler(sc);
		controler.getEvents().addHandler(intHandler);
		controler.run();

		/*
		 *  The route of active agent consists of 12-23-38-84-45 links (home to work) and then 45-56-67-71 (work to home)
		 *  Thus total travel time = (1+73+181+180+72) + (1+37+37+37+36) = 655 // QSim returns 180 sec on link 84 (dont know why)
		 *  This should return total activity duration = 24*3600 - 655 = 85745
		 */

		double sumOfDurationsInAllTimebins = 0.;
		SortedMap<Double, Double[][]> timeBin2Durations = intHandler.getDuration();
		for ( double timeBin : timeBin2Durations.keySet()) {
			Double [] [] durations = timeBin2Durations.get(timeBin);

			for (int ii = 0; ii <durations.length; ii++){
				for (int jj = 0; jj<durations[0].length;jj++){
					sumOfDurationsInAllTimebins += durations[ii][jj];
				}
			}
		}
		Assert.assertEquals("Total activity duration is wrong.", 85745, sumOfDurationsInAllTimebins, MatsimTestUtils.EPSILON );
	}

	@Test
	public void activityDurationForActiveAndPassiveAgentTest ( ) {
		EquilTestSetUp equilTestSetUp = new EquilTestSetUp();
		Scenario sc = equilTestSetUp.createConfig();
		equilTestSetUp.createNetwork(sc);
		equilTestSetUp.createActiveAgents(sc);
		equilTestSetUp.createPassiveAgents(sc);

		EventsManager events = EventsUtils.createEventsManager();

		Double xMin = 0.0;
		Double xMax = 20000.0;
		Double yMin = 0.0;
		Double yMax = 12500.0;

		Integer noOfXCells = 32;
		Integer noOfYCells = 20;

		Integer noOfTimeBins =24;
		Double timeBinSize = sc.getConfig().qsim().getEndTime() / noOfTimeBins;

		GridTools gt = new GridTools(sc.getNetwork().getLinks(), xMin, xMax, yMin, yMax);
		Map<Id<Link>, Integer> links2xCells = gt.mapLinks2Xcells(noOfXCells);
		Map<Id<Link>, Integer> links2yCells = gt.mapLinks2Ycells(noOfYCells);
		IntervalHandler intHandler = new IntervalHandler( timeBinSize, sc.getConfig().qsim().getEndTime(), 32, 20, links2xCells, links2yCells);
		events.addHandler(intHandler);

		Controler controler = new Controler(sc);
		controler.getEvents().addHandler(intHandler);
		controler.run();

		/*
		 *  85745 sec from above. 
		 *  Additionally, all 20 agents work 86399 sec. Thus total activity duration for all agents = 86399*20+85745 = 1813725
		 */

		double sumOfDurationsInAllTimebins = 0.;
		SortedMap<Double, Double[][]> timeBin2Durations = intHandler.getDuration();
		for ( double timeBin : timeBin2Durations.keySet()) {
			Double [] [] durations = timeBin2Durations.get(timeBin);

			for (int ii = 0; ii <durations.length; ii++){
				for (int jj = 0; jj<durations[0].length;jj++){
					sumOfDurationsInAllTimebins += durations[ii][jj];
				}
			}
		}
		Assert.assertEquals("Total activity duration is wrong.", 1813725, sumOfDurationsInAllTimebins, MatsimTestUtils.EPSILON );
	}
}


