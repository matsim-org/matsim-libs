/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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
package playground.southafrica.gauteng;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.southafrica.gauteng.roadpricingscheme.GautengRoadPricingScheme;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollFactorOLD;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollFactor_Subpopulation;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollVehicleType;
import playground.southafrica.gauteng.roadpricingscheme.TollFactorI;
import playground.southafrica.gauteng.routing.PersonSpecificTravelDisutilityInclTollFactory;
import playground.southafrica.gauteng.scoring.GenerationOfMoneyEvents;
import playground.southafrica.gauteng.scoring.PersonSpecificUoMScoringFunctionFactory;
import playground.southafrica.gauteng.utilityofmoney.GautengUtilityOfMoney;
import playground.southafrica.gauteng.utilityofmoney.UtilityOfMoneyI;
import playground.southafrica.utilities.Header;

/**
 *
 * @author jwjoubert
 */
public class GautengControler_subpopulations {
	private final static Logger LOG = Logger.getLogger(GautengControler_subpopulations.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GautengControler_subpopulations.class.toString(), args);
		/* Config must be passed as an argument, everything else is optional. */
		final String configFilename = args[0];
		
		/* Optional arguments. */		
		String plansFilename = null ;
		if ( args.length>1 && args[1]!=null && args[1].length()>0 ) {
			plansFilename = args[1];
		}
		
		String personAttributeFilename = null;
		if (args.length > 2 && args[2] != null && args[2].length()>0 ) {
			personAttributeFilename = args[2];
		}
		
		String networkFilename = null;
		if (args.length > 3 && args[3] != null && args[3].length()>0 ) {
			networkFilename = args[3];
		}
		
		
		String tollFilename = null ;
		if ( args.length>4 && args[4]!=null && args[4].length()>0 ) {
			tollFilename = args[4];
		}
		
		double baseValueOfTime = 110. ;
		double valueOfTimeMultiplier = 4. ;
		if ( args.length>5 && args[5]!=null && args[5].length()>0 ) {
			baseValueOfTime = Double.parseDouble(args[5]);
		}
		
		if ( args.length>6 && args[6]!=null && args[6].length()>0 ) {
			valueOfTimeMultiplier = Double.parseDouble(args[6]);
		}
		
