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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.utils.misc.PopulationUtils;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import playground.southafrica.gauteng.roadpricingscheme.GautengRoadPricingScheme;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollFactor_Subpopulation;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollVehicleType;
import playground.southafrica.gauteng.roadpricingscheme.TollFactorI;
import playground.southafrica.gauteng.routing.PersonSpecificTravelDisutilityInclTollFactory;
import playground.southafrica.gauteng.scoring.GautengScoringFunctionFactory;
import playground.southafrica.gauteng.scoring.GenerationOfMoneyEvents;
import playground.southafrica.gauteng.utilityofmoney.GautengUtilityOfMoney;
import playground.southafrica.gauteng.utilityofmoney.UtilityOfMoneyI;
import playground.southafrica.utilities.Header;

/**
 * 
 * @author jwjoubert
 */
public class GautengControler_subpopulations {
	private static final class SetVehicleInAllNetworkRoutes implements PlanStrategyModule {
		private final Scenario scenario;

		private SetVehicleInAllNetworkRoutes(Scenario scenario) {
			this.scenario = scenario;
		}

		@Override
		public void prepareReplanning(ReplanningContext replanningContext) {}

		@Override
		public void handlePlan(Plan plan) {
			@SuppressWarnings("unchecked")
			Id vehId = (Id) scenario.getPopulation().getPersonAttributes()
				.getAttribute(plan.getPerson().getId().toString(), VEH_ID ) ;
			for ( Leg leg : PopulationUtils.getLegs(plan) ) {
				if ( leg.getRoute()!=null && leg.getRoute() instanceof NetworkRoute ) {
					((NetworkRoute)leg.getRoute()).setVehicleId(vehId);
				}
			}
		}

		@Override
		public void finishReplanning() {}
	}


	private final static Logger LOG = Logger
			.getLogger(GautengControler_subpopulations.class);
	
	private static final String RE_ROUTE_AND_SET_VEHICLE = "ReRouteAndSetVehicle";
	private static String VEH_ID = "TransportModeToVehicleIdMap" ;
	
	public static enum User { johan, kai } ;
	
