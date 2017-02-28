package playground.artemc.heterogeneity;


import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import playground.artemc.analysis.AnalysisCrowdingControlerListener;
import playground.artemc.annealing.SimpleAnnealer;
import playground.artemc.crowding.CrowdednessObserver;
import playground.artemc.crowding.internalization.InternalizationPtControlerListener;
import playground.artemc.crowding.newScoringFunctions.ScoreListener;
import playground.artemc.crowding.newScoringFunctions.ScoreTracker;
import playground.artemc.crowding.rules.SimpleRule;
import playground.artemc.dwellTimeModel.QSimFactory;
import playground.artemc.heterogeneity.eventsBasedPTRouter.TransitRouterEventsAndHeterogeneityBasedWSModule;
import playground.artemc.heterogeneity.routing.TimeDistanceAndHeterogeneityBasedTravelDisutilityFactory;
import playground.artemc.heterogeneity.routing.TimeDistanceTollAndHeterogeneityBasedTravelDisutilityProviderWrapper;
import playground.artemc.heterogeneity.scoring.DisaggregatedHeterogeneousCrowdingScoreAnalyzer;
import playground.artemc.heterogeneity.scoring.HeterogeneousCharyparNagelScoringFunctionForAnalysisAndCrowdingFactory;
import playground.artemc.pricing.LinkOccupancyAnalyzerModule;
import playground.artemc.pricing.RoadPricingWithoutTravelDisutilityModule;
import playground.artemc.pricing.UpdateSocialCostPricingSchemeWithSpillAndOffSwitch;
import playground.artemc.socialCost.MeanTravelTimeCalculator;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ControlerWithHeteroTollComfort {

	private static final Logger log = Logger.getLogger(ControlerWithHeteroTollComfort.class);

	private static String input;
	private static String output;
	private static String simulationType = "homo";
	private static Double heterogeneityFactor = 1.0;
	private static boolean roadpricing = false;
	private static Double fare = 0.0;

	private static CrowdednessObserver observer;
	private static ScoreTracker scoreTracker;
	private static ScoreListener scoreListener;
	private static int numberOfIterations = 1000;

	private static boolean internalizationOfComfortDisutility = false;

	public static void main(String[] args){

		input = args[0];
		if(args.length>1){
			output = args[1];
		}
		if(args.length>2){
			simulationType = args[2];
		}
		if(args.length>3){
			heterogeneityFactor = Double.valueOf(args[3]);			
		}

		if(args.length>4 && args[4].equals("toll")){
			roadpricing = true;
		}

		if(args.length>4 && args[4].equals("crowdInt")){
			internalizationOfComfortDisutility = true;
		}

		if(args.length>5 && args[5].equals("crowdInt")){
			internalizationOfComfortDisutility = true;
		}


		log.info("Simulation type: " + simulationType);
		if(simulationType.equals("hetero")|| simulationType.equals("heteroAlpha") || simulationType.equals("heteroGamma") || simulationType.equals("heteroAlphaRatio") ||simulationType.equals("heteroPropSymmetric") || simulationType.equals("heteroAlphaUniform") || simulationType.equals("heteroGammaUniform")){
			log.info("Heterogeneityfactor: " + heterogeneityFactor);
		}else if(!simulationType.equals("homo")){
			throw new RuntimeException("Unknown income heterogeneity type");
		}

		ControlerWithHeteroTollComfort runner = new ControlerWithHeteroTollComfort();
		runner.run();
	}

	private void run() {

		Scenario scenario = initScenario();
		//System.setProperty("matsim.preferLocalDtds", "true");

		final Controler controler = new Controler(scenario);
		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);

		//Adjust PT Fare
		Double ptConstant = - fare * scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).setConstant(ptConstant);
		log.info("PT Constant was set to "+ptConstant);

		//Adjust heterogeneity parameters from input arguments
		Map<String, String> params = scenario.getConfig().getModule(HeterogeneityConfigGroup.GROUP_NAME).getParams();
		Double adjustedIncomeOnTravelCostLambda = Double.valueOf(scenario.getConfig().getModule(HeterogeneityConfigGroup.GROUP_NAME).getParams().get("incomeOnTravelCostLambda"))*heterogeneityFactor;
		scenario.getConfig().getModule(HeterogeneityConfigGroup.GROUP_NAME).addParam("incomeOnTravelCostLambda", adjustedIncomeOnTravelCostLambda.toString());
		scenario.getConfig().getModule(HeterogeneityConfigGroup.GROUP_NAME).addParam("incomeOnTravelCostType", simulationType);

		log.info("Adding Simple Annealer...");
		controler.addControlerListener(new SimpleAnnealer());

		if(roadpricing==true) {
			log.info("First-best roadpricing enabled!");
//			controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityWithoutTravelDisutilityModule(), new RoadPricingWithoutTravelDisutilityModule(),new UpdateSocialCostPricingSchemeModule());
			controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityModule(), new RoadPricingWithoutTravelDisutilityModule(), new LinkOccupancyAnalyzerModule(), new UpdateSocialCostPricingSchemeWithSpillAndOffSwitch());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(TravelDisutilityFactory.class).toProvider(TimeDistanceTollAndHeterogeneityBasedTravelDisutilityProviderWrapper.TimeDistanceTollAndHeterogeneityBasedTravelDisutilityProvider.class);

				}
			});

		}else{
			log.info("No roadpricing!");
			controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityModule());
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					bind(TravelDisutilityFactory.class).to(TimeDistanceAndHeterogeneityBasedTravelDisutilityFactory.class);
				}});
		}

		//CrowdednessObserver observer = new CrowdednessObserver(scenario, controler.getEvents(), new StochasticRule());
		observer = new CrowdednessObserver(scenario, controler.getEvents(), new SimpleRule());
		controler.getEvents().addHandler(observer);
		scoreTracker = new ScoreTracker();
		scoreListener = new ScoreListener(scoreTracker);

		//Scoring
		HeterogeneousCharyparNagelScoringFunctionForAnalysisAndCrowdingFactory customScoringFunctionFactory = new HeterogeneousCharyparNagelScoringFunctionForAnalysisAndCrowdingFactory(controler.getConfig().planCalcScore(), controler.getScenario().getNetwork(), controler.getEvents(), scoreTracker, controler.getScenario(), internalizationOfComfortDisutility);
		customScoringFunctionFactory.setSimulationType(scenario.getConfig().getModule(HeterogeneityConfigGroup.GROUP_NAME).getParams().get("incomeOnTravelCostType"));
        controler.setScoringFunctionFactory(customScoringFunctionFactory);

		//Routing PT
		WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(controler.getScenario().getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
		log.warn("About to init StopStopTimeCalculator...");
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);

		log.warn("About to init TransitRouterEventsHeteroWSFactory...");
		if(simulationType.equals("homo")){
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(TransitRouter.class).toProvider(new TransitRouterEventsWSFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
				}
			});
		}else
		{
			controler.addOverridingModule(new TransitRouterEventsAndHeterogeneityBasedWSModule(waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
		}

		//Sun's Dwell Time model
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new QSimFactory().createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});

		ScoreTracker scoreTracker  = new ScoreTracker();
		// Kaddoura's externalities
	    // controler.addControlerListener(new InternalizationPtControlerListener( (ScenarioImpl) controler.getScenario(), scoreTracker));

		// Additional analysis
		AnalysisCrowdingControlerListener analysisControlerListener = new AnalysisCrowdingControlerListener((MutableScenario) controler.getScenario());
		controler.addControlerListener(analysisControlerListener);
		controler.addControlerListener(new DisaggregatedHeterogeneousCrowdingScoreAnalyzer((MutableScenario) controler.getScenario(), analysisControlerListener.getTripAnalysisHandler()));


		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();

		//Logger root = Logger.getRootLogger();
		//root.setLevel(Level.ALL);
	}

	private static Scenario initScenario() {

		Config config = ConfigUtils.loadConfig(input+"config.xml", new HeterogeneityConfigGroup(), new RoadPricingConfigGroup());

		config.network().setInputFile(input+"network.xml");
		boolean isPopulationZipped = new File(input+"population.xml.gz").isFile();
		if(isPopulationZipped){
			config.plans().setInputFile(input+"population.xml.gz");
		}else{
			config.plans().setInputFile(input+"population.xml");
		}

		boolean isPersonAttributesZipped = new File(input+"personAttributes.xml.gz").isFile();
		if(isPersonAttributesZipped){
			config.plans().setInputPersonAttributeFile(input+"personAttributes.xml.gz");
		}else{
			config.plans().setInputPersonAttributeFile(input+"personAttributes.xml");
		}

		config.transit().setTransitScheduleFile(input+"transitSchedule.xml");
		config.transit().setVehiclesFile(input + "vehicles.xml");

		if(output!=null){
			config.controler().setOutputDirectory(output);
		}

		//Roadpricing module config
		ConfigUtils.addOrGetModule(config,
		                           RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile(input + "roadpricing.xml");


		Scenario scenario = ScenarioUtils.loadScenario(config);

		return scenario;
	}

	private static class Initializer implements StartupListener {

		@Override
		public void notifyStartup(StartupEvent event) {

			MatsimServices controler = event.getServices();

			// create a plot containing the mean travel times
			Set<String> transportModes = new HashSet<String>();
			transportModes.add(TransportMode.car);
			transportModes.add(TransportMode.pt);
			transportModes.add(TransportMode.walk);
			MeanTravelTimeCalculator mttc = new MeanTravelTimeCalculator(controler.getScenario(), transportModes);
			controler.addControlerListener(mttc);
			controler.getEvents().addHandler(mttc);

		}
	}

	private static class IterationEndsHandler implements IterationEndsListener {

		// To avoid an addition of the externalities along the iterations
		public void notifyIterationEnds(IterationEndsEvent event) {
			if(event.getIteration()<numberOfIterations){
				scoreTracker.getPersonScores().clear();
				scoreTracker.getVehicleExternalities().clear();
				scoreTracker.setTotalCrowdednessUtility(0.0);
				scoreTracker.setTotalCrowdednessExternalityCharges(0.0);
				scoreTracker.setTotalInVehicleTimeDelayExternalityCharges(0.0);
				scoreTracker.setTotalCapacityConstraintsExternalityCharges(0.0);
				scoreTracker.setTotalMoneyPaid(0.0);
			}
		}

	}
}
