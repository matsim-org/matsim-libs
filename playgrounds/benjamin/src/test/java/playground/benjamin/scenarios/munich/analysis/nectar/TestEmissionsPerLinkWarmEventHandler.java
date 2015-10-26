/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerLinkWarmEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.munich.analysis.nectar;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

public class TestEmissionsPerLinkWarmEventHandler {

	/**
	 * test for playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkWarmEventHandler
	 * 
	 * @author Julia
	 * 
	 * initialize some events and handler them
	 * assert that the respective values are set in the two maps
	 * WarmEmissionsPerLinkAndTimeInterval and Time2LinkLeaveCount
	 * 
	 * create one event, handle it and check all values
	 * make sure nothing unwanted is in both maps
	 * 
	 * create and handle another three events
	 * check that summing up the values works as expected
	 * 
	 * 
	 */

	boolean excep = false;		
	Double coValue = 2000., c2Value = 220. , fcValue = 22.2, hcValue = 2.222, nmValue= 0.22222, 
				n2Value = 3000., nxValue = 330., pmValue = 33.3, soValue = 3.333;
	
	double simulationEndTime = 200., simulationStartTime=0.0;
	int noOfTimeBins = 30; //-> interval ends should be 6.666, 13.333, 20.0, 26.666, ... , 200.0
	
	WarmEmissionEvent event1, event2, event3, event4, event5, event6, event7;
	
