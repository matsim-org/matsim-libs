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

package playground.emissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.misc.HandlerToTestEmissionAnalysisModules;
import playground.vsp.emissions.WarmEmissionAnalysisModule;
import playground.vsp.emissions.WarmEmissionAnalysisModule.WarmEmissionAnalysisModuleParameter;
import playground.vsp.emissions.types.HbefaTrafficSituation;
import playground.vsp.emissions.types.HbefaVehicleAttributes;
import playground.vsp.emissions.types.HbefaVehicleCategory;
import playground.vsp.emissions.types.HbefaWarmEmissionFactor;
import playground.vsp.emissions.types.HbefaWarmEmissionFactorKey;
import playground.vsp.emissions.types.WarmPollutant;

//TODO update doc
/*
 * test for playground.vsp.emissions.WarmEmissionAnalysisModule
 * 
 * WarmEmissionAnalysisModule (weam) 
 * public: 
 * weamParameter - testWarmEmissionAnalysisParameter, testWarmEmissionAnalysisModule_exceptions
 * throw warm EmissionEvent - testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent*, testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions
 * check vehicle info and calculate warm emissions -testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent*, testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions
 * get free flow occurences - testCounters*()
 * get fraction occurences - testCounters*()
 * get stop go occurences - testCounters*()
 * get km counter - testCounters*()
 * get free flow km counter - testCounters*()
 * get top go km couter - testCounters*()
 * get warm emission event counter - testCounters*()
 * 
 * private: //TODO werden die implizit getestet? sonst: tests schreiben
 * rescale warm emissions
 * calculate warm emissions
 * convert string 2 tuple
 * 
 */

public class TestWarmEmissionAnalysisModule {
	
	int numberOfWarmPollutants= WarmPollutant.values().length;
	String hbefaRoadCategory = "URB";
	Double rescaleFactor = -.001;
	int roadType =0;
	int leaveTime = 0;
	boolean excep =false;
	String passengercar= "PASSENGER_CAR", heavygoodsvehicle="HEAVY_GOODS_VEHICLE"; 
	
	Map<Integer, String> roadTypeMapping;
	Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
	Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;
	WarmEmissionAnalysisModuleParameter weamp;
	EventsManager emissionEventManager;
	WarmEmissionAnalysisModuleParameter warmEmissionParameterObject;
	WarmEmissionAnalysisModule weam;
	Map<WarmPollutant, Double> warmEmissions;
	
	// saturated and heavy not used so far -> not tested
	HbefaTrafficSituation trafficSituationff = HbefaTrafficSituation.FREEFLOW;
	HbefaTrafficSituation trafficSituationsg = HbefaTrafficSituation.STOPANDGO;
	
	// emission factors for tables - no dublicates!
	Double detailedPetrolFactorFf = .1; 
	Double detailedPetrolFactorSg = .01; 
	Double detailedPcFactorSg = .001;
	Double detailedPcFactorFf = .0001;
	Double detailedDieselFactorSg= .00001;
	Double detailedDieselFactorFf = .000001;
	Double detailedFfOnlyFactorFf = .0000001;
	Double detailedSgOnlyFactorSg = .00000001;
	Double detailedTableFactorFf =  .11;
	Double detailedTableFactorSg =  .011;
	Double detailedZeroFactorFf  =  .0011;
	Double detailedZeroFactorSg  =  .00011;
	Double detailedSgffFactorFf =   .000011;
	Double detailedSgffFactorSg = 	.0000011;
	Double avgPcFactorFf = 1.;
	Double avgPcFactorSg= 10.;
	Double avgDieselFactorFf = 100.;
	Double avgDieselFactorSg = 1000.;
	Double avgLpgFactorFf = 10000.;
	Double avgLpgFactorSg = 100000.;
	Double avgPetrolFactorFf = 1000000.;
	Double avgPetrolFactorSg = 10000000.;
	
