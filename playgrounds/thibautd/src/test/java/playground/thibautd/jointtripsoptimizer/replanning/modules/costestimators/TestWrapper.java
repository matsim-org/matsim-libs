/* *********************************************************************** *
 * project: org.matsim.*
 * TestWrapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.jointtripsoptimizer.run.JointControler;
import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;

/**
 * Tests the Wrapper for travel time
 * @author thibautd
 */
public class TestWrapper {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private Link[] links = null;
	private double[] times = null;

	private TravelTimeCalculator travelTime1 = null;
	private TravelTimeCalculator travelTime2 = null;

	@Before
	public void init() {
		String inputPath = getParentDirectory(utils.getPackageInputDirectory(), 3);
		String outputPath = utils.getOutputDirectory();

		JointControler controler = (JointControler) JointControlerUtils.createControler(inputPath+"/config.xml");
		controler.getConfig().controler().setOutputDirectory(outputPath+"run1");
		controler.getConfig().controler().setFirstIteration(0);
		controler.getConfig().controler().setLastIteration(0);

		controler.run();
		travelTime1 = (TravelTimeCalculator) controler.getTravelTimeCalculator();


		controler = (JointControler) JointControlerUtils.createControler(inputPath+"/config.xml");
		controler.getConfig().controler().setOutputDirectory(outputPath+"run2");
		controler.getConfig().controler().setFirstIteration(0);
		controler.getConfig().controler().setLastIteration(0);

		controler.run();
		travelTime2 = (TravelTimeCalculator) controler.getTravelTimeCalculator();

		Map<Id, Link> linkMap = controler.getNetwork().getLinks();
		links = new Link[]{
			linkMap.get( new IdImpl(1) ),
			linkMap.get( new IdImpl(2) ),
			linkMap.get( new IdImpl(3) ),
			linkMap.get( new IdImpl(4) ),
			linkMap.get( new IdImpl(5) )};

		int nTimes = 600;
		double step = (24 * 3600) / nTimes;
		times = new double[nTimes];

		for (int i=0; i < times.length; i++) {
			times[i] = i * step;
		}
	}


	private String getParentDirectory(final String path, final int levels) {
		String[] pathArray = path.split("/");
		String output = "";

		for (int i=0; i < pathArray.length - levels; i++) {
			output += pathArray[i] + "/";
		}

		return output;
	}



	@Test
	public void testConsistency() {
		TravelTime unWrapped = travelTime1;
		TravelTime wrapped = new TravelTimeEstimatorWrapper(travelTime2);

		for (Link link : links) {
			for (double time : times) {
				Assert.assertEquals(
						"results given by TravelTimeWrapper are different from the ones of the unWrapped instance",
						unWrapped.getLinkTravelTime(link, time),
						wrapped.getLinkTravelTime(link, time),
						MatsimTestUtils.EPSILON);
			}
		}
	}
}

