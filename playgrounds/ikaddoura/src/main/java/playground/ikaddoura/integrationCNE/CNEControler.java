/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.integrationCNE;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.example.EmissionControlerListener;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.NoiseCalculationOnline;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.routing.NoiseTollTimeDistanceTravelDisutilityFactory;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
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
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.InternalizationEmissionAndCongestion.EmissionCongestionTravelDisutilityCalculatorFactory;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.joint.JointCalibrationControler;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripCongestionNoiseAnalysisMain;
import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.ikaddoura.analysis.vtts.VTTScomputation;
import playground.ikaddoura.integrationCN.TollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.integrationCN.VTTSNoiseTollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.integrationCN.VTTSTollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.router.VTTSCongestionTollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.router.VTTSTimeDistanceTravelDisutilityFactory;
import playground.vsp.congestion.controler.AdvancedMarginalCongestionPricingContolerListener;
import playground.vsp.congestion.controler.CongestionAnalysisControlerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;

/**
 * 
 * A controler to start a simultaneous congestion, noise and air pollution pricing run or other joint pricing runs.
 * 
 * TODO: add switch / add different congestion pricing approaches
 * 
 * TODO: add switch / add exhaust emission pricing approaches
 * 
 * TODO: test: money event for each effect + consideration in travel disutility for each effect
 * 
 * TODO: compare simulation outcome with previous runs, e.g. Munich congestion + exhaust emission pricing
 * 
 * TODO: make the code look nicer (requires some modifications in BK's playground)
 * 
 * @author ikaddoura, amit
 *
 */

public class CNEControler {
	private static final Logger log = Logger.getLogger(CNEControler.class);

	private static String configFile;
	private static String outputDirectory;
	private static double sigma;
		
	private static boolean congestionPricing;	
	private static boolean noisePricing;
	private static boolean airPollutionPricing;
	
	private static boolean useTripAndAgentSpecificVTTSForRouting;
	private static CaseStudy caseStudy;
	
//	private static AirPollutionPricingApproach airPollutionPricingApproach;		
//	private static CongestionPricingApproach congestionPricingApproach;
	
//	private enum CongestionPricingApproach {
//		V3, V10, Decongestion
//	}
//	
//	private enum AirPollutionPricingApproach {
//		Emission, Exposure
//	}

	protected enum CaseStudy {
		Berlin, Munich, Test
	}
	
	
	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {
								
			configFile = args[0];
			log.info("Config file: " + configFile);
			
			sigma = Double.parseDouble(args[1]);
			log.info("Sigma: " + sigma);
			
			congestionPricing = Boolean.parseBoolean(args[2]);
			log.info("Congestion Pricing: " + congestionPricing);
			
			noisePricing = Boolean.parseBoolean(args[3]);
			log.info("Noise Pricing: " + noisePricing);
			
			airPollutionPricing = Boolean.parseBoolean(args[4]);
			log.info("Air Pollution Pricing: " + airPollutionPricing);
			
			// TODO: add the other parameters here
			
			throw new RuntimeException("Not yet implemented. Aborting...");
			
		} else {
			
			configFile = "../../../runs-svn/cne_test/input/config.xml";
			
			congestionPricing = false;
			noisePricing = false;
			airPollutionPricing = false;
			
			useTripAndAgentSpecificVTTSForRouting = false;
			caseStudy = CaseStudy.Test;
			outputDirectory = null;
			
			sigma = 0.;
			
//			airPollutionPricingApproach = AirPollutionPricingApproach.Emission;
//			congestionPricingApproach = CongestionPricingApproach.Decongestion;
		}
				
