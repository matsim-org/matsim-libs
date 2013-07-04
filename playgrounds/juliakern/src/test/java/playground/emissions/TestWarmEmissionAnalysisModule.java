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

import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.handler.EventHandler;
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
 * weamParameter
 * throw warm EmissionEvent
 * check vehicle info and calculate warm emissions
 * get free flow occurences
 * get fraction occurences
 * get stop go occurences
 * get km counter
 * get free flow km counter
 * get top go km couter
 * get warm emission event counter 
 * 
 * private: //TODO werden die implizit getestet? sonst: tests schreiben
 * rescale warm emissions
 * calculate warm emissions
 * convert string 2 tuple
 * 
 */

/* TODO delete this
 * weam module parameter //?- implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest 
 * weam - constructor, nothing to test 
 * reset - nothing to test? TODO Benjamin fragen
 * throw warm emission event
 * 
 * calculate cold emissions and throw event - calculateColdEmissionsAndThrowEventTest
 * 
 * private:
 * rescale cold emissions - rescaleColdEmissionsTest
 * calculate cold emissions - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest 
 * convert string to tuple - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest
 */

public class TestWarmEmissionAnalysisModule {
	
	int numberOfWarmPollutants= WarmPollutant.values().length;
	
	String passengercar= "PASSENGER_CAR", heavygoodsvehicle="HEAVY_GOODS_VEHICLE"; 
	
	// strings for test cases
	String hbefaRoadCategory = "URB";
	
	// emission factors for tables - no dublicates!
	Double detailedPetrolFactorFf = .1; 
	Double detailedPetrolFactorSg = .01; 
	Double detailedPcFactorSg = .001;
	Double detailedPcFactorFf = .0001;
	Double detailedDieselFactorSg= .00001;
	Double detailedDieselFactorFf = .000001;
	Double detailedFfOnlyFactorFf = .0000001;
	Double detailedSgOnlyFactorSg = .00000001;
	Double detailedTableFactorFf =  .000000001;
	Double detailedTableFactorSg =  .0000000001;
	Double avgPcFactorFf = 1.;
	Double avgPcFactorSg= 10.;
	Double avgDieselFactorFf = 100.;
	Double avgDieselFactorSg = 1000.;
	Double avgLpgFactorFf = 10000.;
	Double avgLpgFactorSg = 100000.;	
	
	
	//TODO sg speed 0
	
	Double rescaleFactor = -.001;
	int roadType =0;
	boolean excep =false;
	
	// saturated and heavy not used so far -> not tested
	HbefaTrafficSituation trafficSituationff = HbefaTrafficSituation.FREEFLOW;
	HbefaTrafficSituation trafficSituationsg = HbefaTrafficSituation.STOPANDGO;
	
	//vehicleInformation = "PASSENGER_CAR;URB;PC petrol <1,4L; <ECE;petrol (4S);<1,4L"; 
	String 	petrolTechnology = "PC petrol <1,4L",
			petrolSizeClass ="<ECE petrol (4S)",
			petrolConcept ="<1,4L";
	Double petrolSpeedFf = 20., petrolSpeedSg = 10.;
	//vehicleInformation 2 
	String 	pcTechnology = "PC petrol <1,4L <ECE", 
			pcSizeClass = "petrol (4S)", 
			pcConcept = "<1,4L";
	Double pcfreeVelocity = 50., pcsgVelocity= 10.;
	//vehicleInformation for third test case
	String 	dieselTechnology = "PC diesel", 
			dieselSizeClass = "diesel",
			dieselConcept = ">=2L";
	Double dieselFreeVelocity = 100., dieselSgVelocity = 30.;
	// vehicle information for fourth test case
	String 	lpgTechnology = "PC LPG Euro-4",
			lpgSizeClass = "LPG",
			lpgConcept = "not specified";
	Double lpgFreeVelocity = 100., lpgSgVelocity = 35.;
	Double noeFreeSpeed=100.;
	// vehicle information for fifth case - must be different to other test case's strings
	String 	noeTechnology = "technology",
			noeConcept = "concept",
			noeSizeClass = "size";
	// vehicle information for counter cases
	String sgOnlyTechnology ="bifuel CNG/petrol", sgOnlySizeClass="not specified", sgOnlyConcept="PC-Alternative Fuel";
	Double sgOnlysgSpeed = 50.;
	String ffOnlyTechnology="diesel", ffOnlySizeClass=">=2L", ffOnlyConcept="PC-D-Euro-3";
	Double ffOnlyffSpeed = 120.;
	String tableTechnology = "petrol (4S)", tableSizeClass= "<1,4L", tableConcept = "PC-P-Euro-0";
	Double tableffSpeed = 30., tablesgSpeed = 55.;
	