		int numberOfThreads = 1 ;
		if ( args.length>6 && args[7]!=null && args[7].length()>0 ) {
			numberOfThreads = Integer.parseInt(args[7]);
		}

		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, configFilename);
		config.plans().setInputFile(plansFilename);
		config.plans().setInputPersonAttributeFile(personAttributeFilename);
		config.plans().setSubpopulationAttributeName("subpopulation");
		
		/* Set up the strategies for the different subpopulations. */
		{	/* Car:
		 	   - ChangeExpBeta: 70%
		 	   - TimeAllocationMutator: 15%
		 	   - ReRoute: 15%  */ 
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation("car");
			changeExpBetaStrategySettings.setProbability(0.7);
			config.strategy().addStrategySettings(changeExpBetaStrategySettings);

			StrategySettings timeStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings.setModuleName(PlanStrategyRegistrar.Names.TimeAllocationMutator.toString());
			timeStrategySettings.setSubpopulation("car");
			timeStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);

			StrategySettings rerouteStrategySettings = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			rerouteStrategySettings.setModuleName(PlanStrategyRegistrar.Names.ReRoute.toString());
			rerouteStrategySettings.setSubpopulation("car");
			rerouteStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(rerouteStrategySettings);
		}
		
		{	/* Commercial vehicles:
		 	   - ChangeExpBeta: 70%
		 	   - TimeAllocationMutator: 15%
		 	   - ReRoute: 15%  */ 
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation("commercial");
			changeExpBetaStrategySettings.setProbability(0.7);
			config.strategy().addStrategySettings(changeExpBetaStrategySettings);
			
			StrategySettings timeStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings.setModuleName(PlanStrategyRegistrar.Names.TimeAllocationMutator.toString());
			timeStrategySettings.setSubpopulation("commercial");
			timeStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);
			
			StrategySettings rerouteStrategySettings = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			rerouteStrategySettings.setModuleName(PlanStrategyRegistrar.Names.ReRoute.toString());
			rerouteStrategySettings.setSubpopulation("commercial");
			rerouteStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(rerouteStrategySettings);
		}
		
		{	/* Bus:
		 	   - ChangeExpBeta: 70%
		 	   - TimeAllocationMutator: 15%
		 	   - ReRoute: 15%  */ 
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation("bus");
			changeExpBetaStrategySettings.setProbability(0.7);
			config.strategy().addStrategySettings(changeExpBetaStrategySettings);
			
			StrategySettings timeStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings.setModuleName(PlanStrategyRegistrar.Names.TimeAllocationMutator.toString());
			timeStrategySettings.setSubpopulation("bus");
			timeStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);
			
			StrategySettings rerouteStrategySettings = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			rerouteStrategySettings.setModuleName(PlanStrategyRegistrar.Names.ReRoute.toString());
			rerouteStrategySettings.setSubpopulation("bus");
			rerouteStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(rerouteStrategySettings);
		}
		{	/* Taxi:
		 	   - ChangeExpBeta: 70%
		 	   - TimeAllocationMutator: 15%
		 	   - ReRoute: 15%  */ 
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation("taxi");
			changeExpBetaStrategySettings.setProbability(0.7);
			config.strategy().addStrategySettings(changeExpBetaStrategySettings);
			
			StrategySettings timeStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings.setModuleName(PlanStrategyRegistrar.Names.TimeAllocationMutator.toString());
			timeStrategySettings.setSubpopulation("taxi");
			timeStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);
			
			StrategySettings rerouteStrategySettings = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			rerouteStrategySettings.setModuleName(PlanStrategyRegistrar.Names.ReRoute.toString());
			rerouteStrategySettings.setSubpopulation("taxi");
			rerouteStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(rerouteStrategySettings);
		}
		{	/* External traffic:
		 	   - ChangeExpBeta: 70%
		 	   - TimeAllocationMutator: 15%
		 	   - ReRoute: 15%  */ 
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation("ext");
			changeExpBetaStrategySettings.setProbability(0.7);
			config.strategy().addStrategySettings(changeExpBetaStrategySettings);
			
			StrategySettings timeStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings.setModuleName(PlanStrategyRegistrar.Names.TimeAllocationMutator.toString());
			timeStrategySettings.setSubpopulation("ext");
			timeStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);
			
			StrategySettings rerouteStrategySettings = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			rerouteStrategySettings.setModuleName(PlanStrategyRegistrar.Names.ReRoute.toString());
			rerouteStrategySettings.setSubpopulation("ext");
			rerouteStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(rerouteStrategySettings);
		}
			
		final Controler controler = new Controler( config ) ;
		controler.setOverwriteFiles(true) ;
		
		
		/* Allow for the plans file to be passed as argument. */
		if ( plansFilename!=null && plansFilename.length()>0 ) {
			controler.getConfig().plans().setInputFile(plansFilename) ;
		}
		
		/* Allow for network filename to be passed as an argument. */
		if (networkFilename!=null && networkFilename.length()>0){
			controler.getConfig().network().setInputFile(networkFilename);
		}

		/* Allow for the road pricing filename to be passed as an argument. */
		if ( tollFilename!=null && tollFilename.length()>0 ) {
			controler.getConfig().roadpricing().setTollLinksFile(tollFilename);
		}
		
		/* Set number of threads. */
		controler.getConfig().global().setNumberOfThreads(numberOfThreads);
		
		final Scenario sc = controler.getScenario();

		if (sc.getConfig().scenario().isUseRoadpricing()) {
			throw new RuntimeException("roadpricing must NOT be enabled in config.scenario in order to use special " +
					"road pricing features.  aborting ...");
		}

		final TollFactorI tollFactor = new SanralTollFactor_Subpopulation(sc);

		controler.addControlerListener( new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				Map<SanralTollVehicleType,Double> cnt = new HashMap<SanralTollVehicleType,Double>() ;
				for ( Person person : sc.getPopulation().getPersons().values() ) {
					SanralTollVehicleType type = tollFactor.typeOf( person.getId() ) ;
					if ( cnt.get(type)==null ) {
						cnt.put(type, 0.) ;
					}
					cnt.put( type, 1. + cnt.get(type) ) ;
				}
				for ( SanralTollVehicleType type : SanralTollVehicleType.values() ) {
					LOG.info( String.format( "type: %30s; cnt: %8.0f", type.toString() , cnt.get(type) ) );
				}
			}
		}) ;

		// CONSTRUCT VEH-DEP ROAD PRICING SCHEME:
		RoadPricingScheme vehDepScheme = 
			new GautengRoadPricingScheme( sc.getConfig().roadpricing().getTollLinksFile() , sc.getNetwork() , sc.getPopulation(), tollFactor );

		// CONSTRUCT UTILITY OF MONEY:
		
		UtilityOfMoneyI personSpecificUtilityOfMoney = new GautengUtilityOfMoney( sc.getConfig().planCalcScore() , baseValueOfTime, valueOfTimeMultiplier, tollFactor) ;

		// INSTALL ROAD PRICING (in the longer run, re-merge with RoadPricing class):
		// insert into scoring:
		controler.addControlerListener(
				new GenerationOfMoneyEvents( sc.getNetwork(), sc.getPopulation(), vehDepScheme, tollFactor) 
		) ;
		
		controler.setScoringFunctionFactory(
				new PersonSpecificUoMScoringFunctionFactory(sc.getConfig(), sc.getNetwork(), personSpecificUtilityOfMoney )
		);

		// insert into routing:
		controler.setTravelDisutilityFactory( 
				new PersonSpecificTravelDisutilityInclTollFactory( vehDepScheme, personSpecificUtilityOfMoney ) 
		);
		
		
		
		// ADDITIONAL ANALYSIS:
		// This is not truly necessary.  It could be removed or copied in order to remove the dependency on the kai
		// playground.  For the time being, I (kai) would prefer to leave it the way it is since I am running the Gauteng
		// scenario and I don't want to maintain two separate analysis listeners.  But once that period is over, this
		// argument does no longer apply.  kai, mar'12
		//
		// I (JWJ, June '13) commented this listener out as the dependency is not working.
		
		controler.addControlerListener(new KaiAnalysisListener()) ;
		
		// RUN:
		
		controler.getConfig().controler().setOutputDirectory("/Users/jwjoubert/Documents/Temp/sanral-runs");
		controler.run();
		
		Header.printFooter();
	}

}
