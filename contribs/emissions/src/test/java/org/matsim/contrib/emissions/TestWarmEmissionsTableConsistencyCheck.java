/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Map;

/**
 *  @author kturner
 *
 *  Test the different levels of consistency checks for the provided emission tables
 */
public class TestWarmEmissionsTableConsistencyCheck {

	private final Link link = generateLink();
	private final Vehicle vehicleFull = generateFullSpecifiedVehicle();

//TODO Use a second table were all combination should also pass... & introduce another block of tests.

// ---------- Table with pass. car & HGV -------------

	/**
	 * HbefaTableConsistencyCheckingLevel: allCombinations
	 * -> should fail, because  all combination does not work for tables with pass. cas and HGV due to different technologies definition.
	 */
	@Test(expected=RuntimeException.class)
	public void testConsistency_AllCombinations() {
		EmissionModule emissionModule = setUpScenario(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.allCombinations);

		double travelTimeOnLink = 21; //sec. approx freeSpeed of link12 is : (200 m) / (9.72.. m/s) approx 20.57 s
		Map<Pollutant, Double> warmEmissions = emissionModule.getWarmEmissionAnalysisModule().checkVehicleInfoAndCalculateWarmEmissions(vehicleFull, link, travelTimeOnLink);

		double expectedValue = 1.7685253144E9; // = 200m * 151.7492371 g/km
		Assert.assertEquals( expectedValue, warmEmissions.get(Pollutant.NOx), MatsimTestUtils.EPSILON );
	}

	/**
	 * HbefaTableConsistencyCheckingLevel: perVehCat
	 * -> should pass.
	 */
	@Test
	public void testConsistency_PerVehCat() {
		EmissionModule emissionModule = setUpScenario(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.perVehCat);

		double travelTimeOnLink = 21; //sec. approx freeSpeed of link12 is : (200 m) / (9.72.. m/s) approx 20.57 s
		Map<Pollutant, Double> warmEmissions = emissionModule.getWarmEmissionAnalysisModule().checkVehicleInfoAndCalculateWarmEmissions(vehicleFull, link, travelTimeOnLink);

		double expectedValue = 1.7685253144E9; // = 200m * 151.7492371 g/km
		Assert.assertEquals( expectedValue, warmEmissions.get(Pollutant.NOx), MatsimTestUtils.EPSILON );
	}

	/**
	 * HbefaTableConsistencyCheckingLevel: consistend
	 * -> should pass.
	 */
	@Test
	public void testConsistency_Consistent() {
		EmissionModule emissionModule = setUpScenario(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent);

		double travelTimeOnLink = 21; //sec. approx freeSpeed of link12 is : (200 m) / (9.72.. m/s) approx 20.57 s
		Map<Pollutant, Double> warmEmissions = emissionModule.getWarmEmissionAnalysisModule().checkVehicleInfoAndCalculateWarmEmissions(vehicleFull, link, travelTimeOnLink);

		double expectedValue = 1.7685253144E9; // = 200m * 151.7492371 g/km
		Assert.assertEquals( expectedValue, warmEmissions.get(Pollutant.NOx), MatsimTestUtils.EPSILON );
	}

	/**
	 * HbefaTableConsistencyCheckingLevel: none
	 * -> should pass.
	 */
	@Test
	public void testConsistency_None() {
		EmissionModule emissionModule = setUpScenario(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.none);

		double travelTimeOnLink = 21; //sec. approx freeSpeed of link12 is : (200 m) / (9.72.. m/s) approx 20.57 s
		Map<Pollutant, Double> warmEmissions = emissionModule.getWarmEmissionAnalysisModule().checkVehicleInfoAndCalculateWarmEmissions(vehicleFull, link, travelTimeOnLink);

		double expectedValue = 1.7685253144E9; // = 200m * 151.7492371 g/km
		Assert.assertEquals( expectedValue, warmEmissions.get(Pollutant.NOx), MatsimTestUtils.EPSILON );
	}

// ---------- setup and helper methods -------------	

	/**
	 * load and prepare the scenario, create the emissionsModule
	 *
	 * @param consistencyCheckingLevel the EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel
	 * @return EmissionsModule
	 */
	private EmissionModule setUpScenario(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel consistencyCheckingLevel) {
		var scenarioUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario" );
		Config config = ConfigUtils.createConfig();
		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort);
		emissionsConfig.setHbefaTableConsistencyCheckingLevel(consistencyCheckingLevel);
		emissionsConfig.setHbefaRoadTypeSource(EmissionsConfigGroup.HbefaRoadTypeSource.fromLinkAttributes);			//Somehow needed even if deprecated, since a null pointer exception ids thrown when not set :( . kmt mar'20
		emissionsConfig.setDetailedColdEmissionFactorsFile(IOUtils.extendUrl( scenarioUrl, "sample_41_EFA_ColdStart_SubSegm_2020detailed.txt").toString());
		emissionsConfig.setDetailedWarmEmissionFactorsFile(IOUtils.extendUrl( scenarioUrl,"sample_41_EFA_HOT_SubSegm_2020detailed_2Pollutants_Car_LCV_HGV.csv").toString());

		Scenario scenario = ScenarioUtils.loadScenario( config );

		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();

		return new EmissionModule(scenario, emissionEventManager);
	}

	private Link generateLink() {
		Link link = TestWarmEmissionAnalysisModule.createMockLink("link1", 200, 9.72);
		EmissionUtils.setHbefaRoadType(link, "URB/Local/50" );
		return link;
	}

	private Vehicle generateFullSpecifiedVehicle() {
		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("dieselTruckFullSpecified", VehicleType.class));
		EngineInformation engineInformation = vehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory(engineInformation, "HEAVY_GOODS_VEHICLE");
		VehicleUtils.setHbefaTechnology(engineInformation,"diesel");
		VehicleUtils.setHbefaEmissionsConcept(engineInformation, "HGV D Euro-I");
		VehicleUtils.setHbefaSizeClass(engineInformation, "RT >7.5-12t");

		return VehicleUtils.createVehicle(Id.createVehicleId("dieselTruckFullSpecified"), vehicleType);
	}
}
