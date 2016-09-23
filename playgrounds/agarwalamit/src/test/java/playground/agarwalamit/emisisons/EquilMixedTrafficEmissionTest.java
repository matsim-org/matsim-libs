/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.emisisons;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import playground.agarwalamit.emissions.EmissionModalTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Just setting up the equil scenario to get emissions for a mixed traffic conditions.
 * 
 * @author amit
 */

@RunWith(Parameterized.class)
public class EquilMixedTrafficEmissionTest {

	@Rule
	public final MatsimTestUtils helper = new MatsimTestUtils();
	private static final Logger logger = Logger.getLogger(EquilMixedTrafficEmissionTest.class);

	private static final String EQUIL_DIR = "../../matsim/examples/equil-mixedTraffic/";

	private boolean isConsideringCO2Costs ;

	public EquilMixedTrafficEmissionTest (boolean isConsideringCO2Costs) {
		this.isConsideringCO2Costs = isConsideringCO2Costs;
		logger.info("Each parameter will be used in all the tests i.e. all tests will be run while inclusing and excluding CO2 costs.");
	}

	@Parameterized.Parameters(name = "{index}: considerCO2 == {0}")
	public static List<Object> considerCO2 () {
		Object[] considerCO2 = new Object [] { true, false };
		return Arrays.asList(considerCO2);
	}

	@Test @Ignore
	public void emissionTollTest() {
		//see an example with detailed explanations -- package opdytsintegration.example.networkparameters.RunNetworkParameters 

		Config config = ConfigUtils.loadConfig(EQUIL_DIR + "/config.xml");
		config.controler().setOutputDirectory(helper.getOutputDirectory());

		Scenario scenario = ScenarioUtils.loadScenario(config);

		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		emissionSettings(scenario);

		Controler controler = new Controler(scenario);

		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.setEmissionEfficiencyFactor( 1.0 );
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		EmissionCostModule emissionCostModule = new EmissionCostModule( 1.0, isConsideringCO2Costs );
		final EmissionModalTravelDisutilityCalculatorFactory emissionTducf = new EmissionModalTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule, scenario.getConfig().planCalcScore());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(emissionTducf);
			}
		});
		controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));

		MyPersonMoneyEventHandler personMoneyHandler = new MyPersonMoneyEventHandler();
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				addTravelTimeBinding("bicycle").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("bicycle").to(carTravelDisutilityFactoryKey());

				addEventHandlerBinding().toInstance(personMoneyHandler);
			}
		});

		controler.run();
	}
	private void emissionSettings(Scenario scenario){
		String inputFilesDir = "../benjamin/test/input/playground/benjamin/internalization/";

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

	private class MyPersonMoneyEventHandler implements PersonMoneyEventHandler {

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
