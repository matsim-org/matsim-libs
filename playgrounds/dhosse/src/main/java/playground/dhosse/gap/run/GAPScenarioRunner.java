package playground.dhosse.gap.run;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.qsim.CarsharingQsimFactory;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.router.OneWayCarsharingRoutingModule;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.contrib.multimodal.MultiModalControlerListener;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.util.BikeTravelTimeFactory;
import org.matsim.contrib.multimodal.router.util.WalkTravelTimeFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.analysis.SpatialAnalysis;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * 
 * @author dhosse
 *
 */
public class GAPScenarioRunner {

	//0(x), 1189(x), 4711(x), 8192, 6837
	private static final long randomSeed = 6837;
	
	// the input path for the current simulation
	public static final String simInputPath = Global.runInputDir;

	// number of iterations
	private static final int lastIteration = 100;

	// configure innovative strategies you want to use
	private static final boolean addModeChoice = true;
	private static final boolean addTimeChoice = false;

	private static final boolean addLocationChoice = false;

	// carsharing
	private static final boolean carsharing = true;

	// cadyts
	private static final boolean runCadyts = false;

	// multimodal
	private static final boolean multimodal = false;

	// roadpricing
	private static final boolean roadpricing = false;

	// use this to determine the cost structure for carsharing
	// non-reduced costs: time fee and distance fee
	// reduced costs: only time fee
	private static final boolean reducedCosts = true;
	
	//input car sharing stations file
	//stations.txt: base case
	//csStationsAtParkingSpaces.txt: one car sharing station at every parking space in the county 
	private static final String inputCsStationsFile = "csStationsAtParkingSpaces.txt";

	/**
	 * 
	 * @param args
	 */
	public static void main(String args[]) {

		run();
		// runAnalysis();

	}

	/**
	 * Runs the GP scenario.
	 */
	private static void run() {

		// create a new config and a new scenario and load it
		final Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, Global.runInputDir + "config.xml");

		config.controler().setLastIteration(lastIteration);
		config.controler().setOutputDirectory("/home/dhosse/run12/output_" + randomSeed);

		config.controler().setWritePlansInterval(100);
		config.controler().setWriteEventsInterval(100);
		config.controler().setCreateGraphs(false);
		
		config.plans().setInputFile("/home/dhosse/run12/output_plans.xml.gz");
		// disable counts
		 config.counts().setCountsFileName(null);
		 
