package org.matsim.contrib.pseudosimulation;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.contrib.pseudosimulation.mobsim.PSimFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.pt.router.TransitRouter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by pieterfourie on 4/20/15.
 * A class for artemc to where you load the travel time structures with events,
 * and psim repeatedly executes using those fixed times.
 * so produces the plans that are optimal if each agent were the only one to adapt to the system,
 * i.e. if their change did not affect other traffic.
 */
public class ChoiceGenerationControler implements BeforeMobsimListener{

        final WaitTimeStuckCalculator waitTimeCalculator;
        final StopStopTimeCalculator stopStopTimeCalculator;
        final TravelTimeCalculator travelTimeCalculator;
    private final PSimFactory pSimFactory;
    Config config;
        Controler controler;
        Scenario scenario;
public ChoiceGenerationControler(String[] args) {
    config = ConfigUtils.loadConfig(args[0]);
    config.parallelEventHandling().setSynchronizeOnSimSteps(false);
    config.parallelEventHandling().setNumberOfThreads(1);
    config.planCalcScore().setWriteExperiencedPlans(true);
    scenario = ScenarioUtils.loadScenario(config);
    controler = new Controler(scenario);

    waitTimeCalculator = new WaitTimeStuckCalculator(
            controler.getScenario().getPopulation(),
            controler.getScenario().getTransitSchedule(),
            controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(),
            (int) (controler.getConfig().qsim().getEndTime() - controler.getConfig().qsim().getStartTime()));
    stopStopTimeCalculator = new StopStopTimeCalculator(
            controler.getScenario().getTransitSchedule(),
            controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(),
            (int) (controler.getConfig().qsim().getEndTime() - controler.getConfig().qsim().getStartTime()));
    controler.addOverridingModule(new AbstractModule() {
        @Override
        public void install() {
            bind(TransitRouter.class).toProvider(new TransitRouterEventsWSFactory(controler.getScenario(),
                    waitTimeCalculator.getWaitTimes(),
                    stopStopTimeCalculator.getStopStopTimes()));
        }
    });
    //    controler.setScoringFunctionFactory(
//            new CharyparNagelOpenTimesScoringFunctionFactory(controler.getConfig().planCalcScore(),
//                    controler.getScenario()));
    travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), config.travelTimeCalculator());

    EventsManagerImpl eventsManager = new EventsManagerImpl();
    EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
    eventsManager.addHandler(waitTimeCalculator);
    eventsManager.addHandler(stopStopTimeCalculator);
    eventsManager.addHandler(travelTimeCalculator);
    reader.readFile(args[1]);

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
}
    public void run(){

        controler.run();
    }
    public static void main(String[] args) {
        new ChoiceGenerationControler(args).run();
    }


    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        Collection<Plan> plans = new ArrayList<>();
        for(Person person:controler.getScenario().getPopulation().getPersons().values()){
            plans.add(person.getSelectedPlan());
        }
        pSimFactory.setWaitTime(waitTimeCalculator.getWaitTimes());
        pSimFactory.setTravelTime(travelTimeCalculator.getLinkTravelTimes());
        pSimFactory.setStopStopTime(stopStopTimeCalculator.getStopStopTimes());
        pSimFactory.setPlans(plans);
    }
}
