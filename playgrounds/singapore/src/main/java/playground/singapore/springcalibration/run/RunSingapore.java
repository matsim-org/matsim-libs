package playground.singapore.springcalibration.run;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSModule;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.singapore.scoring.CharyparNagelOpenTimesActivityScoring;
import playground.singapore.springcalibration.run.roadpricing.SubpopRoadPricingModule;

public class RunSingapore {	
	private final static Logger log = Logger.getLogger(RunSingapore.class);
	
	

	public static void main(String[] args) {
		log.info("Running SingaporeControlerRunner"); 
     
		Config config = ConfigUtils.loadConfig( args[0], new RoadPricingConfigGroup() ) ;

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		final SingaporeConfigGroup singaporeConfigGroup = ConfigUtils.addOrGetModule(
				scenario.getConfig(), SingaporeConfigGroup.GROUP_NAME, SingaporeConfigGroup.class);
		
		CharyparNagelScoringParametersForPerson parameters = new SubpopulationCharyparNagelScoringParameters( controler.getScenario() );
										
		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			
			@Inject Network network;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, network));			
				// this is the Singaporean scorer with Open times:
				sumScoringFunction.addScoringFunction(new CharyparNagelOpenTimesActivityScoring(params, scenario.getActivityFacilities()));
				//sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;	
				
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));				
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));

				return sumScoringFunction;
			}
		}) ;
		
		final SubpopTravelDisutility.Builder builder_schoolbus =  new SubpopTravelDisutility.Builder("schoolbus", parameters);	
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("schoolbus").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("schoolbus").toInstance(builder_schoolbus);
			}
		});
		
		final SubpopTravelDisutility.Builder builder_passenger =  new SubpopTravelDisutility.Builder("passenger", parameters);	
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("passenger").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("passenger").toInstance(builder_passenger);
			}
		});
		
		final SubpopTravelDisutility.Builder builder_other =  new SubpopTravelDisutility.Builder(TransportMode.other, parameters);	
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.other).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.other).toInstance(builder_other);
			}
		});
		
		final SubpopTravelDisutility.Builder builder_freight =  new SubpopTravelDisutility.Builder("freight", parameters);	
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("freight").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("freight").toInstance(builder_freight);
			}
		});
						
		final SubpopTravelDisutility.Builder builder_taxi =  new SubpopTravelDisutility.Builder("taxi", parameters);	
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("taxi").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("taxi").toInstance(builder_taxi);
			}
		});
	
		SubpopRoadPricingModule rpModule = new SubpopRoadPricingModule(scenario, config);
		controler.setModules(rpModule);
										
		controler.addControlerListener(new SingaporeControlerListener());
		
		controler.addControlerListener(new SingaporeIterationEndsListener());
		
		// Singapore transit router: --------------------------------------------------
		WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(
				controler.getScenario().getPopulation(), 
				controler.getScenario().getTransitSchedule(), 
				controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), 
				(int) (controler.getConfig().qsim().getEndTime() - controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
        
		log.info("About to init StopStopTimeCalculator...");
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(
				controler.getScenario().getTransitSchedule(), 
				controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), 
				(int) (controler.getConfig().qsim().getEndTime() - controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);
        log.info("About to init TransitRouterWSImplFactory...");
        
        controler.addOverridingModule(new TransitRouterEventsWSModule(waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
        
        // TODO: also take into account waiting times and stop times in scoring?!
		// -----------------------------------------------------------------------------
		
		
		controler.run();
		log.info("finished SingaporeControlerRunner");
	}
}
