/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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
package playground.southafrica.population.freight;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.*;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.*;
import playground.southafrica.projects.complexNetworks.pathDependence.DigicorePathDependentNetworkReader_v1;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
import playground.southafrica.utilities.Header;

import java.util.Arrays;

/**
 * @author jwjoubert
 *
 */
public class RunNationalFreight {
	private final static Logger LOG = Logger.getLogger(RunNationalFreight.class);
	
//	private static String HOME = "/Users/jwjoubert/Documents/Temp/freightPopulation/runs/100/";
//	private static String HOME = "/Users/jwjoubert/Documents/Temp/freightPopulation/runs/01perc/";
	private static String HOME = "/Users/jwjoubert/Documents/Temp/freightPopulation/runs/10perc/";
	
	private static String NETWORK = "/Users/jwjoubert/Documents/workspace/Data-southAfrica/network/southAfrica_20131202_coarseNetwork_clean.xml.gz";
	private static String OUTPUT_DIRECTORY = HOME + "output/";
	private static String POPULATION = HOME + "nationalFreight.xml.gz";
	private static String POPULATION_ATTR =  HOME + "nationalFreightAttributes.xml.gz";
	private static String PATH_DEPENDENT_NETWORK = HOME + "pathDependentNetwork.xml.gz";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(RunNationalFreight.class.toString(), args);
		if(args.length == 5){
			NETWORK = args[0];
			POPULATION = args[1];
			POPULATION_ATTR = args[2];
			OUTPUT_DIRECTORY = args[3];
			PATH_DEPENDENT_NETWORK = args[4];
		} else{
			LOG.warn("None, or insufficient run arguments passed. Reverts back to defaults.");
			LOG.warn("Network: " + NETWORK);
			LOG.warn("Population: " + HOME + POPULATION);
			LOG.warn("Population attributes: " + HOME + POPULATION_ATTR);
			LOG.warn("Output: " + HOME + OUTPUT_DIRECTORY);
			LOG.warn("Path-dependent network: " + HOME + PATH_DEPENDENT_NETWORK);
		}
		
		/* Config stuff */
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(OUTPUT_DIRECTORY);
		config.controler().setLastIteration(100);
		config.controler().setWriteEventsInterval(20);
//		config.global().setNumberOfThreads(40); 	// Hobbes
		config.global().setNumberOfThreads(4); 		// Ubuntu
		config.global().setRandomSeed(20141217l); 	// Hobbes
//		config.qsim().setNumberOfThreads(40);		// Hobbes
		config.qsim().setNumberOfThreads(4);		// Ubuntu
		
		config.network().setInputFile(NETWORK);
		config.plans().setInputFile(POPULATION);
		config.plans().setInputPersonAttributeFile(POPULATION_ATTR);
		
		config.global().setCoordinateSystem("WGS84_SA_Albers");
		
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration );
		
		String[] modes ={"commercial"};
		config.qsim().setMainModes( Arrays.asList(modes) );
		config.plansCalcRoute().setNetworkModes(Arrays.asList(modes));
		
			/* PlanCalcScore */
		ActivityParams major = new ActivityParams("major");
		major.setTypicalDuration(10*3600);
		config.planCalcScore().addActivityParams(major);

		ActivityParams minor = new ActivityParams("minor");
		minor.setTypicalDuration(1880);
		config.planCalcScore().addActivityParams(minor);
		
			/* Generic strategy */
		StrategySettings changeExpBetaStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		changeExpBetaStrategySettings.setWeight(0.8);
		config.strategy().addStrategySettings(changeExpBetaStrategySettings);
			/* Subpopulation strategy */
		StrategySettings commercialStrategy = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		commercialStrategy.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		commercialStrategy.setWeight(0.85);
		commercialStrategy.setSubpopulation("commercial");
		config.strategy().addStrategySettings(commercialStrategy);
			/* Subpopulation ReRoute */
		StrategySettings commercialReRoute = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		commercialReRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.name());
		commercialReRoute.setWeight(0.15);
		commercialReRoute.setSubpopulation("commercial");
		config.strategy().addStrategySettings(commercialReRoute);
		//TODO Add the custom strategy module.
//		StrategySettings newStrategy = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
//		newStrategy.setModuleName("Digicore1");
//		newStrategy.setProbability(0.3);
//		newStrategy.setSubpopulation("commercial");
//		config.strategy().addStrategySettings(newStrategy);
		
		/* Scenario stuff */
		Scenario sc = ScenarioUtils.loadScenario(config);
		config.scenario().setUseVehicles(true);
		
		/* Set the population as "subpopulation", and create a vehicle for each. */
		Vehicles vehicles = ((ScenarioImpl)sc).getVehicles();
		VehicleType truckType = new VehicleTypeImpl(Id.create("commercial", VehicleType.class));
		truckType.setMaximumVelocity(100./3.6);
		truckType.setLength(18.);
		vehicles.addVehicleType(truckType);
		
		for(Person person : sc.getPopulation().getPersons().values()){
			/* Subpopulation. */
			sc.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), config.plans().getSubpopulationAttributeName(), "commercial");
			
			/* Vehicles */
			Vehicle truck = VehicleUtils.getFactory().createVehicle(Id.create(person.getId(), Vehicle.class), truckType);
			vehicles.addVehicle(truck);
		}
		
		/* Run the controler */
		Controler controler = new Controler(sc);
		controler.setOverwriteFiles(true);
		
		PlanStrategyFactory newPlanStrategyFactory = new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario,
					EventsManager eventsManager) {
				PlanStrategyImpl strategy = new PlanStrategyImpl( new RandomPlanSelector<Plan, Person>() );
				strategy.addStrategyModule(new NewDigicorePlanStrategyModule());
				return strategy;
			}
		};
		
		controler.addPlanStrategyFactory("newPlan", newPlanStrategyFactory );

		controler.run();
		Header.printFooter();
	}

	private static final class NewDigicorePlanStrategyModule implements PlanStrategyModule {
		private final PathDependentNetwork network;

		public NewDigicorePlanStrategyModule() {
			DigicorePathDependentNetworkReader_v1 nr = new DigicorePathDependentNetworkReader_v1();
			nr.parse(PATH_DEPENDENT_NETWORK);
			network = nr.getPathDependentNetwork();
		}


		@Override
		public void prepareReplanning(ReplanningContext replanningContext) {

		}

		@Override
		public void handlePlan(Plan plan) {
			// TODO Auto-generated method stub
			LOG.info("   ====> Woopie: plan handled.");
		}

		@Override
		public void finishReplanning() {
			// TODO Auto-generated method stub

		}
	}


}
