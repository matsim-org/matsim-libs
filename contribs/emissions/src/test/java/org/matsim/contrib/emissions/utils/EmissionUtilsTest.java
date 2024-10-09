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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.emissions.EmissionUtils;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.matsim.contrib.emissions.Pollutant.*;


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

public class EmissionUtilsTest {

	private String message;

	private Map<Id<Person>, SortedMap<Pollutant, Double>> totalEmissions;
	private Population pop;
	private PopulationFactory populationFactory;
	private Set<Pollutant> pollsFromEU = new HashSet<>(Arrays.asList(CO, CO2_TOTAL, FC, HC, NMHC, NOx, NO2, PM, SO2));

	private boolean nullPointerEx;

	public static Map<Pollutant, Double> createUntypedEmissions() {
		return Stream.of(CO2_TOTAL, CO, NOx, NO2, HC)
				.collect(Collectors.toMap(p -> p, p -> Math.random()));
	}

	@Test
	final void testSumUpEmissions() {
		// test the method EmissionUtils.sumUpEmissions for a complete list of pollutants
		// missing data is not tested here

		Map<Pollutant, Double> warmEmissions = new HashMap<>();
		Map<Pollutant, Double> coldEmissions = new HashMap<>();

		//cold pollutants: CO, FC, HC, NMHC, NO2, NOx, PM
		//warm pollutants: CO, CO2_TOTAL, FC, HC, NMHC, NO2, NOx, PM, SO2

		// complete list of all warm and cold pollutants and corresponding values
		double wcov = .0005, wc2v = .003, wfcv=.01, whcv=.2, wnmv=1., wn2v= 30., wnxv=200., wpmv= 7000., wsov=70000.;
		warmEmissions.put(CO, wcov);
		warmEmissions.put( CO2_TOTAL, wc2v );
		warmEmissions.put(FC, wfcv);
		warmEmissions.put(HC, whcv);
		warmEmissions.put(NMHC, wnmv);
		warmEmissions.put(NO2, wn2v);
		warmEmissions.put(NOx, wnxv);
		warmEmissions.put(PM, wpmv);
		warmEmissions.put(SO2, wsov);

		double ccov=.0003, cfcv=.06, chcv=.7, cnmv=2., cn2v=50., cnxv=400., cpmv=9000.;
		coldEmissions.put(CO, ccov);
		coldEmissions.put(FC, cfcv);
		coldEmissions.put(HC, chcv);
		coldEmissions.put(NMHC, cnmv);
		coldEmissions.put(NO2, cn2v);
		coldEmissions.put(NOx, cnxv);
		coldEmissions.put(PM, cpmv);

		Map<Pollutant, Double> sum = EmissionUtils.sumUpEmissions(warmEmissions, coldEmissions );

		Double cov = sum.get( CO );
		Double c2v = sum.get( CO2_TOTAL );
		Double fcv = sum.get( FC );
		Double hcv = sum.get( HC );
		Double nmv = sum.get( NMHC );
		Double n2v = sum.get( NO2 );
		Double nxv = sum.get( NOx );
		Double pmv = sum.get( PM );
		Double sov = sum.get(SO2);

		Assertions.assertEquals(cov, wcov + ccov, MatsimTestUtils.EPSILON, "Value of CO should be " + (wcov + ccov));
		Assertions.assertEquals(c2v, wc2v, MatsimTestUtils.EPSILON, "Value of CO2_TOTAL should be " + wc2v);
		Assertions.assertEquals(fcv, wfcv + cfcv, MatsimTestUtils.EPSILON, "Value of FC should be " + (wfcv + cfcv));
		Assertions.assertEquals(hcv, whcv + chcv, MatsimTestUtils.EPSILON, "Value of HC should be " + (whcv + chcv));
		Assertions.assertEquals(nmv, wnmv + cnmv, MatsimTestUtils.EPSILON, "Value of NMHC should be " + (wnmv + cnmv));
		Assertions.assertEquals(n2v, wn2v + cn2v, MatsimTestUtils.EPSILON, "Value of NO2 should be " + (wn2v + cn2v));
		Assertions.assertEquals(nxv, wnxv + cnxv, MatsimTestUtils.EPSILON, "Value of NOx should be " + (wnxv + cnxv));
		Assertions.assertEquals(pmv, wpmv + cpmv, MatsimTestUtils.EPSILON, "Value of PM should be ." + (wpmv + cpmv));
		Assertions.assertEquals(sov, wsov, MatsimTestUtils.EPSILON, "Value of SO2 should be " + wsov);
	}