	private static User user = User.johan ;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GautengControler_subpopulations.class.toString(),
				args);
		/* Config must be passed as an argument, everything else is optional. */
		final String configFilename = args[0];
		Config config = ConfigUtils.createConfig();
		if ( configFilename != null ) {
			ConfigUtils.loadConfig(config, configFilename);
		}
		
		/* Required argument:
		 * [0] - Config file;
		 * 
		 * Optional arguments:
		 * [1] - Population file;
		 * [2] - Person attribute file;
		 * [3] - Network file;
		 * [4] - Toll (road pricing) file.
		 * [5] - Base value of time ;
		 * [6] - Value-of-Time multiplier; and
		 * [7] - Number of threads. 
		 * [8] - user // should presumably be earlier in sequence?
		 */
		
		setOptionalArguments(args, config);

		double baseValueOfTime = 110.;
		double valueOfTimeMultiplier = 4.;
		if (args.length > 5 && args[5] != null && args[5].length() > 0) {
			baseValueOfTime = Double.parseDouble(args[5]);
		}

		if (args.length > 6 && args[6] != null && args[6].length() > 0) {
			valueOfTimeMultiplier = Double.parseDouble(args[6]);
		}

		int numberOfThreads = 1;
		if (args.length > 6 && args[7] != null && args[7].length() > 0) {
			numberOfThreads = Integer.parseInt(args[7]);
		}
		
		// ===========================================

		/* Set some other config parameters. */
		config.plans().setSubpopulationAttributeName("subpopulation");
		
		String[] modes ={"car","commercial"};
		config.qsim().setMainModes( Arrays.asList(modes) );
		config.plansCalcRoute().setNetworkModes(Arrays.asList(modes));
		
		assignSubpopulationStrategies(config);
		
		config.planCalcScore().setBrainExpBeta(1.0);
		if ( user==User.johan ) {
			config.controler().setWritePlansInterval(2);
		} else if ( user==User.kai ) {
			config.controler().setWritePlansInterval(50);
		}
		config.timeAllocationMutator().setAffectingDuration(false);

		config.vspExperimental().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);
		
		config.vspExperimental().addParam( VspExperimentalConfigKey.vspDefaultsCheckingLevel, VspExperimentalConfigGroup.ABORT ) ;

		// ===========================================

		final Scenario sc = ScenarioUtils.loadScenario(config);
		config.scenario().setUseVehicles(true);

		/* CREATE VEHICLES. */
		createVehiclePerPerson(sc);

		final Controler controler = new Controler(sc);
		controler.setOverwriteFiles(true);
		
		controler.addControlerListener(new IterationStartsListener() {
			
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				if(event.getIteration() == sc.getConfig().controler().getFirstIteration()){
					for(Person p : sc.getPopulation().getPersons().values()){
						for(Plan plan : p. getPlans()){
							new SetVehicleInAllNetworkRoutes(sc).handlePlan(plan);
						}
					}
				}
			}
		});

		/* Set number of threads. */
		controler.getConfig().global().setNumberOfThreads(numberOfThreads);
		

		// final Scenario sc = controler.getScenario();
		
	
		if (sc.getConfig().scenario().isUseRoadpricing()) {
			throw new RuntimeException(
					"roadpricing must NOT be enabled in config.scenario in order to use special "
							+ "road pricing features.  aborting ...");
		}
		
		// CONSTRUCT ROUTING ALGO WHICH ALSO SETS VEHICLES:
		controler.addPlanStrategyFactory(RE_ROUTE_AND_SET_VEHICLE, new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(final Scenario scenario, EventsManager eventsManager) {
				PlanStrategyImpl planStrategy = new PlanStrategyImpl( new RandomPlanSelector<Plan>() ) ; 
				planStrategy.addStrategyModule( new ReRoute( scenario ) ); 
				planStrategy.addStrategyModule( new SetVehicleInAllNetworkRoutes(scenario));
				return planStrategy ;
			}
		});

		final TollFactorI tollFactor = new SanralTollFactor_Subpopulation(sc);

		// SOME STATISTICS:
		controler.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				Map<SanralTollVehicleType, Double> cnt = new HashMap<SanralTollVehicleType, Double>();
				for (Person person : sc.getPopulation().getPersons().values()) {
					SanralTollVehicleType type = tollFactor.typeOf(person
							.getId());
					if (cnt.get(type) == null) {
						cnt.put(type, 0.);
					}
					cnt.put(type, 1. + cnt.get(type));
				}
				for (SanralTollVehicleType type : SanralTollVehicleType
						.values()) {
					LOG.info(String.format("type: %30s; cnt: %8.0f",
							type.toString(), cnt.get(type)));
				}
			}
		});

		// CONSTRUCT VEH-DEP ROAD PRICING SCHEME:
		RoadPricingScheme vehDepScheme = new GautengRoadPricingScheme(sc
				.getConfig().roadpricing().getTollLinksFile(), sc.getNetwork(),
				sc.getPopulation(), tollFactor);

		// CONSTRUCT UTILITY OF MONEY:

		UtilityOfMoneyI personSpecificUtilityOfMoney = new GautengUtilityOfMoney(
				sc.getConfig().planCalcScore(), baseValueOfTime,
				valueOfTimeMultiplier, tollFactor);

		// INSTALL ROAD PRICING (in the longer run, re-merge with RoadPricing
		// class):
		// insert into scoring:
		controler.addControlerListener(new GenerationOfMoneyEvents(sc
				.getNetwork(), sc.getPopulation(), vehDepScheme, tollFactor));

		controler.setScoringFunctionFactory(new GautengScoringFunctionFactory(
				sc, personSpecificUtilityOfMoney));

		// insert into routing:
		controler
				.setTravelDisutilityFactory(new PersonSpecificTravelDisutilityInclTollFactory(
						vehDepScheme, personSpecificUtilityOfMoney));

		// ADDITIONAL ANALYSIS:
		// This is not truly necessary. It could be removed or copied in order
		// to remove the dependency on the kai
		// playground. For the time being, I (kai) would prefer to leave it the
		// way it is since I am running the Gauteng
		// scenario and I don't want to maintain two separate analysis
		// listeners. But once that period is over, this
		// argument does no longer apply. kai, mar'12
		//
		// I (JWJ, June '13) commented this listener out as the dependency is
		// not working.

		controler.addControlerListener(new KaiAnalysisListener());

		// RUN:

		controler
				.getConfig()
				.controler()
				.setOutputDirectory(
						"/Users/jwjoubert/Documents/Temp/sanral-runs");
		 controler.run();

		Header.printFooter();
	}

	/**
	 * @param sc
	 */
	private static void createVehiclePerPerson(final Scenario sc) {
		/* Create vehicle types. */
		VehiclesFactory vf = VehicleUtils.getFactory();
		LOG.info("Creating vehicle types.");
		VehicleType vehicle_A2 = new VehicleTypeImpl(new IdImpl("a2"));
		vehicle_A2.setDescription("Light vehicle with SANRAL toll class `A2'");

		VehicleType vehicle_B = new VehicleTypeImpl(new IdImpl("b"));
		vehicle_B.setDescription("Light vehicle with SANRAL toll class `B'");
		vehicle_B.setMaximumVelocity(100.0 / 3.6);
		vehicle_B.setLength(10.0);

		VehicleType vehicle_C = new VehicleTypeImpl(new IdImpl("c"));
		vehicle_C.setDescription("Light vehicle with SANRAL toll class `C'");
		vehicle_C.setMaximumVelocity(80.0 / 3.6);
		vehicle_C.setLength(15.0);

		/* Create a vehicle per person. */
		Vehicles vehicles = ((ScenarioImpl) sc).getVehicles();
		for (Person p : sc.getPopulation().getPersons().values()) {
			String vehicleType = (String) sc.getPopulation()
					.getPersonAttributes()
					.getAttribute(p.getId().toString(), "vehicleTollClass");
			Boolean eTag = (Boolean) sc.getPopulation().getPersonAttributes()
					.getAttribute(p.getId().toString(), "eTag");

			/* Create the vehicle. */
			Vehicle v = null;
			if (vehicleType.equalsIgnoreCase("A2")) {
				v = vf.createVehicle(p.getId(), vehicle_A2);
			} else if (vehicleType.equalsIgnoreCase("B")) {
				v = vf.createVehicle(p.getId(), vehicle_B);
			} else if (vehicleType.equalsIgnoreCase("C")) {
				v = vf.createVehicle(p.getId(), vehicle_C);
			} else {
				throw new RuntimeException("Unknown vehicle toll class: "
						+ vehicleType);
			}
			vehicles.addVehicle(v);
					
			vehicles.getVehicleAttributes().putAttribute(v.getId().toString(), "eTag", eTag);

			sc.getPopulation().getPersonAttributes().putAttribute( p.getId().toString(), VEH_ID, v.getId() );
		}
	}

	/**
	 * @param args
	 * @param config
	 */
	private static void setOptionalArguments(String[] args, Config config) {
		/* Optional arguments. */
		String plansFilename = null;
		if (args.length > 1 && args[1] != null && args[1].length() > 0) {
			plansFilename = args[1];
			config.plans().setInputFile(plansFilename);
		}

		String personAttributeFilename = null;
		if (args.length > 2 && args[2] != null && args[2].length() > 0) {
			personAttributeFilename = args[2];
			config.plans().setInputPersonAttributeFile(personAttributeFilename);
		}

		String networkFilename = null;
		if (args.length > 3 && args[3] != null && args[3].length() > 0) {
			networkFilename = args[3];
			config.network().setInputFile(networkFilename);
		}

		String tollFilename = null;
		if (args.length > 4 && args[4] != null && args[4].length() > 0) {
			tollFilename = args[4];
			config.roadpricing().setTollLinksFile(tollFilename);
		}
	}

	/**
	 * @param config
	 */
	private static void assignSubpopulationStrategies(Config config) {
		
		/* Set up the strategies for the different subpopulations. */
		
		{ /*
		 * Car: ChangeExpBeta: 70%; TimeAllocationMutator: 15%; ReRoute: 15%
		 */
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings
					.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta
							.toString());
			changeExpBetaStrategySettings.setSubpopulation("car");
			changeExpBetaStrategySettings.setProbability(0.7);
			config.strategy()
					.addStrategySettings(changeExpBetaStrategySettings);

			StrategySettings timeStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings
					.setModuleName(PlanStrategyRegistrar.Names.TimeAllocationMutator
							.toString());
			timeStrategySettings.setSubpopulation("car");
			timeStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);
			
			StrategySettings reRouteWithId = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			reRouteWithId.setModuleName(RE_ROUTE_AND_SET_VEHICLE);
			reRouteWithId.setProbability(0.15);
			reRouteWithId.setSubpopulation("car");
			config.strategy().addStrategySettings(reRouteWithId);
		}

		{ /*
		 * Commercial vehicles: ChangeExpBeta: 85%; ReRoute: 15%
		 */
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings
					.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta
							.toString());
			changeExpBetaStrategySettings.setSubpopulation("commercial");
			changeExpBetaStrategySettings.setProbability(0.80);
			config.strategy()
					.addStrategySettings(changeExpBetaStrategySettings);

			StrategySettings reRouteWithId = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			reRouteWithId.setModuleName(RE_ROUTE_AND_SET_VEHICLE);
			reRouteWithId.setProbability(0.20);
			reRouteWithId.setSubpopulation("commercial");
			config.strategy().addStrategySettings(reRouteWithId);
		}

		{ /*
		 * Bus: ChangeExpBeta: 70%; TimeAllocationMutator: 15%; ReRoute: 15%
		 */
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings
					.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta
							.toString());
			changeExpBetaStrategySettings.setSubpopulation("bus");
			changeExpBetaStrategySettings.setProbability(0.7);
			config.strategy()
					.addStrategySettings(changeExpBetaStrategySettings);

			StrategySettings timeStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings
					.setModuleName(PlanStrategyRegistrar.Names.TimeAllocationMutator
							.toString());
			timeStrategySettings.setSubpopulation("bus");
			timeStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);

			StrategySettings reRouteWithId = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			reRouteWithId.setModuleName(RE_ROUTE_AND_SET_VEHICLE);
			reRouteWithId.setProbability(0.15);
			reRouteWithId.setSubpopulation("bus");
			config.strategy().addStrategySettings(reRouteWithId);
		}
		{ /*
		 * Taxi: ChangeExpBeta: 70%; TimeAllocationMutator: 15%; ReRoute: 15%
		 */
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings
					.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta
							.toString());
			changeExpBetaStrategySettings.setSubpopulation("taxi");
			changeExpBetaStrategySettings.setProbability(0.7);
			config.strategy()
					.addStrategySettings(changeExpBetaStrategySettings);

			StrategySettings timeStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings
					.setModuleName(PlanStrategyRegistrar.Names.TimeAllocationMutator
							.toString());
			timeStrategySettings.setSubpopulation("taxi");
			timeStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);

			StrategySettings reRouteWithId = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			reRouteWithId.setModuleName(RE_ROUTE_AND_SET_VEHICLE);
			reRouteWithId.setProbability(0.15);
			reRouteWithId.setSubpopulation("taxi");
			config.strategy().addStrategySettings(reRouteWithId);
		}
		{ /*
		 * External traffic: ChangeExpBeta: 70%; TimeAllocationMutator: 15%; ReRoute: 15%
		 */
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			changeExpBetaStrategySettings
					.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta
							.toString());
			changeExpBetaStrategySettings.setSubpopulation("ext");
			changeExpBetaStrategySettings.setProbability(0.7);
			config.strategy()
					.addStrategySettings(changeExpBetaStrategySettings);

			StrategySettings timeStrategySettings = new StrategySettings(
					ConfigUtils.createAvailableStrategyId(config));
			timeStrategySettings
					.setModuleName(PlanStrategyRegistrar.Names.TimeAllocationMutator
							.toString());
			timeStrategySettings.setSubpopulation("ext");
			timeStrategySettings.setProbability(0.15);
			config.strategy().addStrategySettings(timeStrategySettings);

			StrategySettings reRouteWithId = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			reRouteWithId.setModuleName(RE_ROUTE_AND_SET_VEHICLE);
			reRouteWithId.setProbability(0.15);
			reRouteWithId.setSubpopulation("ext");
			config.strategy().addStrategySettings(reRouteWithId);
		}
	}

}
