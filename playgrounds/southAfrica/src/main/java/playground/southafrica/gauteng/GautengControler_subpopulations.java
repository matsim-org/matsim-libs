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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.contrib.matsim4urbansim.utils.network.NetworkSimplifier;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
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
import playground.southafrica.kai.gauteng.ConfigurableTravelDisutilityFactory;
import playground.southafrica.utilities.Header;

/**
 * 
 * @author jwjoubert
 */
public class GautengControler_subpopulations {
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
		OutputDirectoryLogging.catchLogEntries();

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
		 * [9] - Output directory
		 * [10] - Counts file
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

		// ===========================================

		/* Set some other config parameters. */
		config.plans().setSubpopulationAttributeName("subpopulation");
		config.global().setCoordinateSystem("WGS84_SA_Albers");

		String[] modes ={"car","commercial"};
		config.qsim().setMainModes( Arrays.asList(modes) );
		config.plansCalcRoute().setNetworkModes(Arrays.asList(modes));

		// yy note: I doubt that the jdqsim honors "main modes".  Either check, or do not use. kai, jan'14
		if ( !config.controler().getMobsim().equals( ControlerConfigGroup.MobsimType.qsim.toString() ) ) {
			throw new RuntimeException("error") ;
		}

		config.qsim().setLinkDynamics( LinkDynamics.PassingQ.toString() );

		assignSubpopulationStrategies(config);

		config.planCalcScore().setBrainExpBeta(1.0);
		config.controler().setWritePlansInterval(100);

		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.controler().setRoutingAlgorithmType( RoutingAlgorithmType.FastAStarLandmarks );
		config.controler().setLastIteration(1000);
		if ( user==User.kai ) {
			config.controler().setLastIteration(100);
		}
		
		final double sampleFactor = 0.01 ;

		config.counts().setCountsScaleFactor(1./sampleFactor);
		config.qsim().setFlowCapFactor(sampleFactor);
		config.qsim().setStorageCapFactor(Math.pow(sampleFactor, -0.25)); // interpolates between 0.03 @ 0.01 and 1 @ 1
		config.counts().setOutputFormat("all");
		config.qsim().setEndTime(36.*3600.);
		
		config.qsim().setStuckTime(10.);
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setSnapshotPeriod(Double.POSITIVE_INFINITY);
		config.controler().setWriteSnapshotsInterval(0);
		
		if ( user==User.kai ) {
//			config.parallelEventHandling().setNumberOfThreads(1); // even "1" is slowing down my laptop quite a lot
		} else if(user == User.johan){
			config.parallelEventHandling().setNumberOfThreads(1); 
		}

//		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration );
		// is now default.

		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.ABORT ) ;
		config.vspExperimental().setWritingOutputEvents(true);

		config.addConfigConsistencyChecker( new VspConfigConsistencyCheckerImpl() );
		config.checkConsistency();

		// ===========================================

		final Scenario sc = ScenarioUtils.loadScenario(config);
		config.scenario().setUseVehicles(true); // _after_ scenario loading. :-(
		// (there will eventually be something like sc.createVehiclesContainer or ((ScenarioImpl)sc).createVehiclesContainer which will
		// address this problem. kai, feb'14)
		
//		if ( user==User.kai ) {
//			simplifyPopulation(sc) ;
//		}

		/* CREATE VEHICLES. */
		createVehiclePerPerson(sc);

		final Controler controler = new Controler(sc);
		controler.setOverwriteFiles(true);

		// SET VEHICLES ...
		// ... at beginning:
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

		// ... during replanning (this also needs to be registered as strategy in the config):
		controler.addPlanStrategyFactory(RE_ROUTE_AND_SET_VEHICLE, new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(final Scenario scenario, EventsManager eventsManager) {
				PlanStrategyImpl planStrategy = new PlanStrategyImpl( new RandomPlanSelector<Plan>() ) ; 
				planStrategy.addStrategyModule( new ReRoute( scenario ) ); 
				planStrategy.addStrategyModule( new SetVehicleInAllNetworkRoutes(scenario));
				return planStrategy ;
			}
		});

		setUpRoadPricingAndScoring(baseValueOfTime, valueOfTimeMultiplier, sc, controler);

		// ADDITIONAL ANALYSIS:
		controler.addControlerListener(new KaiAnalysisListener());

		// RUN:

		controler.run();

		Header.printFooter();
	}

	@SuppressWarnings("unused")
	private static void installJdqsim(Config config) {
		throw new RuntimeException("cannot use jdqsim with vehicle-based toll") ;
		
//		double sampleFactor = 0.01 ;
//		config.controler().setMobsim(MobsimType.JDEQSim.toString());
//		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.END_TIME, "36:00:00") ;
//		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.FLOW_CAPACITY_FACTOR, Double.toString(sampleFactor) ) ;
//		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.SQUEEZE_TIME, "10" ) ;
//		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.STORAGE_CAPACITY_FACTOR, Double.toString( Math.pow(sampleFactor, -0.25)) ) ;
	}

	/**
	 * I think this method served the following functions:<ul>
	 * <li> change commercial vehicles from mode "commercial" to mode "car" so that the jdqsim would simulate them
	 * <li> remove some of the commercial population since it seemed too many
	 * <li> remove the routes in case the network is simplified 
	 * </ul>
	 * I think that only the last item is still meaningful.  kai, feb'14
	 */