	@Test
	final void testSumUpEmissionsPerId() {
		Map<Id<Person>, Map<Pollutant, Double>> warmEmissions = new HashMap<>();
		Map<Id<Person>, Map<Pollutant, Double>> coldEmissions = new HashMap<>();

		//warm list for person1, warm list for person2, cold list for person1, cold list for person2
		Map<Pollutant, Double> mapWarm1 = new HashMap<>();
		Map<Pollutant, Double> mapWarm2 = new HashMap<>();
		Map<Pollutant, Double> mapCold1 = new HashMap<>();
		Map<Pollutant, Double> mapCold2 = new HashMap<>();

		//what about negativ numbers? ok
		mapWarm1.put(CO, .0002);
		mapWarm1.put(CO2_TOTAL, .004);
		mapWarm1.put(FC, .03);
		mapWarm1.put(HC, .8);
		mapWarm1.put(NMHC, 7.0);
		mapWarm1.put(NO2, 60.0);
		mapWarm1.put(NOx, 200.0);
		mapWarm1.put(PM, 7000.0);
		mapWarm1.put(SO2, 70000.0);

		mapCold1.put(CO, .0008);
		mapCold1.put(FC, .07);
		mapCold1.put(HC, .9);
		mapCold1.put(NMHC, 3.0);
		mapCold1.put(NO2, 40.0);
		mapCold1.put(NOx, 300.0);
		mapCold1.put(PM, 8000.0);

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

		mapWarm2.put(CO, .0006);
		mapWarm2.put(CO2_TOTAL, .006);
		mapWarm2.put(FC, .08);
		mapWarm2.put(HC, .2);
		mapWarm2.put(NMHC, 4.0);
		mapWarm2.put(NO2, 80.0);
		mapWarm2.put(NOx, 400.0);
		mapWarm2.put(PM, 4000.0);
		mapWarm2.put(SO2, 60000.0);

		mapCold2.put(CO, .0009);
		mapCold2.put(FC, .06);
		mapCold2.put(HC, .4);
		mapCold2.put(NMHC, 2.0);
		mapCold2.put(NO2, 70.0);
		mapCold2.put(NOx, 100.0);
		mapCold2.put(PM, 2000.0);

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

		Map<Id<Person>, Map<Pollutant, Double>> sums = EmissionUtils.sumUpEmissionsPerId(warmEmissions, coldEmissions );

		//actual numbers of person1
		double a1co = sums.get(Id.create("id1", Person.class)).get(CO);
		double a1c2 = sums.get(Id.create("id1", Person.class)).get(CO2_TOTAL);
		double a1fc = sums.get(Id.create("id1", Person.class)).get(FC);
		double a1hc = sums.get(Id.create("id1", Person.class)).get(HC);
		double a1nm = sums.get(Id.create("id1", Person.class)).get(NMHC);
		double a1n2 = sums.get(Id.create("id1", Person.class)).get(NO2);
		double a1nx = sums.get(Id.create("id1", Person.class)).get(NOx);
		double a1pm = sums.get(Id.create("id1", Person.class)).get(PM);
		double a1so = sums.get(Id.create("id1", Person.class)).get(SO2);

		//actual numbers of person2
		double a2co = sums.get(Id.create("id2", Person.class)).get(CO);
		double a2c2 = sums.get(Id.create("id2", Person.class)).get(CO2_TOTAL);
		double a2fc = sums.get(Id.create("id2", Person.class)).get(FC);
		double a2hc = sums.get(Id.create("id2", Person.class)).get(HC);
		double a2nm = sums.get(Id.create("id2", Person.class)).get(NMHC);
		double a2n2 = sums.get(Id.create("id2", Person.class)).get(NO2);
		double a2nx = sums.get(Id.create("id2", Person.class)).get(NOx);
		double a2pm = sums.get(Id.create("id2", Person.class)).get(PM);
		double a2so = sums.get(Id.create("id2", Person.class)).get(SO2);

		//assures simultaneously that persons/ids are distinguished correctly
		Assertions.assertEquals(e1co, a1co, MatsimTestUtils.EPSILON, "CO value of person 1 should be" +e1co +"but is ");
		Assertions.assertEquals(e1c2, a1c2, MatsimTestUtils.EPSILON, "CO2 value of person 1 should be" + e1c2 + "but is ");
		Assertions.assertEquals(e1fc, a1fc, MatsimTestUtils.EPSILON, "FC value of person 1 should be" + e1fc + "but is ");
		Assertions.assertEquals(e1hc, a1hc, MatsimTestUtils.EPSILON, "HC value of person 1 should be" + e1hc + "but is ");
		Assertions.assertEquals(e1nm, a1nm, MatsimTestUtils.EPSILON, "NMHC value of person 1 should be" + e1nm + "but is ");
		Assertions.assertEquals(e1n2, a1n2, MatsimTestUtils.EPSILON, "NO2 value of person 1 should be" + e1n2 + "but is ");
		Assertions.assertEquals(e1nx, a1nx, MatsimTestUtils.EPSILON, "NOx value of person 1 should be" + e1nx + "but is ");
		Assertions.assertEquals(e1pm, a1pm, MatsimTestUtils.EPSILON, "PM value of person 1 should be" + e1pm + "but is ");
		Assertions.assertEquals(e1so, a1so, MatsimTestUtils.EPSILON, "SO value of person 1 should be" + e1so + "but is ");

		Assertions.assertEquals(e2co, a2co, MatsimTestUtils.EPSILON, "CO value of person 2 should be" + e2co + "but is ");
		Assertions.assertEquals(e2c2, a2c2, MatsimTestUtils.EPSILON, "CO2 value of person 2 should be" + e2c2 + "but is ");
		Assertions.assertEquals(e2fc, a2fc, MatsimTestUtils.EPSILON, "FC value of person 2 should be" + e2fc + "but is ");
		Assertions.assertEquals(e2hc, a2hc, MatsimTestUtils.EPSILON, "HC value of person 2 should be" + e2hc + "but is ");
		Assertions.assertEquals(e2nm, a2nm, MatsimTestUtils.EPSILON, "NMHC value of person 2 should be" + e2nm + "but is ");
		Assertions.assertEquals(e2n2, a2n2, MatsimTestUtils.EPSILON, "NO2 value of person 2 should be" + e2n2 + "but is ");
		Assertions.assertEquals(e2nx, a2nx, MatsimTestUtils.EPSILON, "NOx value of person 2 should be" + e2nx + "but is ");
		Assertions.assertEquals(e2pm, a2pm, MatsimTestUtils.EPSILON, "PM value of person 2 should be" + e2pm + "but is ");
		Assertions.assertEquals(e2so, a2so, MatsimTestUtils.EPSILON, "SO value of person 2 should be" + e2so + "but is ");

	}

