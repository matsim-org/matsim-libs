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
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.utils.EmissionUtils;

/*
 * test for playground.vsp.emissions.utils.EmissionUtils
 * missing data is not tested here
 * negative emission values are allowed
 * 1 test constructor
 * 2 test sumUpEmissions 
 * 3 test sumUpEmissionPerId 
 * 4 test getTotalEmissions
 * 5 test SetNonCalculatedEmissionsForPopulation
 * - correct input - population does not match map of emissions - empty map of emissions
 * 6 test SetNonCalculatedEmissionsForNetwork
 * 7 test ConvertWarmPollutantMap2String
 * 8 test ConvertColdPollutantMap2String
 */

public class TestEmissionUtils {
	
	String co="CO", c2="CO2_TOTAL", fc = "FC", hc= "HC",
			nm ="NMHC", n2="NO2", nx="NOX", pm="PM", so="SO2";
	int numOfPolls;
	
	@Test
	public final void testConstructor(){
		EmissionUtils eu = new EmissionUtils();
		
		SortedSet<String> euPollutants = eu.getListOfPollutants();
		SortedSet<String> localPolls = new TreeSet<String>();
		fillPollutant(localPolls);
		SortedSet<String> pollsFromEnum = new TreeSet<String>();
		
		//warm and cold pollutant enums match eu.getListOfPollutants
		for(WarmPollutant wp: WarmPollutant.values()){
			pollsFromEnum.add(wp.toString());
			Assert.assertTrue(euPollutants.contains(wp.toString()));
		}
		for(ColdPollutant cp: ColdPollutant.values()){
			pollsFromEnum.add(cp.toString());
			Assert.assertTrue(euPollutants.contains(cp.toString()));
		}
		for(String pollutant: euPollutants){
			Assert.assertTrue(pollsFromEnum.contains(pollutant));
		}
		
		// assure that the local list of pollutants (used here for the test) matches eu.getListOfPolllutants
		// if this fails there is a problem with the test, not with tested class
		for(String pollutant: euPollutants){
			Assert.assertTrue("the list of pollutants used in this test is not correct",localPolls.contains(pollutant));
		}
		// are all of the pollutants from the local list
		// also in the euPollutants list?
		Assert.assertEquals("the list of pollutants used in this test is not correct", localPolls.size(), euPollutants.size());
	}
	
	@Test
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
		
		//assures simultaneously that persons/ids are distinguished correctly
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
	
