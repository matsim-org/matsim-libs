/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestHbefaVehicleAttributesEmission.java                                 *
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

package playground.emissions.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.utils.EmissionUtils;

//test for playground.vsp.emissions.utils.EmissionUtils

public class TestEmissionUtils {
	
	@Test
	//SortedMap<String, Double> sumUpEmissions(Map<WarmPollutant, Double> warmEmissions, Map<ColdPollutant, Double> coldEmissions)
	public final void testSumUpEmissions(){
		EmissionUtils eu = new EmissionUtils();
		
		Map<WarmPollutant, Double> warmEmissions = new HashMap<WarmPollutant, Double>();
		Map<ColdPollutant, Double> coldEmissions = new HashMap<ColdPollutant, Double>();	
		
		//cold pollutants: CO, FC, HC, NMHC, NO2, NOX, PM
		//warm pollutants: CO, CO2_TOTAL, FC, HC, NMHC, NO2, NOX, PM, SO2
		
		//complete list of all pollutants - missing data is not tested here
		warmEmissions.put(WarmPollutant.CO, .0005);
		warmEmissions.put(WarmPollutant.CO2_TOTAL, .003);
		warmEmissions.put(WarmPollutant.FC, .01);
		warmEmissions.put(WarmPollutant.HC, .2);
		warmEmissions.put(WarmPollutant.NMHC, 1.0);
		warmEmissions.put(WarmPollutant.NO2, 30.0);
		warmEmissions.put(WarmPollutant.NOX, 200.0);
		warmEmissions.put(WarmPollutant.PM, 7000.0);
		warmEmissions.put(WarmPollutant.SO2, 70000.0);
		
		coldEmissions.put(ColdPollutant.CO, .0003);
		coldEmissions.put(ColdPollutant.FC, .06);
		coldEmissions.put(ColdPollutant.HC, .7);
		coldEmissions.put(ColdPollutant.NMHC, 2.0);
		coldEmissions.put(ColdPollutant.NO2, 50.0);
		coldEmissions.put(ColdPollutant.NOX, 400.0);
		coldEmissions.put(ColdPollutant.PM, 9000.0);
		
		
		SortedMap<String, Double> sum = eu.sumUpEmissions(warmEmissions, coldEmissions);

		Double co = (Double)sum.get("CO");
		Double c2 = (Double)sum.get("CO2_TOTAL");
		Double fc = (Double)sum.get("FC");
		Double hc = (Double)sum.get("HC");
		Double nm = (Double)sum.get("NMHC");
		Double n2 = (Double)sum.get("NO2");
		Double nx = (Double)sum.get("NOX");
		Double pm = (Double)sum.get("PM");
		Double so = (Double)sum.get("SO2");
		
		Assert.assertEquals("Value of CO should be 0.0008 .", co, 0.0008, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of CO2_TOTAL should be 0.003 .", c2, 0.003, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of FC should be 0.07 .", fc, 0.07, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of HC should be 0.9 .", hc, 0.9, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of NMHC should be 3.0 .", nm, 3.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of NO2 should be 80.0 .", n2, 80.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of NOX should be 600.0 .", nx, 600.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of PM should be 16000.00 .", pm, 16000.0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of SO2 should be 70000.0 .", so, 70000.0, MatsimTestUtils.EPSILON);	
	}
	
	
	@Test 
	public final void testSumUpEmissionsPerId(){
		EmissionUtils eu = new EmissionUtils();
		Map<Id, Map<WarmPollutant, Double>> warmEmissions = new HashMap<Id, Map<WarmPollutant, Double>>();
		Map<Id, Map<ColdPollutant, Double>> coldEmissions = new HashMap<Id, Map<ColdPollutant, Double>>();
		
		//warm list for person1, warm list for person2, cold list for person1, cold list for person2
		Map<WarmPollutant, Double> mapWarm1 = new HashMap<WarmPollutant, Double>();
		Map<WarmPollutant, Double> mapWarm2 = new HashMap<WarmPollutant, Double>();
		Map<ColdPollutant, Double> mapCold1 = new HashMap<ColdPollutant, Double>();
		Map<ColdPollutant, Double> mapCold2 = new HashMap<ColdPollutant, Double>();
		
		//what about negativ numbers? ok
		mapWarm1.put(WarmPollutant.CO, .0002);
		mapWarm1.put(WarmPollutant.CO2_TOTAL, .004);
		mapWarm1.put(WarmPollutant.FC, .03);
		mapWarm1.put(WarmPollutant.HC, .8);
		mapWarm1.put(WarmPollutant.NMHC, 7.0);
		mapWarm1.put(WarmPollutant.NO2, 60.0);
		mapWarm1.put(WarmPollutant.NOX, 200.0);
		mapWarm1.put(WarmPollutant.PM, 7000.0);
		mapWarm1.put(WarmPollutant.SO2, 70000.0);
		
		mapCold1.put(ColdPollutant.CO, .0008);
		mapCold1.put(ColdPollutant.FC, .07);
		mapCold1.put(ColdPollutant.HC, .9);
		mapCold1.put(ColdPollutant.NMHC, 3.0);
		mapCold1.put(ColdPollutant.NO2, 40.0);
		mapCold1.put(ColdPollutant.NOX, 300.0);
		mapCold1.put(ColdPollutant.PM, 8000.0);
		
		//expected numbers of person 1
		double e1co = .0002+.0008;
		double e1c2 = .004;
		double e1fc = .03+.07;
		double e1hc = .8+.9;
		double e1nm = 7.0+3.0;
		double e1n2 = 60.0+ 40;
		double e1nx = 200.+300.;
		double e1pm = 7000.+8000.;
		double e1so = 70000;
		
		mapWarm2.put(WarmPollutant.CO, .0006);
		mapWarm2.put(WarmPollutant.CO2_TOTAL, .006);
		mapWarm2.put(WarmPollutant.FC, .08);
		mapWarm2.put(WarmPollutant.HC, .2);
		mapWarm2.put(WarmPollutant.NMHC, 4.0);
		mapWarm2.put(WarmPollutant.NO2, 80.0);
		mapWarm2.put(WarmPollutant.NOX, 400.0);
		mapWarm2.put(WarmPollutant.PM, 4000.0);
		mapWarm2.put(WarmPollutant.SO2, 60000.0);
		
		mapCold2.put(ColdPollutant.CO, .0009);
		mapCold2.put(ColdPollutant.FC, .06);
		mapCold2.put(ColdPollutant.HC, .4);
		mapCold2.put(ColdPollutant.NMHC, 2.0);
		mapCold2.put(ColdPollutant.NO2, 70.0);
		mapCold2.put(ColdPollutant.NOX, 100.0);
		mapCold2.put(ColdPollutant.PM, 2000.0);
		
		//expected numbers of person 1
		double e2co = .0006+.0009;
		double e2c2 = .006;
		double e2fc = .08+.06;
		double e2hc = .2+.4;
		double e2nm = 4.0+2.;
		double e2n2 = 80.0+ 70;
		double e2nx = 400.+100.;
		double e2pm = 4000.+2000.;
		double e2so = 60000;

		
		//TODO neg numbers
		
		//leere Felder? nicht hier testen - solche warm Em. sollten gar nicht entstehen
		warmEmissions.put(new IdImpl("id1"), mapWarm1);
		warmEmissions.put(new IdImpl("id2"), mapWarm2);
		coldEmissions.put(new IdImpl("id1"), mapCold1);
		coldEmissions.put(new IdImpl("id2"), mapCold2);
		
		Map<Id, SortedMap<String, Double>> sums = eu.sumUpEmissionsPerId(warmEmissions, coldEmissions);
		
		//actual numbers of person1
		double a1co = sums.get(new IdImpl("id1")).get("CO");
		double a1c2 = sums.get(new IdImpl("id1")).get("CO2_TOTAL");
		double a1fc = sums.get(new IdImpl("id1")).get("FC");
		double a1hc = sums.get(new IdImpl("id1")).get("HC");
		double a1nm = sums.get(new IdImpl("id1")).get("NMHC");
		double a1n2 = sums.get(new IdImpl("id1")).get("NO2");
		double a1nx = sums.get(new IdImpl("id1")).get("NOX");
		double a1pm = sums.get(new IdImpl("id1")).get("PM");
		double a1so = sums.get(new IdImpl("id1")).get("SO2");
		
		//actual numbers of person2
		double a2co = sums.get(new IdImpl("id2")).get("CO");
		double a2c2 = sums.get(new IdImpl("id2")).get("CO2_TOTAL");
		double a2fc = sums.get(new IdImpl("id2")).get("FC");
		double a2hc = sums.get(new IdImpl("id2")).get("HC");
		double a2nm = sums.get(new IdImpl("id2")).get("NMHC");
		double a2n2 = sums.get(new IdImpl("id2")).get("NO2");
		double a2nx = sums.get(new IdImpl("id2")).get("NOX");
		double a2pm = sums.get(new IdImpl("id2")).get("PM");
		double a2so = sums.get(new IdImpl("id2")).get("SO2");
		
		//stellt auch sicher, dass die Personen korrekt auseinander gehalten werden
		Assert.assertEquals("CO value of person 1 should be" +e1co +"but is ", e1co, a1co, MatsimTestUtils.EPSILON);
		Assert.assertEquals("CO2 value of person 1 should be" +e1c2 +"but is ", e1c2, a1c2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("FC value of person 1 should be" +e1fc +"but is ", e1fc, a1fc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("HC value of person 1 should be" +e1hc +"but is ", e1hc, a1hc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NMHC value of person 1 should be" +e1nm +"but is ", e1nm, a1nm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NO2 value of person 1 should be" +e1n2 +"but is ", e1n2, a1n2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NOX value of person 1 should be" +e1nx +"but is ", e1nx, a1nx, MatsimTestUtils.EPSILON);
		Assert.assertEquals("PM value of person 1 should be" +e1pm +"but is ", e1pm, a1pm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("SO value of person 1 should be" +e1so +"but is ", e1so, a1so, MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("CO value of person 2 should be" +e2co +"but is ", e2co, a2co, MatsimTestUtils.EPSILON);
		Assert.assertEquals("CO2 value of person 2 should be" +e2c2 +"but is ", e2c2, a2c2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("FC value of person 2 should be" +e2fc +"but is ", e2fc, a2fc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("HC value of person 2 should be" +e2hc +"but is ", e2hc, a2hc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NMHC value of person 2 should be" +e2nm +"but is ", e2nm, a2nm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NO2 value of person 2 should be" +e2n2 +"but is ", e2n2, a2n2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NOX value of person 2 should be" +e2nx +"but is ", e2nx, a2nx, MatsimTestUtils.EPSILON);
		Assert.assertEquals("PM value of person 2 should be" +e2pm +"but is ", e2pm, a2pm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("SO value of person 2 should be" +e2so +"but is ", e2so, a2so, MatsimTestUtils.EPSILON);

	}
	
	@Test @Ignore
	public final void testGetTotalEmissions(){
		
	}
	
	@Test @Ignore
	public final void testSetNonCalculatedEmissionsForPopulation(){
		
	}
	@Test @Ignore
	public final void testSetNonCalculatedEmissionsForNetwork(){
		
	}
	
	@Test @Ignore
	public final void testConvertWarmPollutantMap2String(){
		
	}
	
	@Test @Ignore
	public final void testConvertColdPollutantMap2String(){
		
	}
	
	
}
	