	@Test
	final void testGetTotalEmissions_nullInput() {
		assertThrows(NullPointerException.class, () -> {

			@SuppressWarnings("ConstantConditions")
			SortedMap<Pollutant, Double> totalEmissions = EmissionUtils.getTotalEmissions(null);
			Assertions.fail("Expected NullPointerException, got none.");

		});

	}

	@Test
	final void testGetTotalEmissions_emptyList() {
		//test an empty list as input

		SortedMap<Pollutant, Double> totalEmissions;
		Map<Id<Person>, SortedMap<Pollutant, Double>> persons2emissions = new HashMap<>();

		//test empty list as input
		totalEmissions = EmissionUtils.getTotalEmissions(persons2emissions);
		Assertions.assertEquals(0, totalEmissions.size(), "this map should be empty");
	}

	@Test
	final void testGetTotalEmissions_completeData() {
		//test getTotalEmissions for complete data
		Set<Pollutant> pollsFromEU = new HashSet<>(Arrays.asList(CO, CO2_TOTAL, FC, HC, NMHC, NOx, NO2, PM, SO2));


		SortedMap<Pollutant, Double> totalEmissions;
		Map<Id<Person>, SortedMap<Pollutant, Double>> persons2emissions = new HashMap<>();

		//put some content into the list
		// no incorrect/incomplete input data here
		// warm and cold emissions are already summed up -> sumUpEmissionsPerId is tested separately

		//person1
		SortedMap<Pollutant, Double> allEmissionsP1 = new TreeMap<>();
		Double p1co = .9, p1c2 = 3.2, p1fc = 9.3, p1hc = 1.0, p1nm = -68., p1n2 = .87, p1nx = 5., p1pm = 3.22, p1so = 79.8;
		Id<Person> p1Id = Id.create("p1", Person.class);
		allEmissionsP1.put(CO, p1co);
		allEmissionsP1.put(CO2_TOTAL, p1c2);
		allEmissionsP1.put(FC, p1fc);
		allEmissionsP1.put(HC, p1hc);
		allEmissionsP1.put(NMHC, p1nm);
		allEmissionsP1.put(NO2, p1n2);
		allEmissionsP1.put(NOx, p1nx);
		allEmissionsP1.put(PM, p1pm);
		allEmissionsP1.put(SO2, p1so);

		//person2
		SortedMap<Pollutant, Double> allEmissionsp2 = new TreeMap<>();
		Double p2co = .65, p2c2= -7., p2fc=-.3149, p2hc=54., p2nm=7.9, p2n2=.34, p2nx=-.8, p2pm=4., p2so=-750.;
		Id<Person> p2Id = Id.create("p2", Person.class);
		allEmissionsp2.put( CO, p2co );
		allEmissionsp2.put( CO2_TOTAL, p2c2 );
		allEmissionsp2.put( FC, p2fc );
		allEmissionsp2.put( HC, p2hc );
		allEmissionsp2.put( NMHC, p2nm );
		allEmissionsp2.put( NO2, p2n2 );
		allEmissionsp2.put( NOx, p2nx );
		allEmissionsp2.put( PM, p2pm );
		allEmissionsp2.put( SO2, p2so );

		//person3
		SortedMap<Pollutant, Double> allEmissionsp3 = new TreeMap<>();
		Double p3co=-970., p3c2=-.000012, p3fc=57.21, p3hc=80.8, p3nm=9.52, p3n2=.0074, p3nx=42., p3pm=.38, p3so=70.;
		Id<Person> p3Id = Id.create("p3", Person.class);
		allEmissionsp3.put( CO, p3co );
		allEmissionsp3.put( CO2_TOTAL, p3c2 );
		allEmissionsp3.put( FC, p3fc );
		allEmissionsp3.put( HC, p3hc );
		allEmissionsp3.put( NMHC, p3nm );
		allEmissionsp3.put(NO2, p3n2);
		allEmissionsp3.put(NOx, p3nx);
		allEmissionsp3.put(PM, p3pm);
		allEmissionsp3.put(SO2, p3so);

		// put persons into persons2emission list
		persons2emissions.put(p1Id, allEmissionsP1);
		persons2emissions.put(p2Id, allEmissionsp2);
		persons2emissions.put(p3Id, allEmissionsp3);
		totalEmissions = EmissionUtils.getTotalEmissions(persons2emissions);

		Assertions.assertEquals(p1co + p2co + p3co, totalEmissions.get(CO), MatsimTestUtils.EPSILON, CO + " values are not correct");
		Assertions.assertEquals(p1c2 + p2c2 + p3c2, totalEmissions.get(CO2_TOTAL), MatsimTestUtils.EPSILON, CO2_TOTAL + " values are not correct");
		Assertions.assertEquals(p1fc + p2fc + p3fc, totalEmissions.get(FC), MatsimTestUtils.EPSILON, FC + " values are not correct");
		Assertions.assertEquals(p1hc + p2hc + p3hc, totalEmissions.get(HC), MatsimTestUtils.EPSILON, HC + " values are not correct");
		Assertions.assertEquals(p1nm + p2nm + p3nm, totalEmissions.get(NMHC), MatsimTestUtils.EPSILON, NMHC + " values are not correct");
		Assertions.assertEquals(p1n2 + p2n2 + p3n2, totalEmissions.get(NO2), MatsimTestUtils.EPSILON, NO2 + " values are not correct");
		Assertions.assertEquals(p1nx + p2nx + p3nx, totalEmissions.get(NOx), MatsimTestUtils.EPSILON, NOx + " values are not correct");
		Assertions.assertEquals(p1pm + p2pm + p3pm, totalEmissions.get(PM), MatsimTestUtils.EPSILON, PM + " values are not correct");
		Assertions.assertEquals(p1so + p2so + p3so, totalEmissions.get(SO2), MatsimTestUtils.EPSILON, SO2 + " values are not correct");

		// assume that all maps are complete
		for (Pollutant emission : pollsFromEU) {
			Assertions.assertTrue(totalEmissions.containsKey(emission));
		}
		// nothing else in the list
		Assertions.assertEquals(totalEmissions.keySet().size(), pollsFromEU.size(), "this list should be as long as number of pollutants");

	}