	//Case;VehCat;Year;TrafficScenario;Component;RoadCat;TrafficSit;Gradient;IDSubsegment;Subsegment;Technology;SizeClasse;EmConcept;KM;%OfSubsegment;V;V_0%;V_100%;EFA;EFA_0%;EFA_100%;V_weighted;V_weighted_0%;V_weighted_100%;EFA_weighted;EFA_weighted_0%;EFA_weighted_100%
	
	@Test 
	

	public void testWarmEmissionAnalysisParameter(){
		WarmEmissionAnalysisModuleParameter weamp = new WarmEmissionAnalysisModuleParameter(null, null, null);
		Assert.assertEquals(weamp.getClass(), WarmEmissionAnalysisModuleParameter.class);
		// TODO Benjamin: null als Konstructoreingabe erlaubt, exception erst bei Konstruction des WEAM
		// soll das so sein? 
		// koennten logger-warnungen/abort im weamParameter oder waem einbauen
		// auch  mit assert moeglich
	}
	
	@Test
	public void testWarmEmissionAnalysisModule_exceptions(){
		excep= false;
		
		Map<Integer, String> roadTypeMapping = new HashMap<Integer, String>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		fillRoadTypeMapping(roadTypeMapping);
		fillAverageTable(avgHbefaWarmTable);
		fillDetailedTable(detailedHbefaWarmTable);
		
		WarmEmissionAnalysisModuleParameter weamp = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, avgHbefaWarmTable, detailedHbefaWarmTable);
		Double emissionEfficiencyFactor = 1.0;
		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		
		ArrayList<ArrayList> testCases = new ArrayList<ArrayList>();		

		ArrayList<Object> testCase1= new ArrayList<Object>(), 
						testCase2= new ArrayList<Object>(),
						testCase3= new ArrayList<Object>();
		
		Collections.addAll(testCase1, null, null, null);
		//Collections.addAll(testCaseXX, weamp, emissionEventManager, null); //TODO soll das failen? nein -> text schreiben
		Collections.addAll(testCase2, weamp, null, emissionEfficiencyFactor);
		Collections.addAll(testCase3, null, emissionEventManager, emissionEfficiencyFactor);
		
		testCases.add(testCase1); 
		// testCases.add(testCase2); //TODO Benjamin hier keinen Handler zu uebergeben wirft keine Fehler! 
		testCases.add(testCase3);  
		
		for(List<Object> tc: testCases){
			try{
				WarmEmissionAnalysisModule weam = new WarmEmissionAnalysisModule((WarmEmissionAnalysisModuleParameter)tc.get(0), (EventsManager)tc.get(1),(Double)tc.get(2));
			}catch(NullPointerException e){
				excep = true;
			}catch(Exception f){
				System.out.println(f.getMessage());
			}
			Assert.assertTrue("initilizing a warm emission analysis module with 'null' input should fail", excep);
			excep= false;
		}		
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent(){
		
		//-- set up tables, event handler, parameters, module
		Map<Integer, String> roadTypeMapping = new HashMap<Integer, String>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		fillRoadTypeMapping(roadTypeMapping);
		fillAverageTable(avgHbefaWarmTable);
		fillDetailedTable(detailedHbefaWarmTable);
		
		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
			
		//TODO what happens if these are not set correctly?
		WarmEmissionAnalysisModuleParameter warmEmissionParameterObject = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, avgHbefaWarmTable, detailedHbefaWarmTable);
		//emission efficicy factor 'null' -- no rescaling
		WarmEmissionAnalysisModule weam = new WarmEmissionAnalysisModule(warmEmissionParameterObject, emissionEventManager, null);
		//-- end of set up
		
