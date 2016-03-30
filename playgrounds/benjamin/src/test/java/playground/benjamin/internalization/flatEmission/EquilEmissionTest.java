/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.benjamin.internalization.flatEmission;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.benjamin.internalization.EmissionCostFactors;
import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.EquilTestSetUp;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;

/**
 * @author amit
 */

@RunWith(Parameterized.class)
public class EquilEmissionTest {
	
	@Rule
	public MatsimTestUtils helper = new MatsimTestUtils();

	private static final Logger logger = Logger.getLogger(EquilEmissionTest.class);

	private DecimalFormat df = new DecimalFormat("#.###");

	private boolean isConsideringCO2Costs ;

	public EquilEmissionTest (boolean isConsideringCO2Costs) {
		this.isConsideringCO2Costs = isConsideringCO2Costs;
		logger.info("Each parameter will be used in all the tests i.e. all tests will be run while inclusing and excluding CO2 costs.");
	}

	@Parameters(name = "{index}: considerCO2 == {0}")
	public static List<Object> considerCO2 () {
		Object[] considerCO2 = new Object [] { true, false };
		return Arrays.asList(considerCO2);
	}

	@Test
	public void emissionTollTest () {
		
		EquilTestSetUp equilTestSetUp = new EquilTestSetUp();

		Scenario sc = equilTestSetUp.createConfig();
		equilTestSetUp.createNetwork(sc);
		equilTestSetUp.createActiveAgents(sc);
		equilTestSetUp.createPassiveAgents(sc);

		emissionSettings(sc);

		Controler controler = new Controler(sc);
		String outputDirectory = helper.getOutputDirectory() + "/" + (isConsideringCO2Costs ? "considerCO2Costs/" : "notConsiderCO2Costs/");
		sc.getConfig().controler().setOutputDirectory(outputDirectory);

		EmissionModule emissionModule = new EmissionModule(sc);
		emissionModule.setEmissionEfficiencyFactor( 1.0 );
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		EmissionCostModule emissionCostModule = new EmissionCostModule( 1.0, isConsideringCO2Costs );
		final EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule, sc.getConfig().planCalcScore());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(emissionTducf);
			}
		});
		controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));

		MyPersonMoneyEventHandler personMoneyHandler = new MyPersonMoneyEventHandler();
		controler.getEvents().addHandler(personMoneyHandler);

		controler.run();

		//TODO dont know how to catch emission event during simulation.
		EventsManager events = EventsUtils.createEventsManager();
		EmissionEventsReader reader = new EmissionEventsReader(events);
		MyEmissionEventHandler emissEventHandler = new MyEmissionEventHandler();
		events.addHandler(emissEventHandler);
		int lastIt = sc.getConfig().controler().getLastIteration();
		reader.parse(outputDirectory + "/ITERS/it."+lastIt+"/"+lastIt+".emission.events.xml.gz");

		// first check for cold emission, which are generated only on departure link.
		for (ColdEmissionEvent e : emissEventHandler.coldEvents ) {
			if( ! ( e.getLinkId().equals(Id.createLinkId(12)) || e.getLinkId().equals(Id.createLinkId(45)) ) ) {
				throw new RuntimeException("Cold emission event can occur only on departure link.");	
			}
		}

		// first coldEmission event is on departure link, thus compare money event and coldEmissionEvent
		double firstMoneyEventToll = personMoneyHandler.events.get(0).getAmount();

		Map<ColdPollutant, Double> coldEmiss = emissEventHandler.coldEvents.get(0).getColdEmissions();
		double totalColdEmissAmount = 0.;
		/*
		 * departure time is 21600, so at this time. distance = 1.0km, parking duration = 12h, vehType="PASSENGER_CAR;petrol (4S);&gt;=2L;PC-P-Euro-0"
		 * Thus, coldEmission levels => FC 22.12, HC 5.41, CO 99.97, NO2 -0.00122764240950346, NOx -0-03, PM 0, NMHC 5.12
		 * Thus coldEmissionCost = 0 * 384500. / (1000. * 1000.) + 45.12 * 1700. / (1000. * 1000.) + -0.03 * 9600. / (1000. * 1000.) = 0.008416 
		 */

		for (ColdPollutant cp :coldEmiss.keySet() ){
			totalColdEmissAmount += EmissionCostFactors.getCostFactor(cp.toString()) * coldEmiss.get(cp);
		}

		Assert.assertEquals("Cold emission toll from emission event and emission cost factors does not match from manual calculation.",totalColdEmissAmount, 0.008416, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Cold emission toll from emission event and emission cost factors does not match from money event.",firstMoneyEventToll, - totalColdEmissAmount, MatsimTestUtils.EPSILON);

		/*
		 * There are two routes --> 12-23-38-84-45 and 12-23-39-94-45. 12 is departure link --> no warmEmissionevent. 
		 * Thus, in both routes, 2nd warmEmission event is on the link  (38 or 39) with length 5000m. On this link, only free flow warm emission is thrown.
		 * warmEmission --> linkLength = 5km, FC 77.12, HC 0.08, CO 1.0700000000000001, NO2 0.01, NOX 0.36, CO2(total) 241.78, PM 0.00503000011667609, NMHC 0.07, SO2 0.00123396399430931
		 * Thus if co2Costs is not considered --> warmEmissCost = 0.36 * 5 * 9600. / (1000. * 1000.) +  0.07 * 5 * 1700. / (1000. * 1000.) + 0.00123396399430931* 5 * 11000. / (1000. * 1000.)
		 *  +   0.00503000011667609 * 5 * 384500. / (1000. * 1000.) = 0.027613043
		 *  if co2costs are considerd, then warmEmissCost = 0.01787566 +  241.78 * 5 * 70. / (1000. * 1000.) = 0.112236043
		 */
		Map<WarmPollutant, Double> warmEmiss = emissEventHandler.warmEvents.get(2).getWarmEmissions();
		double warmEmissTime = emissEventHandler.warmEvents.get(2).getTime();
		double totalWarmEmissAmount = 0.;
		for ( WarmPollutant wp : warmEmiss.keySet() ) {
			if( wp.toString().equalsIgnoreCase(WarmPollutant.CO2_TOTAL.toString()) && !isConsideringCO2Costs ) {
				//nothing to do 
			} else {
				totalWarmEmissAmount += EmissionCostFactors.getCostFactor(wp.toString()) * warmEmiss.get(wp);	
			}
		}
		double tollFromMoneyEvent = 0.; 
		for (PersonMoneyEvent e : personMoneyHandler.events) {
			// Money event at the time of 2nd warm emission event.
			if ( e.getTime() == warmEmissTime ) tollFromMoneyEvent = e.getAmount();
		}

		if ( isConsideringCO2Costs ) {
			Assert.assertEquals( "Warm emission toll from emission event and emission cost factors does not match from manual calculation.", df.format( 0.112236043 ), df.format( totalWarmEmissAmount ) );
			Assert.assertEquals("Warm emission toll from emission event and emission cost factors does not match from money event.",tollFromMoneyEvent, - totalWarmEmissAmount, MatsimTestUtils.EPSILON);
		} else {
			Assert.assertEquals("Warm emission toll from emission event and emission cost factors does not match from manual calculation.", df.format( 0.027613043 ), df.format( totalWarmEmissAmount ) );
			Assert.assertEquals("Warm emission toll from emission event and emission cost factors does not match from money event.",tollFromMoneyEvent, - totalWarmEmissAmount, MatsimTestUtils.EPSILON);
		}
	}
	
	private void emissionSettings(Scenario scenario){
		
		// since same files are used for multiple test, files are added to ONE MORE level up then the test package directory
		String packageInputDir = helper.getPackageInputDirectory();
		String inputFilesDir = packageInputDir.substring(0, packageInputDir.lastIndexOf('/') );
		inputFilesDir = inputFilesDir.substring(0, inputFilesDir.lastIndexOf('/') + 1);
		
		String roadTypeMappingFile = inputFilesDir + "/roadTypeMapping.txt";
		String emissionVehicleFile = inputFilesDir + "/equil_emissionVehicles_1pct.xml.gz";

		String averageFleetWarmEmissionFactorsFile = inputFilesDir + "/EFA_HOT_vehcat_2005average.txt";
		String averageFleetColdEmissionFactorsFile = inputFilesDir + "/EFA_ColdStart_vehcat_2005average.txt";

		boolean isUsingDetailedEmissionCalculation = true;
		String detailedWarmEmissionFactorsFile = inputFilesDir + "/EFA_HOT_SubSegm_2005detailed.txt";
		String detailedColdEmissionFactorsFile = inputFilesDir + "/EFA_ColdStart_SubSegm_2005detailed.txt";
		
		
		Config config = scenario.getConfig();
		EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
		ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
		ecg.setEmissionVehicleFile(emissionVehicleFile);

		ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
		ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);

		ecg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
		ecg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
		ecg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);

		config.addModule(ecg);
	}

	private class MyPersonMoneyEventHandler implements PersonMoneyEventHandler  {

		List<PersonMoneyEvent> events = new ArrayList<PersonMoneyEvent>();

		@Override
		public void reset(int iteration) {
			events.clear();
		}

		@Override
		public void handleEvent(PersonMoneyEvent event) {
			events.add(event);
		}
	}

	private class MyEmissionEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

		List<WarmEmissionEvent> warmEvents = new ArrayList<WarmEmissionEvent>();
		List<ColdEmissionEvent> coldEvents = new ArrayList<ColdEmissionEvent>();

		@Override
		public void reset(int iteration) {
			warmEvents.clear();
			coldEvents.clear();
		}

		@Override
		public void handleEvent(ColdEmissionEvent event) {
			coldEvents.add(event);
		}

		@Override
		public void handleEvent(WarmEmissionEvent event) {
			warmEvents.add(event);
		}
	}
}