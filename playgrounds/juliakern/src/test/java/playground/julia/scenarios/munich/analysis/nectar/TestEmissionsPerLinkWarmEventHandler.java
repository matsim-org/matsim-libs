package playground.julia.scenarios.munich.analysis.nectar;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.julia.emissions.EmissionsPerLinkWarmEventHandler;
import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventImpl;
import playground.vsp.emissions.types.WarmPollutant;

public class TestEmissionsPerLinkWarmEventHandler {

	/**
	 * @param args
	 */

	boolean excep = false;		
	Double coValue = 2000., c2Value = 220. , fcValue = 22.2, hcValue = 2.222, nmValue= 0.22222, 
				n2Value = 3000., nxValue = 330., pmValue = 33.3, soValue = 3.333;
	
	@Test
	public void handleEventTest(){
		
		double simulationEndTime = 200., simulationStartTime=0.0;
		int noOfTimeBins = 30; // 6.666, 13.333, 20.0, 26.666, ...
		EmissionsPerLinkWarmEventHandler handler = new EmissionsPerLinkWarmEventHandler(simulationEndTime, noOfTimeBins);
		
		// initialize event 1
		// time inside one time interval, all possible values for pollutants set
		double time = 5.0;
		double expectedEndOfTimeInterval = simulationEndTime/noOfTimeBins*1; // end of time interval should be 6.666...
		Id linkId = new IdImpl("link1");
		Id vehicleId = new IdImpl("veh 1");

		Map<WarmPollutant, Double> warmEmissions = new HashMap<WarmPollutant, Double>();
		warmEmissions.put(WarmPollutant.CO, coValue);
		warmEmissions.put(WarmPollutant.CO2_TOTAL, c2Value);
		warmEmissions.put(WarmPollutant.FC, fcValue);
		warmEmissions.put(WarmPollutant.HC, hcValue);
		warmEmissions.put(WarmPollutant.NMHC, nmValue);
		warmEmissions.put(WarmPollutant.NO2, n2Value);
		warmEmissions.put(WarmPollutant.NOX, nxValue);
		warmEmissions.put(WarmPollutant.PM, pmValue);
		warmEmissions.put(WarmPollutant.SO2, soValue);
		
		WarmEmissionEvent event1 = new WarmEmissionEventImpl(time, linkId, vehicleId, warmEmissions);
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
		// es gibt keinen Eintrag unter dieser LinkId .... das soll vermutlich auch so sein. 
		// ansonsten muesste fuer jedes Zeitintervall fuer jeden link eine leere map hinterlegt werden
		try{
			handler.getWarmEmissionsPerLinkAndTimeInterval().get(0.0).get(linkId);
		}catch(NullPointerException e){
			excep = true;
		}
		Assert.assertTrue(excep); excep = false;
		
		try{
			handler.getWarmEmissionsPerLinkAndTimeInterval().get(expectedEndOfTimeInterval).get(new IdImpl("unused link")).get(WarmPollutant.CO2_TOTAL);
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
		
		Assert.assertNull(handler.getTime2linkIdLeaveCount().get(expectedEndOfTimeInterval).get(new IdImpl("unused link")));
		
		// initialize event 2,3,4 and handle them
		// event belongs to the same time interval as event 1! 
		
		// event 2: time = 0.0 -> interval should be 6.6666...
		WarmEmissionEvent event2 = new WarmEmissionEventImpl(simulationStartTime, linkId, vehicleId, warmEmissions);
		handler.handleEvent(event2);
		// event 3: time = 200.0 -> interval should be 200.0 since this is the end of the simulation
		WarmEmissionEvent event3 = new WarmEmissionEventImpl(simulationEndTime, linkId, vehicleId, warmEmissions);
		handler.handleEvent(event3);
		
		// event 4: time = 200.0/30*3.8=25.3333... -> interval should be 26.6666...
		double timeOfEvent4 = simulationEndTime/noOfTimeBins*3.8;
		double intervalEndOfEvent4 = -1.0;
		double timebinsize = simulationEndTime/noOfTimeBins;
		for(int i=0; i<noOfTimeBins; i++){
			if(i*timebinsize<timeOfEvent4 && (i+1)*timebinsize>=timeOfEvent4){
				intervalEndOfEvent4 = (i+1)* timebinsize;
				break;
			}
		}
		
		Assert.assertTrue(intervalEndOfEvent4>-1.);
		WarmEmissionEvent event4 = new WarmEmissionEventImpl(timeOfEvent4, linkId, vehicleId, warmEmissions);
		
		
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
		
		
		// ins richtige intervall?
		//ins falsche intervall?
		
		double timeOfEvent5 = simulationEndTime/noOfTimeBins*14.3;
		double intervalEndOfEvent5 = 100.0;
		Id link2Id = new IdImpl("link 2");

		WarmEmissionEvent event5 = new WarmEmissionEventImpl(timeOfEvent5, link2Id, vehicleId, null);
		handler.handleEvent(event5);
		Assert.assertEquals(1.0, handler.getTime2linkIdLeaveCount().get(intervalEndOfEvent5).get(link2Id));
		Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(intervalEndOfEvent5).get(link2Id).get(WarmPollutant.NMHC));
		
		//event 6 contains come pollutants but not all
		warmEmissions.remove(WarmPollutant.NMHC);
		warmEmissions.remove(WarmPollutant.NO2);
		warmEmissions.remove(WarmPollutant.NOX);
		Id linkId3 = new IdImpl("link 3");
		WarmEmissionEvent event6 = new WarmEmissionEventImpl(simulationEndTime, linkId3, vehicleId, warmEmissions);
		handler.handleEvent(event6);
		// check event 6
		Assert.assertEquals(1.0, handler.getTime2linkIdLeaveCount().get(simulationEndTime).get(linkId));
		Assert.assertEquals(coValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.CO), MatsimTestUtils.EPSILON);
		Assert.assertEquals(c2Value, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.CO2_TOTAL), MatsimTestUtils.EPSILON);
		Assert.assertEquals(fcValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.FC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(hcValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.HC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.NMHC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.NO2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.NOX), MatsimTestUtils.EPSILON);
		Assert.assertEquals(pmValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		Assert.assertEquals(soValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(simulationEndTime).get(linkId3).get(WarmPollutant.SO2), MatsimTestUtils.EPSILON);
		
		handler.reset(2);
		handler.handleEvent(event6);
		
		// assert
		
		// 'normal', time 0, end of sim, krumme intervallgrenze
		// warmEmissions = null, teilweise initialisiert, vollstaendig
		// test reset + handle event
		// TODO header
	}

}