//	@SuppressWarnings("unused")
	private static void simplifyPopulation(final Scenario sc) {
		
//		simplifyNetwork(sc.getNetwork());

		// modify population:
		PopulationFactory pf = sc.getPopulation().getFactory() ;
//		List<Id> commercialIds = new ArrayList<Id>() ;
		for ( Person pp : sc.getPopulation().getPersons().values() ) {
			Plan plan = pp.getSelectedPlan() ;
			pp.getPlans().clear(); 
			Plan newPlan = pf.createPlan() ;

			for ( PlanElement pe : plan.getPlanElements() ) {
				if ( pe instanceof Activity ) {
					Activity act = (Activity) pe ;
					
					Activity newAct = pf.createActivityFromCoord(act.getType(), act.getCoord()) ;
					newAct.setEndTime(act.getEndTime()); // or don't set at all if not there ???
					newAct.setMaximumDuration(act.getMaximumDuration());

					newPlan.addActivity(newAct);
				} else if (pe instanceof Leg) {
					Leg leg = (Leg) pe ;

					Leg newLeg ;
//					if ( leg.getMode().equals("commercial")) {
//						newLeg = pf.createLeg("car") ;
//						commercialIds.add(pp.getId()) ;
//					} else {
						newLeg = pf.createLeg(leg.getMode()) ;
//					}
					newLeg.setDepartureTime( leg.getDepartureTime() ) ;
					newLeg.setTravelTime( leg.getDepartureTime() );

					newPlan.addLeg(newLeg); 
				}
			}
			
			pp.addPlan( newPlan ) ;
			pp.setSelectedPlan(newPlan);
			
		} // end person
		
//		Random rnd = MatsimRandom.getLocalInstance() ;
//		for ( Id id : commercialIds ) {
//			if ( rnd.nextDouble() < 0.9 ) {
//				sc.getPopulation().getPersons().remove(id) ;
//			}
//		}

	}

	/**
	 * this throws out all the local roads.  A problem is that then the initial routes to not work any more, so this is not that great.  kai, feb'14
	 */
	@SuppressWarnings("unused")
	private static void simplifyNetwork(final Network network) {
		// ...
		List<Id> toBeRemoved = new ArrayList<Id>();
		for ( Link link : network.getLinks().values() ) {
			if ( link.getCapacity() < 999. && link.getNumberOfLanes() <= 1. && link.getFreespeed()<13. ) {
				toBeRemoved.add( link.getId() ) ;
			}
		}
		for ( Id id : toBeRemoved ) {
			network.removeLink( id ) ;
		}
		new org.matsim.core.network.algorithms.NetworkCleaner().run(network);
		
		// try simplifying the network:
		final long numberOfLinksBefore = network.getLinks().size() ;

		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));

		NetworkSimplifier nsimply = new NetworkSimplifier();
		nsimply.setNodesToMerge(nodeTypesToMerge);
		nsimply.setMergeLinkStats(false); //default = false
		nsimply.run(network);
		// yy this might combine freeway links, meaning they would not charge toll any more afterwards

		final long numberOfLinksAfter = network.getLinks().size() ;

		LOG.warn( "number of links before: " + numberOfLinksBefore + "; after: " + numberOfLinksAfter +
				"; difference: " + (numberOfLinksBefore-numberOfLinksAfter) ) ;
	}

	private static void setUpRoadPricingAndScoring(double baseValueOfTime, double valueOfTimeMultiplier, final Scenario sc,
			final Controler controler) {
		// ROAD PRICING:
		if (sc.getConfig().scenario().isUseRoadpricing()) {
			throw new RuntimeException(
					"roadpricing must NOT be enabled in config.scenario in order to use special "
							+ "road pricing features.  aborting ...");
		}

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
				sc, sc.getConfig().planCalcScore(),
				baseValueOfTime, valueOfTimeMultiplier, tollFactor);

		// INSTALL ROAD PRICING (in the longer run, re-merge with RoadPricing
		// class):
		// insert into scoring:
		controler.addControlerListener(new GenerationOfMoneyEvents(
				sc.getNetwork(), sc.getPopulation(), vehDepScheme, tollFactor
				));

		controler.setScoringFunctionFactory(new GautengScoringFunctionFactory(
				sc, personSpecificUtilityOfMoney
				));

		// insert into routing:
