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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura , lkroeger
 *
 */

public class NoiseTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	// Tests the NoisSpatialInfo functionality separately for each function
	@Ignore
	@Test
	public final void test1(){
		
		String configFile = testUtils.getPackageInputDirectory()+"NoiseTest/config1.xml";

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		NoiseSpatialInfo noiseSpatialInfo = new NoiseSpatialInfo(scenario);
		
		noiseSpatialInfo.setActivityCoords();
		
		// test the resulting Map
		Assert.assertEquals("wrong number of activities per grid cell (0/0)", 1, noiseSpatialInfo.getZoneTuple2listOfActivityCoords().get(new Tuple<Integer, Integer>(0 , 0)).size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong number of activities per grid cell (0/1)", 2, noiseSpatialInfo.getZoneTuple2listOfActivityCoords().get(new Tuple<Integer, Integer>(0 , 1)).size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong number of activities per grid cell (1/0)", 1, noiseSpatialInfo.getZoneTuple2listOfActivityCoords().get(new Tuple<Integer, Integer>(1 , 0)).size(), MatsimTestUtils.EPSILON);

		noiseSpatialInfo.setReceiverPoints();
		
		// test the grid of receiver points
		Assert.assertEquals("wrong number of receiver points", 16, noiseSpatialInfo.getReceiverPointId2Coord().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong coord for receiver point Id '10'", new CoordImpl(500, 100).toString(), noiseSpatialInfo.getReceiverPointId2Coord().get(new IdImpl(10)).toString());
		
		// test the allocation of receiver point to grid cell
		Assert.assertEquals("wrong number of grid cells for which receiver points are stored", 9, noiseSpatialInfo.getZoneTuple2listOfReceiverPointIds().size(), MatsimTestUtils.EPSILON);
		
		noiseSpatialInfo.setActivityCoord2NearestReceiverPointId();
		
		// test the allocation of activity coordinates to the nearest receiver point
		Assert.assertEquals("wrong nearest receiver point Id for coord 300/300 (x/y)", "5", noiseSpatialInfo.getActivityCoord2receiverPointId().get(new CoordImpl(300, 300)).toString());
		Assert.assertEquals("wrong nearest receiver point Id for coord 150/150 (x/y)", "9", noiseSpatialInfo.getActivityCoord2receiverPointId().get(new CoordImpl(150, 150)).toString());
		Assert.assertEquals("wrong nearest receiver point Id for coord 100/100 (x/y)", "8", noiseSpatialInfo.getActivityCoord2receiverPointId().get(new CoordImpl(100, 100)).toString());
		Assert.assertEquals("wrong nearest receiver point Id for coord 500/500 (x/y)", "2", noiseSpatialInfo.getActivityCoord2receiverPointId().get(new CoordImpl(500, 500)).toString());
		
		// test the allocation of receiver point coordinates to receiver point Id
		Assert.assertEquals("wrong nearest receiver point Id for coord 500/100 (x/y)", "10", noiseSpatialInfo.getCoord2receiverPointId().get(new CoordImpl(500, 100)).toString());
			
		noiseSpatialInfo.setRelevantLinkIds();
		
		// test the allocation of relevant links to the receiver point
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", new IdImpl("link1"), noiseSpatialInfo.getReceiverPointId2relevantLinkIds().get(new IdImpl("15")).get(0));
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", new IdImpl("linkA1"), noiseSpatialInfo.getReceiverPointId2relevantLinkIds().get(new IdImpl("15")).get(1));
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", new IdImpl("linkB1"), noiseSpatialInfo.getReceiverPointId2relevantLinkIds().get(new IdImpl("15")).get(2));
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", 3, noiseSpatialInfo.getReceiverPointId2relevantLinkIds().get(new IdImpl("15")).size());
		
		// test the distance correction term
		Assert.assertEquals("wrong distance between receiver point Id '8' and link Id '1'", 8.749854822140838, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermDs().get(new IdImpl("8")).get(new IdImpl("link0")), MatsimTestUtils.EPSILON);		
		
		// test the angle correction term
		Assert.assertEquals("wrong immission angle correction for receiver point 14 and link1", -0.8913405699036482, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(new IdImpl("14")).get(new IdImpl("link1")), MatsimTestUtils.EPSILON);		

		double angle0 = 180.;
		double immissionCorrection0 = 10 * Math.log10((angle0) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 12 and link5", immissionCorrection0, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(new IdImpl("12")).get(new IdImpl("link5")), MatsimTestUtils.EPSILON);		
		
		double angle = 65.39222026185993;
		double immissionCorrection = 10 * Math.log10((angle) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 9 and link5", immissionCorrection, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(new IdImpl("9")).get(new IdImpl("link5")), MatsimTestUtils.EPSILON);		

		// for a visualization of the receiver point 8 and the relevant links, see network file
		double angle2 = 0.0000000001;
		double immissionCorrection2 = 10 * Math.log10((angle2) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 8 and link5", immissionCorrection2, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(new IdImpl("8")).get(new IdImpl("link5")), MatsimTestUtils.EPSILON);
		
		double angle3 = 84.28940686250034;
		double immissionCorrection3 = 10 * Math.log10((angle3) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 8 and link1", immissionCorrection3, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(new IdImpl("8")).get(new IdImpl("link1")), MatsimTestUtils.EPSILON);
	
		double angle4 = 180;
		double immissionCorrection4 = 10 * Math.log10((angle4) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 8 and link0", immissionCorrection4, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(new IdImpl("8")).get(new IdImpl("link0")), MatsimTestUtils.EPSILON);
	}
	
	// tests the noise emission and immission
	@Test
	public final void test2(){
		
		String configFile = testUtils.getPackageInputDirectory()+"NoiseTest/config2.xml";

		Controler controler = new Controler(configFile);
		NoiseControlerListener noiseControlerListener = new NoiseControlerListener();
		controler.addControlerListener(noiseControlerListener);
		
		controler.setOverwriteFiles(true);
		controler.run();
		
		// +++++++++++++++++++++++++++++++++++++++++ emission ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		
		Id linkA2Id = new IdImpl("linkA2");
		double timeInterval1011 = 11 * 3600.;
				
		// test the number of linkEnterEvents for linkA2 in time interval 10-11
		List<LinkEnterEvent> linkEnterEvents = noiseControlerListener.getNoiseEmissionHandler().getLinkId2timeInterval2linkEnterEvents().get(linkA2Id).get(timeInterval1011);
		Assert.assertEquals("wrong number of linkEnterEvents on linkA2 at time interval 10-11", 2, linkEnterEvents.size());
		
		// test the noise emission for linkA2 in time interval 10-11
		// first calculate the mittelungspegel
		double hdvShare = 0.;
		double carsAndHdv = 2;
		double pInPercentagePoints = hdvShare * 100.;	
		double mittelungspegel = 37.3 + 10 * Math.log10(carsAndHdv * (1 + (0.082 * pInPercentagePoints)));
		// then calculate the speed correction term Dv
		double vCar = (controler.getScenario().getNetwork().getLinks().get(linkA2Id).getFreespeed()) * 3.6;
		double vHdv = vCar;
		double lCar = 27.7 + (10.0 * Math.log10(1.0 + Math.pow(0.02 * vCar, 3.0)));
		double lHdv = 23.1 + (12.5 * Math.log10(vHdv));
		double d = lHdv - lCar; 
		double geschwindigkeitskorrekturDv = lCar - 37.3 + 10 * Math.log10((100.0 + (Math.pow(10.0, (0.1 * d)) - 1) * pInPercentagePoints ) / (100 + 8.23 * pInPercentagePoints));
		// and then calculate the corrected emission level
		double emission = mittelungspegel + geschwindigkeitskorrekturDv;
		Assert.assertEquals("wrong emission level on linkA2 in timeInterval 10-11", emission, noiseControlerListener.getNoiseEmissionHandler().getLinkId2timeInterval2noiseEmission().get(linkA2Id).get(timeInterval1011), MatsimTestUtils.EPSILON);
				
		// +++++++++++++++++++++++++++++++++++++++++ person tracker ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		
		// test the right allocation to the nearest receiver point
		// homeCoord: x="500.0" y="5.0" --> nearest receiver point: 0 (400/105)
		// workCoord: x="4500" y="5.0" --> nearest receiver point: 16 (4400/105)
		Assert.assertEquals("wrong allocation of home activity location to receiver point", 2, noiseControlerListener.getPersonActivityTracker().getReceiverPointId2ListOfHomeAgents().size());
		Assert.assertEquals("wrong allocation of home activity location to receiver point", 2, noiseControlerListener.getPersonActivityTracker().getReceiverPointId2ListOfHomeAgents().get(new IdImpl("0")).size());
		
		// test the right tracking of person, activity number, start and end time
		Assert.assertEquals("wrong activity performing time", 0., noiseControlerListener.getPersonActivityTracker().getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(new IdImpl("0")).get(new IdImpl("person_car_test2")).get(1).getFirst(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong activity performing time", Time.parseTime("10:17:54") , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(new IdImpl("0")).get(new IdImpl("person_car_test2")).get(1).getSecond(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("wrong activity performing time", 37436. , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(new IdImpl("16")).get(new IdImpl("person_car_test2")).get(2).getFirst(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong activity performing time", null , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(new IdImpl("16")).get(new IdImpl("person_car_test2")).get(1));

		// between 11 and 12 there is no agent at receiver point 0
		Assert.assertEquals("wrong agent units per receiver point and time interval", 0. , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2affectedAgentUnits().get(new IdImpl("0")).get(12 * 3600.), MatsimTestUtils.EPSILON);
		// between 9 and 10 two agents are at receiver point 0
		Assert.assertEquals("wrong agent units per receiver point and time interval", 2. , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2affectedAgentUnits().get(new IdImpl("0")).get(10 * 3600.), MatsimTestUtils.EPSILON);
		// between 5 and 6 there is one agent at receiver point 16
		Assert.assertEquals("wrong agent units per receiver point and time interval", 1. , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2affectedAgentUnits().get(new IdImpl("16")).get(6 * 3600.), MatsimTestUtils.EPSILON);
		// between 10 and 11 both testAgent1 and testAgent2 are partly at receiver point 0 and partly at receiver point 16, testAgent1 is over the entire time interval at receiver point 16
		double partTimeTestAgent3 = 0.;
		double partTimeTestAgent2 = (Time.parseTime("10:17:54") - (10. * 3600)) / 3600.;
		double partTimeTestAgent1 = (Time.parseTime("10:10:53") - (10. * 3600)) / 3600.;
		double affectedAgentUnits = partTimeTestAgent2 + partTimeTestAgent1 + partTimeTestAgent3;
		Assert.assertEquals("wrong agent units per receiver point and time interval", affectedAgentUnits, noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2affectedAgentUnits().get(new IdImpl("0")).get(11 * 3600.), MatsimTestUtils.EPSILON);

		Assert.assertEquals("wrong affected agent units and activity type per receiver point, time interval and person", partTimeTestAgent2, noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType().get(new IdImpl("0")).get(11 * 3600.).get(new IdImpl("person_car_test2")).get(1).getFirst(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong affected agent units and activity type per receiver point, time interval and person", "home", noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType().get(new IdImpl("0")).get(11 * 3600.).get(new IdImpl("person_car_test2")).get(1).getSecond());
		Assert.assertEquals("wrong affected agent units and activity type per receiver point, time interval and person", 1., noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType().get(new IdImpl("16")).get(12 * 3600.).get(new IdImpl("person_car_test2")).get(2).getFirst(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong affected agent units and activity type per receiver point, time interval and person", "work", noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType().get(new IdImpl("16")).get(12 * 3600.).get(new IdImpl("person_car_test2")).get(2).getSecond());

		// ++++++++++++++++++++++++++++++++++++++++++ immission +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		
		// testing the noise immission at receiver point 16 where testAgent3 performs the activity "home"
		// relevant links for receiver point 16 (4400/105): linkA5, link2, linkB5
		// linkA5 has a speed of 1000 m/s, hence the noise emission is very high, the other links are not relevant
		//
		//
		//			(nodeA4: 4000/1000)
		//				|
		//				|
		//				|
		//			  linkA5
		// 				|		(receiver point 16: 4400/105)
		//				|
		//				V
		//			(node3: 4000/0)
		//
		//
		//
		

		// before and after time interval '10 - 11' the noise immission should be zero.
		Assert.assertEquals("wrong immission at receiver point 16", 0., noiseControlerListener.getNoiseImmission().getReceiverPointId2timeInterval2noiseImmission().get(new IdImpl("16")).get(10 * 3600.), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong immission at receiver point 16", 0., noiseControlerListener.getNoiseImmission().getReceiverPointId2timeInterval2noiseImmission().get(new IdImpl("16")).get(12 * 3600.), MatsimTestUtils.EPSILON);
		
		// test the emission
		double emissionLinkA5 = noiseControlerListener.getNoiseEmissionHandler().getLinkId2timeInterval2noiseEmission().get(new IdImpl("linkA5")).get(11 * 3600.);
		Assert.assertEquals("wrong emission level on linkA5 in timeInterval 10-11", 86.43028648510975, emissionLinkA5, MatsimTestUtils.EPSILON);

		// test the distance correction term
		double lotA52receiverPoint16 = 400.;
		double correctionTermDs = 15.8 - (10 * Math.log10(lotA52receiverPoint16)) - (0.0142*(Math.pow(lotA52receiverPoint16,0.9)));
//		Assert.assertEquals("wrong distance between receiver point Id '16' and link Id 'linkA5'", correctionTermDs, noiseControlerListener.getSpatialInfo().getReceiverPointId2relevantLinkId2correctionTermDs().get(new IdImpl("16")).get(new IdImpl("linkA5")), MatsimTestUtils.EPSILON);		
		Assert.assertEquals("wrong distance between receiver point Id '16' and link Id 'linkA5'", correctionTermDs, noiseControlerListener.getSpatialInfo().getReceiverPointId2relevantLinkId2correctionTermDs().get(new IdImpl("16")).get(new IdImpl("linkA5")), 0.000001);		

		// now test the relevant time interval '10 - 11', compute the noise immission based on the noise emission on linkA5
//		Assert.assertEquals("wrong immission at receiver point 16", 0., noiseControlerListener.getNoiseImmission().getReceiverPointId2timeInterval2noiseImmission().get(new IdImpl("16")).get(11 * 3600.), MatsimTestUtils.EPSILON);
				
		
//		System.out.println(noiseControlerListener.getNoiseImmission().getReceiverPointId2timeInterval2noiseImmission());
		
		
		
		
		
		
		
		noiseControlerListener.getNoiseImmission().getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission();

		noiseControlerListener.getNoiseImmission().getNoiseEvents();
		noiseControlerListener.getNoiseImmission().getNoiseEventsAffected();
		
	 }
	
	
}
