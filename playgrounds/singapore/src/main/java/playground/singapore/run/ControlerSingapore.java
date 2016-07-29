package playground.singapore.run;

import com.google.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSModule;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.singapore.ptsim.qnetsimengine.PTQSimFactory;
import playground.singapore.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.singapore.transitLocationChoice.TransitLocationChoiceStrategy;

import java.util.HashSet;
import java.util.Set;

/*import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;*/

public class ControlerSingapore {

	//private static DestinationChoiceBestResponseContext dcContext;
	
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]/*, new DestinationChoiceConfigGroup()*/);
		final Controler controler = new Controler(ScenarioUtils.loadScenario(config));
        Logger logger = Logger.getLogger("SINGAPORECONTROLER");
        /*logger.warn("Doing the workaround to associate facilities with car links...");
		for(Link link:services.getScenario().getNetwork().getLinks().values()) {
			Set<String> modes = new HashSet<String>(link.getAllowedModes());
			modes.add("pt");
			link.setAllowedModes(modes);
		}
		Set<String> carMode = new HashSet<String>();
		carMode.add("car");
		NetworkImpl justCarNetwork = NetworkImpl.createNetwork();
		new TransportModeNetworkFilter(services.getScenario().getNetwork()).filter(justCarNetwork, carMode);
		for(Person person:services.getScenario().getPopulation().getPersons().values())
			for(PlanElement planElement:person.getSelectedPlan().getPlanElements())
				if(planElement instanceof Activity)
					((ActivityImpl)planElement).setLinkId(justCarNetwork.getNearestLinkExactly(((ActivityImpl)planElement).getCoord()).getId());
		for(ActivityFacility facility:services.getScenario().getActivityFacilities().getFacilities().values())
			((ActivityFacilityImpl)facility).setLinkId(justCarNetwork.getNearestLinkExactly(facility.getCoord()).getId());
		services.getConfig().services().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		// services.addControlerListener(new CalibrationStatsListener(services.getEvents(), new String[]{args[1], args[2]}, 1, "Travel Survey (Benchmark)", "Red_Scheme", new HashSet<Id<Person>>()));*/
        WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(controler.getScenario().getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
        logger.warn("About to init StopStopTimeCalculator...");
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);
        logger.warn("About to init TransitRouterWSImplFactory...");
        controler.addOverridingModule(new TransitRouterEventsWSModule(waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
        //services.setTransitRouterFactory(new TransitRouterEventsWSFactory(services.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
		//services.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(services.getConfig().planCalcScore(), services.getScenario()));
		// comment: I would argue that when you add waitTime/stopTime to the router, you also need to adapt the scoring function accordingly.
		// kai, sep'13
		/*services.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("TransitLocationChoice").toProvider(new javax.inject.Provider<PlanStrategy>() {
					@Override
					public PlanStrategy get() {
						return new TransitLocationChoiceStrategy(services.getScenario());
					}
				});
			}
		});*/
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new PTQSimFactory().createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});
	/*dcContext = new DestinationChoiceBestResponseContext(services.getScenario());
		dcContext.init();
		services.addControlerListener(new DestinationChoiceInitializer(dcContext));
		if (Double.parseDouble(services.getConfig().findParam("locationchoice", "restraintFcnExp")) > 0.0 &&
				Double.parseDouble(services.getConfig().findParam("locationchoice", "restraintFcnFactor")) > 0.0) {
					services.addControlerListener(new FacilitiesLoadCalculator(dcContext.getFacilityPenalties()));
		}*/
		controler.run();
	}

}
