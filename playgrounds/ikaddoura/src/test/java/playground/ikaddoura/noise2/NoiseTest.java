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

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author ikaddoura , lkroeger
 *
 */

public class NoiseTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	// Tests the NoisSpatialInfo functionality separately for each function
	@Test
	public final void test1(){
		
		String configFile = testUtils.getPackageInputDirectory()+"NoiseTest/config1.xml";

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		NoiseSpatialInfo noiseSpatialInfo = new NoiseSpatialInfo(scenario);
		
		noiseSpatialInfo.setActivityCoords();
		
		// test the resulting Map
//		Assert.assertEquals("wrong number of activities per grid cell (0/0)", 1, noiseSpatialInfo.getZoneTuple2listOfActivityCoords().get(new Tuple<Integer, Integer>(0 , 0)).size(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("wrong number of activities per grid cell (0/1)", 2, noiseSpatialInfo.getZoneTuple2listOfActivityCoords().get(new Tuple<Integer, Integer>(0 , 1)).size(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("wrong number of activities per grid cell (1/0)", 1, noiseSpatialInfo.getZoneTuple2listOfActivityCoords().get(new Tuple<Integer, Integer>(1 , 0)).size(), MatsimTestUtils.EPSILON);

		noiseSpatialInfo.setReceiverPoints();
		
		// test the grid of receiver points
		Assert.assertEquals("wrong number of receiver points", 16, noiseSpatialInfo.getReceiverPointId2Coord().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong coord for receiver point Id '10'", new CoordImpl(500, 100).toString(), noiseSpatialInfo.getReceiverPointId2Coord().get(Id.create(10, ReceiverPoint.class)).toString());
		
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
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", Id.create("link1", Link.class), noiseSpatialInfo.getReceiverPointId2relevantLinkIds().get(Id.create("15", ReceiverPoint.class)).get(0));
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", Id.create("linkA1", Link.class), noiseSpatialInfo.getReceiverPointId2relevantLinkIds().get(Id.create("15", ReceiverPoint.class)).get(1));
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", Id.create("linkB1", Link.class), noiseSpatialInfo.getReceiverPointId2relevantLinkIds().get(Id.create("15", ReceiverPoint.class)).get(2));
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", 3, noiseSpatialInfo.getReceiverPointId2relevantLinkIds().get(Id.create("15", Link.class)).size());
		
		// test the distance correction term
		Assert.assertEquals("wrong distance between receiver point Id '8' and link Id '1'", 8.749854822140838, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermDs().get(Id.create("8", ReceiverPoint.class)).get(Id.create("link0", Link.class)), MatsimTestUtils.EPSILON);		
		
		// test the angle correction term
		Assert.assertEquals("wrong immission angle correction for receiver point 14 and link1", -0.8913405699036482, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(Id.create("14", ReceiverPoint.class)).get(Id.create("link1", Link.class)), MatsimTestUtils.EPSILON);		

		double angle0 = 180.;
		double immissionCorrection0 = 10 * Math.log10((angle0) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 12 and link5", immissionCorrection0, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(Id.create("12", ReceiverPoint.class)).get(Id.create("link5", Link.class)), MatsimTestUtils.EPSILON);		
		
		double angle = 65.39222026185993;
		double immissionCorrection = 10 * Math.log10((angle) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 9 and link5", immissionCorrection, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(Id.create("9", ReceiverPoint.class)).get(Id.create("link5", Link.class)), MatsimTestUtils.EPSILON);		

		// for a visualization of the receiver point 8 and the relevant links, see network file
		double angle2 = 0.0000000001;
		double immissionCorrection2 = 10 * Math.log10((angle2) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 8 and link5", immissionCorrection2, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(Id.create("8", ReceiverPoint.class)).get(Id.create("link5", Link.class)), MatsimTestUtils.EPSILON);
		
		double angle3 = 84.28940686250034;
		double immissionCorrection3 = 10 * Math.log10((angle3) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 8 and link1", immissionCorrection3, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(Id.create("8", ReceiverPoint.class)).get(Id.create("link1", Link.class)), MatsimTestUtils.EPSILON);
	
		double angle4 = 180;
		double immissionCorrection4 = 10 * Math.log10((angle4) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 8 and link0", immissionCorrection4, noiseSpatialInfo.getReceiverPointId2relevantLinkId2correctionTermAngle().get(Id.create("8", ReceiverPoint.class)).get(Id.create("link0", Link.class)), MatsimTestUtils.EPSILON);
	}
	
	// tests the noise emission, immission and exposures
	@Test
	public final void test2(){
		
		String configFile = testUtils.getPackageInputDirectory()+"NoiseTest/config2.xml";

		Controler controler = new Controler(configFile);
		NoiseCalculationOnline noiseControlerListener = new NoiseCalculationOnline();
		controler.addControlerListener(noiseControlerListener);
		
		controler.setOverwriteFiles(true);
		controler.run();
		
		// +++++++++++++++++++++++++++++++++++++++++ emission ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		
		Id<Link> linkA2Id = Id.create("linkA2", Link.class);
		double timeInterval1011 = 11 * 3600.;
				
		// test the number of linkEnterEvents for linkA2 in time interval 10-11
		List<Id<Vehicle>> linkEnterEvents = noiseControlerListener.getNoiseEmissionHandler().getLinkId2timeInterval2linkEnterVehicleIDs().get(linkA2Id).get(timeInterval1011);
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
		Assert.assertEquals("wrong allocation of home activity location to receiver point", 2, noiseControlerListener.getPersonActivityTracker().getReceiverPointId2ListOfHomeAgents().get(Id.create("0", ReceiverPoint.class)).size());
		
		// test the right tracking of person, activity number, start and end time
		Assert.assertEquals("wrong activity performing time", 0., noiseControlerListener.getPersonActivityTracker().getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(Id.create("0", ReceiverPoint.class)).get(Id.create("person_car_test2", Person.class)).get(1).getFirst(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong activity performing time", Time.parseTime("10:17:54") , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(Id.create("0", ReceiverPoint.class)).get(Id.create("person_car_test2", Person.class)).get(1).getSecond(), MatsimTestUtils.EPSILON);

		Assert.assertEquals("wrong activity performing time", 37375. , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(Id.create("16", ReceiverPoint.class)).get(Id.create("person_car_test2", Person.class)).get(2).getFirst(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("wrong activity performing time", 37436. , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(Id.create("16", ReceiverPoint.class)).get(Id.create("person_car_test2")).get(2).getFirst(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong activity performing time", null , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2personId2actNumber2activityStartAndActivityEnd().get(Id.create("16", ReceiverPoint.class)).get(Id.create("person_car_test2", Person.class)).get(1));

		// between 11 and 12 there is no agent at receiver point 0
		Assert.assertEquals("wrong agent units per receiver point and time interval", 0. , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2affectedAgentUnits().get(Id.create("0", ReceiverPoint.class)).get(12 * 3600.), MatsimTestUtils.EPSILON);
		// between 9 and 10 two agents are at receiver point 0
		Assert.assertEquals("wrong agent units per receiver point and time interval", 2. , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2affectedAgentUnits().get(Id.create("0", ReceiverPoint.class)).get(10 * 3600.), MatsimTestUtils.EPSILON);
		// between 5 and 6 there is one agent at receiver point 16
		Assert.assertEquals("wrong agent units per receiver point and time interval", 1. , noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2affectedAgentUnits().get(Id.create("16", ReceiverPoint.class)).get(6 * 3600.), MatsimTestUtils.EPSILON);
		// between 10 and 11 both testAgent1 and testAgent2 are partly at receiver point 0 and partly at receiver point 16, testAgent1 is over the entire time interval at receiver point 16
		double partTimeTestAgent3 = 0.;
		double partTimeTestAgent2 = (Time.parseTime("10:17:54") - (10. * 3600)) / 3600.;
		double partTimeTestAgent1 = (Time.parseTime("10:10:53") - (10. * 3600)) / 3600.;
		double affectedAgentUnits = partTimeTestAgent2 + partTimeTestAgent1 + partTimeTestAgent3;
		Assert.assertEquals("wrong agent units per receiver point and time interval", affectedAgentUnits, noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2affectedAgentUnits().get(Id.create("0", ReceiverPoint.class)).get(11 * 3600.), MatsimTestUtils.EPSILON);

		Assert.assertEquals("wrong affected agent units and activity type per receiver point, time interval and person", partTimeTestAgent2, noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType().get(Id.create("0", ReceiverPoint.class)).get(11 * 3600.).get(Id.create("person_car_test2", Person.class)).get(1).getFirst(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong affected agent units and activity type per receiver point, time interval and person", "home", noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType().get(Id.create("0", ReceiverPoint.class)).get(11 * 3600.).get(Id.create("person_car_test2", Person.class)).get(1).getSecond());
		Assert.assertEquals("wrong affected agent units and activity type per receiver point, time interval and person", 1., noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType().get(Id.create("16", ReceiverPoint.class)).get(12 * 3600.).get(Id.create("person_car_test2", Person.class)).get(2).getFirst(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong affected agent units and activity type per receiver point, time interval and person", "work", noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType().get(Id.create("16", ReceiverPoint.class)).get(12 * 3600.).get(Id.create("person_car_test2", Person.class)).get(2).getSecond());

		// ++++++++++++++++++++++++++++++++++++++++++ immission +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		
		// testing the noise immission at receiver point 16 where testAgent3 performs the activity "home"
		// relevant links for receiver point 16 (4400/105): linkA5, link2, linkB5
		// linkA5 has a speed of 1000 m/s, hence the noise emission is very high,
		// link2 has a speed of 100 m/s, hence, also here the noise emission is very high,
		// linkB2 doesn't play a role.
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

		// before and after time interval '10 - 11' the noise immission should be zero.
		Assert.assertEquals("wrong immission at receiver point 16", 0., noiseControlerListener.getNoiseImmission().getReceiverPointId2timeInterval2noiseImmission().get(Id.create("16", ReceiverPoint.class)).get(10 * 3600.), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong immission at receiver point 16", 0., noiseControlerListener.getNoiseImmission().getReceiverPointId2timeInterval2noiseImmission().get(Id.create("16", ReceiverPoint.class)).get(12 * 3600.), MatsimTestUtils.EPSILON);
		
		// test the emission
		double emissionLinkA5 = noiseControlerListener.getNoiseEmissionHandler().getLinkId2timeInterval2noiseEmission().get(Id.create("linkA5", Link.class)).get(11 * 3600.);
		Assert.assertEquals("wrong emission level on linkA5 in timeInterval 10-11", 86.43028648510975, emissionLinkA5, MatsimTestUtils.EPSILON);

		// test the distance correction term
		double lotA52receiverPoint16 = 400.;
		double correctionTermDs = 15.8 - (10 * Math.log10(lotA52receiverPoint16)) - (0.0142*(Math.pow(lotA52receiverPoint16,0.9)));
		Assert.assertEquals("wrong distance between receiver point Id '16' and link Id 'linkA5'", correctionTermDs, noiseControlerListener.getSpatialInfo().getReceiverPointId2relevantLinkId2correctionTermDs().get(Id.create("16", ReceiverPoint.class)).get(Id.create("linkA5", Link.class)), 0.0000001); 
		
		double correctionTermAngle = 10 * Math.log10((80.6271289) / (180));
		Assert.assertEquals("wrong angle correction for receiver point Id '16' and link Id 'linkA5'", correctionTermAngle, noiseControlerListener.getSpatialInfo().getReceiverPointId2relevantLinkId2correctionTermAngle().get(Id.create("16", ReceiverPoint.class)).get(Id.create("linkA5", Link.class)), 0.0000001);

		// now test for the relevant time interval '10 - 11' the isolated noise immission that results from each link
		double immissionFromLinkA5 = emissionLinkA5 + correctionTermDs + correctionTermAngle;		
		Assert.assertEquals("wrong immission at receiver point 16", immissionFromLinkA5, noiseControlerListener.getNoiseImmission().getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission().get(Id.create("16", ReceiverPoint.class)).get(11 * 3600.).get(Id.create("linkA5", Link.class)), 0.0000001);
		Assert.assertEquals("wrong immission at receiver point 16", 0., noiseControlerListener.getNoiseImmission().getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission().get(Id.create("16", ReceiverPoint.class)).get(11 * 3600.).get(Id.create("linkB5", Link.class)), MatsimTestUtils.EPSILON);

		// now test for the relevant time interval '10 - 11' the noise immission resulting from all (relevant) links
		double immissionRP16 = 10 * Math.log10((
				Math.pow(10,(0.1 * noiseControlerListener.getNoiseImmission().getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission().get(Id.create("16", ReceiverPoint.class)).get(11 * 3600.).get(Id.create("linkA5", Link.class))))
						+ (Math.pow(10,(0.1 * noiseControlerListener.getNoiseImmission().getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission().get(Id.create("16", ReceiverPoint.class)).get(11 * 3600.).get(Id.create("link2", Link.class)))))
				));		
		Assert.assertEquals("wrong immission at receiver point 16", immissionRP16, noiseControlerListener.getNoiseImmission().getReceiverPointId2timeInterval2noiseImmission().get(Id.create("16", ReceiverPoint.class)).get(11 * 3600.), MatsimTestUtils.EPSILON);
			
		// ++++++++++++++++++++++++++++++++++++++++++ exposures ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		
		// test the translation of immission into total damage costs for all affected agents per receiver point
		double lautheitsgewicht = Math.pow(2.0 , 0.1 * (immissionRP16 - 50));
		double laermEinwohnerGleichwert = lautheitsgewicht * noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2affectedAgentUnits().get(Id.create("16", ReceiverPoint.class)).get(11 * 3600.);
		double annualCostRate = (85.0/(1.95583)) * (Math.pow(1.02, (2014-1995)));
		double damageCostsRP16 = (annualCostRate * laermEinwohnerGleichwert/(365))*(1.0/24.0); // = 0.0664164095284536
		Assert.assertEquals("wrong damage costs", damageCostsRP16, noiseControlerListener.getNoiseDamageCosts().getReceiverPointId2timeInterval2damageCost().get(Id.create("16", ReceiverPoint.class)).get(11 * 3600.), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong damage costs", 0., noiseControlerListener.getNoiseDamageCosts().getReceiverPointId2timeInterval2damageCost().get(Id.create("0", ReceiverPoint.class)).get(11 * 3600.), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong damage costs", 0., noiseControlerListener.getNoiseDamageCosts().getReceiverPointId2timeInterval2damageCost().get(Id.create("16", ReceiverPoint.class)).get(12 * 3600.), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong damage costs", 0., noiseControlerListener.getNoiseDamageCosts().getReceiverPointId2timeInterval2damageCost().get(Id.create("16", ReceiverPoint.class)).get(10 * 3600.), MatsimTestUtils.EPSILON);
		
		double laermEinwohnerGleichwertAffectedAgentUnit = lautheitsgewicht * 1.;
		double damageCostsPerAffectedAgentUnitRP16 = (annualCostRate * laermEinwohnerGleichwertAffectedAgentUnit/(365))*(1.0/24.0);		
		Assert.assertEquals("wrong damage cost per affected agent unit", damageCostsPerAffectedAgentUnitRP16, noiseControlerListener.getNoiseDamageCosts().getReceiverPointId2timeInterval2damageCostPerAffectedAgentUnit().get(Id.create("16", ReceiverPoint.class)).get(11 * 3600.), MatsimTestUtils.EPSILON);
	
		// test the right allocation of total damage cost per receiver point to affected person
		double allocatedExposuresAffectedPersons = 0.;
		for (NoiseEventAffected event : noiseControlerListener.getNoiseDamageCosts().getNoiseEventsAffected()) {
			
			if (event.getrReceiverPointId().toString().equals("16")) {
				allocatedExposuresAffectedPersons = allocatedExposuresAffectedPersons + event.getAmount();

				if (event.getTime() == 11. * 3600. && event.getAffectedAgentId().toString().equals("person_car_test3")) {
					// this agent is affected the entire time interval
					Assert.assertEquals("wrong noise exposure allocation to affected agents", damageCostsPerAffectedAgentUnitRP16 * 1., event.getAmount(), MatsimTestUtils.EPSILON);
				}
				
				if (event.getTime() == 11. * 3600. && event.getAffectedAgentId().toString().equals("person_car_test1")) {
					// this agent is not affected for the duration of the entire time interval
					Assert.assertEquals("wrong noise exposure allocation to affected agents", damageCostsPerAffectedAgentUnitRP16 * noiseControlerListener.getPersonActivityTracker().getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType().get(Id.create("16", ReceiverPoint.class)).get(11 * 3600.).get(Id.create("person_car_test1", Person.class)).get(2).getFirst(), event.getAmount(), MatsimTestUtils.EPSILON);
				}
			}
		}
		Assert.assertEquals("difference between total damage costs (per receiver point) and sum of allocated damage (per person)", damageCostsRP16, allocatedExposuresAffectedPersons, MatsimTestUtils.EPSILON);

		// test the right allocation of total damage cost per receiver point to the relevant links
		// linkB5 should be zero
		Assert.assertEquals("wrong allocation of total damage cost at the receiver point to links", 0., noiseControlerListener.getNoiseDamageCosts().getLinkId2timeInterval2damageCost().get(Id.create("linkB5", Link.class)).get(11 * 3600.), MatsimTestUtils.EPSILON);
		// linkA5
		Assert.assertEquals("wrong allocation of total damage cost at the receiver point to links", Math.pow(((Math.pow(10, (0.05 * immissionFromLinkA5))) / (Math.pow(10, (0.05 * immissionRP16)))), 2)  * damageCostsRP16, noiseControlerListener.getNoiseDamageCosts().getLinkId2timeInterval2damageCost().get(Id.create("linkA5", Link.class)).get(11 * 3600.), MatsimTestUtils.EPSILON);
		// link2
		Assert.assertEquals("wrong allocation of total damage cost at the receiver point to links", Math.pow(((Math.pow(10, (0.05 * noiseControlerListener.getNoiseImmission().getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission().get(Id.create("16", ReceiverPoint.class)).get(11 * 3600.).get(Id.create("link2", Link.class))))) / (Math.pow(10, (0.05 * immissionRP16)))), 2)  * damageCostsRP16, noiseControlerListener.getNoiseDamageCosts().getLinkId2timeInterval2damageCost().get(Id.create("link2", Link.class)).get(11 * 3600.), MatsimTestUtils.EPSILON);

		double allocatedExposuresLinks = noiseControlerListener.getNoiseDamageCosts().getLinkId2timeInterval2damageCost().get(Id.create("linkB5", Link.class)).get(11 * 3600.)
				+ noiseControlerListener.getNoiseDamageCosts().getLinkId2timeInterval2damageCost().get(Id.create("linkA5", Link.class)).get(11 * 3600.)
				+ noiseControlerListener.getNoiseDamageCosts().getLinkId2timeInterval2damageCost().get(Id.create("link2", Link.class)).get(11 * 3600.);
		Assert.assertEquals("wrong noise exposure allocation to links", allocatedExposuresLinks, damageCostsRP16, MatsimTestUtils.EPSILON);
	
		// test the right allocation of link damage costs to vehicle types
		// here there are no hdv, that is, the damage costs are allocated among all cars, here agent 1 and agent 2
		Assert.assertEquals("wrong cost per car for the given link and time interval", noiseControlerListener.getNoiseDamageCosts().getLinkId2timeInterval2damageCost().get(Id.create("linkA5", Link.class)).get(11 * 3600.) / 2., noiseControlerListener.getNoiseDamageCosts().getLinkId2timeInterval2damageCostPerCar().get(Id.create("linkA5", Link.class)).get(11 * 3600.), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong cost per hdv for the given link and time interval", 0., noiseControlerListener.getNoiseDamageCosts().getLinkId2timeInterval2damageCostPerHdvVehicle().get(Id.create("linkA5", Link.class)).get(11 * 3600.), MatsimTestUtils.EPSILON);
		
		for (NoiseEventCaused event : noiseControlerListener.getNoiseDamageCosts().getNoiseEventsCaused()){
			if (event.getTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("linkA5", Link.class))) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", noiseControlerListener.getNoiseDamageCosts().getLinkId2timeInterval2damageCost().get(Id.create("linkA5", Link.class)).get(11 * 3600.) / 2., event.getAmount(), MatsimTestUtils.EPSILON);
			}
		}		
		
	 }
}
