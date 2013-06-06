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

import java.util.HashMap;
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


/*
 * test for playground.vsp.emissions.WarmEmissionAnalysisModule
 * 
 * WarmEmissionAnalysisModule (weam) 
 * public: 
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
	
	String passengercar= "PASSENGER_CAR", heavygoodsvehicle="HEAVY_GOODS_VEHICLE"; //erluabte strings fuer veh. categorie sind genau diese
	
	// strings for test cases
	String hbefaRoadCategory = "URB";
	
	// emission factors for tables - no dublicates!
	Double detailedPetrolFactorff = .1; 
	Double detailedPetrolFactorsg = .01; 
	Double petrolSpeed = 20.;
	
	Double rescaleFactor = -.001;
	boolean excep =false;
	
	// saturated and heavy not used so far -> not tested
	HbefaTrafficSituation trafficSituationff = HbefaTrafficSituation.FREEFLOW;
	HbefaTrafficSituation trafficSituationsg = HbefaTrafficSituation.STOPANDGO;
	
	//vehicleInformation = "PASSENGER_CAR;URB;PC petrol <1,4L; <ECE;petrol (4S);<1,4L"; 
	String petrolTechnology = "PC petrol <1,4L";
	String petrolSizeClass ="<ECE petrol (4S)";
	String petrolConcept ="<1,4L";

	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions(){
		
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
		
		Id personId = new IdImpl("person 1");
		Integer roadType = 0;
		Double linkLength = 200.; // meter?
		String vehicleInformation = passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;
		
		Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeed, linkLength, linkLength/petrolSpeed, vehicleInformation);
		Assert.assertEquals(0.02, warmEmissions.get(WarmPollutant.CO2_TOTAL), MatsimTestUtils.EPSILON);
		
	}

	private void fillDetailedTable(
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable) {
		
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(petrolTechnology);
		vehAtt.setHbefaSizeClass(petrolSizeClass);
		vehAtt.setHbefaEmConcept(petrolConcept);
		
		HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedPetrolFactorff); 
		detWarmFactor.setSpeed(petrolSpeed);

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
		detWarmFactor.setWarmEmissionFactor(detailedPetrolFactorsg); 
		detWarmFactor.setSpeed(petrolSpeed);

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
		// TODO Auto-generated method stub
		
	}

	private void fillRoadTypeMapping(Map<Integer, String> roadTypeMapping) {
		roadTypeMapping.put(0, "URB");
		// TODO Auto-generated method stub
		
	}

	
	
	//TODO copy from TestColdEmissionAnalysisModule

	// TODO warmes module hat wesentlich mehr methoden! -> tests?
		
	
}
	

	

