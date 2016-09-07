package playground.artemc.psim;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSModule;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.contrib.pseudosimulation.mobsim.PSimFactory;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;
import playground.artemc.analysis.AnalysisControlerListener;
import playground.artemc.crowding.events.CrowdedPenaltyEvent;
import playground.artemc.crowding.internalization.CapacityDelayEvent;
import playground.artemc.crowding.internalization.InternalizationPtControlerListener;
import playground.artemc.crowding.internalization.TransferDelayInVehicleEvent;
import playground.artemc.heterogeneity.HeterogeneityConfigGroup;
import playground.artemc.heterogeneity.IncomeHeterogeneityModule;
import playground.artemc.heterogeneity.eventsBasedPTRouter.TransitRouterEventsAndHeterogeneityBasedWSModule;
import playground.artemc.heterogeneity.routing.TimeDistanceAndHeterogeneityBasedTravelDisutilityFactory;
import playground.artemc.heterogeneity.routing.TimeDistanceTollAndHeterogeneityBasedTravelDisutilityProviderWrapper;
import playground.artemc.heterogeneity.scoring.HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory;
import playground.artemc.pricing.RoadPricingWithoutTravelDisutilityModule;
import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by artemc on 4/20/15.
 * A class for to evaluate choice alternatives based on travel times from events.
 * It loads the travel time structures with events and psim repeatedly executes using those fixed times.
 */

public class ChoiceGenerationWithCrowdingControler implements BeforeMobsimListener {

	private static final Logger log = Logger.getLogger(ChoiceGenerationWithCrowdingControler.class);

	private WaitTimeStuckCalculator waitTimeCalculator;
	private StopStopTimeCalculator stopStopTimeCalculator;
	private TravelTimeCalculator travelTimeCalculator;
	private PSimFactory pSimFactory;
	private Scenario scenario;
	private Controler controler;
	private MoneyEventHandler moneyHandler;
	private Config config;
	private String eventsFile;

	private CrowdingPsimHandler crowdingPsimHandler;
	private MarginalCostPricingPtPsimHandler marginalCostPricingPtPsimHandler;

	private boolean internalizationOfComfortDisutility = false;
	private boolean internalizationOfCrowdingDelay = false;

	public void setInternalizationOfCrowdingDelay(boolean internalizationOfCrowdingDelay) {
		this.internalizationOfCrowdingDelay = internalizationOfCrowdingDelay;
	}


	public void setInternalizationOfComfortDisutility(boolean internalizationOfComfortDisutility) {
		this.internalizationOfComfortDisutility = internalizationOfComfortDisutility;
	}

	public void setControler(Controler controler) {
		this.controler = controler;
	}

	public Controler getControler() {
		return controler;
	}

	public MoneyEventHandler getMoneyEventHandler() {
		return moneyHandler;
	}

	public CrowdingPsimHandler getCrowdingPsimHandler() {
		return crowdingPsimHandler;
	}

	public ChoiceGenerationWithCrowdingControler(Config config, String eventsFile) {
		this.config = config;
		this.eventsFile = eventsFile;
	}