		CNEControler cnControler = new CNEControler();
		cnControler.run(configFile, outputDirectory, congestionPricing, noisePricing, airPollutionPricing, caseStudy);
	}

	public void run(String configFile, String outputDirectory, boolean congestionPricing, boolean noisePricing, boolean airPollutionPricing, CaseStudy caseStudy) {
				
		Config config = ConfigUtils.loadConfig(configFile);
		if (outputDirectory == null) {
			if (config.controler().getOutputDirectory() == null || config.controler().getOutputDirectory() == "") {
				throw new RuntimeException("Either provide an output directory in the config file or the controler. Aborting...");
			} else {
				log.info("Using the output directory given in the config file...");
			}
			
		} else {
			if (config.controler().getOutputDirectory() == null || config.controler().getOutputDirectory() == "") {
				log.info("Using the output directory provided in the controler.");
			} else {
				log.warn("The output directory in the config file will overwritten by the directory provided in the controler.");
			}
			config.controler().setOutputDirectory(outputDirectory);
		}
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		final VTTSHandler vttsHandler;
		if (useTripAndAgentSpecificVTTSForRouting) {
			log.info("Using the agent- and trip-specific VTTS for routing.");
			vttsHandler = new VTTSHandler(scenario);
			controler.addControlerListener(new VTTScomputation(vttsHandler));
		} else {
			log.info("Using the approximate and uniform VTTS for routing: (-beta_traveling + beta_performing) / beta_money");
			vttsHandler = null;
		}
						
		// ########################## Noise ##########################
		
		NoiseConfigGroup ncg = new NoiseConfigGroup();
		controler.getConfig().addModule(ncg);
		
		if (noisePricing) {	
			ncg.setInternalizeNoiseDamages(true);
		} else {
			ncg.setInternalizeNoiseDamages(false);
		}

		// computation of noise events
		NoiseContext noiseContext = new NoiseContext(scenario);
		controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
		
		// ########################## Air pollution ##########################
				
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		controler.getConfig().addModule(ecg);
		
		if (caseStudy.toString().equals(CaseStudy.Munich.toString())) {
			
			// Please move as much as possible scenario-specific setting to the config... We can then get rid of these lines of code.
						
			ecg.setAverageColdEmissionFactorsFile("../../../shared-svn/projects/detailedEval/emissions/hbefaForMatsim/EFA_ColdStart_vehcat_2005average.txt");
			ecg.setAverageWarmEmissionFactorsFile("../../../shared-svn/projects/detailedEval/emissions/hbefaForMatsim/EFA_HOT_vehcat_2005average.txt");
			ecg.setDetailedColdEmissionFactorsFile("../../../shared-svn/projects/detailedEval/emissions/hbefaForMatsim/EFA_ColdStart_SubSegm_2005detailed.txt");
			ecg.setDetailedWarmEmissionFactorsFile("../../../shared-svn/projects/detailedEval/emissions/hbefaForMatsim/EFA_HOT_SubSegm_2005detailed.txt");
			
			ecg.setEmissionRoadTypeMappingFile("../../../runs-svn/detEval/emissionCongestionInternalization/iatbr/input/roadTypeMapping.txt");
			config.vehicles().setVehiclesFile("../../../runs-svn/detEval/emissionCongestionInternalization/iatbr/input/emissionVehicles_1pct.xml.gz");
		
			ecg.setUsingDetailedEmissionCalculation(true); 

		} else if (caseStudy.toString().equals(CaseStudy.Berlin.toString())) {
			
			throw new RuntimeException("Not yet implemented. Aborting...");
			// TODO: Use default factors, generate vehicles and roadtypes for berlin
			
		} else if (caseStudy.toString().equals(CaseStudy.Test.toString())) {
			
			// everything specified in config
			
		} else {
			throw new RuntimeException("Unknown case study. Aborting...");
		}
		
		String emissionEfficiencyFactor ="1.0";
		String considerCO2Costs = "true";
		String emissionCostFactor = "1.0";
		
		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		
		Set<Id<Link>> hotSpotLinks = null;
		
		// ########################## Pricing setup ##########################
				
		if (congestionPricing && noisePricing == false && airPollutionPricing == false) {
			// only congestion pricing
			
			final TollHandler congestionTollHandlerQBP = new TollHandler(scenario);
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				final VTTSCongestionTollTimeDistanceTravelDisutilityFactory factory = new VTTSCongestionTollTimeDistanceTravelDisutilityFactory(
						new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, config.planCalcScore()),
						congestionTollHandlerQBP, config.planCalcScore()
					);
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				}); 
				
			} else {
				final CongestionTollTimeDistanceTravelDisutilityFactory factory = new CongestionTollTimeDistanceTravelDisutilityFactory(
						new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config.planCalcScore()),
						congestionTollHandlerQBP, config.planCalcScore()
					);
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				}); 
			}
			
			// computation of congestion events + consideration in scoring
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(scenario, congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), scenario)));	
			
			// air pollution computation
			controler.addControlerListener(new EmissionControlerListener());
			
		} else if (congestionPricing == false && noisePricing && airPollutionPricing == false) {
			// only noise pricing
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				final VTTSNoiseTollTimeDistanceTravelDisutilityFactory factory = new VTTSNoiseTollTimeDistanceTravelDisutilityFactory(
						new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, config.planCalcScore()),
						noiseContext, config.planCalcScore()
					);
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				});
			} else {
				final NoiseTollTimeDistanceTravelDisutilityFactory factory = new NoiseTollTimeDistanceTravelDisutilityFactory(
						new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config.planCalcScore()),
						noiseContext, config.planCalcScore()
					);
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				});
			}
			
			// computation of congestion events + NO consideration in scoring
			controler.addControlerListener(new CongestionAnalysisControlerListener(new CongestionHandlerImplV3(controler.getEvents(), scenario)));
			
			// air pollution computation
			controler.addControlerListener(new EmissionControlerListener());			

		} else if (congestionPricing == false && noisePricing == false && airPollutionPricing) {
			// only air pollution pricing
			
			final EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));

			if (useTripAndAgentSpecificVTTSForRouting) {
				throw new RuntimeException("Not yet implemented. Aborting..."); // TODO

			} else {				
				
				if (sigma != 0.) {
					throw new RuntimeException("The following travel disutility doesn't allow for randomness. Aborting...");
				}
				
				// TODO: doesn't account for randomness, doesn't account for marginal utility of distance, ... 
				final EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, 
						emissionCostModule, config.planCalcScore());
				emissionTducf.setHotspotLinks(hotSpotLinks);
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bindCarTravelDisutilityFactory().toInstance(emissionTducf);
					}
				});
			}
			
			controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));
			
		} else if (congestionPricing && noisePricing && airPollutionPricing) {
			// congestion + noise + airPollution pricing
			
			final EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));
			final TollHandler congestionTollHandlerQBP = new TollHandler(scenario);
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				final VttsCNETimeDistanceTravelDisutilityFactory factory = new VttsCNETimeDistanceTravelDisutilityFactory(
						new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, config.planCalcScore()),
						emissionModule, emissionCostModule,
						noiseContext,
						congestionTollHandlerQBP,
						config.planCalcScore()
					);
				factory.setSigma(sigma);
				factory.setHotspotLinks(null);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				}); 
			} else {
				final CNETimeDistanceTravelDisutilityFactory factory = new CNETimeDistanceTravelDisutilityFactory(
						new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config.planCalcScore()),
						emissionModule, emissionCostModule,
						noiseContext,
						congestionTollHandlerQBP,
						config.planCalcScore()
					);
				factory.setSigma(sigma);
				factory.setHotspotLinks(hotSpotLinks);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				}); 				
			}
				
			// computation of congestion events + consideration in scoring
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(scenario, congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), scenario)));						
		
			// air pollution computation
			controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));
			
		} else if (congestionPricing && noisePricing && airPollutionPricing == false) {
			// congestion + noise pricing
			
			final TollHandler congestionTollHandlerQBP = new TollHandler(scenario);
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				final VTTSTollTimeDistanceTravelDisutilityFactory factory = new VTTSTollTimeDistanceTravelDisutilityFactory(
						new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, config.planCalcScore()),
						noiseContext,
						congestionTollHandlerQBP, config.planCalcScore()
					);
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				});
			} else {
				final TollTimeDistanceTravelDisutilityFactory factory = new TollTimeDistanceTravelDisutilityFactory(
						new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config.planCalcScore()),
						noiseContext,
						congestionTollHandlerQBP, config.planCalcScore()
					);
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				});
			} 
				
			// computation of congestion events + consideration in scoring
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(scenario, congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), scenario)));						
		
			// air pollution computation
			controler.addControlerListener(new EmissionControlerListener());
			
		} else if (congestionPricing && noisePricing == false && airPollutionPricing) {
			
			// congestion + air pollution pricing

			final TollHandler congestionTollHandlerQBP = new TollHandler(scenario);
			
			final EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				throw new RuntimeException("Not yet implemented. Aborting..."); // TODO?!

			} else {
				
				if (sigma != 0.) {
					throw new RuntimeException("The following travel disutility doesn't allow for randomness. Aborting...");
				}
				
				// TODO: doesn't account for randomness, doesn't account for marginal utility of distance, ... 
				final EmissionCongestionTravelDisutilityCalculatorFactory emissionCongestionTravelDisutilityCalculatorFactory = 
						new EmissionCongestionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule, congestionTollHandlerQBP, config.planCalcScore());
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bindCarTravelDisutilityFactory().toInstance(emissionCongestionTravelDisutilityCalculatorFactory);
					}
				});
				
				// air pollution computation
				controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));
				
				// computation of congestion events + consideration in scoring
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(scenario, congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), scenario)));
			}
					
		} else if (congestionPricing == false && noisePricing == false && airPollutionPricing == false) {
			// no pricing
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				final VTTSTimeDistanceTravelDisutilityFactory factory = new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, config.planCalcScore());
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				});
			} else {
				final RandomizingTimeDistanceTravelDisutilityFactory factory = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config.planCalcScore());
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				});
			}
			
			// computation of congestion events + NO consideration in scoring
			controler.addControlerListener(new CongestionAnalysisControlerListener(new CongestionHandlerImplV3(controler.getEvents(), scenario)));
		
			// air pollution computation
			controler.addControlerListener(new EmissionControlerListener());

		} else {
			throw new RuntimeException("Not yet implemented. Aborting...");
		}
		
		// ########################## Scenario-specific settings ##########################
				
		if (caseStudy.toString().equals(CaseStudy.Munich.toString())) {
			
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					String ug = "Rev_Commuter";
					addPlanStrategyBinding(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.name().concat("_").concat(ug)).toProvider(new javax.inject.Provider<PlanStrategy>() {
						final String[] availableModes = {"car", "pt_".concat(ug)};
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
		}
		
		// ##################################################################################
		
		controler.addOverridingModule(new OTFVisFileWriterModule());
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// ##################################### Analysis ###################################

		PersonTripCongestionNoiseAnalysisMain analysis = new PersonTripCongestionNoiseAnalysisMain(controler.getConfig().controler().getOutputDirectory());
		analysis.run();
		
		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		String OUTPUT_DIR = config.controler().getOutputDirectory();
		for (int index =firstIt+1; index <lastIt; index ++){
			String dirToDel = OUTPUT_DIR+"/ITERS/it."+index;
			Logger.getLogger(JointCalibrationControler.class).info("Deleting the directory "+dirToDel);
			IOUtils.deleteDirectory(new File(dirToDel),false);
		}
		
		// scenario-specific analysis
		
		if (caseStudy.toString().equals(CaseStudy.Munich.toString())) {
			new File(OUTPUT_DIR+"/user-group-analysis/").mkdir();
			String outputEventsFile = OUTPUT_DIR+"/output_events.xml.gz";
			
			{
				String userGroup = MunichUserGroup.Urban.toString();
				ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new MunichPersonFilter());
				msc.run();
				msc.writeResults(OUTPUT_DIR+"/user-group-analysis/modalShareFromEvents_"+userGroup+".txt");	
			}
			{
				String userGroup = MunichUserGroup.Rev_Commuter.toString();
				ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new MunichPersonFilter());
				msc.run();
				msc.writeResults(OUTPUT_DIR+"/user-group-analysis/modalShareFromEvents_"+userGroup+".txt");
			}
		}
	}
	
}