	@Test
	public final void testGetTotalEmissions(){
		EmissionUtils eu = new EmissionUtils();
		boolean nullPointer = false;

		//would another format be better? arraylist?
		SortedSet<String> listOfPollutants = new TreeSet<String>();
		fillPollutant(listOfPollutants);
		
		//why do we need sorted maps? 
		SortedMap<String, Double> totalEmissions = new TreeMap<String, Double>();
		//contains some persons with their emission maps
		Map<Id, SortedMap<String, Double>> persons2emissions = new HashMap<Id, SortedMap<String, Double>>();
		
		//test empty input
		try{
			totalEmissions= eu.getTotalEmissions(null);
		}
		catch(NullPointerException e){
			nullPointer = true;
		}
		Assert.assertTrue(nullPointer); 
		nullPointer =false;
		
		//test empty list as input		
		totalEmissions = eu.getTotalEmissions(persons2emissions);
		Assert.assertEquals("this map should be empty", 0, totalEmissions.size());
		
		//put some content into the list
		// no incorrect/incomplete input data here
		// warm and cold emissions are already sumed up -> sumUpEmissionsPerId
		
		//person1
		SortedMap<String, Double> allEmissionsP1 = new TreeMap<String, Double>();
		Double p1co = .9, p1c2 = 3.2, p1fc=9.3, p1hc= 1.0, p1nm=-68., p1n2= .87, p1nx= 5., p1pm = 3.22, p1so=79.8;
		IdImpl p1Id = new IdImpl("p1");
		allEmissionsP1.put(co, p1co);
		allEmissionsP1.put(c2, p1c2);
		allEmissionsP1.put(fc, p1fc);
		allEmissionsP1.put(hc, p1hc);
		allEmissionsP1.put(nm, p1nm);
		allEmissionsP1.put(n2, p1n2);
		allEmissionsP1.put(nx, p1nx);
		allEmissionsP1.put(pm, p1pm);
		allEmissionsP1.put(so, p1so);
		
		//person2
		SortedMap<String, Double> allEmissionsp2 = new TreeMap<String, Double>();
		Double p2co = .65, p2c2= -7., p2fc=-.3149, p2hc=54., p2nm=7.9, p2n2=.34, p2nx=-.8, p2pm=4., p2so=-750.;
		IdImpl p2Id = new IdImpl("p2");
		allEmissionsp2.put(co, p2co);
		allEmissionsp2.put(c2, p2c2);
		allEmissionsp2.put(fc, p2fc);
		allEmissionsp2.put(hc, p2hc);
		allEmissionsp2.put(nm, p2nm);
		allEmissionsp2.put(n2, p2n2);
		allEmissionsp2.put(nx, p2nx);
		allEmissionsp2.put(pm, p2pm);
		allEmissionsp2.put(so, p2so);
		
		//person3
		SortedMap<String, Double> allEmissionsp3 = new TreeMap<String, Double>();
		Double p3co=-970., p3c2=-.000012, p3fc=57.21, p3hc=80.8, p3nm=9.52, p3n2=.0074, p3nx=42., p3pm=.38, p3so=70.;
		IdImpl p3Id = new IdImpl("p3");
		allEmissionsp3.put(co, p3co);
		allEmissionsp3.put(c2, p3c2);
		allEmissionsp3.put(fc, p3fc);
		allEmissionsp3.put(hc, p3hc);
		allEmissionsp3.put(nm, p3nm);
		allEmissionsp3.put(n2, p3n2);
		allEmissionsp3.put(nx, p3nx);
		allEmissionsp3.put(pm, p3pm);
		allEmissionsp3.put(so, p3so);
		
		// put persons into persons2emission list
		persons2emissions.put(p1Id, allEmissionsP1);
		persons2emissions.put(p2Id, allEmissionsp2);
		persons2emissions.put(p3Id, allEmissionsp3);
		totalEmissions = eu.getTotalEmissions(persons2emissions);
		
		Assert.assertEquals(co+" values are not correct", p1co+p2co+p3co, totalEmissions.get(co), MatsimTestUtils.EPSILON);
		Assert.assertEquals(c2+" values are not correct", p1c2+p2c2+p3c2, totalEmissions.get(c2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(fc+" values are not correct", p1fc+p2fc+p3fc, totalEmissions.get(fc), MatsimTestUtils.EPSILON);
		Assert.assertEquals(hc+" values are not correct", p1hc+p2hc+p3hc, totalEmissions.get(hc), MatsimTestUtils.EPSILON);
		Assert.assertEquals(nm+" values are not correct", p1nm+p2nm+p3nm, totalEmissions.get(nm), MatsimTestUtils.EPSILON);
		Assert.assertEquals(n2+" values are not correct", p1n2+p2n2+p3n2, totalEmissions.get(n2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(nx+" values are not correct", p1nx+p2nx+p3nx, totalEmissions.get(nx), MatsimTestUtils.EPSILON);
		Assert.assertEquals(pm+" values are not correct", p1pm+p2pm+p3pm, totalEmissions.get(pm), MatsimTestUtils.EPSILON);
		Assert.assertEquals(so+" values are not correct", p1so+p2so+p3so, totalEmissions.get(so), MatsimTestUtils.EPSILON);
		
		// assume that all maps are complete 
		for(String emission : listOfPollutants){
			Assert.assertTrue(totalEmissions.containsKey(emission));
		}
		// nothing else in the list
		Assert.assertEquals("this list should be as long as number of pollutants",totalEmissions.keySet().size(), listOfPollutants.size());
		
	}
	
	private SortedSet<String> fillPollutant(SortedSet<String> listOfPollutants){
		listOfPollutants.clear();
		listOfPollutants.add(co);
		listOfPollutants.add(c2);
		listOfPollutants.add(fc);
		listOfPollutants.add(hc);
		listOfPollutants.add(nm);
		listOfPollutants.add(n2);
		listOfPollutants.add(nx);
		listOfPollutants.add(pm);
		listOfPollutants.add(so);
		return listOfPollutants;
		
	}
	
	@Test
	public final void testSetNonCalculatedEmissionsForPopulation(){
		//IN: population
		//		map <person id, personal emission map>
		//		personal emission map = <name of emission, value of emission>
		
		//OUT: returns a big map: <person id, personal emission map>
		//personal emission map = <name of emission, value of emission>
		
		boolean nullPointerEx = false;
		EmissionUtils eu = new EmissionUtils();
		SortedSet<String> localPolls = new TreeSet<String>();
		fillPollutant(localPolls);
		Map<Id, SortedMap<String, Double>> totalEmissions = new TreeMap<Id, SortedMap<String, Double>>();
		
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Population pop = sc.getPopulation();
		PopulationFactory populationFactory = pop.getFactory();
		
		//correct data: two persons with complete data
		//nothing should be set to zero 
		
		//person1
		Double cov1=.0005, c2v1= .003, fcv1 = .01, hcv1=.2, nmv1=1., n2v1=30., nxv1=200., pmv1 =7000., sov1=70000.;
		
		SortedMap<String, Double> p1Emissions = new TreeMap<String, Double>();
		//complete list of all pollutants - missing data is not tested here
		p1Emissions.put(co, cov1);
		p1Emissions.put(c2, c2v1);
		p1Emissions.put(fc, fcv1);
		p1Emissions.put(hc, hcv1);
		p1Emissions.put(nm, nmv1);
		p1Emissions.put(n2, n2v1);
		p1Emissions.put(nx, nxv1);
		p1Emissions.put(pm, pmv1);
		p1Emissions.put(so, sov1);
		
		Id idp1 = new IdImpl("p1");
		Person p1 = populationFactory.createPerson(idp1);
		pop.addPerson(p1);
		totalEmissions.put(idp1, p1Emissions);
		
		//person2
		Double cov2=.0007, c2v2= .006, fcv2 = .04, hcv2=.5, nmv2=7., n2v2=60., nxv2=800., pmv2 =1000., sov2=90000.;
		
		SortedMap<String, Double> p2Emissions = new TreeMap<String, Double>();
		//complete list of all pollutants - missing data is not tested here
		p2Emissions.put(co, cov2);
		p2Emissions.put(c2, c2v2);
		p2Emissions.put(fc, fcv2);
		p2Emissions.put(hc, hcv2);
		p2Emissions.put(nm, nmv2);
		p2Emissions.put(n2, n2v2);
		p2Emissions.put(nx, nxv2);
		p2Emissions.put(pm, pmv2);
		p2Emissions.put(so, sov2);
		
		Id idp2 = new IdImpl("p2");
		Person p2 = populationFactory.createPerson(idp2);
		pop.addPerson(p2);
		totalEmissions.put(idp2, p2Emissions);
		
		Map<Id, SortedMap<String, Double>> finalMap = eu.setNonCalculatedEmissionsForPopulation(pop, totalEmissions);
		
		//check: all persons added to the population are contained in the finalMap
		Assert.assertTrue("the calculated map should contain person 1", finalMap.containsKey(idp1));
		Assert.assertTrue("the calculated map should contain person 2", finalMap.containsKey(idp2));
		//nothing else in the finalMap
		Assert.assertEquals("the calculated map should contain two persons but contains "+
		finalMap.size() + "persons." ,pop.getPersons().keySet().size(), finalMap.size());
		
		//check: all values for person 1 and 2 are not null or zero
		// and of type double
		for(Object id : finalMap.keySet()){
			Assert.assertTrue(id.getClass()== Id.class||id.getClass()==IdImpl.class);
			for(Object pollutant: finalMap.get(id).values()){
				Assert.assertTrue(pollutant.getClass()==Double.class);
				Assert.assertNotSame(0.0, (Double)pollutant);
				Assert.assertNotNull(pollutant);
			}
			//check: alle Emissionstypen kommen vor
			for(String emission : localPolls){
				Assert.assertTrue(finalMap.get(id).containsKey(emission));
			}
			//nothing else in the list
			int numOfPolls = localPolls.size();
			Assert.assertEquals("the number of pullutants is " + finalMap.get(id).keySet().size()+ " but should be" + numOfPolls, 
					numOfPolls, finalMap.get(id).keySet().size());
		}
		
		//check: values for all emissions are correct -person 1
		Assert.assertEquals("CO value for person 1 is not correct", cov1, finalMap.get(idp1).get(co), MatsimTestUtils.EPSILON);	
		Assert.assertEquals("CO2 value for person 1 is not correct", c2v1, finalMap.get(idp1).get(c2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("FC value for person 1 is not correct", fcv1, finalMap.get(idp1).get(fc), MatsimTestUtils.EPSILON);
		Assert.assertEquals("HC value for person 1 is not correct", hcv1, finalMap.get(idp1).get(hc), MatsimTestUtils.EPSILON);
		Assert.assertEquals("NMHC value for person 1 is not correct", nmv1, finalMap.get(idp1).get(nm), MatsimTestUtils.EPSILON);
		Assert.assertEquals("NO2 value for person 1 is not correct", n2v1, finalMap.get(idp1).get(n2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("NOX value for person 1 is not correct", nxv1, finalMap.get(idp1).get(nx), MatsimTestUtils.EPSILON);
		Assert.assertEquals("PM value for person 1 is not correct", pmv1, finalMap.get(idp1).get(pm), MatsimTestUtils.EPSILON);
		Assert.assertEquals("SO value for person 1 is not correct", sov1, finalMap.get(idp1).get(so), MatsimTestUtils.EPSILON);
		
		//check: values for all emissions are correct -person 2
		Assert.assertEquals("CO value for person 2 is not correct", cov2, finalMap.get(idp2).get(co), MatsimTestUtils.EPSILON);	
		Assert.assertEquals("CO2 value for person 2 is not correct", c2v2, finalMap.get(idp2).get(c2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("FC value for person 2 is not correct", fcv2, finalMap.get(idp2).get(fc), MatsimTestUtils.EPSILON);
		Assert.assertEquals("HC value for person 2 is not correct", hcv2, finalMap.get(idp2).get(hc), MatsimTestUtils.EPSILON);
		Assert.assertEquals("NMHC value for person 2 is not correct", nmv2, finalMap.get(idp2).get(nm), MatsimTestUtils.EPSILON);
		Assert.assertEquals("NO2 value for person 2 is not correct", n2v2, finalMap.get(idp2).get(n2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("NOX value for person 2 is not correct", nxv2, finalMap.get(idp2).get(nx), MatsimTestUtils.EPSILON);
		Assert.assertEquals("PM value for person 2 is not correct", pmv2, finalMap.get(idp2).get(pm), MatsimTestUtils.EPSILON);
		Assert.assertEquals("SO value for person 2 is not correct", sov2, finalMap.get(idp2).get(so), MatsimTestUtils.EPSILON);
				
		//person 3 in population but not in totalEmissions
				Id idp3 = new IdImpl("p3");
				Person p3 = populationFactory.createPerson(idp3);
				pop.addPerson(p3);
				
				finalMap = eu.setNonCalculatedEmissionsForPopulation(pop, totalEmissions);
				
				//check: all persons added to the population are contained in the finalMap
				Assert.assertTrue("the calculated map should contain person 1", finalMap.containsKey(idp1));
				Assert.assertTrue("the calculated map should contain person 2", finalMap.containsKey(idp2));
				Assert.assertTrue("the calculated map should contain person 3", finalMap.containsKey(idp3));
				
				//nothing else in the finalMap
				Assert.assertEquals("the calculated map should contain three persons but contains "+
				finalMap.size() + "persons." ,pop.getPersons().keySet().size(), finalMap.size());
				
				//check: all values for the third are zero and of type double
				Assert.assertTrue(finalMap.keySet().contains(idp3));
				//TODO
				//Assert.assertTrue(finalMap.get(idp3).getClass()== Id.class||idp3.getClass()==IdImpl.class);
					for(Object pollutant: finalMap.get(idp3).values()){
						Assert.assertTrue(pollutant.getClass()==Double.class);
						Assert.assertEquals(0.0, (Double)pollutant, MatsimTestUtils.EPSILON);
						Assert.assertNotNull(pollutant);
					}
					//check: alle Emissionstypen kommen vor
					for(String emission : localPolls){
						Assert.assertTrue(finalMap.get(idp3).containsKey(emission));
					}
					//nothing else in the list
					numOfPolls = localPolls.size();
					Assert.assertEquals("the number of pullutants is " + finalMap.get(idp3).keySet().size()+ " but should be" + numOfPolls, 
							numOfPolls, finalMap.get(idp3).keySet().size());
				
		
		//person 4 in totalEmissions but not in population
					Double cov4=.0008, c2v4= .004, fcv4 = .07, hcv4=.9, nmv4=1., n2v4=50., nxv4=700., pmv4 =4000., sov4=30000.;
					
					SortedMap<String, Double> p4Emissions = new TreeMap<String, Double>();
					//complete list of all pollutants - missing data is not tested here
					p4Emissions.put(co, cov4);
					p4Emissions.put(c2, c2v4);
					p4Emissions.put(fc, fcv4);
					p4Emissions.put(hc, hcv4);
					p4Emissions.put(nm, nmv4);
					p4Emissions.put(n2, n2v4);
					p4Emissions.put(nx, nxv4);
					p4Emissions.put(pm, pmv4);
					p4Emissions.put(so, sov4);
					
					Id idp4 = new IdImpl("p4");
					Person p4 = populationFactory.createPerson(idp4);
					totalEmissions.put(idp4, p4Emissions);
					
					finalMap = eu.setNonCalculatedEmissionsForPopulation(pop, totalEmissions);
					
					//check: person 4 is not in the finalMap
					Assert.assertFalse("the calculated map should not contain person 4", finalMap.containsKey(idp4));
					//nothing else in the finalMap
					Assert.assertEquals("the calculated map should contain three persons but contains "+
					finalMap.size() + "persons." ,pop.getPersons().keySet().size(), finalMap.size());
							
		//missing emissions map 
					//TODO -> benjamin fragen
					try {
						Map<Id, SortedMap<String, Double>> missingMap = eu.setNonCalculatedEmissionsForPopulation(pop, null);
						//check: all persons added to the population are contained in the finalMap
						Assert.assertTrue("the calculated map should contain person 1", missingMap.containsKey(idp1));
						Assert.assertTrue("the calculated map should contain person 2", missingMap.containsKey(idp2));
						Assert.assertTrue("the calculated map should contain person 3", missingMap.containsKey(idp3));
						//nothing else in the finalMap
						Assert.assertEquals("the calculated map should contain three persons but contains "+
						finalMap.size() + "persons." ,pop.getPersons().keySet().size(), missingMap.size());
						
						//check: all values for all persons are zero and of type double
						for(Object id : finalMap.keySet()){
							Assert.assertTrue(id.getClass()== Id.class||id.getClass()==IdImpl.class);
							for(Object pollutant: finalMap.get(id).values()){
								Assert.assertTrue(pollutant.getClass()==Double.class);
								Assert.assertEquals("map of pollutants was missing. Therefore all values should be set to zero.", 
										0.0, (Double)pollutant, MatsimTestUtils.EPSILON);
								Assert.assertNotNull(pollutant);
							}
							//check: alle types of emissions appear
							for(String emission : localPolls){
								Assert.assertTrue(finalMap.get(id).containsKey(emission));
							}
							//nothing else in the list
							int numOfPolls = localPolls.size();
							Assert.assertEquals("the number of pullutants is " + finalMap.get(id).keySet().size()+ " but should be" + numOfPolls, 
									numOfPolls, finalMap.get(id).keySet().size());
						}
					} catch (NullPointerException e) {
						nullPointerEx=true;
					}
					Assert.assertTrue(nullPointerEx);
					nullPointerEx = false;

					//empty emissions map 
					Map<Id, SortedMap<String, Double>> emptyEmissions = new TreeMap<Id, SortedMap<String, Double>>();
					Map<Id, SortedMap<String, Double>> emptyMap = eu.setNonCalculatedEmissionsForPopulation(pop, emptyEmissions );
					
					

			//check: all persons added to the population are contained in the finalMap
			Assert.assertTrue("the calculated map should contain person 1", emptyMap.containsKey(idp1));
			Assert.assertTrue("the calculated map should contain person 2", emptyMap.containsKey(idp2));
			Assert.assertTrue("the calculated map should contain person 3", emptyMap.containsKey(idp3));
			//nothing else in the finalMap
			Assert.assertEquals("the calculated map should contain three persons but contains "+
			emptyMap.size() + "persons." ,pop.getPersons().keySet().size(), emptyMap.size());
			
			//check: all values for all persons are zero and of type double
			for(Object id : emptyMap.keySet()){
				Assert.assertTrue(id.getClass()== Id.class||id.getClass()==IdImpl.class);
				for(Object pollutant: emptyMap.get(id).values()){
					Assert.assertTrue(pollutant.getClass()==Double.class);
					Assert.assertEquals("map of pollutants was missing. Therefore all values should be set to zero.", 
							0.0, (Double)pollutant, MatsimTestUtils.EPSILON);
					Assert.assertNotNull(pollutant);
				}
				//check: alle Emissionstypen kommen vor
				for(String emission : localPolls){
					Assert.assertTrue(emptyMap.get(id).containsKey(emission));
				}
				//nothing else in the list
				int numOfPolls = localPolls.size();
				Assert.assertEquals("the number of pullutants is " + emptyMap.get(id).keySet().size()+ " but should be" + numOfPolls, 
						numOfPolls, emptyMap.get(id).keySet().size());
			}
					
			// throw exception if population isnt set	
			try{
				Map<Id, SortedMap<String, Double>> noPopulation = eu.setNonCalculatedEmissionsForPopulation(null, totalEmissions);
			}
			catch(NullPointerException e){
				nullPointerEx = true;
			}
			Assert.assertTrue("setNonCalculatedEmissionForPopulation should throw an exeption for empty population as input", nullPointerEx);
			nullPointerEx = false;

			Config config2 = ConfigUtils.createConfig();
			Scenario sc2 = ScenarioUtils.createScenario(config2);
			Population pop2 = sc2.getPopulation();
			
			Map<Id, SortedMap<String, Double>> emptyPopulation = eu.setNonCalculatedEmissionsForPopulation(pop2, totalEmissions);
			Assert.assertEquals("this list should be empty", 0, emptyPopulation.keySet().size());
		
		
	}
	
	


	@Test
	public final void testSetNonCalculatedEmissionsForNetwork(){
		
		//IN: network,totalEmissions 
		//totalEmissions = map <linkId, SpecificEmissionMap>
		
		//OUT: map <linkId, SpecificEmissionMap>
		//SpecificEmissionMap= <pollutant, value>
		
		EmissionUtils eu = new EmissionUtils();
		
		SortedSet<String> localPolls = new TreeSet<String>();
		fillPollutant(localPolls);
		
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network network = sc.getNetwork();
		addLinksToNetwork(network, sc);
		
		Map<Id, SortedMap<String, Double>> totalEmissions = new HashMap<Id, SortedMap<String, Double>>();
	
		//complete link - link12
		Id link12id = new IdImpl("link12");
		SortedMap<String, Double> emLink12 = new TreeMap<String, Double>();
		Double c2link12v=.0008, colink12v=.001, fclink12v=.05,
				hclink12v=.8, nmlink12v=1., n2link12v=50.,
				nxlink12v=600., pmlink12v=2000., solink12v=60000.;
		emLink12.put(c2, c2link12v); emLink12.put(co, colink12v); emLink12.put(fc, fclink12v); 
		emLink12.put(hc, hclink12v); emLink12.put(nm, nmlink12v); emLink12.put(n2, n2link12v); 
		emLink12.put(nx, nxlink12v); emLink12.put(pm, pmlink12v); emLink12.put(so, solink12v);
		totalEmissions.put(link12id, emLink12 );
		
		//complete link - link13
		Id link13id = new IdImpl("link13");
		SortedMap<String, Double> emLink13 = new TreeMap<String, Double>();
		Double c2link13v=.0003, colink13v=.008, fclink13v=.03,
				hclink13v=.7, nmlink13v=6., n2link13v=40.,
				nxlink13v=800., pmlink13v=1000., solink13v=90000.;
		emLink13.put(c2, c2link13v); emLink13.put(co, colink13v); emLink13.put(fc, fclink13v); 
		emLink13.put(hc, hclink13v); emLink13.put(nm, nmlink13v); emLink13.put(n2, n2link13v); 
		emLink13.put(nx, nxlink13v); emLink13.put(pm, pmlink13v); emLink13.put(so, solink13v);
		totalEmissions.put(new IdImpl("link13"), emLink13 );
		
		//missing map - link14
		Id link14id = new IdImpl("link14");
		//TODO does not work
		totalEmissions.put(new IdImpl("link14"), null);
		//TODO delete this workaround!
		SortedMap<String, Double> emLink14 = new TreeMap<String, Double>();
		totalEmissions.put(new IdImpl("link14"), emLink14);
		
		//partial map - link 23
		Id link23id = new IdImpl("link23");
		SortedMap<String, Double> emLink23 = new TreeMap<String, Double>();
		Double nxlink23v=900., pmlink23v=6000., solink23v=20000.;
		emLink23.put(nx, nxlink23v); emLink23.put(pm, pmlink23v); emLink23.put(so, solink23v);
		totalEmissions.put(new IdImpl("link23"), emLink23 );
		
		//empty map - link 24
		Id link24id = new IdImpl("link24");
		SortedMap<String, Double> emLink24 = new TreeMap<String, Double>();
		totalEmissions.put(new IdImpl("link24"), emLink24);
		
		//not put into totalEmissionsMap - link 34
		Id link34id = new IdImpl("link34");
		
		eu.setNonCalculatedEmissionsForNetwork(network, totalEmissions);
		//each link of the network and each type of emission
		for(Link link: network.getLinks().values()){
			
			Id linkId = sc.createId(link.getId().toString());

			if(link.getId().toString()!=sc.createId("link34").toString()){
				Assert.assertTrue(totalEmissions.containsKey(linkId));
				SortedMap<String, Double> emissionMapForLink = totalEmissions.get(linkId);
				for(String pollutant: localPolls){
					Assert.assertTrue(pollutant + "not found for link " +linkId.toString(), 
							emissionMapForLink.containsKey(pollutant));
					Assert.assertEquals(Double.class, emissionMapForLink.get(pollutant).getClass());
				}
			}else{
				//TODO benjamin fragen
				Assert.assertFalse("not in emission map", totalEmissions.containsKey(sc.createId(link.getId().toString())));
			}			
		}
		//check values
		//link 12 and 13
		Assert.assertEquals(totalEmissions.get(link12id).get(c2), c2link12v, MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link12id).get(co), colink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link12id).get(fc), fclink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link12id).get(hc), hclink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link12id).get(nm), nmlink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link12id).get(n2), n2link12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link12id).get(nx), nxlink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link12id).get(pm), pmlink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link12id).get(so), solink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link13id).get(c2), c2link13v, MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link13id).get(co), colink13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link13id).get(fc), fclink13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link13id).get(hc), hclink13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link13id).get(nm), nmlink13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link13id).get(n2), n2link13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link13id).get(nx), nxlink13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link13id).get(pm), pmlink13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link13id).get(so), solink13v,  MatsimTestUtils.EPSILON);
		
		//link 14
		//TODO siehe oben -> benjamin fragen
		
		//link 23 - partial
		Assert.assertEquals(totalEmissions.get(link23id).get(c2), .0, MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link23id).get(co), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link23id).get(fc), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link23id).get(hc), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link23id).get(nm), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link23id).get(n2), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link23id).get(nx), nxlink23v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link23id).get(pm), pmlink23v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link23id).get(so), solink23v,  MatsimTestUtils.EPSILON);
		
		//link 24 - empty
		Assert.assertEquals(totalEmissions.get(link24id).get(c2), .0, MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link24id).get(co), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link24id).get(fc), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link24id).get(hc), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link24id).get(nm), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link24id).get(n2), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link24id).get(nx), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link24id).get(pm), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissions.get(link24id).get(so), .0,  MatsimTestUtils.EPSILON);
		
	}
	

	private void addLinksToNetwork(Network nw, Scenario sc) {
		NetworkImpl network = (NetworkImpl) sc.getNetwork();
		Node node1 = network.createAndAddNode(sc.createId("node1"), sc.createCoord(.0, .0));
		Node node2 = network.createAndAddNode(sc.createId("node2"), sc.createCoord(.0, 1000.));
		Node node3 = network.createAndAddNode(sc.createId("node3"), sc.createCoord(1000., .0));
		Node node4 = network.createAndAddNode(sc.createId("node4"), sc.createCoord(1000., 1000.));
		
		network.createAndAddLink(sc.createId("link12"), node1, node2, 1000., 20., 3600, 2); //w/o orig id and type
		network.createAndAddLink(sc.createId("link13"), node1, node3, 1000., 20., 3600, 2); //w/o orig id and type
		network.createAndAddLink(sc.createId("link14"), node1, node4, 1000., 20., 3600, 2); //w/o orig id and type
		network.createAndAddLink(sc.createId("link23"), node2, node3, 1000., 20., 3600, 2); //w/o orig id and type
		network.createAndAddLink(sc.createId("link24"), node2, node4, 1000., 20., 3600, 2); //w/o orig id and type
		network.createAndAddLink(sc.createId("link34"), node3, node4, 1000., 20., 3600, 2); //w/o orig id and type
	}

	@Test
	public final void testConvertWarmPollutantMap2String(){
		EmissionUtils eu = new EmissionUtils();
		
		Map<WarmPollutant, Double> warmEmissions = new HashMap<WarmPollutant, Double>();	
		//warm pollutants: CO, CO2_TOTAL, FC, HC, NMHC, NO2, NOX, PM, SO2
		//values for warm polls
		Double cov=.0005, c2v= .003, fcv = .01, hcv=.2, nmv=1., n2v=30., nxv=200., pmv =7000., sov=70000.;
		
		//complete list of all pollutants - missing data is not tested here
		warmEmissions.put(WarmPollutant.CO, cov);
		warmEmissions.put(WarmPollutant.CO2_TOTAL, c2v);
		warmEmissions.put(WarmPollutant.FC, fcv);
		warmEmissions.put(WarmPollutant.HC, hcv);
		warmEmissions.put(WarmPollutant.NMHC, nmv);
		warmEmissions.put(WarmPollutant.NO2, n2v);
		warmEmissions.put(WarmPollutant.NOX, nxv);
		warmEmissions.put(WarmPollutant.PM, pmv);
		warmEmissions.put(WarmPollutant.SO2, sov);
		
		SortedMap<String, Double> convertedWarmMap = eu.convertWarmPollutantMap2String(warmEmissions);
		
		Assert.assertEquals(co + " values do not match", cov,convertedWarmMap.get(co), MatsimTestUtils.EPSILON);
		Assert.assertEquals(c2 + " values do not match", c2v,convertedWarmMap.get(c2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(fc + " values do not match", fcv,convertedWarmMap.get(fc), MatsimTestUtils.EPSILON);
		Assert.assertEquals(hc + " values do not match", hcv,convertedWarmMap.get(hc), MatsimTestUtils.EPSILON);
		Assert.assertEquals(nm + " values do not match", nmv,convertedWarmMap.get(nm), MatsimTestUtils.EPSILON);
		Assert.assertEquals(n2 + " values do not match", n2v,convertedWarmMap.get(n2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(nx + " values do not match", nxv,convertedWarmMap.get(nx), MatsimTestUtils.EPSILON);
		Assert.assertEquals(pm + " values do not match", pmv,convertedWarmMap.get(pm), MatsimTestUtils.EPSILON);
		Assert.assertEquals(so + " values do not match", sov,convertedWarmMap.get(so), MatsimTestUtils.EPSILON);
		
		//no unwanted key in the map
		Assert.assertEquals("something in the converted WarmMap that does not belong here", WarmPollutant.values().length, convertedWarmMap.size());
		
	}

	@Test
	public final void testConvertColdPollutantMap2String(){{
		EmissionUtils eu = new EmissionUtils();
		
		Map<ColdPollutant, Double> coldEmissions = new HashMap<ColdPollutant, Double>();	
		//values for cold polls
		Double cov=.0005, fcv = .01, hcv=.2, nmv=1., n2v=30., nxv=200., pmv =7000.;
		
		//complete list of all pollutants - missing data is not tested here
		coldEmissions.put(ColdPollutant.CO, cov);
		coldEmissions.put(ColdPollutant.FC, fcv);
		coldEmissions.put(ColdPollutant.HC, hcv);
		coldEmissions.put(ColdPollutant.NMHC, nmv);
		coldEmissions.put(ColdPollutant.NO2, n2v);
		coldEmissions.put(ColdPollutant.NOX, nxv);
		coldEmissions.put(ColdPollutant.PM, pmv);
	
		SortedMap<String, Double> convertedColdMap = eu.convertColdPollutantMap2String(coldEmissions);
		
		Assert.assertEquals(co + " values do not match", cov,convertedColdMap.get(co), MatsimTestUtils.EPSILON);
		Assert.assertEquals(fc + " values do not match", fcv,convertedColdMap.get(fc), MatsimTestUtils.EPSILON);
		Assert.assertEquals(hc + " values do not match", hcv,convertedColdMap.get(hc), MatsimTestUtils.EPSILON);
		Assert.assertEquals(nm + " values do not match", nmv,convertedColdMap.get(nm), MatsimTestUtils.EPSILON);
		Assert.assertEquals(n2 + " values do not match", n2v,convertedColdMap.get(n2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(nx + " values do not match", nxv,convertedColdMap.get(nx), MatsimTestUtils.EPSILON);
		Assert.assertEquals(pm + " values do not match", pmv,convertedColdMap.get(pm), MatsimTestUtils.EPSILON);
		
		//no unwanted key in the map
		Assert.assertEquals("something in the converted ColdMap that does not belong here", ColdPollutant.values().length, convertedColdMap.size());
		
	}
	
		
	}
	
	
}
	