	// vehicle information for regular test cases  
	// case 1 - data in both tables -> use detailed
	String 	petrolTechnology = "PC petrol <1,4L", petrolSizeClass ="<ECE petrol (4S)", petrolConcept ="<1,4L";
	Double petrolSpeedFf = 20., petrolSpeedSg = 10.;
	// case 2 - free flow entry in both tables, stop go entry in average table -> use average
	String 	pcTechnology = "PC petrol <1,4L <ECE", pcSizeClass = "petrol (4S)", pcConcept = "<1,4L";
	Double pcfreeVelocity = 50., pcsgVelocity= 10.;
	// case 3 - stop go entry in both tables, free flow entry in average table -> use average
	String 	dieselTechnology = "PC diesel", dieselSizeClass = "diesel", dieselConcept = ">=2L";
	Double dieselFreeVelocity = 100., dieselSgVelocity = 30.;
	// case 4 - data in average table
	String 	lpgTechnology = "PC LPG Euro-4", lpgSizeClass = "LPG", lpgConcept = "not specified";
	Double lpgFreeVelocity = 100., lpgSgVelocity = 35.;
	// case 5 - no entry in any table - must be different to other test case's strings
	String 	noeTechnology = "technology", noeConcept = "concept", noeSizeClass = "size";
	Double noeFreeSpeed=100.;
	// case 6 - data in detailed table, stop go speed zero
	String zeroTechnology = "zero technology", zeroConcept = "zero concept", zeroSizeClass = "zero size class";
	Double zeroFreeVelocity = 100., zeroSgVelocity = 0.;
	// case 7 - data in detailed table, stop go speed = free flow speed
	String sgffTechnology = "sg ff technology", sgffConcept = "sg ff concept", sgffSizeClass = "sg ff size class";
	Double sgffDetailedFfSpeed = 44., sgffDetailedSgSpeed = 44.;
	// case 8 - sg entry in detailed table, no free flow entry anywhere 
	String sgOnlyTechnology ="bifuel CNG/petrol", sgOnlySizeClass="not specified", sgOnlyConcept="PC-Alternative Fuel";
	Double sgOnlysgSpeed = 50.;
	// case 9 - ff entry in detailed table, no stop go entry anywhere
	String ffOnlyTechnology="diesel", ffOnlySizeClass=">=2L", ffOnlyConcept="PC-D-Euro-3";
	Double ffOnlyffSpeed = 120.;
	// case 10 - data in detailed table, stop go speed > free flow speed
	String tableTechnology = "petrol (4S)", tableSizeClass= "<1,4L", tableConcept = "PC-P-Euro-0";
	Double tableffSpeed = 30., tablesgSpeed = 55.;
	
	@Test 
	public void testWarmEmissionAnalysisParameter(){
		WarmEmissionAnalysisModuleParameter weamp = new WarmEmissionAnalysisModuleParameter(null, null, null);
		Assert.assertEquals(weamp.getClass(), WarmEmissionAnalysisModuleParameter.class);
		//null als Konstructoreingabe erlaubt, exception erst bei Konstruction des WEAM
		// soll das so sein? 
		// koennten logger-warnungen/abort im weamParameter oder waem einbauen - ja TODO
		// auch  mit assert moeglich
		// dann diesen test anpassen
	}
	
	@Test
	public void testWarmEmissionAnalysisModule_exceptions(){
		
		/* test the constructor of warmEmissionAnalysisModule
		 * WarmEmissionAnalysisModule(WarmEmissionAnalysisModuleParameter, EventsManager, EmissionEfficiencyFactor)
		 * if all arguments or the WarmEmissionAnalysisModuleParameter or the EventsManager are 'null' -> throw exception
		 * EmissionEfficiencyFactor = 'null' is allowed and therefor not tested here
		 */
		
		setUp();
		excep= false;
		
		WarmEmissionAnalysisModule weam2;
		weamp = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, avgHbefaWarmTable, detailedHbefaWarmTable);
		Double emissionEfficiencyFactor = 1.0;
		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		
		ArrayList<ArrayList> testCases = new ArrayList<ArrayList>();		
		ArrayList<Object> testCase1= new ArrayList<Object>(), testCase2= new ArrayList<Object>(), testCase3= new ArrayList<Object>();
		
		Collections.addAll(testCase1, null, null, null);
		Collections.addAll(testCase2, weamp, null, emissionEfficiencyFactor); //TODO abort
		Collections.addAll(testCase3, null, emissionEventManager, emissionEfficiencyFactor);
		
		testCases.add(testCase1); 
		// testCases.add(testCase2); // hier keinen Handler zu uebergeben wirft keine Fehler! TODO Test wieder hinzufuegen, wenn die methode korrigiert ist 
		testCases.add(testCase3);  
		
