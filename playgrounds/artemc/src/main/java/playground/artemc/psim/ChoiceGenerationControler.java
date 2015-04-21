package playground.artemc.psim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.contrib.pseudosimulation.mobsim.PSimFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

/**
 * Created by fouriep on 4/20/15.
 */
public class ChoiceGenerationControler {

    public static void main(String[] args) {
        final WaitTimeStuckCalculator waitTimeCalculator;
        final StopStopTimeCalculator stopStopTimeCalculator;
        final TravelTimeCalculator travelTimeCalculator;
        Config config;
        Controler controler;
        Scenario scenario;

        config = ConfigUtils.loadConfig(args[0]);
        scenario = ScenarioUtils.loadScenario(config);
        controler = new Controler(scenario);
        config.planCalcScore().setWriteExperiencedPlans(true);
        config.parallelEventHandling().setSynchronizeOnSimSteps(false);

        waitTimeCalculator = new WaitTimeStuckCalculator(
                controler.getScenario().getPopulation(),
                controler.getScenario().getTransitSchedule(),
                controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(),
                (int) (controler.getConfig().qsim().getEndTime() - controler.getConfig().qsim().getStartTime()));
        stopStopTimeCalculator = new StopStopTimeCalculator(
                controler.getScenario().getTransitSchedule(),
                controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(),
                (int) (controler.getConfig().qsim().getEndTime() - controler.getConfig().qsim().getStartTime()));
        controler.setTransitRouterFactory(
                new TransitRouterEventsWSFactory(controler.getScenario(),
                        waitTimeCalculator.getWaitTimes(),
                        stopStopTimeCalculator.getStopStopTimes()));
        controler.setScoringFunctionFactory(
                new CharyparNagelOpenTimesScoringFunctionFactory(controler.getConfig().planCalcScore(),
                        controler.getScenario()));
        travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), config.travelTimeCalculator());

        EventsManagerImpl eventsManager = new EventsManagerImpl();
        EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
        eventsManager.addHandler(waitTimeCalculator);
        eventsManager.addHandler(stopStopTimeCalculator);
        eventsManager.addHandler(travelTimeCalculator);
        reader.parse(args[1]);

        PSimFactory pSimFactory = new PSimFactory();
        controler.setMobsimFactory(pSimFactory);
        pSimFactory.setWaitTime(waitTimeCalculator.getWaitTimes());
        pSimFactory.setTravelTime(travelTimeCalculator.getLinkTravelTimes());
        pSimFactory.setStopStopTime(stopStopTimeCalculator.getStopStopTimes());
    }


}
