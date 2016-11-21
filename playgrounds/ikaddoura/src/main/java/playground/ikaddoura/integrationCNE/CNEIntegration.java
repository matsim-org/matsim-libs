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
import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.ikaddoura.analysis.vtts.VTTScomputation;
import playground.ikaddoura.integrationCN.TollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.integrationCN.VTTSNoiseTollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.integrationCN.VTTSTollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.router.VTTSCongestionTollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.router.VTTSTimeDistanceTravelDisutilityFactory;
import playground.vsp.airPollution.exposure.*;
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
 * TODO: compare simulation outcome with previous runs, e.g. Munich congestion + exhaust emission pricing
 * 
 * TODO: make the code look nicer (requires some modifications in BK's playground)
 * 
 * @author ikaddoura, amit
 *
 */

public class CNEIntegration {
	private static final Logger log = Logger.getLogger(CNEIntegration.class);

	private String configFile = null;
	private Controler controler = null;
	private String outputDirectory = null;
	private double sigma = 0.;
		
	private boolean congestionPricing = false;	
	private boolean noisePricing = false;
	private boolean airPollutionPricing = false;
	
	private boolean congestionAnalysis = false;
	private boolean noiseAnalysis = false;
	private boolean airPollutionAnalysis = false;
	
	private boolean useTripAndAgentSpecificVTTSForRouting = false;

	private final GridTools gridTools;
	private final ResponsibilityGridTools responsibilityGridTools ;

	public CNEIntegration(String configFile, String outputDirectory) {
		this.configFile = configFile;
		this.outputDirectory = outputDirectory;
		this.responsibilityGridTools = null;
		this.gridTools = null;
	}

	public CNEIntegration(String configFile) {
		this (configFile, null);
	}
	
	public CNEIntegration(Controler controler, GridTools gridTools, ResponsibilityGridTools responsibilityGridTools) {
		this.controler = controler;
		this.gridTools = gridTools;
		this.responsibilityGridTools = responsibilityGridTools;
	}

	public CNEIntegration(Controler controler) {
		this(controler, null, null);
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
		
		Controler controler = null;
		
		if (configFile != null) {
			Config config = ConfigUtils.loadConfig(configFile);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			controler = new Controler(scenario);
		} else {
			if (this.controler == null) {
				throw new RuntimeException("Controler is null. Aborting...");
			}
			controler = this.controler;
		}
		
		if (outputDirectory == null) {
			if (controler.getConfig().controler().getOutputDirectory() == null || controler.getConfig().controler().getOutputDirectory() == "") {
				throw new RuntimeException("Either provide an output directory in the config file or the controler. Aborting...");
			} else {
				log.info("Using the output directory given in the config file...");
			}
			
		} else {
			if (controler.getConfig().controler().getOutputDirectory() == null || controler.getConfig().controler().getOutputDirectory() == "") {
				log.info("Using the output directory provided in the controler.");
			} else {
				log.warn("The output directory in the config file will overwritten by the directory provided in the controler.");
			}
			controler.getConfig().controler().setOutputDirectory(outputDirectory);
		}

		if( airPollutionPricing && (this.gridTools == null || this.responsibilityGridTools == null) ) {
			throw new RuntimeException("To internalize air pollution exposure, grid tools " +
					"and responsibility grid tools must be passed to the constructor.");
		}
		
		final VTTSHandler vttsHandler;
		if (useTripAndAgentSpecificVTTSForRouting) {
			log.info("Using the agent- and trip-specific VTTS for routing.");
			vttsHandler = new VTTSHandler(controler.getScenario());
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
			noiseContext = new NoiseContext(controler.getScenario());
		}
		
		// ########################## Air pollution ##########################
				
		final double emissionEfficiencyFactor = 1.0;
		final boolean considerCO2Costs = true;
		final double emissionCostFactor = 1.0;
		final Set<Id<Link>> hotSpotLinks = null;
		
		EmissionModule emissionModule = null;
		
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		controler.getConfig().addModule(ecg);
		
		if (this.airPollutionPricing == true || this.airPollutionAnalysis == true) {
			
			emissionModule = new EmissionModule(controler.getScenario());
			emissionModule.setEmissionEfficiencyFactor(emissionEfficiencyFactor);
			emissionModule.createLookupTables();
			emissionModule.createEmissionHandler();			
		}
		
		// ########################## Pricing setup ##########################
				
		if (congestionPricing && noisePricing == false && airPollutionPricing == false) {
			
			// only congestion pricing
			
			final TollHandler congestionTollHandlerQBP = new TollHandler(controler.getScenario());
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				final VTTSCongestionTollTimeDistanceTravelDisutilityFactory factory = new VTTSCongestionTollTimeDistanceTravelDisutilityFactory(
						new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, controler.getConfig().planCalcScore()),
						congestionTollHandlerQBP, controler.getConfig().planCalcScore()
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
						new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()),
						congestionTollHandlerQBP, controler.getConfig().planCalcScore()
					);
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				}); 
			}
			
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario())));	
			
		} else if (congestionPricing == false && noisePricing && airPollutionPricing == false) {
			// only noise pricing
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				final VTTSNoiseTollTimeDistanceTravelDisutilityFactory factory = new VTTSNoiseTollTimeDistanceTravelDisutilityFactory(
						new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, controler.getConfig().planCalcScore()),
						noiseContext, controler.getConfig().planCalcScore()
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
						new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()),
						noiseContext, controler.getConfig().planCalcScore()
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
			final EmissionResponsibilityCostModule emissionCostModule = new EmissionResponsibilityCostModule( emissionCostFactor, considerCO2Costs, this.responsibilityGridTools, this.gridTools);

			if (useTripAndAgentSpecificVTTSForRouting) {
				throw new RuntimeException("Not yet implemented. Aborting...");

			} else {				
				
				if (sigma != 0.) {
					throw new RuntimeException("The following travel disutility doesn't allow for randomness. Aborting...");
				}
				EmissionResponsibilityTravelDisutilityCalculatorFactory emfac = new EmissionResponsibilityTravelDisutilityCalculatorFactory(
						new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()),
						emissionModule,
						emissionCostModule,
						controler.getConfig().planCalcScore()
				);
				controler.addOverridingModule(new AbstractModule() {

					@Override
					public void install() {
						bindCarTravelDisutilityFactory().toInstance(emfac);
					}
				});
			}
			controler.addControlerListener(new InternalizeEmissionResponsibilityControlerListener(emissionModule, emissionCostModule, this.responsibilityGridTools, this.gridTools));

		} else if (congestionPricing && noisePricing && airPollutionPricing) {
			// congestion + noise + airPollution pricing

			final EmissionResponsibilityCostModule emissionCostModule = new EmissionResponsibilityCostModule( emissionCostFactor, considerCO2Costs, this.responsibilityGridTools, this.gridTools);
			final TollHandler congestionTollHandlerQBP = new TollHandler(controler.getScenario());
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				final VttsCNETimeDistanceTravelDisutilityFactory factory = new VttsCNETimeDistanceTravelDisutilityFactory(
						new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, controler.getConfig().planCalcScore()),
						emissionModule, emissionCostModule,
						noiseContext,
						congestionTollHandlerQBP,
						controler.getConfig().planCalcScore()
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
						new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()),
						emissionModule, emissionCostModule,
						noiseContext,
						congestionTollHandlerQBP,
						controler.getConfig().planCalcScore()
					);
				factory.setSigma(sigma); // TODO : I dont know, why we are setting sigma here and not anywhere else. amit nov 16.
				factory.setHotspotLinks(hotSpotLinks);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				}); 				
			}
				
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario())));						
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			controler.addControlerListener(new InternalizeEmissionResponsibilityControlerListener(emissionModule, emissionCostModule, this.responsibilityGridTools, this.gridTools));
			
		} else if (congestionPricing && noisePricing && airPollutionPricing == false) {
			// congestion + noise pricing
			
			final TollHandler congestionTollHandlerQBP = new TollHandler(controler.getScenario());
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				final VTTSTollTimeDistanceTravelDisutilityFactory factory = new VTTSTollTimeDistanceTravelDisutilityFactory(
						new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, controler.getConfig().planCalcScore()),
						noiseContext,
						congestionTollHandlerQBP, controler.getConfig().planCalcScore()
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
						new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()),
						noiseContext,
						congestionTollHandlerQBP, controler.getConfig().planCalcScore()
					);
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				});
			} 
				
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario())));											
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));

		} else if (congestionPricing && noisePricing == false && airPollutionPricing) {
			// congestion + air pollution pricing
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				throw new RuntimeException("Not yet implemented. Aborting...");

			} else {
				throw new RuntimeException("Not yet implemented. Aborting...");
			}
					
		} else if (congestionPricing == false && noisePricing == false && airPollutionPricing == false) {
			// no pricing
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				final VTTSTimeDistanceTravelDisutilityFactory factory = new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, controler.getConfig().planCalcScore());
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				});
			} else {
				final RandomizingTimeDistanceTravelDisutilityFactory factory = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore());
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
			if (congestionAnalysis) controler.addControlerListener(new CongestionAnalysisControlerListener(new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario())));
			
			// add air pollution analysis
			if (airPollutionAnalysis) controler.addControlerListener(new EmissionControlerListener());			

		} else if (congestionPricing == false && noisePricing == false && airPollutionPricing) {
			// only air pollution pricing
				
			// add congestion analysis
			if (congestionAnalysis) controler.addControlerListener(new CongestionAnalysisControlerListener(new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario())));
						
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
			if (congestionAnalysis)	controler.addControlerListener(new CongestionAnalysisControlerListener(new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario())));
		
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
