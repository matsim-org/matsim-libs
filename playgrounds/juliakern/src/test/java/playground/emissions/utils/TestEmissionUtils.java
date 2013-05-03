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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.utils.EmissionUtils;

//test for playground.vsp.emissions.utils.EmissionUtils

public class TestEmissionUtils {
	
	String co="CO", c2="CO2_TOTAL", fc = "FC", hc= "HC",
			nm ="NMHC", n2="NO2", nx="NOX", pm="PM", so="SO2";
	
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
		
		//local list of pollutants (used here for the test) matches eu.getListOfPolllutants
		//if this fails there is a problem with the test, not with tested class
		for(String pollutant: euPollutants){
			Assert.assertTrue("the list of pollutants used in this test is not correct",localPolls.contains(pollutant));
		}
		//sind alle pollutants aus aus der lokalen Liste auch in der euPollutants-Liste?
		Assert.assertEquals("the list of pollutants used in this test is not correct", localPolls.size(), euPollutants.size());
	}
	
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
		
		//TODO names vs values
		
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
	
	@Test
	public final void testGetTotalEmissions(){
		EmissionUtils eu = new EmissionUtils();
		boolean nullPointer = false;

		//besser anderes Format? ArrayList?
		SortedSet<String> listOfPollutants = new TreeSet<String>();
		fillPollutant(listOfPollutants);
		
		//hashmap ist keine sorted map... warum brauchen wir sorted?
		SortedMap<String, Double> totalEmissions = new TreeMap<String, Double>();
		//enthaelt mehrere personen mit ihren emissions-maps
		Map<Id, SortedMap<String, Double>> persons2emissions = new HashMap<Id, SortedMap<String, Double>>();
		
		//test empty input
		try{
			totalEmissions= eu.getTotalEmissions(null);
		}
		catch(NullPointerException e){
			nullPointer = true;
		}
		Assert.assertTrue(nullPointer); nullPointer =false;
		
		//test empty list as input		
		totalEmissions = eu.getTotalEmissions(persons2emissions);
		Assert.assertEquals("this map should be empty", 0, totalEmissions.size());
		
		//put some content into the list
		//keine Fehlerhaften Eingabedaten hier
		//warme und kalte Emissionen sind schon zusammengerechnet.. dafuer gibt es sumUpEmissionsPerId
		
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
		
		//personen in die persons2emissionliste einfuegen
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
		
		//das geht davon aus, dass immer vollstaendige maps uebergeben werden
		for(String emission : listOfPollutants){
			Assert.assertTrue(totalEmissions.containsKey(emission));
		}
		//nichts anderes in der Liste enthalten
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

	/*
	 * 	public Map<Id, SortedMap<String, Double>> setNonCalculatedEmissionsForPopulation(Population population, Map<Id, SortedMap<String, Double>> totalEmissions) {
		Map<Id, SortedMap<String, Double>> personId2Emissions = new HashMap<Id, SortedMap<String, Double>>();

		for(Person person : population.getPersons().values()){
			Id personId = person.getId();
			SortedMap<String, Double> emissionType2Value;
			if(totalEmissions.get(personId) == null){ // person not in map yet (e.g. pt user)
				emissionType2Value = new TreeMap<String, Double>();
				for(String pollutant : listOfPollutants){
					emissionType2Value.put(pollutant, 0.0);
				}
			} else { // person in map, but some emissions are not set; setting these to 0.0 
				emissionType2Value = totalEmissions.get(personId);
				for(String pollutant : listOfPollutants){ 
					if(emissionType2Value.get(pollutant) == null){
						emissionType2Value.put(pollutant, 0.0);
					} else {
						// do nothing
					}
				}
			}
			personId2Emissions.put(personId, emissionType2Value);
		}
		return personId2Emissions;
	}
	 */
	
	//prob no need - there should be no in- or out-files!
	//@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	@Test
	public final void testSetNonCalculatedEmissionsForPopulation(){
		//IN: population
		//		map <personen-id, persoenlicheEmissionsmap>
		//		persoenlicheEmissionsmap = <Emissionsname, EmissionsWert>
		
		//OUT: returnt eine grosse Map: <personen-Id, persoenlicheEmissionsmap>
		//persoenlicheEmissionsmap = <EmissionsName, EmissionsWert>
		
		EmissionUtils eu = new EmissionUtils();
		SortedSet<String> localPolls = new TreeSet<String>();
		fillPollutant(localPolls);
		Map<Id, SortedMap<String, Double>> totalEmissions = new TreeMap<Id, SortedMap<String, Double>>();
		
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		//Network network = sc.getNetwork();
		//TODO kann ich ne Population ohne config und scenario haben?
		Population pop = sc.getPopulation();
		PopulationFactory populationFactory = pop.getFactory();
		
		
		
//		Id id = new IdImpl("1");
//		Person person = populationFactory.createPerson(id);
//		pop.addPerson(person);

		//TODO logger?

		//korrekte Daten: zwei Personen, jeweils vollstaendige Daten.
		//dann sollte nichts auf Null gesetzt werden. 
		
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
		//TODO werte hier aendern
		Double cov2=.0005, c2v2= .003, fcv2 = .01, hcv2=.2, nmv2=1., n2v2=30., nxv2=200., pmv2 =7000., sov2=70000.;
		
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
		
		//check: alle Personen der Population sind auch in der finalMap
		Assert.assertTrue(finalMap.containsKey(idp1));
		Assert.assertTrue(finalMap.containsKey(idp2));
		
		//check: alle Werte fuer Person 1 und 2 sind ungleich null und ungleich 0, insbesondere sind es Double
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
		}
		
		//TODO ....
		
		//population stimmt nicht mit id-map ueberein: jemand fehlt/ist zu viel
		
		//leere pEM
		
		//leere grosse Map
		
		
		
	}
//	@Test
//	public final void testSetNonCalculatedEmissionsForNetwork(){
//		
//	}
	
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
		//TODO cold pollutants: CO, CO2_TOTAL, FC, HC, NMHC, NO2, NOX, PM, SO2
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
	
