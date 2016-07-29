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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.southafrica.projects.complexNetworks.pathDependence.DigicorePathDependentNetworkReader_v1;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
import playground.southafrica.utilities.Header;

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
		
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		
		/* Set up the commercial mode by setting it as a network mode, and 
		 * FIXME adding its routing parameters. */
		String[] modes ={"car","commercial"};
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
		
		/* Ensure that 'commercial' is an available mode for each link. */
		/* FIXME This must ultimately be fixed/addressed in 
		 * playground.southafrica.utilities.network.ConvertOsmToMatsim !! */
		LOG.warn("Ensuring all links take 'commercial' as mode...");
		Collection<String> modesCollection = Arrays.asList(modes);
		Set<String> modesSet = new HashSet<>(modesCollection);
		for(Link link : sc.getNetwork().getLinks().values()){
			link.setAllowedModes(modesSet);
		}
		LOG.warn("Done adding 'commercial' modes. This must be fixed in ConvertOsmToMatsim");
		
		/* Set the population as "subpopulation", and create a vehicle for each. */
		Vehicles vehicles = ((MutableScenario)sc).getVehicles();
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
		
		/* Run the controler. */
		final Provider<PlanStrategy> newPlanStrategyFactory = new javax.inject.Provider<PlanStrategy>() {
			@Override
			public PlanStrategy get() {
				PlanSelector<Plan, Person> planSelector = new ExpBetaPlanSelector<>(1.0);
				Builder builder = new Builder(planSelector );
				builder.addStrategyModule(new NewDigicorePlanStrategyModule());
				return builder.build();
			}
		};
		
		Controler controler = new Controler(sc);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("newPlan").toProvider(newPlanStrategyFactory);
				addTravelTimeBinding("commercial").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("commercial").to(carTravelDisutilityFactoryKey());
			}
		});

		controler.run();
		Header.printFooter();
	}

	private static final class NewDigicorePlanStrategyModule implements PlanStrategyModule {
		private final PathDependentNetwork network;

		public NewDigicorePlanStrategyModule() {
			DigicorePathDependentNetworkReader_v1 nr = new DigicorePathDependentNetworkReader_v1();
			nr.readFile(PATH_DEPENDENT_NETWORK);
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