	@Test
	final void testSetNonCalculatedEmissionsForPopulation_completeData(){
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
		double cov1 = .0005, c2v1 = .003, fcv1 = .01, hcv1 = .2, nmv1 = 1., n2v1 = 30., nxv1 = 200., pmv1 = 7000., sov1 = 70000.;

		SortedMap<Pollutant, Double> p1Emissions = new TreeMap<>();
		//complete list of all pollutants - missing data is not tested here
		p1Emissions.put(CO, cov1);
		p1Emissions.put(CO2_TOTAL, c2v1);
		p1Emissions.put(FC, fcv1);
		p1Emissions.put(HC, hcv1);
		p1Emissions.put(NMHC, nmv1);
		p1Emissions.put(NO2, n2v1);
		p1Emissions.put(NOx, nxv1);
		p1Emissions.put(PM, pmv1);
		p1Emissions.put(SO2, sov1);

		Id<Person> idp1 = Id.create("p1", Person.class);
		Person p1 = populationFactory.createPerson(idp1);
		pop.addPerson(p1);
		totalEmissions.put(idp1, p1Emissions);

		//person2
		double cov2 = .0007, c2v2 = .006, fcv2 = .04, hcv2 = .5, nmv2 = 7., n2v2 = 60., nxv2 = 800., pmv2 = 1000., sov2 = 90000.;

		SortedMap<Pollutant, Double> p2Emissions = new TreeMap<>();
		//complete list of all pollutants - missing data is not tested here
		p2Emissions.put(CO, cov2);
		p2Emissions.put(CO2_TOTAL, c2v2);
		p2Emissions.put(FC, fcv2);
		p2Emissions.put(HC, hcv2);
		p2Emissions.put(NMHC, nmv2);
		p2Emissions.put(NO2, n2v2);
		p2Emissions.put(NOx, nxv2);
		p2Emissions.put(PM, pmv2);
		p2Emissions.put(SO2, sov2);

		Id<Person> idp2 = Id.create("p2", Person.class);
		Person p2 = populationFactory.createPerson(idp2);
		pop.addPerson(p2);
		totalEmissions.put(idp2, p2Emissions);

		Map<Id<Person>, SortedMap<Pollutant, Double>> finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, totalEmissions, pollsFromEU);

