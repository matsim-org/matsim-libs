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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.example.EmissionControlerListener;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.NoiseCalculationOnline;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.analysis.emission.AirPollutionExposureAnalysisControlerListener;
import playground.agarwalamit.analysis.emission.experienced.ExperiencedEmissionCostHandler;
import playground.agarwalamit.utils.PersonFilter;
import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.ikaddoura.analysis.vtts.VTTScomputation;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.decongestion.DecongestionControlerListener;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.handler.DelayAnalysis;
import playground.ikaddoura.decongestion.handler.IntervalBasedTolling;
import playground.ikaddoura.decongestion.handler.IntervalBasedTollingAll;
import playground.ikaddoura.decongestion.handler.PersonVehicleTracker;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollSetting;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollingPID;
import playground.ikaddoura.moneyTravelDisutility.MoneyEventAnalysis;
import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.router.VTTSTimeDistanceTravelDisutilityFactory;
import playground.vsp.airPollution.exposure.EmissionResponsibilityCostModule;
import playground.vsp.airPollution.exposure.GridTools;
import playground.vsp.airPollution.exposure.InternalizeEmissionResponsibilityControlerListener;
import playground.vsp.airPollution.exposure.ResponsibilityGridTools;
import playground.vsp.congestion.controler.AdvancedMarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;

/**
 * 
 * A controler to start a simultaneous congestion, noise and air pollution pricing run or other joint pricing runs.
 * 
 * @author ikaddoura, amit
 *
 */

public class CNEIntegration {
	private static final Logger log = Logger.getLogger(CNEIntegration.class);

	private Controler controler = null;
	private String outputDirectory = null;
	private double sigma = 0.;
		
	private boolean congestionPricing = false;	
	private boolean noisePricing = false;
	private boolean airPollutionPricing = false;
	
	private boolean useTripAndAgentSpecificVTTSForRouting = false;
	
	private CongestionTollingApproach congestionTollingApproach = CongestionTollingApproach.DecongestionPID;
	private double kP = 0.;

	private PersonFilter personFilter = null; // TODO : i think, we can somehow merge the personFilter or agentFilter. amit
	private AgentFilter agentFilter  = null;

	private final GridTools gridTools;
	private final ResponsibilityGridTools responsibilityGridTools ;

	public enum CongestionTollingApproach {
        DecongestionPID, QBPV3, QBPV9
	}
	
	public CNEIntegration(String configFile, String outputDirectory) {
		
		Config config = ConfigUtils.loadConfig(configFile, new NoiseConfigGroup(), new EmissionsConfigGroup());
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		this.controler = new Controler(scenario);
		
		this.outputDirectory = outputDirectory;
		this.responsibilityGridTools = null;
		this.gridTools = null;	
	}

	public CNEIntegration(String configFile) {
		this (configFile, null);
	}
	
	public CNEIntegration(Controler controler, GridTools gridTools, ResponsibilityGridTools responsibilityGridTools) {
		this.controler = controler;
		
		this.responsibilityGridTools = responsibilityGridTools;
		this.gridTools = gridTools;
	}

	public CNEIntegration(Controler controler) {
		this(controler, null, null);
	}

	public Controler prepareControler() {
		
		boolean analyzeAirPollution = false;
		boolean analyzeNoise = false;
				
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
		
		if (controler.getConfig().getModules().get(EmissionsConfigGroup.GROUP_NAME) != null && gridTools != null && responsibilityGridTools != null) {
			log.info("Controler contains the emissions module. Air pollution exposures will be analyzed." );
			analyzeAirPollution = true;
		} else {
			log.info("Controler doesn't contain the emission module. Air pollution exposures will not be analyzed.");
		}
		
		if (controler.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME) != null) {
			log.info("Controler contains the noise module. Noise exposures will be analyzed." );
			analyzeNoise = true;
		} else {
			log.info("Controler doesn't contain the noise module. Noise exposures will not be analyzed.");
		}
		
		// check consistency
		
		if( airPollutionPricing && (this.gridTools == null || this.responsibilityGridTools == null || controler.getConfig().getModules().get(EmissionsConfigGroup.GROUP_NAME) == null) ) {
			throw new RuntimeException("To internalize air pollution exposure costs, the emission config group must be loaded"
					+ "and grid tools and responsibility grid tools must be passed to the constructor. Aborting...");
		}
		
