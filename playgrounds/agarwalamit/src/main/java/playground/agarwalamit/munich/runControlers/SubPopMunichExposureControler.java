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
package playground.agarwalamit.munich.runControlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.munich.controlerListner.MyTollAveragerControlerListner;
import playground.benjamin.scenarios.munich.exposure.EmissionResponsibilityCostModule;
import playground.benjamin.scenarios.munich.exposure.EmissionResponsibilityTravelDisutilityCalculatorFactory;
import playground.benjamin.scenarios.munich.exposure.GridTools;
import playground.benjamin.scenarios.munich.exposure.InternalizeEmissionResponsibilityControlerListener;
import playground.benjamin.scenarios.munich.exposure.ResponsibilityGridTools;

/**
 * @author amit
 */

public class SubPopMunichExposureControler {

	private static Integer noOfXCells = 160;
	private static Integer noOfYCells = 120;
	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;
	private static Double timeBinSize = 3600.;
	private static int noOfTimeBins = 30;

	public static void main(String[] args) {

		boolean isRunningOnCluster = false;
		if ( args.length > 0) isRunningOnCluster = true;

		if(! isRunningOnCluster){

			args = new String [] {
					"../../../../repos/runs-svn/detEval/emissionCongestionInternalization/otherRuns/input/config_subActivities_subPop_baseCaseCtd.xml",
					"1.0",
					"true",
					"1.0",
					"../../../../repos/runs-svn/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run10/policies/backcasting/ExI/",
					"true" };
		}

		String configFile = args[0];
		String emissionEfficiencyFactor = args[1];
		String considerCO2Costs = args[2];
		String emissionCostFactor = args[3];

		String outputDir = args[4];
		
		boolean isAveragingTollAfterRePlanning = Boolean.valueOf(args [5]);

		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(outputDir);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// allowing car and ride on all links
		for (Link l : scenario.getNetwork().getLinks().values()){
			Set<String> modes = new HashSet<>(Arrays.asList("car","ride"));
			l.setAllowedModes(modes);
		}
		
		final Controler controler = new Controler(scenario);

		// following is used for commuter and reverse commuters whereas default SubTourModeChoice in config is used for rest 
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);

				addPlanStrategyBinding("SubtourModeChoice_".concat("COMMUTER_REV_COMMUTER")).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {"car", "pt_COMMUTER_REV_COMMUTER"};
					String[] chainBasedModes = {"car", "bike"};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false, tripRouterProvider));
						builder.addStrategyModule(new ReRoute(controler.getScenario(), tripRouterProvider));
						return builder.build();
					}
				});
			}
		});
		
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		controler.getConfig().addModule(ecg);
		
		ecg.setUsingDetailedEmissionCalculation(true);
		
		String hbefaDirectory;

		if (isRunningOnCluster ) hbefaDirectory = "../../matsimHBEFAStandardsFiles/";
		else hbefaDirectory = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/otherRuns/input/hbefaForMatsim/";
		
		ecg.setAverageColdEmissionFactorsFile(hbefaDirectory+"EFA_ColdStart_vehcat_2005average.txt");
		ecg.setAverageWarmEmissionFactorsFile(hbefaDirectory+"EFA_HOT_vehcat_2005average.txt");
		ecg.setDetailedColdEmissionFactorsFile(hbefaDirectory+"EFA_ColdStart_SubSegm_2005detailed.txt");
		ecg.setDetailedWarmEmissionFactorsFile(hbefaDirectory+"EFA_HOT_SubSegm_2005detailed.txt");
		
		String emissionRelatedInputFilesDir ;
		
		if(isRunningOnCluster) emissionRelatedInputFilesDir = "../../munich/input/";
		else emissionRelatedInputFilesDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/otherRuns/input/";
		
		ecg.setEmissionRoadTypeMappingFile(emissionRelatedInputFilesDir + "/roadTypeMapping.txt");
		ecg.setEmissionVehicleFile(emissionRelatedInputFilesDir + "/emissionVehicles_1pct.xml.gz");
		
		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		GridTools gt = new GridTools(scenario.getNetwork().getLinks(), xMin, xMax, yMin, yMax);
		Map<Id<Link>, Integer> links2xCells = gt.mapLinks2Xcells(noOfXCells);
		Map<Id<Link>, Integer> links2yCells = gt.mapLinks2Ycells(noOfYCells);
		
		ResponsibilityGridTools rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, links2xCells, links2yCells, noOfXCells, noOfYCells);
		EmissionResponsibilityCostModule emissionCostModule = new EmissionResponsibilityCostModule(Double.parseDouble(emissionCostFactor),	Boolean.parseBoolean(considerCO2Costs), rgt, links2xCells, links2yCells);
		final EmissionResponsibilityTravelDisutilityCalculatorFactory emfac = new EmissionResponsibilityTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(emfac);
			}
		});
		
		controler.addControlerListener(new InternalizeEmissionResponsibilityControlerListener(emissionModule, emissionCostModule, rgt, links2xCells, links2yCells));
		controler.getConfig().controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		
		if(isAveragingTollAfterRePlanning){
			controler.addControlerListener(new MyTollAveragerControlerListner());
		}

		// additional things to get the networkRoute for ride mode. For this, ride mode must be assigned in networkModes of the config file.
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
			addTravelTimeBinding("ride").to(networkTravelTime());
			addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());
			}
		});

		controler.run();
	}
}