		//check: all persons added to the population are contained in the finalMap
		Assertions.assertTrue(finalMap.containsKey(idp1), "the calculated map should contain person 1");
		Assertions.assertTrue(finalMap.containsKey(idp2), "the calculated map should contain person 2");
		//nothing else in the finalMap
		Assertions.assertEquals(pop.getPersons().keySet().size(), finalMap.size(), "the calculated map should contain two persons but contains "+
		finalMap.size() + "persons.");

		//check: all values for person 1 and 2 are not null or zero
		// and of type double
		for(Object id : finalMap.keySet()) {
			Assertions.assertInstanceOf(Id.class, id);
			for (Object pollutant : finalMap.get(id).values()) {
				Assertions.assertSame(pollutant.getClass(), Double.class);
				Assertions.assertNotSame(0.0, pollutant);
				Assertions.assertNotNull(pollutant);
			}
			//check: all emission types appear
			for (Pollutant emission : pollsFromEU) {
				Assertions.assertTrue(finalMap.get(id).containsKey(emission));
			}
			//nothing else in the list
			int numOfPolls = pollsFromEU.size();
			Assertions.assertEquals(numOfPolls, finalMap.get(id).keySet().size(), "the number of pollutants is " + finalMap.get(id).keySet().size() + " but should be" + numOfPolls);
		}

		//check: values for all emissions are correct -person 1
		Assertions.assertEquals(cov1, finalMap.get(idp1).get( CO ), MatsimTestUtils.EPSILON, "CO value for person 1 is not correct" );
		Assertions.assertEquals(c2v1, finalMap.get(idp1).get( CO2_TOTAL ), MatsimTestUtils.EPSILON, "CO2 value for person 1 is not correct" );
		Assertions.assertEquals(fcv1, finalMap.get(idp1).get( FC ), MatsimTestUtils.EPSILON, "FC value for person 1 is not correct" );
		Assertions.assertEquals(hcv1, finalMap.get(idp1).get( HC ), MatsimTestUtils.EPSILON, "HC value for person 1 is not correct" );
		Assertions.assertEquals(nmv1, finalMap.get(idp1).get( NMHC ), MatsimTestUtils.EPSILON, "NMHC value for person 1 is not correct" );
		Assertions.assertEquals(n2v1, finalMap.get(idp1).get( NO2 ), MatsimTestUtils.EPSILON, "NO2 value for person 1 is not correct" );
		Assertions.assertEquals(nxv1, finalMap.get(idp1).get( NOx ), MatsimTestUtils.EPSILON, "NOx value for person 1 is not correct" );
		Assertions.assertEquals(pmv1, finalMap.get(idp1).get( PM ), MatsimTestUtils.EPSILON, "PM value for person 1 is not correct" );
		Assertions.assertEquals(sov1, finalMap.get(idp1).get( SO2 ), MatsimTestUtils.EPSILON, "SO value for person 1 is not correct" );