		if (noisePricing && controler.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME) == null) {
			throw new RuntimeException("To internalize noise exposure costs, the noise config group must be loaded. Aborting...");
		}
		
		// ########################## trip-specific VTTS routing ##########################
		
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
		
		if (analyzeNoise) {
			
			noiseContext = new NoiseContext(controler.getScenario());
			NoiseConfigGroup ncg = (NoiseConfigGroup) controler.getScenario().getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME);
			
			if (noisePricing) {	
				ncg.setInternalizeNoiseDamages(true);
				controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			} else {
				ncg.setInternalizeNoiseDamages(false);
				controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			}
		}
						
		// ########################## Congestion ##########################
				
		if (congestionPricing) {
						
			if (congestionTollingApproach.toString().equals(CongestionTollingApproach.QBPV3.toString())) {
				final TollHandler congestionTollHandlerQBP = new TollHandler(controler.getScenario());
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario())));	
			
			} else if (congestionTollingApproach.toString().equals(CongestionTollingApproach.QBPV9.toString())) {
				final TollHandler congestionTollHandlerQBP = new TollHandler(controler.getScenario());
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandlerQBP, new CongestionHandlerImplV9(controler.getEvents(), controler.getScenario())));
				
			} else if (congestionTollingApproach.toString().equals(CongestionTollingApproach.DecongestionPID.toString())) {
							
				final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
				decongestionSettings.setKp(kP);
				decongestionSettings.setKi(0.);
				decongestionSettings.setKd(0.);
				decongestionSettings.setMsa(true);
				decongestionSettings.setRUN_FINAL_ANALYSIS(false);
				decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
				controler.getConfig().addModule(decongestionSettings);
				
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						
						this.bind(DecongestionInfo.class).asEagerSingleton();
						
						this.bind(DecongestionTollSetting.class).to(DecongestionTollingPID.class);
						this.bind(IntervalBasedTolling.class).to(IntervalBasedTollingAll.class);

						this.bind(IntervalBasedTollingAll.class).asEagerSingleton();
						this.bind(DelayAnalysis.class).asEagerSingleton();
						this.bind(PersonVehicleTracker.class).asEagerSingleton();
										
						this.addEventHandlerBinding().to(IntervalBasedTollingAll.class);
						this.addEventHandlerBinding().to(DelayAnalysis.class);
						this.addEventHandlerBinding().to(PersonVehicleTracker.class);
						
						this.addControlerListenerBinding().to(DecongestionControlerListener.class);

					}
				});
				
			} else {
				throw new RuntimeException("Unknown congestion pricing approach. Aborting...");
			}
		
		}

		// ########################## Air pollution ##########################

		EmissionResponsibilityCostModule emissionCostModule = null;

		if (analyzeAirPollution) {

			final double emissionEfficiencyFactor = 1.0;
			final boolean considerCO2Costs = true;
			final double emissionCostFactor = 1.0;

			/* TODO : since these params are in config now, plz check if the settings are same in the  output config.
			 If these values are overridden by config file, we need to add them to config. Amit Mar'17 */
			EmissionsConfigGroup emissionsConfigGroup = ((EmissionsConfigGroup) this.controler.getConfig().getModules().get(EmissionsConfigGroup.GROUP_NAME));
			emissionsConfigGroup.setConsideringCO2Costs(considerCO2Costs);
			emissionsConfigGroup.setEmissionCostMultiplicationFactor(emissionCostFactor);
			emissionsConfigGroup.setEmissionEfficiencyFactor(emissionEfficiencyFactor);

			if (airPollutionPricing) {
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bind(GridTools.class).toInstance(gridTools);
						bind(ResponsibilityGridTools.class).toInstance(responsibilityGridTools);
						bind(EmissionModule.class).asEagerSingleton();
						bind(EmissionResponsibilityCostModule.class).asEagerSingleton();
						addControlerListenerBinding().to(InternalizeEmissionResponsibilityControlerListener.class);
					}
				});

			} else {
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bind(GridTools.class).toInstance(gridTools);
						bind(ResponsibilityGridTools.class).toInstance(responsibilityGridTools);
						bind(EmissionResponsibilityCostModule.class).asEagerSingleton();

						addControlerListenerBinding().to(EmissionControlerListener.class); // just to write the emission events

						if(personFilter!=null) bind(PersonFilter.class).toInstance(personFilter);
						bind(ExperiencedEmissionCostHandler.class);
						addControlerListenerBinding().to(AirPollutionExposureAnalysisControlerListener.class);
					}
				});
			}
		}

		// ########################## Travel disutility ##########################
		
		if (this.congestionPricing || this.noisePricing || this.airPollutionPricing) {
			
			if (useTripAndAgentSpecificVTTSForRouting) {
				final VTTSMoneyTimeDistanceTravelDisutilityFactory factory = new VTTSMoneyTimeDistanceTravelDisutilityFactory(
						new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, controler.getConfig().planCalcScore())
						);
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						
						if (agentFilter != null) this.bind(AgentFilter.class).toInstance(agentFilter);
										
						// travel disutility
						this.bindCarTravelDisutilityFactory().toInstance(factory);
						this.bind(MoneyEventAnalysis.class).asEagerSingleton();
						
						// person money event handler + controler listener
						this.addControlerListenerBinding().to(MoneyEventAnalysis.class);
						this.addEventHandlerBinding().to(MoneyEventAnalysis.class);
					}
				}); 			
				
			} else {
				final MoneyTimeDistanceTravelDisutilityFactory factory = new MoneyTimeDistanceTravelDisutilityFactory(
						new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()));
				
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						
						if (agentFilter != null) this.bind(AgentFilter.class).toInstance(agentFilter);
										
						// travel disutility
						this.bindCarTravelDisutilityFactory().toInstance(factory);
						this.bind(MoneyEventAnalysis.class).asEagerSingleton();
						
						// person money event handler + controler listener
						this.addControlerListenerBinding().to(MoneyEventAnalysis.class);
						this.addEventHandlerBinding().to(MoneyEventAnalysis.class);
					}
				}); 
			}		
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

	public void setUseTripAndAgentSpecificVTTSForRouting(boolean useTripAndAgentSpecificVTTSForRouting) {
		this.useTripAndAgentSpecificVTTSForRouting = useTripAndAgentSpecificVTTSForRouting;
	}

	public void setCongestionTollingApproach(CongestionTollingApproach congestionTollingApproach) {
		this.congestionTollingApproach = congestionTollingApproach;
	}

	public void setkP(double kP) {
		this.kP = kP;
	}

	public void setPersonFilter(PersonFilter personFilter) {
		this.personFilter = personFilter;
	}
	
	public void setAgentFilter(AgentFilter agentFilter) {
		this.agentFilter = agentFilter;
	}
}