	public void load(){


	//public ChoiceGenerationWithCrowdingControler(Config config, String eventsFile) {
		config.parallelEventHandling().setSynchronizeOnSimSteps(false);
		config.parallelEventHandling().setNumberOfThreads(1);
		config.planCalcScore().setWriteExperiencedPlans(true);
		scenario = ScenarioUtils.loadScenario(config);
		controler = new Controler(scenario);

		Integer inputDirectoryDepth = controler.getConfig().plans().getInputFile().split("/").length;
		String inputDirectory = controler.getConfig().plans().getInputFile().split("/")[inputDirectoryDepth - 2];
		boolean roadpricing = inputDirectory.contains("toll");

		if (roadpricing == true) {
			log.info("First-best roadpricing enabled!");
//			controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityWithoutTravelDisutilityModule(), new RoadPricingWithoutTravelDisutilityModule(),new UpdateSocialCostPricingSchemeModule());
			controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityModule(), new RoadPricingWithoutTravelDisutilityModule());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(TravelDisutilityFactory.class).toProvider(TimeDistanceTollAndHeterogeneityBasedTravelDisutilityProviderWrapper.TimeDistanceTollAndHeterogeneityBasedTravelDisutilityProvider.class);

				}
			});

		} else {
			log.info("No roadpricing!");
			controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityModule());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(TravelDisutilityFactory.class).to(TimeDistanceAndHeterogeneityBasedTravelDisutilityFactory.class);
				}
			});
		}

		waitTimeCalculator = new WaitTimeStuckCalculator(controler.getScenario().getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime() - controler.getConfig().qsim().getStartTime()));
		stopStopTimeCalculator = new StopStopTimeCalculator(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime() - controler.getConfig().qsim().getStartTime()));


		log.warn("About to init TransitRouterEventsHeteroWSFactory...");
		if (controler.getConfig().getModule(HeterogeneityConfigGroup.GROUP_NAME).getParams().get("incomeOnTravelCostType").equals("homo")) {
			controler.addOverridingModule(new TransitRouterEventsWSModule(waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
		} else {
			controler.addOverridingModule(new TransitRouterEventsAndHeterogeneityBasedWSModule(waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
		}

		//CrowdednessObserver observer = new CrowdednessObserver(scenario, controler.getEvents(), new StochasticRule());
		//observer = new CrowdednessObserver(scenario, controler.getEvents(), new SimpleRule());
		//controler.getEvents().addHandler(observer);
		//scoreTracker = new ScoreTracker();
		//scoreListener = new ScoreListener(scoreTracker);

		//Scoring
		HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory customScoringFunctionFactory = new HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(controler.getConfig().planCalcScore(), controler.getScenario().getNetwork());
		customScoringFunctionFactory.setSimulationType(controler.getConfig().getModule(HeterogeneityConfigGroup.GROUP_NAME).getParams().get("incomeOnTravelCostType"));
		controler.setScoringFunctionFactory(customScoringFunctionFactory);

		// Additional analysis
		AnalysisControlerListener analysisControlerListener = new AnalysisControlerListener((MutableScenario) controler.getScenario());
		controler.addControlerListener(analysisControlerListener);

		controler.getScenario().getConfig().controler().setLastIteration(0);

//		controler.setTransitRouterFactory(new TransitRouterEventsWSFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
//      controler.setScoringFunctionFactory(
//            new CharyparNagelOpenTimesScoringFunctionFactory(controler.getConfig().planCalcScore(),
//                    controler.getScenario()));

		/*Fill travel times and crowding cost from events*/

		travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), config.travelTimeCalculator());

		EventsReaderXMLv1.CustomEventMapper crowdedPenaltyEventMapper = new EventsReaderXMLv1.CustomEventMapper() {
			@Override
			public Event apply(GenericEvent event) {
				Map<String, String> atts = event.getAttributes();
					Id<Person> personId = Id.create(atts.get("person"), Person.class);
					Id<Vehicle> vehicleId = Id.create("dummy", Vehicle.class);
					return new CrowdedPenaltyEvent(event.getTime(), personId, vehicleId, Double.parseDouble(atts.get("penalty")), Double.parseDouble(atts.get("externalities")));
			}
		};


		EventsReaderXMLv1.CustomEventMapper transferDelayInVehicleEventEventMapper = new EventsReaderXMLv1.CustomEventMapper() {
			@Override
			public Event apply(GenericEvent event) {
				Map<String, String> atts = event.getAttributes();
				Id<Person> personId = Id.create(atts.get(TransferDelayInVehicleEvent.ATTRIBUTE_PERSON), Person.class);
				int affectedAgents = Integer.parseInt(atts.get(TransferDelayInVehicleEvent.ATTRIBUTE_AFFECTED_AGENTS));
				Id<Vehicle> vehicleId = Id.create(atts.get(TransferDelayInVehicleEvent.ATTRIBUTE_VEHICLE), Vehicle.class);
				double delay = Double.parseDouble(atts.get(TransferDelayInVehicleEvent.ATTRIBUTE_DELAY));
				return new TransferDelayInVehicleEvent(personId, vehicleId, event.getTime(), affectedAgents, delay);
			}
		};

		EventsReaderXMLv1.CustomEventMapper capacityDelayEventEventEventMapper = new EventsReaderXMLv1.CustomEventMapper() {
			@Override
			public Event apply(GenericEvent event) {
				Map<String, String> atts = event.getAttributes();
				Id<Person> personId = Id.create(atts.get(CapacityDelayEvent.ATTRIBUTE_PERSON), Person.class);
				Id<Person> affectedPersonId = Id.create(atts.get(CapacityDelayEvent.ATTRIBUTE_AFFECTED_AGENT), Person.class);
				Id<Vehicle> vehicleId = Id.create(atts.get(CapacityDelayEvent.ATTRIBUTE_VEHICLE), Vehicle.class);
				double delay = Double.parseDouble(atts.get(CapacityDelayEvent.ATTRIBUTE_DELAY));
				return new CapacityDelayEvent(event.getTime(), personId, affectedPersonId, vehicleId, delay);
			}
		};



		EventsManagerImpl eventsManager = new EventsManagerImpl();
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
		eventsManager.addHandler(waitTimeCalculator);
		eventsManager.addHandler(stopStopTimeCalculator);
		eventsManager.addHandler(travelTimeCalculator);
		reader.addCustomEventMapper("CrowdedPenaltyEvent", crowdedPenaltyEventMapper);
		reader.addCustomEventMapper("TransferDelayInVehicleEvent", transferDelayInVehicleEventEventMapper);
		reader.addCustomEventMapper("CapacityDelayEvent", capacityDelayEventEventEventMapper);


		CrowdingEventsHandler crowdingEventsHandler = new CrowdingEventsHandler();
		TransferDelayInVehicleEventHandler transferDelayInVehicleEventHandler = new TransferDelayInVehicleEventHandler(controler.getScenario());
		CapacityDelayEventHandler capacityDelayEventHandler = new CapacityDelayEventHandler(controler.getScenario());

		eventsManager.addHandler(crowdingEventsHandler);
		eventsManager.addHandler(transferDelayInVehicleEventHandler);
		eventsManager.addHandler(capacityDelayEventHandler);
		reader.readFile(eventsFile);

		HashMap<Integer, Double> avgCrowdingDisutility = new HashMap<Integer, Double>();
		HashMap<Integer, Double> avgCrowdingExternality = new HashMap<Integer, Double>();
		for (Integer bin : crowdingEventsHandler.getTimeBinToPenalty().keySet()) {
			Double avgDisutility = crowdingEventsHandler.getTimeBinToPenalty().get(bin).doubleValue() / crowdingEventsHandler.getTimeBinToPassengers().get(bin);
			Double avgExternality = crowdingEventsHandler.getTimeBinToExternality().get(bin).doubleValue() / crowdingEventsHandler.getTimeBinToPassengers().get(bin).doubleValue();
			avgCrowdingDisutility.put(bin, avgDisutility);
			avgCrowdingExternality.put(bin, avgExternality);
			System.out.println(bin * 300 + ":   " + avgDisutility + "   " + avgExternality);
		}

		HashMap<Integer, Double> avgTransferDelayExternality = new HashMap<Integer, Double>();
		for (Integer bin : transferDelayInVehicleEventHandler.getTimeBinToTransferDelayExternality().keySet()) {;
			Double avgExternality = transferDelayInVehicleEventHandler.getTimeBinToTransferDelayExternality().get(bin).doubleValue() / transferDelayInVehicleEventHandler.getTimeBinToPassengers().get(bin).doubleValue();
			avgTransferDelayExternality.put(bin, avgExternality);
			System.out.println(bin * 300 + ":  " + avgExternality);
		}

		HashMap<Integer, Double> avgCapacityDelayExternality = new HashMap<Integer, Double>();
		for (Integer bin : capacityDelayEventHandler.getTimeBinToCapacityDelayExternality().keySet()) {;
			Double avgExternality = capacityDelayEventHandler.getTimeBinToCapacityDelayExternality().get(bin).doubleValue() / capacityDelayEventHandler.getTimeBinToPassengers().get(bin).doubleValue();
			avgCapacityDelayExternality.put(bin, avgExternality);
			System.out.println(bin * 300 + ":  " + avgExternality);
		}

		//Money payment analysis
		moneyHandler = new MoneyEventHandler();
		controler.getEvents().addHandler(moneyHandler);

		crowdingPsimHandler = new CrowdingPsimHandler(controler.getEvents(), avgCrowdingDisutility, avgCrowdingExternality, controler.getScenario().getConfig().planCalcScore().getMarginalUtilityOfMoney(), moneyHandler, internalizationOfComfortDisutility);
		controler.getEvents().addHandler(crowdingPsimHandler);

		if(internalizationOfCrowdingDelay){
			marginalCostPricingPtPsimHandler = new MarginalCostPricingPtPsimHandler(controler.getEvents(), avgTransferDelayExternality, avgCapacityDelayExternality, controler.getScenario().getConfig().planCalcScore().getMarginalUtilityOfMoney(), moneyHandler);
			controler.getEvents().addHandler(marginalCostPricingPtPsimHandler);
		}

		pSimFactory = new PSimFactory();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return pSimFactory.createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});


		controler.addControlerListener(this);
		//controler.run();
	}

	public void run() {
		//controler.setOverwriteFiles(true);
		controler.run();
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Collection<Plan> plans = new ArrayList<>();
		for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
			plans.add(person.getSelectedPlan());
		}
		pSimFactory.setWaitTime(waitTimeCalculator.getWaitTimes());
		pSimFactory.setTravelTime(travelTimeCalculator.getLinkTravelTimes());
		pSimFactory.setStopStopTime(stopStopTimeCalculator.getStopStopTimes());
		pSimFactory.setPlans(plans);
	}




}
