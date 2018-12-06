/* *******i**************************************************************** *
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

package org.matsim.contrib.emissions.utils;

import java.util.*;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;


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
	
	private final String co="CO";
    private final String c2="CO2(total)";
    private final String fc = "FC";
    private final String hc= "HC";
    private final String nm ="NMHC";
    private final String n2="NO2";
    private final String nx="NOx";
    private final String pm="PM";
    private final String so="SO2";
	private String message;

	private Map<Id<Person>, SortedMap<String, Double>> totalEmissions;
    private Population pop;
	private PopulationFactory populationFactory;
	private Set<String> pollsFromEU = new HashSet<>(Arrays.asList("CO", "CO2(total)", "FC", "HC", "NMHC", "NOx", "NO2","PM", "SO2"));

	private boolean nullPointerEx;

	
	@Test
	public final void testSumUpEmissions(){
		// test the method EmissionUtils.sumUpEmissions for a complete list of pollutants
		// missing data is not tested here

		Map<String, Double> warmEmissions = new HashMap<>();
		Map<String, Double> coldEmissions = new HashMap<>();
		
		//cold pollutants: CO, FC, HC, NMHC, NO2, NOx, PM
		//warm pollutants: CO, CO2_TOTAL, FC, HC, NMHC, NO2, NOx, PM, SO2
		
		// complete list of all warm and cold pollutants and corresponding values
		double wcov = .0005, wc2v = .003, wfcv=.01, whcv=.2, wnmv=1., wn2v= 30., wnxv=200., wpmv= 7000., wsov=70000.;
		warmEmissions.put("CO", wcov);
		warmEmissions.put("CO2(total)", wc2v);
		warmEmissions.put("FC", wfcv);
		warmEmissions.put("HC", whcv);
		warmEmissions.put("NMHC", wnmv);
		warmEmissions.put("NO2", wn2v);
		warmEmissions.put("NOx", wnxv);
		warmEmissions.put("PM", wpmv);
		warmEmissions.put("SO2", wsov);
		
		double ccov=.0003, cfcv=.06, chcv=.7, cnmv=2., cn2v=50., cnxv=400., cpmv=9000.;
		coldEmissions.put("CO", ccov);
		coldEmissions.put("FC", cfcv);
		coldEmissions.put("HC", chcv);
		coldEmissions.put("NMHC", cnmv);
		coldEmissions.put("NO2", cn2v);
		coldEmissions.put("NOx", cnxv);
		coldEmissions.put("PM", cpmv);
		
		Map<String, Double> sum = EmissionUtils.sumUpEmissions(warmEmissions, coldEmissions);

		Double cov = sum.get(co);
		Double c2v = sum.get(c2);
		Double fcv = sum.get(fc);
		Double hcv = sum.get(hc);
		Double nmv = sum.get(nm);
		Double n2v = sum.get(n2);
		Double nxv = sum.get(nx);
		Double pmv = sum.get(pm);
		Double sov = sum.get(so);
		
		Assert.assertEquals("Value of CO should be " +(wcov+ccov), cov, wcov+ccov, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of CO2_TOTAL should be " +wc2v, c2v, wc2v, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of FC should be " + (wfcv+cfcv), fcv, wfcv+cfcv, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of HC should be " + (whcv+chcv), hcv, whcv+chcv, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of NMHC should be " + (wnmv+cnmv), nmv, wnmv+cnmv, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of NO2 should be " + (wn2v+cn2v), n2v, wn2v+cn2v, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of NOx should be " + (wnxv+cnxv), nxv, wnxv+cnxv, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of PM should be ." + (wpmv+cpmv), pmv, wpmv+cpmv, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Value of SO2 should be "+ wsov, sov, wsov, MatsimTestUtils.EPSILON);	
	}
	
	@Test 
	public final void testSumUpEmissionsPerId(){
		Map<Id<Person>, Map<String, Double>> warmEmissions = new HashMap<>();
		Map<Id<Person>, Map<String, Double>> coldEmissions = new HashMap<>();
		
		//warm list for person1, warm list for person2, cold list for person1, cold list for person2
		Map<String, Double> mapWarm1 = new HashMap<>();
		Map<String, Double> mapWarm2 = new HashMap<>();
		Map<String, Double> mapCold1 = new HashMap<>();
		Map<String, Double> mapCold2 = new HashMap<>();
		
		//what about negativ numbers? ok
		mapWarm1.put("CO", .0002);
		mapWarm1.put("CO2(total)", .004);
		mapWarm1.put("FC", .03);
		mapWarm1.put("HC", .8);
		mapWarm1.put("NMHC", 7.0);
		mapWarm1.put("NO2", 60.0);
		mapWarm1.put("NOx", 200.0);
		mapWarm1.put("PM", 7000.0);
		mapWarm1.put("SO2", 70000.0);
		
		mapCold1.put("CO", .0008);
		mapCold1.put("FC", .07);
		mapCold1.put("HC", .9);
		mapCold1.put("NMHC", 3.0);
		mapCold1.put("NO2", 40.0);
		mapCold1.put("NOx", 300.0);
		mapCold1.put("PM", 8000.0);
		
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
		
		mapWarm2.put("CO", .0006);
		mapWarm2.put("CO2(total)", .006);
		mapWarm2.put("FC", .08);
		mapWarm2.put("HC", .2);
		mapWarm2.put("NMHC", 4.0);
		mapWarm2.put("NO2", 80.0);
		mapWarm2.put("NOx", 400.0);
		mapWarm2.put("PM", 4000.0);
		mapWarm2.put("SO2", 60000.0);
		
		mapCold2.put("CO", .0009);
		mapCold2.put("FC", .06);
		mapCold2.put("HC", .4);
		mapCold2.put("NMHC", 2.0);
		mapCold2.put("NO2", 70.0);
		mapCold2.put("NOx", 100.0);
		mapCold2.put("PM", 2000.0);
		
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
		
		warmEmissions.put(Id.create("id1", Person.class), mapWarm1);
		warmEmissions.put(Id.create("id2", Person.class), mapWarm2);
		coldEmissions.put(Id.create("id1", Person.class), mapCold1);
		coldEmissions.put(Id.create("id2", Person.class), mapCold2);
		
		Map<Id<Person>, Map<String, Double>> sums = EmissionUtils.sumUpEmissionsPerId(warmEmissions, coldEmissions);
		
		//actual numbers of person1
		double a1co = sums.get(Id.create("id1", Person.class)).get("CO");
		double a1c2 = sums.get(Id.create("id1", Person.class)).get("CO2(total)");
		double a1fc = sums.get(Id.create("id1", Person.class)).get("FC");
		double a1hc = sums.get(Id.create("id1", Person.class)).get("HC");
		double a1nm = sums.get(Id.create("id1", Person.class)).get("NMHC");
		double a1n2 = sums.get(Id.create("id1", Person.class)).get("NO2");
		double a1nx = sums.get(Id.create("id1", Person.class)).get("NOx");
		double a1pm = sums.get(Id.create("id1", Person.class)).get("PM");
		double a1so = sums.get(Id.create("id1", Person.class)).get("SO2");
		
		//actual numbers of person2
		double a2co = sums.get(Id.create("id2", Person.class)).get("CO");
		double a2c2 = sums.get(Id.create("id2", Person.class)).get("CO2(total)");
		double a2fc = sums.get(Id.create("id2", Person.class)).get("FC");
		double a2hc = sums.get(Id.create("id2", Person.class)).get("HC");
		double a2nm = sums.get(Id.create("id2", Person.class)).get("NMHC");
		double a2n2 = sums.get(Id.create("id2", Person.class)).get("NO2");
		double a2nx = sums.get(Id.create("id2", Person.class)).get("NOx");
		double a2pm = sums.get(Id.create("id2", Person.class)).get("PM");
		double a2so = sums.get(Id.create("id2", Person.class)).get("SO2");
		
		//assures simultaneously that persons/ids are distinguished correctly
		Assert.assertEquals("CO value of person 1 should be" +e1co +"but is ", e1co, a1co, MatsimTestUtils.EPSILON);
		Assert.assertEquals("CO2 value of person 1 should be" +e1c2 +"but is ", e1c2, a1c2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("FC value of person 1 should be" +e1fc +"but is ", e1fc, a1fc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("HC value of person 1 should be" +e1hc +"but is ", e1hc, a1hc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NMHC value of person 1 should be" +e1nm +"but is ", e1nm, a1nm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NO2 value of person 1 should be" +e1n2 +"but is ", e1n2, a1n2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NOx value of person 1 should be" +e1nx +"but is ", e1nx, a1nx, MatsimTestUtils.EPSILON);
		Assert.assertEquals("PM value of person 1 should be" +e1pm +"but is ", e1pm, a1pm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("SO value of person 1 should be" +e1so +"but is ", e1so, a1so, MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("CO value of person 2 should be" +e2co +"but is ", e2co, a2co, MatsimTestUtils.EPSILON);
		Assert.assertEquals("CO2 value of person 2 should be" +e2c2 +"but is ", e2c2, a2c2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("FC value of person 2 should be" +e2fc +"but is ", e2fc, a2fc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("HC value of person 2 should be" +e2hc +"but is ", e2hc, a2hc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NMHC value of person 2 should be" +e2nm +"but is ", e2nm, a2nm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NO2 value of person 2 should be" +e2n2 +"but is ", e2n2, a2n2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NOx value of person 2 should be" +e2nx +"but is ", e2nx, a2nx, MatsimTestUtils.EPSILON);
		Assert.assertEquals("PM value of person 2 should be" +e2pm +"but is ", e2pm, a2pm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("SO value of person 2 should be" +e2so +"but is ", e2so, a2so, MatsimTestUtils.EPSILON);

	}
	
	@Test
	public final void testGetTotalEmissions_nullInput(){

		try{
			SortedMap<String, Double> totalEmissions = EmissionUtils.getTotalEmissions(null);
			Assert.fail("Expected NullPointerException, got none.");
		}
		catch(NullPointerException e){
			// as expected
		}
	}
	
	@Test
	public final void testGetTotalEmissions_emptyList(){
		//test an empty list as input

		SortedMap<String, Double> totalEmissions = new TreeMap<>();
		Map<Id<Person>, SortedMap<String, Double>> persons2emissions = new HashMap<>();
		
		//test empty list as input		
		totalEmissions = EmissionUtils.getTotalEmissions(persons2emissions);
		Assert.assertEquals("this map should be empty", 0, totalEmissions.size());
	}
	
	@Test
	public final void testGetTotalEmissions_completeData(){
		//test getTotalEmissions for complete data
		Set<String> pollsFromEU = new HashSet<>(Arrays.asList("CO", "CO2(total)", "FC", "HC", "NMHC", "NOx", "NO2","PM", "SO2"));


		SortedMap<String, Double> totalEmissions = new TreeMap<>();
		Map<Id<Person>, SortedMap<String, Double>> persons2emissions = new HashMap<>();
		
		//put some content into the list
		// no incorrect/incomplete input data here
		// warm and cold emissions are already sumed up -> sumUpEmissionsPerId is tested seperatly
		
		//person1
		SortedMap<String, Double> allEmissionsP1 = new TreeMap<>();
		Double p1co = .9, p1c2 = 3.2, p1fc=9.3, p1hc= 1.0, p1nm=-68., p1n2= .87, p1nx= 5., p1pm = 3.22, p1so=79.8;
		Id<Person> p1Id = Id.create("p1", Person.class);
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
		SortedMap<String, Double> allEmissionsp2 = new TreeMap<>();
		Double p2co = .65, p2c2= -7., p2fc=-.3149, p2hc=54., p2nm=7.9, p2n2=.34, p2nx=-.8, p2pm=4., p2so=-750.;
		Id<Person> p2Id = Id.create("p2", Person.class);
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
		SortedMap<String, Double> allEmissionsp3 = new TreeMap<>();
		Double p3co=-970., p3c2=-.000012, p3fc=57.21, p3hc=80.8, p3nm=9.52, p3n2=.0074, p3nx=42., p3pm=.38, p3so=70.;
		Id<Person> p3Id = Id.create("p3", Person.class);
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
		totalEmissions = EmissionUtils.getTotalEmissions(persons2emissions);
		
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
		for(String emission : pollsFromEU){
			Assert.assertTrue(totalEmissions.containsKey(emission));
		}
		// nothing else in the list
		Assert.assertEquals("this list should be as long as number of pollutants",totalEmissions.keySet().size(), pollsFromEU.size());
		
	}
	
	@Test
	public final void testSetNonCalculatedEmissionsForPopulation_completeData(){
		//test setNonCalculatedEmissionsForPopulation for three persons with complete lists of emissions
		//check values
		
		//IN: population
		//		map <person id, personal emission map>
		//		personal emission map = <name of emission, value of emission>
		
		//OUT: returns a big map: <person id, personal emission map>
		//personal emission map = <name of emission, value of emission>
		
		setUpForNonCaculatedEmissions();
		
		//correct data: two persons with complete data
		//nothing should be set to zero 
		
		//person1
		Double cov1=.0005, c2v1= .003, fcv1 = .01, hcv1=.2, nmv1=1., n2v1=30., nxv1=200., pmv1 =7000., sov1=70000.;
		
		SortedMap<String, Double> p1Emissions = new TreeMap<>();
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
		
		Id<Person> idp1 = Id.create("p1", Person.class);
		Person p1 = populationFactory.createPerson(idp1);
		pop.addPerson(p1);
		totalEmissions.put(idp1, p1Emissions);
		
		//person2
		Double cov2=.0007, c2v2= .006, fcv2 = .04, hcv2=.5, nmv2=7., n2v2=60., nxv2=800., pmv2 =1000., sov2=90000.;
		
		SortedMap<String, Double> p2Emissions = new TreeMap<>();
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
		
		Id<Person> idp2 = Id.create("p2", Person.class);
		Person p2 = populationFactory.createPerson(idp2);
		pop.addPerson(p2);
		totalEmissions.put(idp2, p2Emissions);
		
		Map<Id<Person>, SortedMap<String, Double>> finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, totalEmissions, pollsFromEU );
		
		//check: all persons added to the population are contained in the finalMap
		Assert.assertTrue("the calculated map should contain person 1", finalMap.containsKey(idp1));
		Assert.assertTrue("the calculated map should contain person 2", finalMap.containsKey(idp2));
		//nothing else in the finalMap
		Assert.assertEquals("the calculated map should contain two persons but contains "+
		finalMap.size() + "persons." ,pop.getPersons().keySet().size(), finalMap.size());
		
		//check: all values for person 1 and 2 are not null or zero
		// and of type double
		for(Object id : finalMap.keySet()){
			Assert.assertTrue(id instanceof Id);
			for(Object pollutant: finalMap.get(id).values()){
				Assert.assertTrue(pollutant.getClass()==Double.class);
				Assert.assertNotSame(0.0, pollutant);
				Assert.assertNotNull(pollutant);
			}
			//check: all emission types appear
			for(String emission : pollsFromEU){
				Assert.assertTrue(finalMap.get(id).containsKey(emission));
			}
			//nothing else in the list
			int numOfPolls = pollsFromEU.size();
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
		Assert.assertEquals("NOx value for person 1 is not correct", nxv1, finalMap.get(idp1).get(nx), MatsimTestUtils.EPSILON);
		Assert.assertEquals("PM value for person 1 is not correct", pmv1, finalMap.get(idp1).get(pm), MatsimTestUtils.EPSILON);
		Assert.assertEquals("SO value for person 1 is not correct", sov1, finalMap.get(idp1).get(so), MatsimTestUtils.EPSILON);
		
		//check: values for all emissions are correct -person 2
		Assert.assertEquals("CO value for person 2 is not correct", cov2, finalMap.get(idp2).get(co), MatsimTestUtils.EPSILON);	
		Assert.assertEquals("CO2 value for person 2 is not correct", c2v2, finalMap.get(idp2).get(c2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("FC value for person 2 is not correct", fcv2, finalMap.get(idp2).get(fc), MatsimTestUtils.EPSILON);
		Assert.assertEquals("HC value for person 2 is not correct", hcv2, finalMap.get(idp2).get(hc), MatsimTestUtils.EPSILON);
		Assert.assertEquals("NMHC value for person 2 is not correct", nmv2, finalMap.get(idp2).get(nm), MatsimTestUtils.EPSILON);
		Assert.assertEquals("NO2 value for person 2 is not correct", n2v2, finalMap.get(idp2).get(n2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("NOx value for person 2 is not correct", nxv2, finalMap.get(idp2).get(nx), MatsimTestUtils.EPSILON);
		Assert.assertEquals("PM value for person 2 is not correct", pmv2, finalMap.get(idp2).get(pm), MatsimTestUtils.EPSILON);
		Assert.assertEquals("SO value for person 2 is not correct", sov2, finalMap.get(idp2).get(so), MatsimTestUtils.EPSILON);
				
	}
	
	@Test
	public final void testSetNonCalculatedEmissionsForPopulation_missingMap(){
		
		setUpForNonCaculatedEmissions();
		
		//person 3 in population but its emission map is missing (e.g. not in totalEmissions)
				Id<Person> idp3 = Id.create("p3", Person.class);
				Person p3 = populationFactory.createPerson(idp3);
				pop.addPerson(p3);
				Map<Id<Person>, SortedMap<String, Double>> finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, totalEmissions, pollsFromEU);
				
				//check: person 3 is contained in the finalMap
				Assert.assertTrue("the calculated map should contain person 3", finalMap.containsKey(idp3));				
				//nothing else in the finalMap
				message = "the calculated map should contain "+ pop.getPersons().size()+ " person(s) but contains "+ finalMap.keySet().size() + "person(s)." ;
				Assert.assertEquals(message, pop.getPersons().keySet().size(), finalMap.keySet().size());
				
				//check: all values for the this person are zero and of type double
					for(Object pollutant: finalMap.get(idp3).values()){
						Assert.assertTrue(pollutant.getClass()==Double.class);
						Assert.assertEquals(0.0, (Double)pollutant, MatsimTestUtils.EPSILON);
						Assert.assertNotNull(pollutant);
					}
					//check: all types of emissions appear
					for(String emission : pollsFromEU){
						Assert.assertTrue(finalMap.get(idp3).containsKey(emission));
					}
					//nothing else in the list
        int numOfPolls = pollsFromEU.size();
					message = "the number of pullutants is " + finalMap.get(idp3).keySet().size()+ " but should be" + numOfPolls;
					Assert.assertEquals(message, numOfPolls, finalMap.get(idp3).keySet().size());
				
	}
	
	@Test
	public final void testSetNonCalculatedEmissionsForPopulation_missingPerson(){
		
		setUpForNonCaculatedEmissions();
		
		Map<Id<Person>, SortedMap<String, Double>> finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, totalEmissions, pollsFromEU);
		
		//person 4 in totalEmissions but not in population
					Double cov4=.0008, c2v4= .004, fcv4 = .07, hcv4=.9, nmv4=1., n2v4=50., nxv4=700., pmv4 =4000., sov4=30000.;
					
					SortedMap<String, Double> p4Emissions = new TreeMap<>();
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
					
					Id<Person> idp4 = Id.create("p4", Person.class);
					totalEmissions.put(idp4, p4Emissions);
					
					finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, totalEmissions, pollsFromEU);
					
					//check: all persons added to the population are contained in the finalMap
					Assert.assertFalse("the calculated map should not contain person 4", finalMap.containsKey(idp4));				
					//nothing else in the finalMap
					message = "the calculated map should contain "+ pop.getPersons().size()+ " person(s) but contains "+ finalMap.keySet().size() + "person(s)." ;
					Assert.assertEquals(message, pop.getPersons().keySet().size(), finalMap.keySet().size());
						
	}
	
	private void setUpForNonCaculatedEmissions() {
		//intern method to set some parameters
		totalEmissions = new TreeMap<>();
        Config config = ConfigUtils.createConfig();
        Scenario sc = ScenarioUtils.createScenario(config);
		pop = sc.getPopulation();
		populationFactory = pop.getFactory();
		pollsFromEU = new HashSet<>(Arrays.asList("CO", "CO2(total)", "FC", "HC", "NMHC", "NOx", "NO2","PM", "SO2"));
		;
		nullPointerEx = false;
	}
	
	@Test
	public final void testSetNonCalculatedEmissionsForPopulation_emptyEmissionMap(){
		//test setNonCalculatedEmissionsForPopulation with an empty emission map
		setUpForNonCaculatedEmissions();
		
		Id<Person> idp5 = Id.create("p5", Person.class);
		Person p5 = populationFactory.createPerson(idp5);
		pop.addPerson(p5);
		Id<Person> idp6 = Id.create("p6", Person.class);
		Person p6 = populationFactory.createPerson(idp6);
		pop.addPerson(p6);

		//empty emissions map
		Map<Id<Person>, SortedMap<String, Double>> finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, totalEmissions, pollsFromEU);
		
		//check: all persons added to the population are contained in the finalMap
		Assert.assertTrue("the calculated map should contain person 5", finalMap.containsKey(idp5));
		Assert.assertTrue("the calculated map should contain person 6", finalMap.containsKey(idp6));
		//nothing else in the finalMap
		message = "the calculated map should contain "+ pop.getPersons().size()+ " person(s) but contains "+ finalMap.keySet().size() + "person(s)." ;
		Assert.assertEquals(message, pop.getPersons().keySet().size(), finalMap.keySet().size());
							
		//check: all values for all persons are zero and of type double
		for(Object id : finalMap.keySet()){
							for(Object pollutant: finalMap.get(id).values()){
								Assert.assertTrue(pollutant.getClass()==Double.class);
								Assert.assertEquals("map of pollutants was missing. Therefore all values should be set to zero.", 
										0.0, (Double)pollutant, MatsimTestUtils.EPSILON);
								Assert.assertNotNull(pollutant);
							}
							//check: alle types of emissions appear
							for(String emission : pollsFromEU){
								Assert.assertTrue(finalMap.get(id).containsKey(emission));
							}
							//nothing else in the list
							int numOfPolls = pollsFromEU.size();
							Assert.assertEquals("the number of pullutants is " + finalMap.get(id).keySet().size()+ " but should be" + numOfPolls, 
									numOfPolls, finalMap.get(id).keySet().size());


		}

	}
	
	@Test
	public final void testSetNonCalculatedEmissionsForPopulation_nullEmissions(){
		//test setNonCalculatedEmissionsForPopulation with 'null'
		// throw nullpointer exception
		setUpForNonCaculatedEmissions();
		
		Id<Person> idp5 = Id.create("p5", Person.class);
		Person p5 = populationFactory.createPerson(idp5);
		pop.addPerson(p5);
		Id<Person> idp6 = Id.create("p6", Person.class);
		Person p6 = populationFactory.createPerson(idp6);
		pop.addPerson(p6);

		try {
		//empty emissions map
		Map<Id<Person>, SortedMap<String, Double>> finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, null, pollsFromEU);
		} catch (NullPointerException e) {
			nullPointerEx = true;
		}
		Assert.assertTrue(nullPointerEx);
	}
	
	@Test
	public final void testSetNonCalculatedEmissionsForPopulation_emptyPopulation(){
		// test setNonCalculatedEmissionsForPopulation with an empty population
		// empty list should be returned
		setUpForNonCaculatedEmissions();

		//person 7 in totalEmissions but not in population		
		SortedMap<String, Double> p7Emissions = new TreeMap<>();
		//complete list of all pollutants - missing data is not tested here
		p7Emissions.put(co, .0);
		p7Emissions.put(c2, .0);
		p7Emissions.put(fc, .0);
		p7Emissions.put(hc, .0);
		p7Emissions.put(nm, .0);
		p7Emissions.put(n2, .0);
		p7Emissions.put(nx, .0);
		p7Emissions.put(pm, .0);
		p7Emissions.put(so, .0);
		
		Id<Person> idp7 = Id.create("p7", Person.class);
		totalEmissions.put(idp7, p7Emissions);
		
		//empty population
		Map<Id<Person>, SortedMap<String, Double>> finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, totalEmissions, pollsFromEU);
		
		//nothing in the finalMap
		message = "the calculated map should contain "+ pop.getPersons().size()+ " person(s) but contains "+ finalMap.keySet().size() + "person(s)." ;
		Assert.assertEquals(message, pop.getPersons().keySet().size(), finalMap.keySet().size());

	}

	@Test 
	public final void testSetNonCalculatedEmissionsForNetwork(){
		//test setNonCalculatedEmissionsForNetwork
		// network consists of four nodes 1,2,3,4
		// and six links 12, 13, 14, 23, 24, 34
		// link 12 and 13: complete emission map -> check values
		// link 14: no emission map - 'null' -> create map and set to 0.0
		// link 23: some but not all pollutants in emission map -> check values or set to 0.0
		// link 24: empty emission map -> set values to 0.0
		// link 34: no emission map added to the list of link-emission-maps -> create map and set to 0.0
		
		//IN: network,totalEmissions 
		//totalEmissions = map <linkId, SpecificEmissionMap>
		
		//OUT: map <linkId, SpecificEmissionMap>
		//SpecificEmissionMap= <pollutant, value>
		

		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network network = sc.getNetwork();
		addLinksToNetwork(sc);

		Map<Id<Link>, SortedMap<String, Double>> totalEmissions = new HashMap<>();
	
		//complete link - link12
		Id<Link> link12id = Id.create("link12", Link.class);
		SortedMap<String, Double> emLink12 = new TreeMap<>();
		Double c2link12v=.0008, colink12v=.001, fclink12v=.05,
				hclink12v=.8, nmlink12v=1., n2link12v=50.,
				nxlink12v=600., pmlink12v=2000., solink12v=60000.;
		emLink12.put(c2, c2link12v); emLink12.put(co, colink12v); emLink12.put(fc, fclink12v); 
		emLink12.put(hc, hclink12v); emLink12.put(nm, nmlink12v); emLink12.put(n2, n2link12v); 
		emLink12.put(nx, nxlink12v); emLink12.put(pm, pmlink12v); emLink12.put(so, solink12v);
		totalEmissions.put(link12id, emLink12 );
		
		//complete link - link13
		Id<Link> link13id = Id.create("link13", Link.class);
		SortedMap<String, Double> emLink13 = new TreeMap<>();
		Double c2link13v=.0003, colink13v=.008, fclink13v=.03,
				hclink13v=.7, nmlink13v=6., n2link13v=40.,
				nxlink13v=800., pmlink13v=1000., solink13v=90000.;
		emLink13.put(c2, c2link13v); emLink13.put(co, colink13v); emLink13.put(fc, fclink13v); 
		emLink13.put(hc, hclink13v); emLink13.put(nm, nmlink13v); emLink13.put(n2, n2link13v); 
		emLink13.put(nx, nxlink13v); emLink13.put(pm, pmlink13v); emLink13.put(so, solink13v);
		totalEmissions.put(Id.create("link13", Link.class), emLink13 );
		
		//missing map - link14
		Id<Link> link14id = Id.create("link14", Link.class);
		totalEmissions.put(Id.create("link14", Link.class), null);
		
		//partial map - link 23
		Id<Link> link23id = Id.create("link23", Link.class);
		SortedMap<String, Double> emLink23 = new TreeMap<>();
		Double nxlink23v=900., pmlink23v=6000., solink23v=20000.;
		emLink23.put(nx, nxlink23v); emLink23.put(pm, pmlink23v); emLink23.put(so, solink23v);
		totalEmissions.put(Id.create("link23", Link.class), emLink23 );
		
		//empty map - link 24
		Id<Link> link24id = Id.create("link24", Link.class);
		SortedMap<String, Double> emLink24 = new TreeMap<>();
		totalEmissions.put(Id.create("link24", Link.class), emLink24);
		
		//not put into totalEmissionsMap - link 34
		Id<Link> link34id = Id.create("link34", Link.class);
		
		Map<Id<Link>, SortedMap<String, Double>> totalEmissionsFilled = EmissionUtils.setNonCalculatedEmissionsForNetwork(network, totalEmissions, pollsFromEU);
		//each link of the network and each type of emission
		for(Link link: network.getLinks().values()){
			
			Id<Link> linkId = link.getId();

				Assert.assertTrue(totalEmissionsFilled.containsKey(linkId));
				SortedMap<String, Double> emissionMapForLink = totalEmissionsFilled.get(linkId);
				for(String pollutant: pollsFromEU){
					System.out.println("pollutant: " + pollutant + "; linkId: " + linkId);
					Assert.assertTrue(pollutant + "not found for link " +linkId.toString(), 
							emissionMapForLink.containsKey(pollutant));
					Assert.assertEquals(Double.class, emissionMapForLink.get(pollutant).getClass());
				
			}		
		}
		//check values
		//link 12 and 13
		Assert.assertEquals(totalEmissionsFilled.get(link12id).get(c2), c2link12v, MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link12id).get(co), colink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link12id).get(fc), fclink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link12id).get(hc), hclink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link12id).get(nm), nmlink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link12id).get(n2), n2link12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link12id).get(nx), nxlink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link12id).get(pm), pmlink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link12id).get(so), solink12v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link13id).get(c2), c2link13v, MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link13id).get(co), colink13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link13id).get(fc), fclink13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link13id).get(hc), hclink13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link13id).get(nm), nmlink13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link13id).get(n2), n2link13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link13id).get(nx), nxlink13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link13id).get(pm), pmlink13v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link13id).get(so), solink13v,  MatsimTestUtils.EPSILON);
		
		//link 14 and 34
		for(String pollutant: pollsFromEU){
			Assert.assertEquals(totalEmissionsFilled.get(link14id).get(pollutant), .0, MatsimTestUtils.EPSILON);
			Assert.assertEquals(totalEmissionsFilled.get(link34id).get(pollutant), .0, MatsimTestUtils.EPSILON);
		}
		
		//link 23 - partial
		Assert.assertEquals(totalEmissionsFilled.get(link23id).get(c2), .0, MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link23id).get(co), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link23id).get(fc), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link23id).get(hc), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link23id).get(nm), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link23id).get(n2), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link23id).get(nx), nxlink23v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link23id).get(pm), pmlink23v,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link23id).get(so), solink23v,  MatsimTestUtils.EPSILON);
		
		//link 24 - empty
		Assert.assertEquals(totalEmissionsFilled.get(link24id).get(c2), .0, MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link24id).get(co), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link24id).get(fc), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link24id).get(hc), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link24id).get(nm), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link24id).get(n2), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link24id).get(nx), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link24id).get(pm), .0,  MatsimTestUtils.EPSILON);
		Assert.assertEquals(totalEmissionsFilled.get(link24id).get(so), .0,  MatsimTestUtils.EPSILON);
		
	}
	
	private void addLinksToNetwork(Scenario sc) {
		//intern method to set up a network with nodes and links
		Network network = (Network) sc.getNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("node1", Node.class), new Coord(.0, .0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("node2", Node.class), new Coord(.0, 1000.));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("node3", Node.class), new Coord(1000., .0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("node4", Node.class), new Coord(1000., 1000.));
		final Node fromNode = node1;
		final Node toNode = node2;
		
		NetworkUtils.createAndAddLink(network,Id.create("link12", Link.class), fromNode, toNode, 1000., 20., (double) 3600, (double) 2 );
		final Node fromNode1 = node1;
		final Node toNode1 = node3; //w/o orig id and type
		NetworkUtils.createAndAddLink(network,Id.create("link13", Link.class), fromNode1, toNode1, 1000., 20., (double) 3600, (double) 2 );
		final Node fromNode2 = node1;
		final Node toNode2 = node4; //w/o orig id and type
		NetworkUtils.createAndAddLink(network,Id.create("link14", Link.class), fromNode2, toNode2, 1000., 20., (double) 3600, (double) 2 );
		final Node fromNode3 = node2;
		final Node toNode3 = node3; //w/o orig id and type
		NetworkUtils.createAndAddLink(network,Id.create("link23", Link.class), fromNode3, toNode3, 1000., 20., (double) 3600, (double) 2 );
		final Node fromNode4 = node2;
		final Node toNode4 = node4; //w/o orig id and type
		NetworkUtils.createAndAddLink(network,Id.create("link24", Link.class), fromNode4, toNode4, 1000., 20., (double) 3600, (double) 2 );
		final Node fromNode5 = node3;
		final Node toNode5 = node4; //w/o orig id and type
		NetworkUtils.createAndAddLink(network,Id.create("link34", Link.class), fromNode5, toNode5, 1000., 20., (double) 3600, (double) 2 ); //w/o orig id and type
	}
	
}
	