		 //set random seed
		 config.global().setRandomSeed(randomSeed);
		 
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// create a second scenario containing only the cleaned (road) network
		// in order to map agents on car links
		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario2.getNetwork()).readFile(config.network()
				.getInputFile());
		Set<Id<Link>> linkIds = new HashSet<>();
		for (Link link : scenario2.getNetwork().getLinks().values()) {
			if (link.getFreespeed() > 50 / 3.6) {
				linkIds.add(link.getId());
			}
		}
		for (Id<Link> linkId : linkIds)
			scenario2.getNetwork().removeLink(linkId);
		new NetworkCleaner().run(scenario2.getNetwork());

		XY2Links xy2links = new XY2Links(scenario2);

		ObjectAttributes atts = new ObjectAttributes();
		new ObjectAttributesXmlReader(atts).parse(Global.runInputDir
				+ "demographicAtts.xml");

		for (Person person : scenario.getPopulation().getPersons().values()) {
			xy2links.run(person);
			String age = (String) atts.getAttribute(person.getId().toString(),
					Global.AGE);
			if (age != null) {
				person.getCustomAttributes().put("age", Integer.parseInt(age));
			}

			String sex = (String) atts.getAttribute(person.getId().toString(),
					Global.SEX);
			if (sex != null) {
				if (sex.equals("0"))
					sex = "m";
				else
					sex = "f";
				person.getCustomAttributes().put("sex", sex);
			}
		}

		// create a new controler
		final Controler controler = new Controler(scenario);

		if (runCadyts) {
			addCadyts(controler);
		}

		// by default, route choice is the only innovative strategy.
		// additional strategies can be switched on/off via boolean members (see
		// above)

		if (addModeChoice) {

			addModeChoice(controler);

		}

		if (addTimeChoice) {

			addTimeChoice(controler);

		}

		if (addLocationChoice) {

			addLocationChoice(controler);

		}

		// END

		// add the extensions you want to use

		if (multimodal) {

			addMultimodal(controler);

		}

		if (carsharing) {

			addCarsharing(controler);

		}

		if (roadpricing) {

			addRoadpricing(controler);

		}

		// END

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).toInstance(
						new FreespeedTravelTimeAndDisutility(controler
								.getConfig().planCalcScore()));
				addTravelDisutilityFactoryBinding(TransportMode.ride)
						.toInstance(
								new RandomizingTimeDistanceTravelDisutility.Builder(
										TransportMode.ride));
				addRoutingModuleBinding(TransportMode.ride)
						.toProvider(
								new NetworkRouting(
										TransportMode.ride));

			}
		});

		// finally, add controler listeners and event handlers
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				
				addEventHandlerBinding().toInstance(new ZugspitzbahnFareHandler(controler));
				
			}
		});

		// start of the simulation
		controler.run();

	}

	private static void addRoadpricing(final Controler controler) {

		RoadPricingConfigGroup rp = new RoadPricingConfigGroup();
		rp.setTollLinksFile("/home/dhosse/roadpricing.xml");
		rp.setRoutingRandomness(3.);
		controler.getConfig().addModule(rp);

		controler.setModules(new ControlerDefaultsWithRoadPricingModule());

	}

	private static void addCadyts(final Controler controler) {

		// create the cadyts context and add it to the control(l)er:
		// Counts<Link> counts = new Counts<>();
		// new
		// CountsReaderMatsimV1(counts).parse("/home/dhosse/run11/input/counts.xml");

		controler.addOverridingModule(new CadytsCarModule());
		// include cadyts into the plan scoring (this will add the cadyts
		// corrections to the scores):
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			private final CharyparNagelScoringParametersForPerson parameters = new SubpopulationCharyparNagelScoringParameters(
					controler.getScenario());
			@Inject private CadytsContext cContext;

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {

				final CharyparNagelScoringParameters params = parameters
						.getScoringParameters(person);

				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator
						.addScoringFunction(new CharyparNagelLegScoring(params,
								controler.getScenario().getNetwork()));
				scoringFunctionAccumulator
						.addScoringFunction(new CharyparNagelActivityScoring(
								params));
				scoringFunctionAccumulator
						.addScoringFunction(new CharyparNagelAgentStuckScoring(
								params));

				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(
						person.getSelectedPlan(), controler.getConfig(),
						cContext);
				final double cadytsScoringWeight = 100. * controler.getConfig()
						.planCalcScore().getBrainExpBeta();
				scoringFunction
						.setWeightOfCadytsCorrection(cadytsScoringWeight);
				scoringFunctionAccumulator.addScoringFunction(scoringFunction);

				return scoringFunctionAccumulator;
			}
		});

	}

	private static void addMultimodal(final Controler controler) {

		// services.getConfig().services().setMobsim("myMobsim");
		controler.getConfig().travelTimeCalculator().setFilterModes(true);

		final MultiModalConfigGroup mm = new MultiModalConfigGroup();
		mm.setMultiModalSimulationEnabled(true);
		mm.setSimulatedModes("bike");
		controler.getConfig().addModule(mm);

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {

				for (String mode : CollectionUtils.stringToSet(mm
						.getSimulatedModes())) {

					if (mode.equals(TransportMode.bike)) {

						addTravelTimeBinding(mode).toProvider(
								new BikeTravelTimeFactory(controler.getConfig()
										.plansCalcRoute()));
						addTravelDisutilityFactoryBinding(mode)
								.toInstance(
										new RandomizingTimeDistanceTravelDisutility.Builder(
												mode));
						addRoutingModuleBinding(mode)
								.toProvider(
										new NetworkRouting(
												mode));

					} else if (mode.equals(TransportMode.walk)) {

						addTravelTimeBinding(mode).toProvider(
								new WalkTravelTimeFactory(controler.getConfig()
										.plansCalcRoute()));
						addTravelDisutilityFactoryBinding(mode)
								.toInstance(
										new RandomizingTimeDistanceTravelDisutility.Builder(
												mode));
						addRoutingModuleBinding(mode)
								.toProvider(
										new NetworkRouting(
												mode));

					}

				}

				addControlerListenerBinding().to(
						MultiModalControlerListener.class);
				// bindMobsim().toProvider(MultimodalQSimFactory.class);

			}

		});

	}

	/**
	 * Adds time choice as additional replanning strategy. The method creates
	 * separate strategy settings for each subpopulation.
	 * 
	 * @param controler
	 */
	private static void addTimeChoice(final MatsimServices controler) {

		// New strategy settings are created
		// Specify a name, a subpopulation and a weight for the new strategy and
		// add if to the existing ones in the strategy config group
		StrategySettings tam = new StrategySettings();
		tam.setStrategyName("TimeAllocationMutator");
		tam.setSubpopulation(null);
		tam.setWeight(0.1);
//		tam.setDisableAfter((int) (lastIteration * 0.6));
		controler.getConfig().strategy().addStrategySettings(tam);

		StrategySettings car = new StrategySettings();
		car.setStrategyName("TimeAllocationMutator");
		car.setSubpopulation(Global.GP_CAR);
		car.setWeight(0.1);
//		car.setDisableAfter((int) (lastIteration * 0.6));
		controler.getConfig().strategy().addStrategySettings(car);

		StrategySettings license = new StrategySettings();
		license.setStrategyName("TimeAllocationMutator");
		license.setSubpopulation(Global.LICENSE_OWNER);
		license.setWeight(0.1);
//		license.setDisableAfter((int) (lastIteration * 0.6));
		controler.getConfig().strategy().addStrategySettings(license);

		StrategySettings commuter = new StrategySettings();
		commuter.setStrategyName("TimeAllocationMutator");
		commuter.setSubpopulation(Global.COMMUTER);
		commuter.setWeight(0.1);
//		commuter.setDisableAfter((int) (lastIteration * 0.6));
		controler.getConfig().strategy().addStrategySettings(commuter);

	}

	/**
	 * Adds destination choice as innovative strategy.
	 * 
	 * @param controler
	 */
	private static void addLocationChoice(final Controler controler) {

		DestinationChoiceConfigGroup dccg = new DestinationChoiceConfigGroup();

		StringBuffer sb = new StringBuffer();
		StringBuffer epsilons = new StringBuffer();
		for (ActivityParams params : controler.getConfig().planCalcScore()
				.getActivityParams()) {

			if (params.getActivityType().contains("shop")
					|| params.getActivityType().contains("othe")
					|| params.getActivityType().contains("leis")) {
				if (sb.length() < 1) {
					sb.append(params.getActivityType());
					epsilons.append("1.0");
				} else {
					sb.append(", " + params.getActivityType());
					epsilons.append(", 1.0");
				}
			}

		}

		dccg.setFlexibleTypes(sb.toString());
		dccg.setEpsilonScaleFactors(epsilons.toString());
		dccg.setpkValuesFile("/home/danielhosse/run9a/personsKValues.xml");
		dccg.setfkValuesFile("/home/danielhosse/run9a/facilitiesKValues.xml");
		dccg.setScaleFactor(1);

		controler.getConfig().addModule(dccg);
		DestinationChoiceBestResponseContext dcbr = new DestinationChoiceBestResponseContext(
				controler.getScenario());
		dcbr.init();
		DCScoringFunctionFactory scFactory = new DCScoringFunctionFactory(
				controler.getScenario(), dcbr);
		scFactory.setUsingConfigParamsForScoring(true);
		controler.addControlerListener(new DestinationChoiceInitializer(dcbr));

		if (Double.parseDouble(controler.getConfig().findParam(
				"locationchoice", "restraintFcnExp")) > 0.0
				&& Double.parseDouble(controler.getConfig().findParam(
						"locationchoice", "restraintFcnFactor")) > 0.0) {
			controler.addControlerListener(new FacilitiesLoadCalculator(dcbr
					.getFacilityPenalties()));
		}

		controler.setScoringFunctionFactory(scFactory);

		StrategySettings dc = new StrategySettings();
		dc.setStrategyName("org.matsim.contrib.locationchoice.BestReplyLocationChoicePlanStrategy");
		dc.setSubpopulation(null);
		dc.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(dc);

	}

	/**
	 * Adds subtour mode choice strategy settings to the controler. These
	 * strategies are configured for different types of subpopulations (persons
	 * with car and license, commuters, persons without license).
	 * 
	 * @param controler
	 */
	private static void addModeChoice(final Controler controler) {

		StrategySettings carAvail = new StrategySettings();
		carAvail.setStrategyName("SubtourModeChoice_".concat(Global.GP_CAR));
		carAvail.setSubpopulation(Global.GP_CAR);
		carAvail.setWeight(0.1);
//		carAvail.setDisableAfter((int) (0.7 * lastIteration));
		controler.getConfig().strategy().addStrategySettings(carAvail);

		StrategySettings license = new StrategySettings();
		license.setStrategyName("SubtourModeChoice_"
				.concat(Global.LICENSE_OWNER));
		license.setSubpopulation(Global.LICENSE_OWNER);
		license.setWeight(0.1);
//		license.setDisableAfter((int) (0.7 * lastIteration));
		controler.getConfig().strategy().addStrategySettings(license);

		StrategySettings nonCarAvail = new StrategySettings();
		nonCarAvail.setStrategyName("SubtourModeChoice_".concat("NO_CAR"));
		nonCarAvail.setSubpopulation(null);
		nonCarAvail.setWeight(0.1);
//		nonCarAvail.setDisableAfter((int) (0.7 * lastIteration));
		controler.getConfig().strategy().addStrategySettings(nonCarAvail);

		StrategySettings commuter = new StrategySettings();
		commuter.setStrategyName("SubtourModeChoice_".concat(Global.COMMUTER));
		commuter.setSubpopulation(Global.COMMUTER);
		commuter.setWeight(0.1);
//		commuter.setDisableAfter((int) (0.7 * lastIteration));
		controler.getConfig().strategy().addStrategySettings(commuter);

		setModeChoiceModules(controler, carsharing);

	}

	private static void setModeChoiceModules(final Controler controler,
			boolean carsharingEnabled) {

		if (carsharingEnabled) {

			controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					addPlanStrategyBinding(
							"SubtourModeChoice_".concat(Global.GP_CAR))
							.toProvider(
									new javax.inject.Provider<PlanStrategy>() {
										String[] availableModes = {
												TransportMode.car,
												TransportMode.pt,
												TransportMode.bike,
												TransportMode.walk,
												"onewaycarsharing" };
										String[] chainBasedModes = {
												TransportMode.car,
												TransportMode.bike };

										@Override
										public PlanStrategy get() {
											final Builder builder = new Builder(
													new RandomPlanSelector<Plan, Person>());
											builder.addStrategyModule(new SubtourModeChoice(
													controler
															.getConfig()
															.global()
															.getNumberOfThreads(),
													availableModes,
													chainBasedModes, false,tripRouterProvider));
											builder.addStrategyModule(new ReRoute(
													controler.getScenario(), tripRouterProvider));
											return builder.build();
										}
									});

				}
			});

			controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					addPlanStrategyBinding(
							"SubtourModeChoice_".concat("NO_CAR")).toProvider(
							new javax.inject.Provider<PlanStrategy>() {
								String[] availableModes = { TransportMode.pt,
										TransportMode.bike, TransportMode.walk };
								String[] chainBasedModes = { TransportMode.bike };

								@Override
								public PlanStrategy get() {
									final Builder builder = new Builder(
											new RandomPlanSelector<Plan, Person>());
									builder.addStrategyModule(new SubtourModeChoice(
											controler.getConfig().global()
													.getNumberOfThreads(),
											availableModes, chainBasedModes,
											false, tripRouterProvider));
									builder.addStrategyModule(new ReRoute(
											controler.getScenario(), tripRouterProvider));
									return builder.build();
								}
							});

				}
			});

			controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					addPlanStrategyBinding(
							"SubtourModeChoice_".concat(Global.COMMUTER))
							.toProvider(
									new javax.inject.Provider<PlanStrategy>() {
										String[] availableModes = {
												TransportMode.car,
												TransportMode.pt };
										String[] chainBasedModes = {
												TransportMode.car,
												TransportMode.bike };

										@Override
										public PlanStrategy get() {
											final Builder builder = new Builder(
													new RandomPlanSelector<Plan, Person>());
											builder.addStrategyModule(new SubtourModeChoice(
													controler
															.getConfig()
															.global()
															.getNumberOfThreads(),
													availableModes,
													chainBasedModes, false, tripRouterProvider));
											builder.addStrategyModule(new ReRoute(
													controler.getScenario(), tripRouterProvider));
											return builder.build();
										}
									});

				}
			});

			controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					addPlanStrategyBinding(
							"SubtourModeChoice_".concat(Global.LICENSE_OWNER))
							.toProvider(
									new javax.inject.Provider<PlanStrategy>() {
										String[] availableModes = {
												TransportMode.pt,
												TransportMode.bike,
												TransportMode.walk,
												"onewaycarsharing" };
										String[] chainBasedModes = { TransportMode.bike };

										@Override
										public PlanStrategy get() {
											final Builder builder = new Builder(
													new RandomPlanSelector<Plan, Person>());
											builder.addStrategyModule(new SubtourModeChoice(
													controler
															.getConfig()
															.global()
															.getNumberOfThreads(),
													availableModes,
													chainBasedModes, false, tripRouterProvider));
											builder.addStrategyModule(new ReRoute(
													controler.getScenario(), tripRouterProvider));
											return builder.build();
										}
									});

				}
			});

		} else {

			controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					addPlanStrategyBinding(
							"SubtourModeChoice_".concat(Global.GP_CAR))
							.toProvider(
									new javax.inject.Provider<PlanStrategy>() {
										String[] availableModes = {
												TransportMode.car,
												TransportMode.pt,
												TransportMode.bike,
												TransportMode.walk };
										String[] chainBasedModes = {
												TransportMode.car,
												TransportMode.bike };

										@Override
										public PlanStrategy get() {
											final Builder builder = new Builder(
													new RandomPlanSelector<Plan, Person>());
											builder.addStrategyModule(new SubtourModeChoice(
													controler
															.getConfig()
															.global()
															.getNumberOfThreads(),
													availableModes,
													chainBasedModes, false, tripRouterProvider));
											builder.addStrategyModule(new ReRoute(
													controler.getScenario(), tripRouterProvider));
											return builder.build();
										}
									});

				}
			});

			controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					addPlanStrategyBinding(
							"SubtourModeChoice_".concat("NO_CAR")).toProvider(
							new javax.inject.Provider<PlanStrategy>() {
								String[] availableModes = { TransportMode.pt,
										TransportMode.bike, TransportMode.walk };
								String[] chainBasedModes = { TransportMode.bike };

								@Override
								public PlanStrategy get() {
									final Builder builder = new Builder(
											new RandomPlanSelector<Plan, Person>());
									builder.addStrategyModule(new SubtourModeChoice(
											controler.getConfig().global()
													.getNumberOfThreads(),
											availableModes, chainBasedModes,
											false, tripRouterProvider));
									builder.addStrategyModule(new ReRoute(
											controler.getScenario(), tripRouterProvider));
									return builder.build();
								}
							});

				}
			});

			controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					addPlanStrategyBinding(
							"SubtourModeChoice_".concat(Global.COMMUTER))
							.toProvider(
									new javax.inject.Provider<PlanStrategy>() {
										String[] availableModes = {
												TransportMode.car,
												TransportMode.pt };
										String[] chainBasedModes = {
												TransportMode.car,
												TransportMode.bike };

										@Override
										public PlanStrategy get() {
											final Builder builder = new Builder(
													new RandomPlanSelector<Plan, Person>());
											builder.addStrategyModule(new SubtourModeChoice(
													controler
															.getConfig()
															.global()
															.getNumberOfThreads(),
													availableModes,
													chainBasedModes, false, tripRouterProvider));
											builder.addStrategyModule(new ReRoute(
													controler.getScenario(), tripRouterProvider));
											return builder.build();
										}
									});

				}
			});

			controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					addPlanStrategyBinding(
							"SubtourModeChoice_".concat(Global.LICENSE_OWNER))
							.toProvider(
									new javax.inject.Provider<PlanStrategy>() {
										String[] availableModes = {
												TransportMode.pt,
												TransportMode.bike,
												TransportMode.walk };
										String[] chainBasedModes = { TransportMode.bike };

										@Override
										public PlanStrategy get() {
											final Builder builder = new Builder(
													new RandomPlanSelector<Plan, Person>());
											builder.addStrategyModule(new SubtourModeChoice(
													controler
															.getConfig()
															.global()
															.getNumberOfThreads(),
													availableModes,
													chainBasedModes, false, tripRouterProvider));
											builder.addStrategyModule(new ReRoute(
													controler.getScenario(), tripRouterProvider));
											return builder.build();
										}
									});

				}
			});

		}

	}

	private static void addCarsharing(final Controler controler){
		
		OneWayCarsharingConfigGroup ow = new OneWayCarsharingConfigGroup();
		ow.setConstantOneWayCarsharing("-0.0");
		if(!reducedCosts){
			ow.setDistanceFeeOneWayCarsharing("-0.00026");
			ow.setRentalPriceTimeOneWayCarsharing("-0.000625");
		} else{
			ow.setDistanceFeeOneWayCarsharing("-0.0");
			ow.setRentalPriceTimeOneWayCarsharing("0.004833333");
		}
		ow.setsearchDistance("2000");
		ow.setTimeFeeOneWayCarsharing("-0.0");
		ow.setTimeParkingFeeOneWayCarsharing("-0.0");
		ow.setUtilityOfTravelling("-6");
		ow.setUseOneWayCarsharing(true);
		ow.setvehiclelocations(Global.runInputDir + inputCsStationsFile);
		controler.getConfig().addModule(ow);
		
		TwoWayCarsharingConfigGroup tw = new TwoWayCarsharingConfigGroup();
		tw.setUseTwoWayCarsharing(false);
		controler.getConfig().addModule(tw);
		
		FreeFloatingConfigGroup ff = new FreeFloatingConfigGroup();
		ff.setUseFeeFreeFloating(false);
		controler.getConfig().addModule(ff);
		
		CarsharingConfigGroup cs = new CarsharingConfigGroup();
		cs.setStatsWriterFrequency("1");
		controler.getConfig().addModule(cs);
		
		//add carsharing to the main (congested) modes
		String[] mainModes = new String[]{"car", "onewaycarsharing"};
		controler.getConfig().qsim().setMainModes(Arrays.asList(mainModes));
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to( RandomTripToCarsharingStrategy.class ) ;
				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy").to( CarsharingSubtourModeChoiceStrategy.class ) ;				
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				bindMobsim().toProvider( CarsharingQsimFactory.class );
				
				addRoutingModuleBinding("onewaycarsharing").toInstance(new OneWayCarsharingRoutingModule());
				
				bind(MainModeIdentifier.class).toInstance(new MainModeIdentifier() {
                    final MainModeIdentifier defaultModeIdentifier = new MainModeIdentifierImpl();

                    @Override
                    public String identifyMainMode(
                            final List<? extends PlanElement> tripElements) {
                        // we still need to provide a way to identify our trips
                        // as being twowaycarsharing trips.
                        // This is for instance used at re-routing.
                        for ( PlanElement pe : tripElements ) {
                            if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "twowaycarsharing" ) ) {
                                return "twowaycarsharing";
                            }
                            else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "onewaycarsharing" ) ) {
                                return "onewaycarsharing";
                            }
                            else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "freefloating" ) ) {
                                return "freefloating";
                            }
                        }
                        // if the trip doesn't contain a carsharing leg,
                        // fall back to the default identification method.
                        return defaultModeIdentifier.identifyMainMode( tripElements );
                    }
                });
			}
		});		
		//setting up the scoring function factory, inside different scoring functions are set-up
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
			}
		});

		controler.addControlerListener(new CarsharingListener(controler,
				cs.getStatsWriterFrequency() ) ) ;
		
	}

	/**
	 * An entry point for some analysis methods...
	 */
	private static void runAnalysis() {

		// PersonAnalysis.createLegModeDistanceDistribution(Global.matsimInputDir
		// + "Pläne/plansV3.xml.gz", "/home/danielhosse/Dokumente/lmdd/");
		SpatialAnalysis.writePopulationToShape(
				"/home/dhosse/Dokumente/01_eGAP/plansV4.xml.gz",
				"/home/dhosse/Dokumente/01_eGAP/popV4.shp");

	}

}
