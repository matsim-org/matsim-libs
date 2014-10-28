/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.noise2;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura , lkroeger
 *
 */

public class NoiseTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	
	// ...
	@Ignore
	@Test
	public final void test1(){
		
		String configFile = testUtils.getPackageInputDirectory()+"NoiseTest/config.xml";

		Controler controler = new Controler(configFile);	
		controler.addControlerListener(new NoiseControlerListener());
			
		controler.setOverwriteFiles(true);
		controler.run();

		
	 }
	
	@Test
	public final void test2(){
		
		String configFile = testUtils.getPackageInputDirectory()+"NoiseTest/config2.xml";

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		NoiseSpatialInfo noiseSpatialInfo = new NoiseSpatialInfo(scenario);
		
		noiseSpatialInfo.setActivityCoords();
		// 4)
		Assert.assertEquals("wrong number of activities per grid cell (0/0)", 1, noiseSpatialInfo.getZoneTuple2listOfActivityCoords().get(new Tuple<Integer, Integer>(0 , 0)).size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong number of activities per grid cell (0/1)", 2, noiseSpatialInfo.getZoneTuple2listOfActivityCoords().get(new Tuple<Integer, Integer>(0 , 1)).size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong number of activities per grid cell (1/0)", 1, noiseSpatialInfo.getZoneTuple2listOfActivityCoords().get(new Tuple<Integer, Integer>(1 , 0)).size(), MatsimTestUtils.EPSILON);

		noiseSpatialInfo.setReceiverPoints();
		// 1)
		Assert.assertEquals("wrong number of receiver points", 16, noiseSpatialInfo.getReceiverPointId2Coord().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong coord for receiver point Id '10'", new CoordImpl(500, 100).toString(), noiseSpatialInfo.getReceiverPointId2Coord().get(new IdImpl(10)).toString());
		// 2)
		Assert.assertEquals("wrong number of grid cells for which receiver points are stored", 9, noiseSpatialInfo.getZoneTuple2listOfReceiverPointIds().size(), MatsimTestUtils.EPSILON);
		
		noiseSpatialInfo.setActivityCoord2NearestReceiverPointId();
		// 1)
		Assert.assertEquals("wrong nearest receiver point Id for coord 300/300 (x/y)", "5", noiseSpatialInfo.getActivityCoord2receiverPointId().get(new CoordImpl(300, 300)).toString());
		Assert.assertEquals("wrong nearest receiver point Id for coord 150/150 (x/y)", "9", noiseSpatialInfo.getActivityCoord2receiverPointId().get(new CoordImpl(150, 150)).toString());
		Assert.assertEquals("wrong nearest receiver point Id for coord 100/100 (x/y)", "8", noiseSpatialInfo.getActivityCoord2receiverPointId().get(new CoordImpl(100, 100)).toString());
		Assert.assertEquals("wrong nearest receiver point Id for coord 500/500 (x/y)", "2", noiseSpatialInfo.getActivityCoord2receiverPointId().get(new CoordImpl(500, 500)).toString());
		// 2)
		Assert.assertEquals("wrong nearest receiver point Id for coord 500/100 (x/y)", "10", noiseSpatialInfo.getCoord2receiverPointId().get(new CoordImpl(500, 100)).toString());
			
		noiseSpatialInfo.setRelevantLinkIds();
		// 1)
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", new IdImpl("link1"), noiseSpatialInfo.getReceiverPointId2relevantLinkIds().get(new IdImpl("15")).get(0));
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", new IdImpl("linkA1"), noiseSpatialInfo.getReceiverPointId2relevantLinkIds().get(new IdImpl("15")).get(1));
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", new IdImpl("linkB1"), noiseSpatialInfo.getReceiverPointId2relevantLinkIds().get(new IdImpl("15")).get(2));
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", 3, noiseSpatialInfo.getReceiverPointId2relevantLinkIds().get(new IdImpl("15")).size());
		// 2)
		Assert.assertEquals("wrong distance between receiver point Id '8' and link Id '1'", 8.749854822140838, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermDs().get(new IdImpl("8")).get(new IdImpl("link0")), MatsimTestUtils.EPSILON);		
		// 3)
		Assert.assertEquals("wrong immission angle correction for receiver point 14 and link1", -0.8913405699036482, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(new IdImpl("14")).get(new IdImpl("link1")), MatsimTestUtils.EPSILON);		
		
		double angle = 180 - 114.60777973814005;
		double immissionCorrection = 10 * Math.log10((angle) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 9 and link5", immissionCorrection, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(new IdImpl("9")).get(new IdImpl("link5")), MatsimTestUtils.EPSILON);		

//		System.out.println(noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(new IdImpl("8")));
	}
	
}