		//check: values for all emissions are correct -person 2
		Assertions.assertEquals(cov2, finalMap.get(idp2).get( CO ), MatsimTestUtils.EPSILON, "CO value for person 2 is not correct" );
		Assertions.assertEquals(c2v2, finalMap.get(idp2).get( CO2_TOTAL ), MatsimTestUtils.EPSILON, "CO2 value for person 2 is not correct" );
		Assertions.assertEquals(fcv2, finalMap.get(idp2).get( FC ), MatsimTestUtils.EPSILON, "FC value for person 2 is not correct" );
		Assertions.assertEquals(hcv2, finalMap.get(idp2).get( HC ), MatsimTestUtils.EPSILON, "HC value for person 2 is not correct" );
		Assertions.assertEquals(nmv2, finalMap.get(idp2).get( NMHC ), MatsimTestUtils.EPSILON, "NMHC value for person 2 is not correct" );
		Assertions.assertEquals(n2v2, finalMap.get(idp2).get( NO2 ), MatsimTestUtils.EPSILON, "NO2 value for person 2 is not correct" );
		Assertions.assertEquals(nxv2, finalMap.get(idp2).get( NOx ), MatsimTestUtils.EPSILON, "NOx value for person 2 is not correct" );
		Assertions.assertEquals(pmv2, finalMap.get(idp2).get( PM ), MatsimTestUtils.EPSILON, "PM value for person 2 is not correct" );
		Assertions.assertEquals(sov2, finalMap.get(idp2).get( SO2 ), MatsimTestUtils.EPSILON, "SO value for person 2 is not correct" );

	}

	@Test
	final void testSetNonCalculatedEmissionsForPopulation_missingMap() {

		setUpForNonCaculatedEmissions();

		//person 3 in population but its emission map is missing (e.g. not in totalEmissions)
		Id<Person> idp3 = Id.create("p3", Person.class);
		Person p3 = populationFactory.createPerson(idp3);
		pop.addPerson(p3);
		Map<Id<Person>, SortedMap<Pollutant, Double>> finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, totalEmissions, pollsFromEU);

		//check: person 3 is contained in the finalMap
		Assertions.assertTrue(finalMap.containsKey(idp3), "the calculated map should contain person 3");
		//nothing else in the finalMap
		message = "the calculated map should contain " + pop.getPersons().size() + " person(s) but contains " + finalMap.keySet().size() + "person(s).";
		Assertions.assertEquals(pop.getPersons().keySet().size(), finalMap.keySet().size(), message);

		//check: all values for this person are zero and of type double
		for (Double pollutantValues : finalMap.get(idp3).values()) {
			Assertions.assertSame(pollutantValues.getClass(), Double.class);
			Assertions.assertEquals(0.0, pollutantValues, MatsimTestUtils.EPSILON);
			Assertions.assertNotNull(pollutantValues);
		}
		//check: all types of emissions appear
		for (Pollutant emission : pollsFromEU) {
			Assertions.assertTrue(finalMap.get(idp3).containsKey(emission));
		}
		//nothing else in the list
		int numOfPolls = pollsFromEU.size();
		message = "the number of pollutants is " + finalMap.get(idp3).keySet().size() + " but should be" + numOfPolls;
		Assertions.assertEquals(numOfPolls, finalMap.get(idp3).keySet().size(), message);

	}

	private void setUpForNonCaculatedEmissions() {
		//intern method to set some parameters
		totalEmissions = new TreeMap<>();
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		pop = sc.getPopulation();
		populationFactory = pop.getFactory();
		pollsFromEU = new HashSet<>(Arrays.asList(CO, CO2_TOTAL, FC, HC, NMHC, NOx, NO2, PM, SO2));
		nullPointerEx = false;
	}

	@Test
	final void testSetNonCalculatedEmissionsForPopulation_missingPerson() {

		setUpForNonCaculatedEmissions();


		//person 4 in totalEmissions but not in population
		Double cov4 = .0008, c2v4 = .004, fcv4 = .07, hcv4 = .9, nmv4 = 1., n2v4 = 50., nxv4 = 700., pmv4 = 4000., sov4 = 30000.;

		SortedMap<Pollutant, Double> p4Emissions = new TreeMap<>();
		//complete list of all pollutants - missing data is not tested here
		p4Emissions.put(CO, cov4);
		p4Emissions.put(CO2_TOTAL, c2v4);
		p4Emissions.put(FC, fcv4);
		p4Emissions.put(HC, hcv4);
		p4Emissions.put(NMHC, nmv4);
		p4Emissions.put(NO2, n2v4);
		p4Emissions.put(NOx, nxv4);
		p4Emissions.put(PM, pmv4);
		p4Emissions.put(SO2, sov4);

		Id<Person> idp4 = Id.create("p4", Person.class);
		totalEmissions.put(idp4, p4Emissions);

		Map<Id<Person>, SortedMap<Pollutant, Double>> finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, totalEmissions, pollsFromEU);

		//check: all persons added to the population are contained in the finalMap
		Assertions.assertFalse(finalMap.containsKey(idp4), "the calculated map should not contain person 4");
		//nothing else in the finalMap
		message = "the calculated map should contain " + pop.getPersons().size() + " person(s) but contains " + finalMap.keySet().size() + "person(s).";
		Assertions.assertEquals(pop.getPersons().keySet().size(), finalMap.keySet().size(), message);

	}

	@Test
	final void testSetNonCalculatedEmissionsForPopulation_nullEmissions(){
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
		Map<Id<Person>, SortedMap<Pollutant, Double>> finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, null, pollsFromEU);
		} catch (NullPointerException e) {
			nullPointerEx = true;
		}
		Assertions.assertTrue(nullPointerEx);
	}

	@Test
	final void testSetNonCalculatedEmissionsForPopulation_emptyPopulation(){
		// test setNonCalculatedEmissionsForPopulation with an empty population
		// empty list should be returned
		setUpForNonCaculatedEmissions();

		//person 7 in totalEmissions but not in population
		SortedMap<Pollutant, Double> p7Emissions = new TreeMap<>();
		//complete list of all pollutants - missing data is not tested here
		p7Emissions.put( CO, .0 );
		p7Emissions.put( CO2_TOTAL, .0 );
		p7Emissions.put( FC, .0 );
		p7Emissions.put( HC, .0 );
		p7Emissions.put( NMHC, .0 );
		p7Emissions.put( NO2, .0 );
		p7Emissions.put( NOx, .0 );
		p7Emissions.put(PM, .0);
		p7Emissions.put(SO2, .0);

		Id<Person> idp7 = Id.create("p7", Person.class);
		totalEmissions.put(idp7, p7Emissions);

		//empty population
		Map<Id<Person>, SortedMap<Pollutant, Double>> finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, totalEmissions, pollsFromEU);

		//nothing in the finalMap
		message = "the calculated map should contain " + pop.getPersons().size() + " person(s) but contains " + finalMap.keySet().size() + "person(s).";
		Assertions.assertEquals(pop.getPersons().keySet().size(), finalMap.keySet().size(), message);

	}

	@Test
	final void testSetNonCalculatedEmissionsForPopulation_emptyEmissionMap() {
		//test setNonCalculatedEmissionsForPopulation with an empty emission map
		setUpForNonCaculatedEmissions();

		Id<Person> idp5 = Id.create("p5", Person.class);
		Person p5 = populationFactory.createPerson(idp5);
		pop.addPerson(p5);
		Id<Person> idp6 = Id.create("p6", Person.class);
		Person p6 = populationFactory.createPerson(idp6);
		pop.addPerson(p6);

		//empty emissions map
		Map<Id<Person>, SortedMap<Pollutant, Double>> finalMap = EmissionUtils.setNonCalculatedEmissionsForPopulation(pop, totalEmissions, pollsFromEU);

		//check: all persons added to the population are contained in the finalMap
		Assertions.assertTrue(finalMap.containsKey(idp5), "the calculated map should contain person 5");
		Assertions.assertTrue(finalMap.containsKey(idp6), "the calculated map should contain person 6");
		//nothing else in the finalMap
		message = "the calculated map should contain " + pop.getPersons().size() + " person(s) but contains " + finalMap.keySet().size() + "person(s).";
		Assertions.assertEquals(pop.getPersons().keySet().size(), finalMap.keySet().size(), message);

		//check: all values for all persons are zero and of type double
		for (Id<Person> id : finalMap.keySet()) {
			for (Double pollutant : finalMap.get(id).values()) {
				Assertions.assertSame(pollutant.getClass(), Double.class);
				Assertions.assertEquals(0.0, pollutant, MatsimTestUtils.EPSILON, "map of pollutants was missing. Therefore all values should be set to zero.");
				Assertions.assertNotNull(pollutant);
			}
			//check: alle types of emissions appear
			for (Pollutant emission : pollsFromEU) {
				Assertions.assertTrue(finalMap.get(id).containsKey(emission));
			}
			//nothing else in the list
			int numOfPolls = pollsFromEU.size();
			Assertions.assertEquals(numOfPolls, finalMap.get(id).keySet().size(), "the number of pullutants is " + finalMap.get(id).keySet().size() + " but should be" + numOfPolls);


		}

	}

	@Test
	final void testSetNonCalculatedEmissionsForNetwork() {
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

		Map<Id<Link>, SortedMap<Pollutant, Double>> totalEmissions = new HashMap<>();

		//complete link - link12
		Id<Link> link12id = Id.create("link12", Link.class);
		SortedMap<Pollutant, Double> emLink12 = new TreeMap<>();
		double c2link12v = .0008, colink12v = .001, fclink12v = .05,
				hclink12v = .8, nmlink12v = 1., n2link12v = 50.,
				nxlink12v = 600., pmlink12v = 2000., solink12v = 60000.;
		emLink12.put(CO2_TOTAL, c2link12v);
		emLink12.put(CO, colink12v);
		emLink12.put(FC, fclink12v);
		emLink12.put(HC, hclink12v);
		emLink12.put(NMHC, nmlink12v);
		emLink12.put(NO2, n2link12v);
		emLink12.put(NOx, nxlink12v);
		emLink12.put(PM, pmlink12v);
		emLink12.put(SO2, solink12v);
		totalEmissions.put(link12id, emLink12);

		//complete link - link13
		Id<Link> link13id = Id.create("link13", Link.class);
		SortedMap<Pollutant, Double> emLink13 = new TreeMap<>();
		double c2link13v = .0003, colink13v = .008, fclink13v = .03,
				hclink13v = .7, nmlink13v = 6., n2link13v = 40.,
				nxlink13v = 800., pmlink13v = 1000., solink13v = 90000.;
		emLink13.put(CO2_TOTAL, c2link13v);
		emLink13.put(CO, colink13v);
		emLink13.put(FC, fclink13v);
		emLink13.put(HC, hclink13v);
		emLink13.put(NMHC, nmlink13v);
		emLink13.put(NO2, n2link13v);
		emLink13.put(NOx, nxlink13v);
		emLink13.put(PM, pmlink13v);
		emLink13.put(SO2, solink13v);
		totalEmissions.put(Id.create("link13", Link.class), emLink13);

		//missing map - link14
		Id<Link> link14id = Id.create("link14", Link.class);
		totalEmissions.put(Id.create("link14", Link.class), null);

		//partial map - link 23
		Id<Link> link23id = Id.create("link23", Link.class);
		SortedMap<Pollutant, Double> emLink23 = new TreeMap<>();
		double nxlink23v = 900., pmlink23v = 6000., solink23v = 20000.;
		emLink23.put(NOx, nxlink23v);
		emLink23.put(PM, pmlink23v);
		emLink23.put(SO2, solink23v);
		totalEmissions.put(Id.create("link23", Link.class), emLink23);

		//empty map - link 24
		Id<Link> link24id = Id.create("link24", Link.class);
		SortedMap<Pollutant, Double> emLink24 = new TreeMap<>();
		totalEmissions.put(Id.create("link24", Link.class), emLink24);

		//not put into totalEmissionsMap - link 34
		Id<Link> link34id = Id.create("link34", Link.class);

		Map<Id<Link>, SortedMap<Pollutant, Double>> totalEmissionsFilled = EmissionUtils.setNonCalculatedEmissionsForNetwork(network, totalEmissions, pollsFromEU);
		//each link of the network and each type of emission
		for(Link link: network.getLinks().values()) {

			Id<Link> linkId = link.getId();

			Assertions.assertTrue(totalEmissionsFilled.containsKey(linkId));
			SortedMap<Pollutant, Double> emissionMapForLink = totalEmissionsFilled.get(linkId);
			for (Pollutant pollutant : pollsFromEU) {
				System.out.println("pollutant: " + pollutant + "; linkId: " + linkId);
				Assertions.assertTrue(emissionMapForLink.containsKey(pollutant),
						pollutant + "not found for link " + linkId.toString());
				Assertions.assertEquals(Double.class, emissionMapForLink.get(pollutant).getClass());

			}
		}
		//check values
		//link 12 and 13
		Assertions.assertEquals(totalEmissionsFilled.get(link12id).get( CO2_TOTAL ), c2link12v, MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link12id).get( CO ), colink12v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link12id).get( FC ), fclink12v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link12id).get( HC ), hclink12v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link12id).get( NMHC ), nmlink12v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link12id).get( NO2 ), n2link12v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link12id).get( NOx ), nxlink12v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link12id).get( PM ), pmlink12v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link12id).get( SO2 ), solink12v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link13id).get( CO2_TOTAL ), c2link13v, MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link13id).get( CO ), colink13v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link13id).get( FC ), fclink13v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link13id).get( HC ), hclink13v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link13id).get( NMHC ), nmlink13v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link13id).get( NO2 ), n2link13v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link13id).get( NOx ), nxlink13v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link13id).get( PM ), pmlink13v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link13id).get( SO2 ), solink13v,  MatsimTestUtils.EPSILON );

		//link 14 and 34
		for(Pollutant pollutant: pollsFromEU){
			Assertions.assertEquals(totalEmissionsFilled.get(link14id).get(pollutant), .0, MatsimTestUtils.EPSILON);
			Assertions.assertEquals(totalEmissionsFilled.get(link34id).get(pollutant), .0, MatsimTestUtils.EPSILON);
		}

		//link 23 - partial
		Assertions.assertEquals(totalEmissionsFilled.get(link23id).get( CO2_TOTAL ), .0, MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link23id).get( CO ), .0,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link23id).get( FC ), .0,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link23id).get( HC ), .0,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link23id).get( NMHC ), .0,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link23id).get( NO2 ), .0,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link23id).get( NOx ), nxlink23v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link23id).get( PM ), pmlink23v,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link23id).get( SO2 ), solink23v,  MatsimTestUtils.EPSILON );

		//link 24 - empty
		Assertions.assertEquals(totalEmissionsFilled.get(link24id).get( CO2_TOTAL ), .0, MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link24id).get( CO ), .0,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link24id).get( FC ), .0,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link24id).get( HC ), .0,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link24id).get( NMHC ), .0,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link24id).get( NO2 ), .0,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link24id).get( NOx ), .0,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link24id).get( PM ), .0,  MatsimTestUtils.EPSILON );
		Assertions.assertEquals(totalEmissionsFilled.get(link24id).get( SO2 ), .0,  MatsimTestUtils.EPSILON );

	}
	public static Map<Pollutant,Double> createEmissions() {
		return Arrays.stream( Pollutant.values() ).collect( Collectors.toMap( p -> p, p -> Math.random() ) ) ;
	}

	public static Map<Pollutant,Double> createEmissionsWithFixedValue( double value ) {
		return Arrays.stream( Pollutant.values() ).collect( Collectors.toMap( p -> p, p -> value ) ) ;
	}

	private void addLinksToNetwork(Scenario sc) {
		//intern method to set up a network with nodes and links
		Network network = sc.getNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("node1", Node.class), new Coord(.0, .0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("node2", Node.class), new Coord(.0, 1000.));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("node3", Node.class), new Coord(1000., .0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("node4", Node.class), new Coord(1000., 1000.));
		NetworkUtils.createAndAddLink(network, Id.create("link12", Link.class), node1, node2, 1000., 20., 3600, 2);
		NetworkUtils.createAndAddLink(network, Id.create("link13", Link.class), node1, node3, 1000., 20., 3600, 2);
		NetworkUtils.createAndAddLink(network, Id.create("link14", Link.class), node1, node4, 1000., 20., 3600, 2);
		NetworkUtils.createAndAddLink(network, Id.create("link23", Link.class), node2, node3, 1000., 20., 3600, 2);
		NetworkUtils.createAndAddLink(network, Id.create("link24", Link.class), node2, node4, 1000., 20., 3600, 2);
		NetworkUtils.createAndAddLink(network, Id.create("link34", Link.class), node3, node4, 1000., 20., 3600, 2); //w/o orig id and type
	}

}

