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
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.benjamin.scenarios.munich.exposure.EmissionResponsibilityTravelDisutilityCalculatorFactory;
import playground.vsp.airPollution.exposure.EmissionResponsibilityCostModule;
import playground.vsp.airPollution.exposure.GridTools;
import playground.vsp.airPollution.exposure.InternalizeEmissionResponsibilityControlerListener;
import playground.vsp.airPollution.exposure.ResponsibilityGridTools;

/**
 * @author amit
 */

public class SubPopMunichExposureControler {

	private static final Integer noOfXCells = 160;
	private static final Integer noOfYCells = 120;
	static final double xMin = 4452550.25;
	static final double xMax = 4479483.33;
	static final double yMin = 5324955.00;
	static final double yMax = 5345696.81;
	private static final Double timeBinSize = 3600.;
	private static final int noOfTimeBins = 30;

	public static void main(String[] args) {

		boolean isRunningOnCluster = false;
		if ( args.length > 0) isRunningOnCluster = true;

		if(! isRunningOnCluster){

			args = new String [] {
					FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/otherRuns/input/config_subActivities_subPop_baseCaseCtd.xml",
					"1.0",
					"true",
					"1.0",
					FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run10/policies/backcasting/test_oldMethod/",
					 };
		}

		String configFile = args[0];
		String emissionEfficiencyFactor = args[1];
		String considerCO2Costs = args[2];
		String emissionCostMultiplicationFactor = args[3];

		String outputDir = args[4];
		
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

				addPlanStrategyBinding(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.name().concat("_").concat("COMMUTER_REV_COMMUTER")).toProvider(new javax.inject.Provider<PlanStrategy>() {
					final String[] availableModes = {"car", "pt_COMMUTER_REV_COMMUTER"};
					final String[] chainBasedModes = {"car", "bike"};
					@Inject
					Scenario sc;

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new SubtourModeChoice(sc.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false, tripRouterProvider));
						builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
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
		else hbefaDirectory = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/otherRuns/input/hbefaForMatsim/";
		
		ecg.setAverageColdEmissionFactorsFile(hbefaDirectory+"EFA_ColdStart_vehcat_2005average.txt");
		ecg.setAverageWarmEmissionFactorsFile(hbefaDirectory+"EFA_HOT_vehcat_2005average.txt");
		ecg.setDetailedColdEmissionFactorsFile(hbefaDirectory+"EFA_ColdStart_SubSegm_2005detailed.txt");
		ecg.setDetailedWarmEmissionFactorsFile(hbefaDirectory+"EFA_HOT_SubSegm_2005detailed.txt");
		ecg.setUsingVehicleTypeIdAsVehicleDescription(true);

		String emissionRelatedInputFilesDir ;
		
		if(isRunningOnCluster) emissionRelatedInputFilesDir = "../../munich/input/";
		else emissionRelatedInputFilesDir = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/otherRuns/input/";
		
		ecg.setEmissionRoadTypeMappingFile(emissionRelatedInputFilesDir + "/roadTypeMapping.txt");

		ecg.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));

		GridTools gt = new GridTools(scenario.getNetwork().getLinks(), xMin, xMax, yMin, yMax, noOfXCells, noOfYCells);

		ResponsibilityGridTools rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, gt);
		ecg.setConsideringCO2Costs(Boolean.parseBoolean(considerCO2Costs));
		ecg.setEmissionCostMultiplicationFactor(Double.parseDouble(emissionCostMultiplicationFactor));

		final EmissionResponsibilityTravelDisutilityCalculatorFactory emfac = new EmissionResponsibilityTravelDisutilityCalculatorFactory();
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				bind(GridTools.class).toInstance(gt);
				bind(ResponsibilityGridTools.class).toInstance(rgt);
				bind(EmissionModule.class).asEagerSingleton();
				bind(EmissionResponsibilityCostModule.class).asEagerSingleton();
				addControlerListenerBinding().to(InternalizeEmissionResponsibilityControlerListener.class);
				bindCarTravelDisutilityFactory().toInstance(emfac);
			}
		});
		
//		controler.addControlerListener(new InternalizeEmissionResponsibilityControlerListener(emissionModule, emissionCostModule, rgt, gt));
		controler.getConfig().controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		
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
