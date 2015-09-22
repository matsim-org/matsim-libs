package playground.artemc.heterogeneity;


import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSModule;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import playground.artemc.analysis.AnalysisControlerListener;
import playground.artemc.annealing.SimpleAnnealer;
import playground.artemc.dwellTimeModel.QSimFactory;
import playground.artemc.heterogeneity.eventsBasedPTRouter.TransitRouterEventsAndHeterogeneityBasedWSModule;
import playground.artemc.heterogeneity.routing.TimeDistanceAndHeterogeneityBasedTravelDisutilityFactory;
import playground.artemc.heterogeneity.routing.TimeDistanceTollAndHeterogeneityBasedTravelDisutilityProviderWrapper;
import playground.artemc.heterogeneity.scoring.DisaggregatedHeterogeneousScoreAnalyzer;
import playground.artemc.heterogeneity.scoring.HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory;
import playground.artemc.pricing.LinkOccupancyAnalyzerModule;
import playground.artemc.pricing.RoadPricingWithoutTravelDisutilityModule;
import playground.artemc.pricing.UpdateSocialCostPricingSchemeWithSpillOverModule;
import playground.artemc.socialCost.MeanTravelTimeCalculator;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ControlerWithHeteroAndToll {

	private static final Logger log = Logger.getLogger(ControlerWithHeteroAndToll.class);

	private static String input;
	private static String output;
	private static String simulationType = "homo";
	private static Double heterogeneityFactor = 1.0;
	private static boolean roadpricing = false;

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

		log.info("Simulation type: " + simulationType);
		if(simulationType.equals("hetero")|| simulationType.equals("heteroAlpha") || simulationType.equals("heteroGamma") || simulationType.equals("heteroAlphaRatio") ||simulationType.equals("heteroPropSymmetric") || simulationType.equals("heteroAlphaUniform") || simulationType.equals("heteroGammaUniform")){
			log.info("Heterogeneityfactor: " + heterogeneityFactor);
		}else if(!simulationType.equals("homo")){
			throw new RuntimeException("Unknown income heterogeneity type");
		}

		ControlerWithHeteroAndToll runner = new ControlerWithHeteroAndToll();
		runner.run();
	}

	private void run() {

		Scenario scenario = initScenario();
		//System.setProperty("matsim.preferLocalDtds", "true");

		final Controler controler = new Controler(scenario);
		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);

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
			controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityModule(), new RoadPricingWithoutTravelDisutilityModule(), new LinkOccupancyAnalyzerModule(), new UpdateSocialCostPricingSchemeWithSpillOverModule());
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

		//Scoring
		HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory customScoringFunctionFactory = new HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(controler.getConfig().planCalcScore(), controler.getScenario().getNetwork());
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
			controler.addOverridingModule(new TransitRouterEventsWSModule(waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
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

		// Additional analysis
		AnalysisControlerListener analysisControlerListener = new AnalysisControlerListener((ScenarioImpl) controler.getScenario());
		controler.addControlerListener(analysisControlerListener);
		controler.addControlerListener(new DisaggregatedHeterogeneousScoreAnalyzer((ScenarioImpl) controler.getScenario(), analysisControlerListener.getTripAnalysisHandler()));


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

			Controler controler = event.getControler();

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
}