//		controler.setTravelDisutilityFactory(new PersonSpecificTravelDisutilityInclTollFactory(
//				vehDepScheme, personSpecificUtilityOfMoney
//				));
		
		final ConfigurableTravelDisutilityFactory travelDisutilityFactory = new ConfigurableTravelDisutilityFactory( sc );
		// ---
		travelDisutilityFactory.setRoadPricingScheme( vehDepScheme ); // including toll. Needed for all experiments
//		travelDisutilityFactory.setUom(personSpecificUtilityOfMoney); 
//		travelDisutilityFactory.setScoringFunctionFactory(scoringFunctionFactory); // including auto-sensing.  Not needed for abmtrans paper
		travelDisutilityFactory.setRandomness(3);
		// ---
		controler.setTravelDisutilityFactory( travelDisutilityFactory );

	}

	/**
	 * @param sc
	 */
	private static void createVehiclePerPerson(final Scenario sc) {
		/* Create vehicle types. */
		VehiclesFactory vf = VehicleUtils.getFactory();
		LOG.info("Creating vehicle types.");
		VehicleType vehicle_A2 = new VehicleTypeImpl(new IdImpl("A2"));
		vehicle_A2.setDescription("Light vehicle with SANRAL toll class `A2'");

		VehicleType vehicle_B = new VehicleTypeImpl(new IdImpl("B"));
		vehicle_B.setDescription("Short commercial vehicle with SANRAL toll class `B'");
		vehicle_B.setMaximumVelocity(100.0 / 3.6);
		vehicle_B.setLength(10.0);

		VehicleType vehicle_C = new VehicleTypeImpl(new IdImpl("C"));
		vehicle_C.setDescription("Medium/long commercial vehicle with SANRAL toll class `C'");
		vehicle_C.setMaximumVelocity(80.0 / 3.6);
		vehicle_C.setLength(15.0);

		/* Create a vehicle per person. */
		Vehicles vehicles = ((ScenarioImpl) sc).getVehicles();
		for (Person p : sc.getPopulation().getPersons().values()) {
			String vehicleType = (String) sc.getPopulation()
					.getPersonAttributes()
					.getAttribute(p.getId().toString(), SanralTollFactor_Subpopulation.VEH_TOLL_CLASS_ATTRIB_NAME);
			Boolean eTag = (Boolean) sc.getPopulation().getPersonAttributes()
					.getAttribute(p.getId().toString(), SanralTollFactor_Subpopulation.E_TAG_ATTRIBUTE_NAME);

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

			vehicles.getVehicleAttributes().putAttribute(v.getId().toString(), SanralTollFactor_Subpopulation.E_TAG_ATTRIBUTE_NAME, eTag);

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
		
		int numberOfThreads = 1;
		if (args.length > 7 && args[7] != null && args[7].length() > 0) {
			numberOfThreads = Integer.parseInt(args[7]);
			config.global().setNumberOfThreads(numberOfThreads);
		}

		if (args.length > 8 && args[8] != null && args[8].length() > 0) {
			user = User.valueOf( args[8] ) ;
		}
		
		String outputDirectory = null;
		if(args.length > 9 && args[9] != null && args[9].length() > 0) {
			outputDirectory = args[9];
			config.controler().setOutputDirectory(outputDirectory);
		}

		String countsFilename = null;
		if(args.length > 10 && args[10] != null && args[10].length() > 0) {
			countsFilename = args[10];
			config.counts().setCountsFileName(countsFilename);
		}
	}

	/**
	 * @param config
	 */
	private static void assignSubpopulationStrategies(Config config) {

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8); 

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

	private static final class SetVehicleInAllNetworkRoutes implements PlanStrategyModule {
		private final Scenario scenario;

		private SetVehicleInAllNetworkRoutes(Scenario scenario) {
			this.scenario = scenario;
		}

		@Override
		public void prepareReplanning(ReplanningContext replanningContext) {}

		@Override
		public void handlePlan(Plan plan) {
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



}
