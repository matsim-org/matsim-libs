/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestEmission.java                                                       *
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

package playground;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.analysis.modules.emissionsAnalyzer.*;
import playground.vsp.emissions.ColdEmissionAnalysisModule;
import playground.vsp.emissions.events.ColdEmissionEvent;
import playground.vsp.emissions.events.ColdEmissionEventImpl;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonColdEventHandler;
import playground.vsp.emissions.utils.*;

//test for playground.vsp.emissions.utils.EmissionUtils

public class TestEmissionUtils {
	
	@Test
	//SortedMap<String, Double> sumUpEmissions(Map<WarmPollutant, Double> warmEmissions, Map<ColdPollutant, Double> coldEmissions)
	public final void testSumUpEmissions(){
		EmissionUtils eu = new EmissionUtils();
		
		Map<WarmPollutant, Double> warmEmissions = new HashMap<WarmPollutant, Double>();
		Map<ColdPollutant, Double> coldEmissions = new HashMap<ColdPollutant, Double>();
		
		warmEmissions.put(WarmPollutant.CO, 20.5);
		warmEmissions.put(WarmPollutant.FC, 3.001);
		coldEmissions.put(ColdPollutant.FC, 2000.);
		warmEmissions.put(WarmPollutant.CO2_TOTAL, .003);
		
		coldEmissions.put(ColdPollutant.HC, 100.7);
		coldEmissions.put(ColdPollutant.FC, 1000.);
		warmEmissions.put(WarmPollutant.PM, 52.89);
		coldEmissions.put(ColdPollutant.PM, 52.89);
		
		SortedMap<String, Double> sum = eu.sumUpEmissions(warmEmissions, coldEmissions);
		
		//cold pollutants: CO, FC, HC, NMHC, NO2, NOX, PM
		//warm pollutants: CO, CO2_TOTAL, FC, HC, NMHC, NO2, NOX, PM, SO2
		
		Double co = (Double)sum.get("CO");
		Double c2 = (Double)sum.get("CO_TOTAL");
		Double fc = (Double)sum.get("FC");
		Double hc = (Double)sum.get("HC");
		Double nm = (Double)sum.get("NMHC");
		Double n2 = (Double)sum.get("NO2");
		Double nx = (Double)sum.get("NOX");
		Double pm = (Double)sum.get("PM");
		Double so = (Double)sum.get("SO2");
		
		//TODO resolve Pollutant not found error 
		//TODO but is...
		Assert.assertEquals("Value of CO should be 20.5 .", co, 20.5, MatsimTestUtils.EPSILON);
		//Assert.assertEquals("Value of CO2_TOTAL should be 0.003 .", c2, 0.003, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of FC should be 1003.001 .", fc, 1003.001, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of HC should be 100.7 .", hc, 100.7, MatsimTestUtils.EPSILON);
		//Assert.assertEquals("Value of NMHC should be 0.0 .", nm, .0, MatsimTestUtils.EPSILON);
		//Assert.assertEquals("Value of NO2 should be 0.0 .", n2, .0, MatsimTestUtils.EPSILON);
		//Assert.assertEquals("Value of NOX should be 0.0 .", nx, .0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of PM should be 105.78 .", pm, 105.78, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of SO2 should be 0.0 .", so, .0, MatsimTestUtils.EPSILON);
		
		
	}
	@Test 
	//public Map<Id, SortedMap<String, Double>> sumUpEmissionsPerId(Map<Id, Map<WarmPollutant, Double>> warmEmissions,Map<Id, Map<ColdPollutant, Double>> coldEmissions)
	public final void testSumUpEmissionsPerId(){
		EmissionUtils eu = new EmissionUtils();
		Map<Id, Map<WarmPollutant, Double>> warmEmissions = new HashMap<Id, Map<WarmPollutant, Double>>();
		Map<Id, Map<ColdPollutant, Double>> coldEmissions = new HashMap<Id, Map<ColdPollutant, Double>>();
		
		Map<WarmPollutant, Double> mapWarm1 = new HashMap<WarmPollutant, Double>();
		Map<WarmPollutant, Double> mapWarm2 = new HashMap<WarmPollutant, Double>();
		Map<ColdPollutant, Double> mapCold1 = new HashMap<ColdPollutant, Double>();
		Map<ColdPollutant, Double> mapCold2 = new HashMap<ColdPollutant, Double>();
		
		//TODO what about negativ numbers?
		
		mapWarm1.put(WarmPollutant.CO, 20.0);
		mapCold1.put(ColdPollutant.CO, 44.05);
		mapWarm1.put(WarmPollutant.CO2_TOTAL, 1.0031);
		mapCold1.put(ColdPollutant.FC, 110.1888);
		mapWarm2.put(WarmPollutant.CO, 7.7);
		mapCold2.put(ColdPollutant.NMHC, 0.006);
		mapWarm2.put(WarmPollutant.CO2_TOTAL, 0.0);
		mapCold2.put(ColdPollutant.HC, 1.0);
		mapWarm2.put(WarmPollutant.HC, -5.0);
		
		
		//TODO leere Felder?
		warmEmissions.put(new IdImpl("id1"), mapWarm1);
		warmEmissions.put(new IdImpl("id2"), mapWarm2);
		coldEmissions.put(new IdImpl("id1"), mapCold1);
		coldEmissions.put(new IdImpl("id2"), mapCold2);
		
		Map<Id, SortedMap<String, Double>> sums = eu.sumUpEmissionsPerId(warmEmissions, coldEmissions);
		
		Double coId1 = sums.get(new IdImpl("id1")).get("CO");
		Double coId2 = sums.get(new IdImpl("id2")).get("CO");
		Double co2Id1 = sums.get(new IdImpl("id1")).get("CO2_TOTAL");
		Double co2Id2 = sums.get(new IdImpl("id2")).get("CO2_TOTAL");
		Double fcId1 = sums.get(new IdImpl("id1")).get("FC");
		Double fcId2 = sums.get(new IdImpl("id2")).get("FC");
		Double nmId1 = sums.get(new IdImpl("id1")).get("NMHC");
		Double nmId2 = sums.get(new IdImpl("id2")).get("NMHC");
		Double hcId1 = sums.get(new IdImpl("id1")).get("HC");
		Double hcId2 = sums.get(new IdImpl("id2")).get("HC");
		
		Assert.assertEquals("CO value of person 1 should be 64.05 but is " + coId1, 64.05, coId1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("CO value of person 2 should be 7.7 but is " + coId2, 7.7, coId2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("CO2 value of person 1 should be 1.0031 but is " + co2Id1, 1.0031, co2Id1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("CO2 value of person 2 should be 0.0 but is " + co2Id2, .0, co2Id2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("FC value of person 1 should be 110.1888 but is " + fcId1, 110.1888, fcId1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("FC value of person 2 should be 0.0 but is " + fcId2, .0, fcId2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NMHC value of person 1 should be 0.0 but is " + nmId1, .0, nmId1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NMHC value of person 2 should be 0.006 but is " + nmId2, 0.006, nmId2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("HC value of person 1 should be 0.0 but is " + hcId1, .0, hcId1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("HC value of person 2 should be -4.0 but is " + hcId2, -4.0, hcId2, MatsimTestUtils.EPSILON);
	}
	@Test @Ignore
	public final void testGetTotalEmissions(){
		
	}
	
}
	

	

