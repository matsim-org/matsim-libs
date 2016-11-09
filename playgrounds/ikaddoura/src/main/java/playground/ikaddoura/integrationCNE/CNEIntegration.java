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

import java.io.IOException;
import java.util.Set;

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
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.InternalizationEmissionAndCongestion.EmissionCongestionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;
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

public class CNEIntegration {
	private static final Logger log = Logger.getLogger(CNEIntegration.class);

	private String configFile;
	private String outputDirectory;
	private double sigma = 0.;
		
	private boolean congestionPricing = false;	
	private boolean noisePricing = false;
	private boolean airPollutionPricing = false;
	
	private boolean congestionAnalysis = false;
	private boolean noiseAnalysis = false;
	private boolean airPollutionAnalysis = false;
	
	private boolean useTripAndAgentSpecificVTTSForRouting = false;
		
	public CNEIntegration(String configFile, String outputDirectory) {
		this.configFile = configFile;
		this.outputDirectory = outputDirectory;
	}
	
	public CNEIntegration(String configFile) {
		this.configFile = configFile;
		this.outputDirectory = null;
	}

	public static void main(String[] args) throws IOException {
		
		String configFile;
		
		if (args.length > 0) {
								
			// TODO
			throw new RuntimeException("Not yet implemented. Aborting...");
			
		} else {
			
			configFile = "../../../runs-svn/cne_test/input/config.xml";
		}
				
		CNEIntegration cneIntegration = new CNEIntegration(configFile);
		
		Controler controler = cneIntegration.prepareControler();
		
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();		
	}

	public Controler prepareControler() {
						
		final Config config = ConfigUtils.loadConfig(configFile);
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
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);
		
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
		
		NoiseContext noiseContext = null;
		
		NoiseConfigGroup ncg = new NoiseConfigGroup();
		controler.getConfig().addModule(ncg);
		
		if (this.noisePricing == true || this.noiseAnalysis == true) {
			
			if (noisePricing) {	
				ncg.setInternalizeNoiseDamages(true);
			} else {
				ncg.setInternalizeNoiseDamages(false);
			}

			// computation of noise events
			noiseContext = new NoiseContext(scenario);
		}
		
		// ########################## Air pollution ##########################
				
		final String emissionEfficiencyFactor ="1.0";
		final String considerCO2Costs = "true";
		final String emissionCostFactor = "1.0";
		final Set<Id<Link>> hotSpotLinks = null;
		
		EmissionModule emissionModule = null;
		
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		controler.getConfig().addModule(ecg);
		
		if (this.airPollutionPricing == true || this.airPollutionAnalysis == true) {
			
			emissionModule = new EmissionModule(scenario);
			emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
			emissionModule.createLookupTables();
			emissionModule.createEmissionHandler();			
		}
		
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
			
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(scenario, congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), scenario)));	
			
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
			
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));

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
				
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(scenario, congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), scenario)));						
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
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
				
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(scenario, congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), scenario)));											
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));

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
				
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(scenario, congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), scenario)));
				controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));
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

		} else {
			throw new RuntimeException("Not yet implemented. Aborting...");
		}
		
		// ########################## Analysis setup ##########################
		
		if (congestionPricing && noisePricing == false && airPollutionPricing == false) {
			// only congestion pricing
			
			// add air pollution analysis
			if (airPollutionAnalysis) controler.addControlerListener(new EmissionControlerListener());
			
			// add noise analysis
			if (noiseAnalysis) controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			
		} else if (congestionPricing == false && noisePricing && airPollutionPricing == false) {
			// only noise pricing
			
			// add congestion analysis
			if (congestionAnalysis) controler.addControlerListener(new CongestionAnalysisControlerListener(new CongestionHandlerImplV3(controler.getEvents(), scenario)));
			
			// add air pollution analysis
			if (airPollutionAnalysis) controler.addControlerListener(new EmissionControlerListener());			

		} else if (congestionPricing == false && noisePricing == false && airPollutionPricing) {
			// only air pollution pricing
				
			// add congestion analysis
			if (congestionAnalysis) controler.addControlerListener(new CongestionAnalysisControlerListener(new CongestionHandlerImplV3(controler.getEvents(), scenario)));
						
			// add noise analysis
			if (noiseAnalysis) controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			
		} else if (congestionPricing && noisePricing && airPollutionPricing) {
			// congestion + noise + airPollution pricing
			
			// no additional analysis to add
			
		} else if (congestionPricing && noisePricing && airPollutionPricing == false) {
			// congestion + noise pricing
				
			// add air pollution analysis
			if (airPollutionAnalysis) controler.addControlerListener(new EmissionControlerListener());
			
		} else if (congestionPricing && noisePricing == false && airPollutionPricing) {
			
			// congestion + air pollution pricing

			// add noise analysis
			if (noiseAnalysis) controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
					
		} else if (congestionPricing == false && noisePricing == false && airPollutionPricing == false) {
			// no pricing
			
			// add congestion analysis
			if (congestionAnalysis)	controler.addControlerListener(new CongestionAnalysisControlerListener(new CongestionHandlerImplV3(controler.getEvents(), scenario)));
		
			// add air pollution analysis
			if (airPollutionAnalysis) controler.addControlerListener(new EmissionControlerListener());
			
			// add noise analysis
			if (noiseAnalysis) controler.addControlerListener(new NoiseCalculationOnline(noiseContext));

		} else {
			throw new RuntimeException("Not yet implemented. Aborting...");
		}
		
		return controler;
	}
	
	public void setSigma(double sigma) {
		this.sigma = sigma;
	}

	public void setCongestionPricing(boolean congestionPricing) {
		this.congestionPricing = congestionPricing;
	}

	public void setNoisePricing(boolean noisePricing) {
		this.noisePricing = noisePricing;
	}

	public void setAirPollutionPricing(boolean airPollutionPricing) {
		this.airPollutionPricing = airPollutionPricing;
	}

	public void setCongestionAnalysis(boolean congestionAnalysis) {
		this.congestionAnalysis = congestionAnalysis;
	}

	public void setNoiseAnalysis(boolean noiseAnalysis) {
		this.noiseAnalysis = noiseAnalysis;
	}

	public void setAirPollutionAnalysis(boolean airPollutionAnalysis) {
		this.airPollutionAnalysis = airPollutionAnalysis;
	}

	public void setUseTripAndAgentSpecificVTTSForRouting(boolean useTripAndAgentSpecificVTTSForRouting) {
		this.useTripAndAgentSpecificVTTSForRouting = useTripAndAgentSpecificVTTSForRouting;
	}

}
