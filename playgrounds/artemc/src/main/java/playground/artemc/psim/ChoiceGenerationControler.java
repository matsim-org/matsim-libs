package playground.artemc.psim;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
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
import playground.artemc.analysis.AnalysisControlerListener;
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

/**
 * Created by artemc on 4/20/15.
 * A class for to evaluate choice alternatives based on travel times from events.
 * It loads the travel time structures with events and psim repeatedly executes using those fixed times.
 */

public class ChoiceGenerationControler implements BeforeMobsimListener {
	
	private static final Logger log = Logger.getLogger(ChoiceGenerationControler.class);

	private WaitTimeStuckCalculator waitTimeCalculator;
	private StopStopTimeCalculator stopStopTimeCalculator;
	private TravelTimeCalculator travelTimeCalculator;
	private PSimFactory pSimFactory;
	private Scenario scenario;
	private Controler controler;
	private MoneyEventHandler moneyHandler;

	public void setControler(Controler controler) {
		this.controler = controler;
	}

	public Controler getControler() {
		return controler;
	}

	public MoneyEventHandler getMoneyEventHandler() {
		return moneyHandler;
	}

	public ChoiceGenerationControler(Config config, String eventsFile) {

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
		if(controler.getConfig().getModule(HeterogeneityConfigGroup.GROUP_NAME).getParams().get("incomeOnTravelCostType").equals("homo")){
			controler.addOverridingModule(new TransitRouterEventsWSModule(waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
		}else
		{
			controler.addOverridingModule(new TransitRouterEventsAndHeterogeneityBasedWSModule(waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
		}

		//Scoring
		HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory customScoringFunctionFactory = new HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(controler.getConfig().planCalcScore(), controler.getScenario().getNetwork());
		customScoringFunctionFactory.setSimulationType(controler.getConfig().getModule(HeterogeneityConfigGroup.GROUP_NAME).getParams().get("incomeOnTravelCostType"));
		controler.setScoringFunctionFactory(customScoringFunctionFactory);

		// Additional analysis
		AnalysisControlerListener analysisControlerListener = new AnalysisControlerListener((MutableScenario) controler.getScenario());
		controler.addControlerListener(analysisControlerListener);

		//Money payment analysis
		moneyHandler = new MoneyEventHandler();
		controler.getEvents().addHandler(moneyHandler);

		controler.getScenario().getConfig().controler().setLastIteration(0);

//		controler.setTransitRouterFactory(new TransitRouterEventsWSFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
//      controler.setScoringFunctionFactory(
//            new CharyparNagelOpenTimesScoringFunctionFactory(controler.getConfig().planCalcScore(),
//                    controler.getScenario()));

        travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), config.travelTimeCalculator());

		EventsManagerImpl eventsManager = new EventsManagerImpl();
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
		eventsManager.addHandler(waitTimeCalculator);
		eventsManager.addHandler(stopStopTimeCalculator);
		eventsManager.addHandler(travelTimeCalculator);
		reader.parse(eventsFile);

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
