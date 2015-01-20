package playground.pieter.distributed;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.scenario.ScenarioUtils;
import playground.singapore.ptsim.qnetsimengine.PTQSimFactory;
import playground.singapore.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.singapore.transitLocationChoice.TransitLocationChoiceStrategy;
import playground.singapore.transitRouterEventsBased.TransitRouterWSImplFactory;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTime;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculatorSerializable;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTime;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeCalculatorSerializable;

import java.util.HashSet;
import java.util.Set;

public class ControlerReference {
	private final Controler delegate;

	private ControlerReference(String configString) {
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configString,new DestinationChoiceConfigGroup() ));
        Config config = scenario.getConfig();
        this.delegate = new Controler(scenario);
		delegate.setOverwriteFiles(true);
        delegate.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(delegate.getConfig().planCalcScore(), delegate.getScenario()));
//        delegate.addPlanStrategyFactory("TransitLocationChoice", new PlanStrategyFactory() {
//            @Override
//            public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
//                return new TransitLocationChoiceStrategy(scenario);
//            }
//        });

        for (Link link : scenario.getNetwork().getLinks().values()) {
            Set<String> modes = new HashSet<>(link.getAllowedModes());
            modes.add("pt");
            link.setAllowedModes(modes);
        }
        //this is some more magic hacking to get location choice by car to work, by sergioo
        //sergioo creates a car-only network, then associates each activity and facility with a car link.
        Set<String> carMode = new HashSet<>();
        carMode.add("car");
        NetworkImpl justCarNetwork = NetworkImpl.createNetwork();
        new TransportModeNetworkFilter(scenario.getNetwork()).filter(justCarNetwork, carMode);
        for (Person person : scenario.getPopulation().getPersons().values())
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements())
                if (planElement instanceof Activity)
                    ((ActivityImpl) planElement).setLinkId(justCarNetwork.getNearestLinkExactly(((ActivityImpl) planElement).getCoord()).getId());
        for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values())
            ((ActivityFacilityImpl) facility).setLinkId(justCarNetwork.getNearestLinkExactly(facility.getCoord()).getId());

        StopStopTimeCalculatorSerializable stopStopTimes = new StopStopTimeCalculatorSerializable(scenario.getTransitSchedule(),
                config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config
                .qsim().getEndTime() - config.qsim().getStartTime()));

        WaitTimeCalculatorSerializable waitTimes = new WaitTimeCalculatorSerializable(scenario.getTransitSchedule(),
                config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config
                .qsim().getEndTime() - config.qsim().getStartTime()));
        delegate.getEvents().addHandler(waitTimes);
        delegate.getEvents().addHandler(stopStopTimes);
        delegate.setTransitRouterFactory(new TransitRouterWSImplFactory(delegate.getScenario(), waitTimes.getWaitTimes(), stopStopTimes.getStopStopTimes()));
        delegate.setMobsimFactory(new PTQSimFactory());
	}

	public static void main(String args[]) {
		new ControlerReference(args[0]).run();
	}

	private void run() {
		delegate.run();

	}
}