		//-- test cases
		// 1. ff + sg im detailed table (TODO doppelten eintrag fuer aver. table machen, der soll nicht benutzt werden)
		Id personId = new IdImpl("person 1");
		Id vehicleId = new IdImpl("veh 1");
		int leaveTime = 0;
		Integer roadType = 0;
		Double linkLength = 200.; // meter?
		String vehicleInformation = passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;
		// 2. ff im detailed table, sg nicht -> use average
		// gefahrene geschw? --- zwei tests....
		Id pcPersonId = new IdImpl("person 2"); 
		Id pcVehicleId = new IdImpl("veh 2");
		Double pclinkLength= 100.;
		String pcVehicleInformation = passengercar + ";"+ pcTechnology + ";"+pcSizeClass+";"+pcConcept;
		// 3. ff nicht im detailed table, sg schon -> use average
		Id dieselPersonId = new IdImpl("person 3");
		Id dieselVehicleId = new IdImpl("veh 3");
		Double dieselLinkLength= 20.;
		String dieselVehicleInformation = passengercar +";"+ dieselTechnology+ ";"+ dieselSizeClass+";"+dieselConcept;
		// 4. ff und sg nicht im detailed table -> use average
		Id lpgPersonId = new IdImpl("person4");
		Id lpgVehicleId = new IdImpl("veh 4");
		Double lpgLinkLength = 700.;
		String lpgVehicleInformation = passengercar + ";"+ lpgTechnology+";"+lpgSizeClass+";"+lpgConcept;
	
		
		//first test case
		//TODO *3600?
		Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf, linkLength, linkLength/petrolSpeedFf, vehicleInformation);
		Assert.assertEquals(0.02, warmEmissions.get(WarmPollutant.CO2_TOTAL), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, personId, vehicleId, warmEmissions);
		Assert.assertEquals(0.18, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
		
		// second test case - two sub cases: free flow speed and stopgo speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcPersonId, roadType, pcfreeVelocity, pclinkLength, pclinkLength/pcfreeVelocity*3600, pcVehicleInformation);
		Assert.assertEquals(avgPcFactorFf, warmEmissions.get(WarmPollutant.NMHC), MatsimTestUtils.EPSILON);
		weam.throwWarmEmissionEvent(leaveTime, pcPersonId, pcVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgPcFactorFf, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();

		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcPersonId, roadType, pcfreeVelocity, pclinkLength, pclinkLength/pcsgVelocity*3600, pcVehicleInformation);
		// hier werden ff werte benutzt!! sollte aber n ciht so sein
		//Assert.assertEquals(avgPcFactorSg, warmEmissions.get(WarmPollutant.NMHC), MatsimTestUtils.EPSILON);
		weam.throwWarmEmissionEvent(leaveTime, pcPersonId, pcVehicleId, warmEmissions);
		//Assert.assertEquals(numberOfWarmPollutants*avgPcFactorSg, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
		
		// third test case - speed = ff... sollte dann avgtable trotzdem benutzt werden?
		// im avg-table ff eintrag aber kein sg-eintrag
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselPersonId, roadType, dieselFreeVelocity, dieselLinkLength, dieselLinkLength/dieselFreeVelocity*3600, dieselVehicleInformation);
		//Assert.assertEquals(avgDieselFactorFf, warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, dieselPersonId, dieselVehicleId, warmEmissions);
		//Assert.assertEquals(numberOfWarmPollutants*avgDieselFactorFf, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
		
		//fourt test case 
		// two sub cases- speed: free flow, stop go
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(lpgPersonId, roadType, lpgFreeVelocity, lpgLinkLength, lpgLinkLength/lpgFreeVelocity*3600, lpgVehicleInformation);
		//Assert.assertEquals(avgLpgFactorFf, warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, lpgPersonId, lpgVehicleId, warmEmissions);
		//Assert.assertEquals(numberOfWarmPollutants*avgLpgFactorFf, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();

		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(lpgPersonId, roadType, lpgFreeVelocity, lpgLinkLength, lpgLinkLength/lpgSgVelocity*3600, lpgVehicleInformation);
		//Assert.assertEquals(avgLpgFactorSg, warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, lpgPersonId, lpgVehicleId, warmEmissions);
		//Assert.assertEquals(numberOfWarmPollutants*avgLpgFactorSg, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions(){
		//-- set up tables, event handler, parameters, module
		Map<Integer, String> roadTypeMapping = new HashMap<Integer, String>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		fillRoadTypeMapping(roadTypeMapping);
		fillAverageTable(avgHbefaWarmTable);
		fillDetailedTable(detailedHbefaWarmTable);
		
		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
			
		WarmEmissionAnalysisModuleParameter warmEmissionParameterObject = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, avgHbefaWarmTable, detailedHbefaWarmTable);
		//emission efficicy factor 'null' -- no rescaling
		WarmEmissionAnalysisModule weam = new WarmEmissionAnalysisModule(warmEmissionParameterObject, emissionEventManager, null);
		//-- end of set up
		
		//-- test cases
		// 5. in keiner tabelle - exception!
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
		
		// 6. ""
		noePersonId = new IdImpl("person 6");
		noeVehicleId = new IdImpl("veh 6");
		noeVehicleInformation = "";
		
		excep= false;
		try{
			Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noePersonId, roadType, noeFreeSpeed, 22., 1.5*22./noeFreeSpeed, noeVehicleInformation);
			weam.throwWarmEmissionEvent(10., noePersonId, noeVehicleId, warmEmissions);
		}catch(Exception e){
			excep = true;
		}
		Assert.assertTrue(excep); excep=false;
		
		// 7. ";;;"
		noePersonId = new IdImpl("person 7");
		noeVehicleId = new IdImpl("veh 7");
		noeVehicleInformation = ";;;";
		
		excep= false;
		try{
			Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noePersonId, roadType, noeFreeSpeed, 22., 1.5*22./noeFreeSpeed, noeVehicleInformation);
			weam.throwWarmEmissionEvent(10., noePersonId, noeVehicleId, warmEmissions);
		}catch(Exception e){
			excep = true;
		}
		Assert.assertTrue(excep); excep=false;
		
		// 8. veh string =  null
		noePersonId = new IdImpl("person 8");
		noeVehicleId = new IdImpl("veh 8");
		noeVehicleInformation = null;
		
		excep= false;
		try{
			Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noePersonId, roadType, noeFreeSpeed, 22., 1.5*22./noeFreeSpeed, noeVehicleInformation);
			weam.throwWarmEmissionEvent(10., noePersonId, noeVehicleId, warmEmissions);
		}catch(Exception e){
			excep = true;
		}
		Assert.assertTrue(excep); excep=false;
		// linklenght 0., speed0? 
		// minimal veh info passcar
		// heavy goods veh
		// avg > free velocity
		// sg> free
		// sg < avg < free
		// avg < sg
		//TODO rechnung pruefen bei geschw. zwischen free und sg
	}
	
	
	@Test
	public void testCounters1(){
		WarmEmissionAnalysisModule weam = setupForCounterTest();
		weam.reset();
		Map<WarmPollutant, Double> warmEmissions;
		
		// ff + sg im detailed table 
		Id personId = new IdImpl("person 1");
		Integer roadType = 0;
		Double linkLength = 2*1000.; //in meter
		String vehicleInformation = passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;

		// <stop&go speed
		Double travelTime = linkLength/petrolSpeedSg*1.2; //linkLength/petrolSpeedSg = travelTimeSg
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
		// in seconds
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
		WarmEmissionAnalysisModule weam = setupForCounterTest();
		weam.reset();
		Map<WarmPollutant, Double> warmEmissions;
		
		// ff und sg nicht im detailed table -> use average
		Id personId = new IdImpl("person4");
		Double lpgLinkLength = 2000.*1000;
		String lpgVehicleInformation = passengercar + ";"+ lpgTechnology+";"+lpgSizeClass+";"+lpgConcept;
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
		WarmEmissionAnalysisModule weam = setupForCounterTest();
		weam.reset();
		Map<WarmPollutant, Double> warmEmissions;
		
		// c) ff im detailed table, sg nicht -> use average
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
		WarmEmissionAnalysisModule weam = setupForCounterTest();
		weam.reset();
		Map<WarmPollutant, Double> warmEmissions;
		
						// d) ff nicht im detailed table, sg schon -> use average
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
		WarmEmissionAnalysisModule weam = setupForCounterTest();
		weam.reset();
		Map<WarmPollutant, Double> warmEmissions;
		
		// g) uebergebener ff-speed != ff in tabelle .... use case a)
		Id inconffPersonId = new IdImpl("person 7");
		Double inconff = 30. * 1000;
		Double inconffavgSpeed = petrolSpeedFf*2.2;
		String inconffVehicleInformation = passengercar + ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;
		//TODO Benjamin: inkonsistenter ff-speed wird nicht ueberprueft. darf das so bleiben?
		// wenn ja: diese methode unnoetig -> dok, dann loeschen
		
		//zu hohe ff-geschwindigkeit uebergeben, reisezeit entspricht aber 'richtigen' ff werten
		warmEmissions =weam.checkVehicleInfoAndCalculateWarmEmissions(inconffPersonId, roadType, inconffavgSpeed/3.6, inconff, inconff/petrolSpeedFf*3.6, inconffVehicleInformation);
		Assert.assertEquals(1, weam.getFractionOccurences());
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(inconff/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		
		weam.reset();
		warmEmissions =weam.checkVehicleInfoAndCalculateWarmEmissions(inconffPersonId, roadType, inconffavgSpeed/3.6, inconff, inconff/inconffavgSpeed*3.6, inconffVehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(inconff/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		
		warmEmissions =weam.checkVehicleInfoAndCalculateWarmEmissions(inconffPersonId, roadType, inconffavgSpeed/3.6, inconff, 2*inconff/(petrolSpeedFf+petrolSpeedSg)*3.6, inconffVehicleInformation);
	}
	@Test
	public void testCounters7(){
		WarmEmissionAnalysisModule weam = setupForCounterTest();
		weam.reset();
		Map<WarmPollutant, Double> warmEmissions;
	
		// h) 
		Id tablePersonId = new IdImpl("person 8");
		Double tableLinkLength= 30.*1000;
		String tableVehicleInformation = passengercar + ";" + tableTechnology +";" + tableSizeClass+";"+tableConcept;
		
		//TODO Benjamin: falls die sg-geschwindigkeit > ff-geschwindigkeit 
		// wird das beim Erstellen der Tabelle ueberprueft?
		
		// ff < avg < sg - wird als free flow behandelt
		weam.checkVehicleInfoAndCalculateWarmEmissions(tablePersonId, roadType, tableffSpeed/3.6, tableLinkLength, 2* tableLinkLength/(tableffSpeed+tablesgSpeed)*3.6, tableVehicleInformation);

		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(tableLinkLength/1000., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(tableLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// ff < sg < avg - wird genauso als ff behandelt
		// avg < ff < sg - wird als sg behandelt
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
		WarmEmissionAnalysisModule weam = setupForCounterTest();
		weam.reset();
		Map<WarmPollutant, Double> warmEmissions;
		
		// ff + sg im detailed table 
		Id personId = new IdImpl("person 1");
		Integer roadType = 0;
		Double linkLength = 2*1000.; //in meter
		String vehicleInformation = passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;
		Double travelTime = linkLength/petrolSpeedSg*1.2; //linkLength/petrolSpeedSg = travelTimeSg
		
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
			
				// fallunterscheidung: a) detailed table b)avg. table c) ff im detailed, s&g im avg. d) ff im avg. s&g im detailed
				//			e) ff nicht vorhanden f) s&g nicht vorhanden
				// g) uebergebener ff-speed stimmt nicht mit tabelle ueberein h) ff<s&g (ist der interessante fall dann, dass die tatsaechliche geschw. dazwischen liegt?
				
				// fallunterscheidung geschwindigkeit: i) unter s&g, ii) s&g, iii) s&g<xx<ff iv) ff v) >ff


	private WarmEmissionAnalysisModule setupForCounterTest() {
		//-- set up tables, event handler, parameters, module
		Map<Integer, String> roadTypeMapping = new HashMap<Integer, String>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>();
		fillRoadTypeMapping(roadTypeMapping);
		fillAverageTable(avgHbefaWarmTable);
		fillDetailedTable(detailedHbefaWarmTable);
		
		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
			
		WarmEmissionAnalysisModuleParameter warmEmissionParameterObject = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, avgHbefaWarmTable, detailedHbefaWarmTable);
		//emission efficicy factor 'null' -- no rescaling
		WarmEmissionAnalysisModule weam = new WarmEmissionAnalysisModule(warmEmissionParameterObject, emissionEventManager, null);
		//-- end of set up
		return weam;
	}
	
	

	

	
	@Test
	public void testRescaleWarmEmissions(){
		//TODO
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
		
	}

	private void fillAverageTable(
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable) {
		
		//entry for second test case "pc"
		// free flow
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(pcTechnology);
		vehAtt.setHbefaSizeClass(pcSizeClass);
		vehAtt.setHbefaEmConcept(pcConcept);
		
		HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor();
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
		roadTypeMapping.put(0, "URB");
		// TODO Auto-generated method stub
		
	}		
	
}
	

	

