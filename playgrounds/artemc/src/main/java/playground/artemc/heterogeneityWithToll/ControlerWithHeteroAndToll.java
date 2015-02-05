package playground.artemc.heterogeneityWithToll;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import playground.artemc.analysis.AnalysisControlerListener;
import playground.artemc.annealing.SimpleAnnealer;
import playground.artemc.heterogeneity.HeterogeneityConfigGroup;
import playground.artemc.heterogeneity.IncomeHeterogeneityWithoutTravelDisutilityModule;
import playground.artemc.heterogeneity.TravelDisutilityIncomeHeterogeneityProviderWrapper;
import playground.artemc.pricing.ControlerDefaultsWithRoadPricingWithoutTravelDisutilityModule;
import playground.artemc.pricing.UpdateSocialCostPricingSchemeModule;
import playground.artemc.scoring.DisaggregatedHeterogeneousScoreAnalyzer;
import playground.artemc.scoring.HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory;
import playground.artemc.socialCost.MeanTravelTimeCalculator;
import playground.artemc.transitRouter.TransitRouterEventsHeteroWSModule;
import playground.artemc.transitRouter.stopStopTimes.StopStopTimeCalculator;
import playground.artemc.transitRouter.waitTimes.WaitTimeStuckCalculator;

import java.io.File;
import java.util.HashSet;
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

		ControlerWithHeteroAndToll runner = new ControlerWithHeteroAndToll();
		runner.run();
	}



	private void run() {

		Scenario scenario = initScenario();
		//System.setProperty("matsim.preferLocalDtds", "true");



		Controler controler = new Controler(scenario);

		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);

		log.info("Adding Simple Annealer...");
		controler.addControlerListener(new SimpleAnnealer());

//		controler.setModules(new IncomeHeterogeneityModule(input));

		if(roadpricing==true) {
			log.info("First-best roadpricing enabled!");
			controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityWithoutTravelDisutilityModule(), new ControlerDefaultsWithRoadPricingWithoutTravelDisutilityModule(), new UpdateSocialCostPricingSchemeModule());
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					bindToProvider(TravelDisutilityFactory.class, TravelDisutilityTollAndIncomeHeterogeneityProviderWrapper.TravelDisutilityWithPricingAndHeterogeneityProvider.class);
				}});
		}else{
			log.info("No roadpricing!");
			controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityWithoutTravelDisutilityModule());
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					bindToProvider(TravelDisutilityFactory.class, TravelDisutilityIncomeHeterogeneityProviderWrapper.TravelDisutilityIncludingIncomeHeterogeneityFactoryProvider.class);
				}});
		}

		log.info("Simulation type: "+simulationType);
		if(simulationType.equals("hetero")|| simulationType.equals("heteroAlpha") || simulationType.equals("heteroGamma") || simulationType.equals("heteroGammaProp") || simulationType.equals("heteroAlphaProp")) {
			log.info("Heterogeneityfactor: " + heterogeneityFactor);
		}else if(!simulationType.equals("homo")){
			throw new RuntimeException("Unknown income heterogeneity type");
		}


		//Routing PT
        WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(controler.getScenario().getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
		log.warn("About to init StopStopTimeCalculator...");
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);

		log.warn("About to init TransitRouterEventsHeteroWSFactory...");
		controler.addOverridingModule(new TransitRouterEventsHeteroWSModule(waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));

		//Scoring
        controler.setScoringFunctionFactory(new HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(controler.getConfig().planCalcScore(), controler.getScenario().getNetwork()));
		controler.setOverwriteFiles(true);
		
		// Additional analysis
		AnalysisControlerListener analysisControlerListener = new AnalysisControlerListener((ScenarioImpl) controler.getScenario());
		controler.addControlerListener(analysisControlerListener);
		controler.addControlerListener(new DisaggregatedHeterogeneousScoreAnalyzer((ScenarioImpl) controler.getScenario(),analysisControlerListener.getTripAnalysisHandler()));
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

		config.transit().setTransitScheduleFile(input+"transitSchedule.xml");
		config.transit().setVehiclesFile(input+"vehicles.xml");

		if(output!=null){
			config.controler().setOutputDirectory(output);
		}

		//Roadpricing module config
		ConfigUtils.addOrGetModule(config,
		                           RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile(input+"roadpricing.xml");

		//Heterogeneity module config
		Double newIncomeLambda = heterogeneityFactor * Double.parseDouble(ConfigUtils.addOrGetModule(config, HeterogeneityConfigGroup.GROUP_NAME, HeterogeneityConfigGroup.class).getLambdaIncomeTravelcost());

		ConfigUtils.addOrGetModule(config,HeterogeneityConfigGroup.GROUP_NAME,HeterogeneityConfigGroup.class).setIncomeFile(input+ConfigUtils.addOrGetModule(config,HeterogeneityConfigGroup.GROUP_NAME,HeterogeneityConfigGroup.class).getIncomeFile());
		ConfigUtils.addOrGetModule(config,HeterogeneityConfigGroup.GROUP_NAME,HeterogeneityConfigGroup.class).setLambdaIncomeTravelcost(Double.toString(newIncomeLambda));
		ConfigUtils.addOrGetModule(config,HeterogeneityConfigGroup.GROUP_NAME,HeterogeneityConfigGroup.class).setIncomeOnTravelCostType(simulationType);


		//config.controler().setLastIteration(10);
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
