package playground.julia.scenarios.munich.analysis.nectar;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.julia.emissions.EmissionsPerLinkWarmEventHandler;
//import playground.julia.scenarios.munich.analysis.nectar.*;
import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventImpl;
import playground.vsp.emissions.types.WarmPollutant;

public class TestEmissionsPerLinkWarmEventHandler {

	/**
	 * @param args
	 */

	
	@Test
	public void handleEventTest(){
		
		double simulationEndTime = 200.;
		int noOfTimeBins = 30; // 0.0, 6.666, 13.333, 20.0, ...
		EmissionsPerLinkWarmEventHandler handler = new EmissionsPerLinkWarmEventHandler(simulationEndTime, noOfTimeBins);
		
		// event 1
		// time inside one time intervall, all possible values for pollutants set
		double time = 5.0;
		double endOfTimeIntervall = simulationEndTime/noOfTimeBins*1;
		Id linkId = new IdImpl("link1");
		Id vehicleId = new IdImpl("veh 1");
		Double coValue = 2000., c2Value = 220. , fcValue = 22.2, hcValue = 2.222, nmValue= 0.22222, 
				n2Value = 3000., nxValue = 330., pmValue = 33.3, soValue = 3.333;
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
		// test event 1
		Assert.assertEquals(1.0, handler.getTime2linkIdLeaveCount().get(endOfTimeIntervall).get(linkId));
		Assert.assertEquals(coValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(endOfTimeIntervall).get(linkId).get(WarmPollutant.CO), MatsimTestUtils.EPSILON);
		Assert.assertEquals(c2Value, handler.getWarmEmissionsPerLinkAndTimeInterval().get(endOfTimeIntervall).get(linkId).get(WarmPollutant.CO2_TOTAL), MatsimTestUtils.EPSILON);
		Assert.assertEquals(fcValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(endOfTimeIntervall).get(linkId).get(WarmPollutant.FC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(hcValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(endOfTimeIntervall).get(linkId).get(WarmPollutant.HC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(nmValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(endOfTimeIntervall).get(linkId).get(WarmPollutant.NMHC), MatsimTestUtils.EPSILON);
		Assert.assertEquals(n2Value, handler.getWarmEmissionsPerLinkAndTimeInterval().get(endOfTimeIntervall).get(linkId).get(WarmPollutant.NO2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(nxValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(endOfTimeIntervall).get(linkId).get(WarmPollutant.NOX), MatsimTestUtils.EPSILON);
		Assert.assertEquals(pmValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(endOfTimeIntervall).get(linkId).get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		Assert.assertEquals(soValue, handler.getWarmEmissionsPerLinkAndTimeInterval().get(endOfTimeIntervall).get(linkId).get(WarmPollutant.SO2), MatsimTestUtils.EPSILON);
		// everything else zero or null
		Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(0.0).get(linkId).get(WarmPollutant.CO2_TOTAL), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.0, handler.getWarmEmissionsPerLinkAndTimeInterval().get(endOfTimeIntervall).get(new IdImpl("unused link")).get(WarmPollutant.CO2_TOTAL), MatsimTestUtils.EPSILON);
		Assert.assertNull(handler.getWarmEmissionsPerLinkAndTimeInterval().get(endOfTimeIntervall).get(linkId).get("some pollutant")); //TODO exception?
		Assert.assertEquals(0.0, handler.getTime2linkIdLeaveCount().get(0.0).get(linkId));
		Assert.assertNull(handler.getTime2linkIdLeaveCount().get(endOfTimeIntervall).get(new IdImpl("unused link")));
		
		// TODO go on here
		
		WarmEmissionEvent event2 = new WarmEmissionEventImpl(0.0, linkId, vehicleId, warmEmissions);
		handler.handleEvent(event2);
		WarmEmissionEvent event3 = new WarmEmissionEventImpl(simulationEndTime, linkId, vehicleId, warmEmissions);
		handler.handleEvent(event3);
		WarmEmissionEvent event4 = new WarmEmissionEventImpl(simulationEndTime/noOfTimeBins*2, linkId, vehicleId, warmEmissions);
		handler.handleEvent(event4);
		
		// ins richtige intervall?
		//ins falsche intervall?
		
		WarmEmissionEvent event5 = new WarmEmissionEventImpl(endOfTimeIntervall, linkId, vehicleId, null);
		handler.handleEvent(event5);
		
		warmEmissions.remove(WarmPollutant.NMHC);
		warmEmissions.remove(WarmPollutant.NO2);
		warmEmissions.remove(WarmPollutant.NOX);
		WarmEmissionEvent event6 = new WarmEmissionEventImpl(endOfTimeIntervall, linkId, vehicleId, warmEmissions);
		handler.handleEvent(event6);
		//assert
		
		handler.reset(2);
		handler.handleEvent(event6);
		
		
		
		// 'normal', time 0, end of sim, krumme intervallgrenze
		// warmEmissions = null, teilweise initialisiert, vollstaendig
		// test reset + handle event
		// TODO header
	}

}