	@Test
	public void handleEventTest(){
		
		EmissionsPerLinkWarmEventHandler handler = new EmissionsPerLinkWarmEventHandler(simulationEndTime, noOfTimeBins);
		
		// initialize event 1
		// time of the event is 5.0 -> its interval end should be 6.666...
		// all possible values for pollutants set
		double timeOfEvent1 = 5.0;
		double expectedEndOfTimeInterval = getIntervalEnd(timeOfEvent1); // end of time interval should be 6.666...
		Id<Link> linkId = Id.create("link1", Link.class);
		Id<Vehicle> vehicleId = Id.create("veh 1", Vehicle.class);
		Map<WarmPollutant, Double> warmEmissions1 = new HashMap<WarmPollutant, Double>();
		event1 = setUpEvent(timeOfEvent1, linkId, vehicleId, warmEmissions1);
		
		handler.handleEvent(event1);
		
		// get end of time interval as calculated by handler
		double calulatedEndOfTimeInterval = -1.;
		for(double endOfInterval : handler.getTime2linkIdLeaveCount().keySet()){
			if (Math.abs(endOfInterval-expectedEndOfTimeInterval)<simulationEndTime/noOfTimeBins){
				calulatedEndOfTimeInterval = endOfInterval;
			}
		}
		
		Assert.assertFalse(calulatedEndOfTimeInterval==-1.);
		
		// test event 1 
		Assert.assertEquals(1.0, handler.getTime2linkIdLeaveCount().get(calulatedEndOfTimeInterval).get(linkId));
		Assert.assertEquals(coValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(calulatedEndOfTimeInterval).get(linkId).get(WarmPollutant.CO), MatsimTestUtils.EPSILON);
		Assert.assertEquals(c2Value, handler.getWarmEmissionsPerLinkAndTimeInterval().get(calulatedEndOfTimeInterval).get(linkId).get(WarmPollutant.CO2_TOTAL), MatsimTestUtils.EPSILON);
		Assert.assertEquals(fcValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(calulatedEndOfTimeInterval).get(linkId).get(WarmPollutant.FC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(hcValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(calulatedEndOfTimeInterval).get(linkId).get(WarmPollutant.HC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(nmValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(calulatedEndOfTimeInterval).get(linkId).get(WarmPollutant.NMHC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(n2Value, handler.getWarmEmissionsPerLinkAndTimeInterval().get(calulatedEndOfTimeInterval).get(linkId).get(WarmPollutant.NO2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(nxValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(calulatedEndOfTimeInterval).get(linkId).get(WarmPollutant.NOX), MatsimTestUtils.EPSILON);
		Assert.assertEquals(pmValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(calulatedEndOfTimeInterval).get(linkId).get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		Assert.assertEquals(soValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(calulatedEndOfTimeInterval).get(linkId).get(WarmPollutant.SO2), MatsimTestUtils.EPSILON);
		
		// everything else should be zero or null or throw an exception
		// there is no entry for these link id, time and pollutant combinations -> throw exception
		try{
			handler.getWarmEmissionsPerLinkAndTimeInterval().get(0.0).get(linkId);
		}catch(NullPointerException e){
			excep = true;
		}
		Assert.assertTrue(excep); excep = false;
		
		try{
			handler.getWarmEmissionsPerLinkAndTimeInterval().get(expectedEndOfTimeInterval).get(Id.create("unused link", Link.class)).get(WarmPollutant.CO2_TOTAL);
		}catch(NullPointerException e){
			excep = true;
		}
		Assert.assertTrue(excep); excep = false;
		Assert.assertNull(handler.getWarmEmissionsPerLinkAndTimeInterval().get(expectedEndOfTimeInterval).get(linkId).get("some pollutant"));
		
		try{
			handler.getTime2linkIdLeaveCount().get(0.0).get(linkId);
		}catch(NullPointerException e){
			excep=true;
		}
		Assert.assertTrue(excep); excep = false;
		
		Assert.assertNull(handler.getTime2linkIdLeaveCount().get(expectedEndOfTimeInterval).get(Id.create("unused link", Link.class)));
		
		Map<WarmPollutant, Double> warmEmissions2= new HashMap<WarmPollutant, Double>();
		// initialize event 2,3,4 and handle them
		// event 2: time = 0.0 -> interval end should be 6.6666...
		// event belongs to the same time interval as event 1! 
		// and has the same emissions as event 1
		WarmEmissionEvent event2 = setUpEvent(simulationStartTime, linkId, vehicleId, warmEmissions2);
		handler.handleEvent(event2);
		Map<WarmPollutant, Double> warmEmissions3= new HashMap<WarmPollutant, Double>();
		// event 3: time = 200.0 -> interval should be 200.0 since this is the end of the simulation
		// has the same emissions as event 1
		WarmEmissionEvent event3 = setUpEvent(simulationEndTime, linkId, vehicleId, warmEmissions3);
		handler.handleEvent(event3);
		
		// event 4: time = 200.0/30*3.8=25.3333... -> interval should be 26.6666...
		double timeOfEvent4 = simulationEndTime/noOfTimeBins*3.8;
		double intervalEndOfEvent4 = getIntervalEnd(timeOfEvent4); 
		Map<WarmPollutant, Double> warmEmissions4= new HashMap<WarmPollutant, Double>();
		event4 = setUpEvent(timeOfEvent4, linkId, vehicleId, warmEmissions4);
		handler.handleEvent(event4);
		
		// check time2linkIdLeaveCount
		Assert.assertEquals(2.0, handler.getTime2linkIdLeaveCount().get(calulatedEndOfTimeInterval).get(linkId)); // 6.6666 - events 1 and 2
		Assert.assertEquals(1.0, handler.getTime2linkIdLeaveCount().get(simulationEndTime).get(linkId));  // 200.00 - event 3
		Assert.assertEquals(1.0, handler.getTime2linkIdLeaveCount().get(intervalEndOfEvent4).get(linkId)); // 26.6666 - event 4
		// handler.getTime2linkIdLeaveCount should contain nothing else
		Assert.assertEquals(3, handler.getTime2linkIdLeaveCount().size());
		
		// check warmEmissionsPerLinkAndTimeInterval
		Assert.assertEquals(2*coValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(calulatedEndOfTimeInterval).get(linkId).get(WarmPollutant.CO), MatsimTestUtils.EPSILON);
		Assert.assertEquals(coValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId).get(WarmPollutant.CO), MatsimTestUtils.EPSILON);
		Assert.assertEquals(coValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(intervalEndOfEvent4).get(linkId).get(WarmPollutant.CO), MatsimTestUtils.EPSILON);
		
		double timeOfEvent5 = simulationEndTime/noOfTimeBins*14.3;
		double intervalEndOfEvent5 = getIntervalEnd(timeOfEvent5);
		Id<Link> linkId2 = Id.create("link 2", Link.class);

		// event 5 has no map of emissions - should be handled like 0.0
		event5 = new WarmEmissionEvent(timeOfEvent5, linkId2, vehicleId, null);
		handler.handleEvent(event5);
		// check event 5
		for (WarmPollutant wp: WarmPollutant.values()){
			Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(intervalEndOfEvent5).get(linkId2).get(wp), MatsimTestUtils.EPSILON);
		}		
		Assert.assertEquals(1.0, handler.getTime2linkIdLeaveCount().get(intervalEndOfEvent5).get(linkId2));
		
		//event 6 contains come pollutants but not all
		
		Id<Link> linkId3 = Id.create("link 3", Link.class);
		Map<WarmPollutant, Double> warmEmissions6= new HashMap<WarmPollutant, Double>();
		event6 = setUpEvent(simulationEndTime, linkId3, vehicleId, warmEmissions6);
		warmEmissions6.remove(WarmPollutant.NMHC);
		warmEmissions6.remove(WarmPollutant.NO2);
		warmEmissions6.remove(WarmPollutant.NOX);
		handler.handleEvent(event6);
		// check event 6
		Assert.assertEquals(1.0, handler.getTime2linkIdLeaveCount().get(simulationEndTime).get(linkId3));
		Assert.assertEquals(coValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.CO), MatsimTestUtils.EPSILON);
		Assert.assertEquals(c2Value, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.CO2_TOTAL), MatsimTestUtils.EPSILON);
		Assert.assertEquals(fcValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.FC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(hcValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.HC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.NMHC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.NO2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.NOX), MatsimTestUtils.EPSILON);
		Assert.assertEquals(pmValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		Assert.assertEquals(soValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.SO2), MatsimTestUtils.EPSILON);
		
		// test the reset - maps should exist but be empty
		handler.reset(2);
		Assert.assertEquals(0, handler.getWarmEmissionsPerLinkAndTimeInterval().size());
		Assert.assertEquals(0, handler.getTime2linkIdLeaveCount().size());
		
		// handle one event again and check its values
		handler.handleEvent(event6);
		Assert.assertEquals(1.0, handler.getTime2linkIdLeaveCount().get(simulationEndTime).get(linkId3));
		Assert.assertEquals(coValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.CO), MatsimTestUtils.EPSILON);
		Assert.assertEquals(c2Value, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.CO2_TOTAL), MatsimTestUtils.EPSILON);
		Assert.assertEquals(fcValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.FC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(hcValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.HC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.NMHC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.NO2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.NOX), MatsimTestUtils.EPSILON);
		Assert.assertEquals(pmValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		Assert.assertEquals(soValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.SO2), MatsimTestUtils.EPSILON);
		

	
	}
	
	private double getIntervalEnd(double timeOfEvent) {
		double timebinsize = simulationEndTime/noOfTimeBins;
		double intervalEnd = -1.;
		// calculate the interval end intuitively to test the method used in the EmissionPerLinkWarmEventHandler
		for(int i=0; i<noOfTimeBins; i++){
			if(i*timebinsize<timeOfEvent && (i+1)*timebinsize>=timeOfEvent){
				intervalEnd = (i+1)* timebinsize;
				break;
			}
		}
		Assert.assertTrue(intervalEnd>-1.);
		return intervalEnd;
	}

	private WarmEmissionEvent setUpEvent(double timeOfEvent1, Id<Link> linkId, Id<Vehicle> vehicleId, Map<WarmPollutant, Double> warmEmissions) {
		//warmEmissions = new HashMap<WarmPollutant, Double>();
		warmEmissions.put(WarmPollutant.CO, new Double(coValue));
		warmEmissions.put(WarmPollutant.CO2_TOTAL, new Double(c2Value));
		warmEmissions.put(WarmPollutant.FC, new Double(fcValue));
		warmEmissions.put(WarmPollutant.HC, new Double(hcValue));
		warmEmissions.put(WarmPollutant.NMHC, new Double(nmValue));
		warmEmissions.put(WarmPollutant.NO2, new Double( n2Value));
		warmEmissions.put(WarmPollutant.NOX, new Double(nxValue));
		warmEmissions.put(WarmPollutant.PM, new Double(pmValue));
		warmEmissions.put(WarmPollutant.SO2, new Double(soValue));
		
		return new WarmEmissionEvent(timeOfEvent1, linkId, vehicleId, warmEmissions);
		
	}

}