		for(List<Object> tc: testCases){
			try{
				weam2 = new WarmEmissionAnalysisModule((WarmEmissionAnalysisModuleParameter)tc.get(0), (EventsManager)tc.get(1),(Double)tc.get(2));
			}catch(NullPointerException e){
				excep = true;
			}catch(Exception f){
				Assert.fail("something wrong with the test itself. This should not happen");
				System.out.println(f.getMessage());
			}
			Assert.assertTrue("initilizing a warm emission analysis module with 'null' input should fail", excep);
			excep= false;
		}		
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent1(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// case 1 - data in both tables -> use detailed
		Id personId = new IdImpl("person 1");
		Id vehicleId = new IdImpl("veh 1");		
		Double linkLength = 200.; 
		String vehicleInformation = passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;
		
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, linkLength, linkLength/petrolSpeedFf*3.6, vehicleInformation);
		Assert.assertEquals(detailedPetrolFactorFf*linkLength/1000., warmEmissions.get(WarmPollutant.CO2_TOTAL), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		
		weam.throwWarmEmissionEvent(leaveTime, personId, vehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*detailedPetrolFactorFf*linkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent2(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// case 2 - free flow entry in both tables, stop go entry in average table -> use average
		Id pcPersonId = new IdImpl("person 2"); 
		Id pcVehicleId = new IdImpl("veh 2");
		Double pclinkLength= 100.;
		String pcVehicleInformation = passengercar + ";"+ pcTechnology + ";"+pcSizeClass+";"+pcConcept;
		
		// sub case avg speed = free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcPersonId, roadType, pcfreeVelocity/3.6, pclinkLength, pclinkLength/pcfreeVelocity*3.6, pcVehicleInformation);
		Assert.assertEquals(avgPcFactorFf*pclinkLength/1000., warmEmissions.get(WarmPollutant.NMHC), MatsimTestUtils.EPSILON);
		weam.throwWarmEmissionEvent(leaveTime, pcPersonId, pcVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgPcFactorFf*pclinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();

		// sub case avg speed = stop go speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcPersonId, roadType, pcfreeVelocity/3.6, pclinkLength, pclinkLength/pcsgVelocity*3.6, pcVehicleInformation);
		Assert.assertEquals(avgPcFactorSg*pclinkLength/1000., warmEmissions.get(WarmPollutant.NMHC), MatsimTestUtils.EPSILON);
		weam.throwWarmEmissionEvent(leaveTime, pcPersonId, pcVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgPcFactorSg*pclinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent3(){
		
		//-- set up tables, event handler, parameters, module
		setUp();

		// case 3 - stop go entry in both tables, free flow entry in average table -> use average
		Id dieselPersonId = new IdImpl("person 3");
		Id dieselVehicleId = new IdImpl("veh 3");
		Double dieselLinkLength= 20.;
		String dieselVehicleInformation = passengercar +";"+ dieselTechnology+ ";"+ dieselSizeClass+";"+dieselConcept;
		
		// sub case avg speed = free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselPersonId, roadType, dieselFreeVelocity/3.6, dieselLinkLength, dieselLinkLength/dieselFreeVelocity*3.6, dieselVehicleInformation);
		Assert.assertEquals(avgDieselFactorFf*dieselLinkLength/1000., warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, dieselPersonId, dieselVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgDieselFactorFf*dieselLinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();

		// sub case avg speed = stop go speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselPersonId, roadType, dieselFreeVelocity/3.6, dieselLinkLength, dieselLinkLength/dieselSgVelocity*3.6, dieselVehicleInformation);
		Assert.assertEquals(avgDieselFactorSg*dieselLinkLength/1000., warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, dieselPersonId, dieselVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgDieselFactorSg*dieselLinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}

	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent4(){
		
		//-- set up tables, event handler, parameters, module
		setUp();

		// case 4 - data in average table
		Id lpgPersonId = new IdImpl("person4");
		Id lpgVehicleId = new IdImpl("veh 4");
		Double lpgLinkLength = 700.;
		String lpgVehicleInformation = passengercar + ";"+ lpgTechnology+";"+lpgSizeClass+";"+lpgConcept;		
		
		// sub case avg speed = free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(lpgPersonId, roadType, lpgFreeVelocity/3.6, lpgLinkLength, lpgLinkLength/lpgFreeVelocity*3.6, lpgVehicleInformation);
		Assert.assertEquals(avgLpgFactorFf*lpgLinkLength/1000., warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, lpgPersonId, lpgVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgLpgFactorFf*lpgLinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();

		// sub case avg speed = stop go speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(lpgPersonId, roadType, lpgFreeVelocity/3.6, lpgLinkLength, lpgLinkLength/lpgSgVelocity*3.6, lpgVehicleInformation);
		Assert.assertEquals(avgLpgFactorSg*lpgLinkLength/1000., warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, lpgPersonId, lpgVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgLpgFactorSg*lpgLinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}
	
	@Test 
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent5(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// case 6 - data in detailed table, stop go speed zero
		// use free flow factor to calculate emissions
		Id zeroPersonId = new IdImpl("person zero");
		Id zeroLinkId = new IdImpl("link zero");
		Double zeroLinklength = 3000.;
		String zeroVehicleInformation = passengercar + ";"+ zeroTechnology + ";" + zeroSizeClass + ";" + zeroConcept;
		
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(zeroPersonId, roadType, zeroFreeVelocity/3.6, zeroLinklength, 2*zeroLinklength/(zeroFreeVelocity+zeroSgVelocity)*3.6, zeroVehicleInformation);
		Assert.assertEquals(detailedZeroFactorFf*zeroLinklength/1000., warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		
		weam.throwWarmEmissionEvent(22., zeroLinkId, zeroPersonId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*detailedZeroFactorFf*zeroLinklength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
		
		// TODO warning - im em. module tab. konstruktion anguckene, dort evtl konsitenztest einbauen
		
	}
	
	@Test 
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent6(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		Id sgffPersonId = new IdImpl("person sg equals ff");
		Double sgffLinklength = 4000.;
		String sgffVehicleInformaition = passengercar + ";" + sgffTechnology + ";"+ sgffSizeClass + ";"+sgffConcept;
		
		//avg>ff 
		warmEmissions= weam.checkVehicleInfoAndCalculateWarmEmissions(sgffPersonId, roadType, sgffDetailedFfSpeed/3.6, sgffLinklength, .5*sgffLinklength/sgffDetailedFfSpeed*3.6, sgffVehicleInformaition);
		Assert.assertEquals(detailedSgffFactorFf*sgffLinklength/1000., warmEmissions.get(WarmPollutant.NO2), MatsimTestUtils.EPSILON);
		//avg=ff=sg -> use ff factors
		warmEmissions= weam.checkVehicleInfoAndCalculateWarmEmissions(sgffPersonId, roadType, sgffDetailedFfSpeed/3.6, sgffLinklength, sgffLinklength/sgffDetailedFfSpeed*3.6, sgffVehicleInformaition);
		Assert.assertEquals(detailedSgffFactorFf*sgffLinklength/1000., warmEmissions.get(WarmPollutant.NO2), MatsimTestUtils.EPSILON);
		//avg<sg -> use sg factors 
		warmEmissions= weam.checkVehicleInfoAndCalculateWarmEmissions(sgffPersonId, roadType, sgffDetailedFfSpeed/3.6, sgffLinklength, 2*sgffLinklength/sgffDetailedFfSpeed*3.6, sgffVehicleInformaition);
		Assert.assertEquals(detailedSgffFactorSg*sgffLinklength/1000., warmEmissions.get(WarmPollutant.NO2), MatsimTestUtils.EPSILON);
		
		//TODO warning
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions1(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// case 5 - no entry in any table - must be different to other test case's strings	
		Id noePersonId = new IdImpl("person 5");
		Id noeVehicleId = new IdImpl("veh 5");
		String noeVehicleInformation = passengercar + ";"+ noeTechnology + ";" + noeSizeClass + ";" + noeConcept;
		
		excep= false;
		try{
			Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noePersonId, roadType, noeFreeSpeed, 22., 1.5*22./noeFreeSpeed, noeVehicleInformation);
			weam.throwWarmEmissionEvent(10., noePersonId, noeVehicleId, warmEmissions);
		}catch(Exception e){
			excep = true;
		}
		Assert.assertTrue(excep); excep=false;
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions2(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// no vehicle information given
		Id noePersonId = new IdImpl("person 6");
		Id noeVehicleId = new IdImpl("veh 6");
		String noeVehicleInformation = "";
		
		excep= false;
		try{
			Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noePersonId, roadType, noeFreeSpeed, 22., 1.5*22./noeFreeSpeed, noeVehicleInformation);
			weam.throwWarmEmissionEvent(10., noePersonId, noeVehicleId, warmEmissions);
		}catch(Exception e){
			excep = true;
		}
		Assert.assertTrue(excep); excep=false;
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions3(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// empty vehicle information 
		Id noePersonId = new IdImpl("person 7");
		Id noeVehicleId = new IdImpl("veh 7");
		String noeVehicleInformation = ";;;";
		
		excep= false;
		try{
			Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noePersonId, roadType, noeFreeSpeed, 22., 1.5*22./noeFreeSpeed, noeVehicleInformation);
			weam.throwWarmEmissionEvent(10., noePersonId, noeVehicleId, warmEmissions);
		}catch(Exception e){
			excep = true;
		}
		Assert.assertTrue(excep); excep=false;
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions4(){
		//-- set up tables, event handler, parameters, module
		setUp();
		//  vehicle information string is 'null'
		Id noePersonId = new IdImpl("person 8");
		Id noeVehicleId = new IdImpl("veh 8");
		String noeVehicleInformation = null;
		
		excep= false;
		try{
			Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noePersonId, roadType, noeFreeSpeed, 22., 1.5*22./noeFreeSpeed, noeVehicleInformation);
			weam.throwWarmEmissionEvent(10., noePersonId, noeVehicleId, warmEmissions);
		}catch(Exception e){
			excep = true;
		}
		Assert.assertTrue(excep); excep=false;
		
	}
	
	@Test
	public void testCounters1(){
		setUp();
		weam.reset();
		
		/*
		 * using the same case as above - case 1 and check the counters for all possible combinations of avg, stop go and free flow speed 
		 */
		 
		Id personId = new IdImpl("person 1");
		Integer roadType = 0;
		Double linkLength = 2*1000.; //in meter
		String vehicleInformation = passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;

		// <stop&go speed
		Double travelTime = linkLength/petrolSpeedSg*1.2; 
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, travelTime*3.6, vehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(linkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(linkLength/1000, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// = s&g speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, linkLength/petrolSpeedSg*3.6, vehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(linkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(linkLength/1000, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
				
		// > s&g speed, <ff speed
		// speed in km/h
		travelTime = .5 * linkLength/petrolSpeedFf *3.6 + .5* (linkLength/petrolSpeedSg)*3.6; //540 seconds
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, travelTime , vehicleInformation);
		Assert.assertEquals(1, weam.getFractionOccurences());
		Assert.assertEquals(1., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(linkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		Assert.assertEquals(weam.getKmCounter(), (weam.getStopGoKmCounter()+weam.getFreeFlowKmCounter()), MatsimTestUtils.EPSILON);
		Assert.assertEquals(travelTime, 3600*weam.getFreeFlowKmCounter()/petrolSpeedFf+3600*weam.getStopGoKmCounter()/petrolSpeedSg, MatsimTestUtils.EPSILON);
		weam.reset();
		
		// = ff speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, linkLength/petrolSpeedFf*3.6, vehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(linkLength/1000, weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(linkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		//> ff speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, 0.4*linkLength/petrolSpeedFf*3.6, vehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(linkLength/1000, weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(linkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(.0, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		
	}
	
	@Test
	public void testCounters2(){
		setUp();
		weam.reset();
		
		// ff und sg nicht im detailed table -> use average table
		Id personId = new IdImpl("person4");
		Double lpgLinkLength = 2000.*1000;
		String lpgVehicleInformation = passengercar + ";"+ lpgTechnology+";"+lpgSizeClass+";"+lpgConcept;
		
		// free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, lpgFreeVelocity/3.6, 
				lpgLinkLength, lpgLinkLength/lpgFreeVelocity*3.6, lpgVehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(lpgLinkLength/1000, weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(lpgLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(.0, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, lpgFreeVelocity/3.6, 
				lpgLinkLength, lpgLinkLength/lpgSgVelocity*3.6, lpgVehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(lpgLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(lpgLinkLength/1000, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
	}
	
	@Test
	public void testCounters3(){
		setUp();
		weam.reset();
		
		// case 2 - free flow entry in both tables, stop go entry in average table -> use average
		Id pcPersonId = new IdImpl("person 2"); 
		Double pclinkLength= 20.*1000;
		String pcVehicleInformation = passengercar + ";"+ pcTechnology + ";"+pcSizeClass+";"+pcConcept;
		
		//ff
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcPersonId, roadType, pcfreeVelocity/3.6, pclinkLength, pclinkLength/pcfreeVelocity*3.6, pcVehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(pclinkLength/1000, weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(pclinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		//sg
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcPersonId, roadType, pcfreeVelocity/3.6, pclinkLength, pclinkLength/pcsgVelocity*3.6, pcVehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(pclinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(pclinkLength/1000, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
	}
	
	@Test
	public void testCounters4(){
	setUp();
	weam.reset();
			
	// case 3 - stop go entry in both tables, free flow entry in average table -> use average
	Id dieselPersonId = new IdImpl("person 3");
	Double dieselLinkLength= 200.*1000;
	String dieselVehicleInformation = passengercar +";"+ dieselTechnology+ ";"+ dieselSizeClass+";"+dieselConcept;
	
	//ff
	warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselPersonId, roadType, dieselFreeVelocity/3.6, dieselLinkLength, dieselLinkLength/dieselFreeVelocity*3.6, dieselVehicleInformation);
	Assert.assertEquals(0, weam.getFractionOccurences());
	Assert.assertEquals(dieselLinkLength/1000., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(1, weam.getFreeFlowOccurences());
	Assert.assertEquals(dieselLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(0., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(0, weam.getStopGoOccurences());
	Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
	weam.reset();
	
	//sg
	warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselPersonId, roadType, dieselFreeVelocity/3.6, dieselLinkLength, dieselLinkLength/dieselSgVelocity*3.6, dieselVehicleInformation);
	Assert.assertEquals(0, weam.getFractionOccurences());
	Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(0, weam.getFreeFlowOccurences());
	Assert.assertEquals(dieselLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(dieselLinkLength/1000., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(1, weam.getStopGoOccurences());
	Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
	weam.reset();
	}
	
	@Test
	public void testCounters5(){
		setUp();
		weam.reset();
		
		// case 1 - data in both tables -> use detailed
		// free flow velocity inconsistent -> different value in table
		Id inconffPersonId = new IdImpl("person 7");
		Double inconff = 30. * 1000;
		Double inconffavgSpeed = petrolSpeedFf*2.2;
		String inconffVehicleInformation = passengercar + ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;
		//TODO  inkonsistenter ff-speed wird nicht ueberprueft. darf das so bleiben? - gehoert ins emission module, kkonstruktion tabellen
		// wenn ja: diese methode unnoetig -> dok, dann loeschen
		
		// average speed equals free flow speed from table
		warmEmissions =weam.checkVehicleInfoAndCalculateWarmEmissions(inconffPersonId, roadType, inconffavgSpeed/3.6, inconff, inconff/petrolSpeedFf*3.6, inconffVehicleInformation);
		Assert.assertEquals(1, weam.getFractionOccurences());
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(inconff/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// average speed equals wrong free flow speed
		warmEmissions =weam.checkVehicleInfoAndCalculateWarmEmissions(inconffPersonId, roadType, inconffavgSpeed/3.6, inconff, inconff/inconffavgSpeed*3.6, inconffVehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(inconff/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		
		warmEmissions =weam.checkVehicleInfoAndCalculateWarmEmissions(inconffPersonId, roadType, inconffavgSpeed/3.6, inconff, 2*inconff/(petrolSpeedFf+petrolSpeedSg)*3.6, inconffVehicleInformation);
		// TODO test ueberdenken nachdem emission module geaendert wurde
	}
	
	@Test
	public void testCounters7(){
		setUp();
		weam.reset();
	
		// case 10 - data in detailed table, stop go speed > free flow speed
		Id tablePersonId = new IdImpl("person 8");
		Double tableLinkLength= 30.*1000;
		String tableVehicleInformation = passengercar + ";" + tableTechnology +";" + tableSizeClass+";"+tableConcept;
		
		//TODO : falls die sg-geschwindigkeit > ff-geschwindigkeit 
		// wird das beim Erstellen der Tabelle ueberprueft?
		
		// ff < avg < sg - handled like free flow 
		weam.checkVehicleInfoAndCalculateWarmEmissions(tablePersonId, roadType, tableffSpeed/3.6, tableLinkLength, 2* tableLinkLength/(tableffSpeed+tablesgSpeed)*3.6, tableVehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(tableLinkLength/1000., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(tableLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// ff < sg < avg - handled like free flow as well - no additional test
		// avg < ff < sg - handled like stop go 
		weam.checkVehicleInfoAndCalculateWarmEmissions(tablePersonId, roadType, tableffSpeed/3.6, tableLinkLength, 2* tableLinkLength/(tableffSpeed)*3.6, tableVehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(tableLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(tableLinkLength/1000, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
	}
	
	@Test
	public void testCounters8(){
		setUp();
		weam.reset();
		
		// test summing up of counters
		
		// case 1 - data in both tables -> use detailed
		Id personId = new IdImpl("person 1");
		Integer roadType = 0;
		Double linkLength = 2*1000.; //in meter
		String vehicleInformation = passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;
		Double travelTime = linkLength/petrolSpeedSg*1.2; 
		
		weam.reset();
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(0., weam.getFreeFlowKmCounter());
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(0., weam.getKmCounter());
		Assert.assertEquals(0., weam.getStopGoKmCounter());
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(0, weam.getWarmEmissionEventCounter());
	
		// < s&g speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, travelTime*3.6, vehicleInformation);
		// = s&g speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, linkLength/petrolSpeedSg*3.6, vehicleInformation);
		// > s&g speed, <ff speed
		travelTime = .5 * linkLength/petrolSpeedFf *3.6 + .5* (linkLength/petrolSpeedSg)*3.6; //540 seconds
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, travelTime , vehicleInformation);
		// = ff speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, linkLength/petrolSpeedFf*3.6, vehicleInformation);
		//> ff speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, 0.4*linkLength/petrolSpeedFf*3.6, vehicleInformation);
		
		Assert.assertEquals(1, weam.getFractionOccurences());
		Assert.assertEquals(2.5* linkLength/1000., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2, weam.getFreeFlowOccurences());
		Assert.assertEquals(5* linkLength/1000., weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2.5* linkLength/1000, weam.getStopGoKmCounter());
		Assert.assertEquals(2, weam.getStopGoOccurences());
		Assert.assertEquals(5, weam.getWarmEmissionEventCounter());

	}
	
	@Test 
	public void rescaleWarmEmissionsTest() {
		
		// setup ----
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		Map<Integer, String> roadTypeMapping = new HashMap<Integer, String>();
		fillAverageTable(avgHbefaWarmTable);
		fillDetailedTable(detailedHbefaWarmTable);
		fillRoadTypeMapping(roadTypeMapping);
		Map<WarmPollutant, Double> warmEmissions;
		
		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		
		Double rescaleF = 1.0003;
		
		WarmEmissionAnalysisModuleParameter weamParameter = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, avgHbefaWarmTable, detailedHbefaWarmTable);
		WarmEmissionAnalysisModule weam = new WarmEmissionAnalysisModule(weamParameter , emissionEventManager, rescaleF);
		HandlerToTestEmissionAnalysisModules.reset();
		
		// ---- end of setup
		
		// case 3 - stop go entry in both tables, free flow entry in average table -> use average
		Id idForAvgTable = new IdImpl("link id avg");
		Id personIdForAvgTable = new IdImpl("person avg");
		String dieselVehicleInformation = passengercar +";"+ dieselTechnology+ ";"+ dieselSizeClass+";"+dieselConcept;
		
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personIdForAvgTable, roadType, dieselFreeVelocity/3.6, 1000., 1000./dieselFreeVelocity*3.6, dieselVehicleInformation);
		weam.throwWarmEmissionEvent(10, idForAvgTable, personIdForAvgTable, warmEmissions);
		
		int numberOfWarmEmissions = WarmPollutant.values().length;
		
		String message = "The expected rescaled emissions for this event are (calculated emissions * rescalefactor) = " 
				+ (numberOfWarmEmissions*avgDieselFactorFf) + " * " + rescaleF + " = " +
				(numberOfWarmEmissions*avgDieselFactorFf*rescaleF) + " but were " + HandlerToTestEmissionAnalysisModules.getSum();
		
		Assert.assertEquals(message, rescaleF*numberOfWarmEmissions*avgDieselFactorFf, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		
	}
	
	private void setUp() {
		roadTypeMapping = new HashMap<Integer, String>();
		avgHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		detailedHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		
		fillRoadTypeMapping(roadTypeMapping);
		fillAverageTable(avgHbefaWarmTable);
		fillDetailedTable(detailedHbefaWarmTable);
		
		emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		warmEmissionParameterObject = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, avgHbefaWarmTable, detailedHbefaWarmTable);
		weam = new WarmEmissionAnalysisModule(warmEmissionParameterObject, emissionEventManager, null);
	}
	
	private void fillDetailedTable(
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable) {
		
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(petrolTechnology);
		vehAtt.setHbefaSizeClass(petrolSizeClass);
		vehAtt.setHbefaEmConcept(petrolConcept);
		
		HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedPetrolFactorFf); 
		detWarmFactor.setSpeed(petrolSpeedFf);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
			
		}
		
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(petrolTechnology);
		vehAtt.setHbefaSizeClass(petrolSizeClass);
		vehAtt.setHbefaEmConcept(petrolConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedPetrolFactorSg); 
		detWarmFactor.setSpeed(petrolSpeedSg);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// entry for second test case "pc" -- should not be used
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(pcTechnology);
		vehAtt.setHbefaSizeClass(pcSizeClass);
		vehAtt.setHbefaEmConcept(pcConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedPcFactorFf); 
		detWarmFactor.setSpeed(pcfreeVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// entry for third test case "diesel"
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(dieselTechnology);
		vehAtt.setHbefaSizeClass(dieselSizeClass);
		vehAtt.setHbefaEmConcept(dieselConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedDieselFactorSg); 
		detWarmFactor.setSpeed(dieselSgVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}

		// entry for ffOnlyTestcase
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaEmConcept(ffOnlyConcept);
		vehAtt.setHbefaSizeClass(ffOnlySizeClass);
		vehAtt.setHbefaTechnology(ffOnlyTechnology);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedFfOnlyFactorFf);
		detWarmFactor.setSpeed(ffOnlyffSpeed);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// entry for sgOnlyTestcase
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaEmConcept(sgOnlyConcept);
		vehAtt.setHbefaSizeClass(sgOnlySizeClass);
		vehAtt.setHbefaTechnology(sgOnlyTechnology);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedSgOnlyFactorSg);
		detWarmFactor.setSpeed(sgOnlysgSpeed);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// entries for table testcase
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaEmConcept(tableConcept);
		vehAtt.setHbefaSizeClass(tableSizeClass);
		vehAtt.setHbefaTechnology(tableTechnology);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedTableFactorFf);
		detWarmFactor.setSpeed(tableffSpeed);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedTableFactorSg);
		detWarmFactor.setSpeed(tablesgSpeed);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		//entries for zero case
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaEmConcept(zeroConcept);
		vehAtt.setHbefaSizeClass(zeroSizeClass);
		vehAtt.setHbefaTechnology(zeroTechnology);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedZeroFactorFf);
		detWarmFactor.setSpeed(zeroFreeVelocity);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedZeroFactorSg);
		detWarmFactor.setSpeed(zeroSgVelocity);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// entries for case sg speed = ff speed
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaEmConcept(sgffConcept);
		vehAtt.setHbefaSizeClass(sgffSizeClass);
		vehAtt.setHbefaTechnology(sgffTechnology);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedSgffFactorFf);
		detWarmFactor.setSpeed(sgffDetailedFfSpeed);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaEmConcept(sgffConcept);
		vehAtt.setHbefaSizeClass(sgffSizeClass);
		vehAtt.setHbefaTechnology(sgffTechnology);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedSgffFactorSg);
		detWarmFactor.setSpeed(sgffDetailedFfSpeed);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
	}

	private void fillAverageTable(	Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable) {
		
		// entries for first case "petrol" should not be used since there are entries in the detailed table
		// free flow
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaEmConcept(petrolConcept);
		vehAtt.setHbefaSizeClass(petrolSizeClass);
		vehAtt.setHbefaTechnology(petrolTechnology);
		
		HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgPetrolFactorFf);
		detWarmFactor.setSpeed(petrolSpeedFf);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// stop and go
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgPetrolFactorSg);
		detWarmFactor.setSpeed(petrolSpeedSg);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		//entry for second test case "pc"
		// free flow
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(pcTechnology);
		vehAtt.setHbefaSizeClass(pcSizeClass);
		vehAtt.setHbefaEmConcept(pcConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgPcFactorFf); 
		detWarmFactor.setSpeed(pcfreeVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		//stop and go
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(pcTechnology);
		vehAtt.setHbefaSizeClass(pcSizeClass);
		vehAtt.setHbefaEmConcept(pcConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgPcFactorSg); 
		detWarmFactor.setSpeed(pcsgVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// entries for third test case "diesel"
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(dieselTechnology);
		vehAtt.setHbefaSizeClass(dieselSizeClass);
		vehAtt.setHbefaEmConcept(dieselConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgDieselFactorFf); 
		detWarmFactor.setSpeed(dieselFreeVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(dieselTechnology);
		vehAtt.setHbefaSizeClass(dieselSizeClass);
		vehAtt.setHbefaEmConcept(dieselConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgDieselFactorSg); 
		detWarmFactor.setSpeed(dieselSgVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		//entries for fourth test case "lpg"
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(lpgTechnology);
		vehAtt.setHbefaSizeClass(lpgSizeClass);
		vehAtt.setHbefaEmConcept(lpgConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgLpgFactorFf); 
		detWarmFactor.setSpeed(lpgFreeVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(lpgTechnology);
		vehAtt.setHbefaSizeClass(lpgSizeClass);
		vehAtt.setHbefaEmConcept(lpgConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgLpgFactorSg); 
		detWarmFactor.setSpeed(lpgSgVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		
	}

	private void fillRoadTypeMapping(Map<Integer, String> roadTypeMapping) {
		roadTypeMapping.put(0, hbefaRoadCategory);
		
	}		
	
}
	

	

