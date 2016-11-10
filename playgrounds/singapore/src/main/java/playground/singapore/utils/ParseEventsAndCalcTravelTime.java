package playground.singapore.utils;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import playground.singapore.ptsim.qnetsimengine.PTQSimFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by fouriep on 24/10/16.
 */
public class ParseEventsAndCalcTravelTime {
    private final TransitRouter transitRouter;
    private final Scenario scenario;
    private final EventsManager eventsManager;


    public ParseEventsAndCalcTravelTime(String configFileName, String eventsFileName) {
        scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig(configFileName));
        eventsManager = EventsUtils.createEventsManager();

        WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(scenario.getPopulation(), scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime() - scenario.getConfig().qsim().getStartTime()));
        eventsManager.addHandler(waitTimeCalculator);

        StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime() - scenario.getConfig().qsim().getStartTime()));
        eventsManager.addHandler(stopStopTimeCalculator);

        final TransitRouterEventsWSFactory factory = new TransitRouterEventsWSFactory(scenario, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes());

        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(stopStopTimeCalculator);
        eventsManager.addHandler(waitTimeCalculator);

        (new MatsimEventsReader(eventsManager)).readFile(eventsFileName);

        transitRouter = factory.get();
    }

    public double getTransitTravelTime(Coord fromCoord, Coord toCoord, double departureTime) {
        List<Leg> legs = transitRouter.calcRoute(new FacilityWrapper(fromCoord), new FacilityWrapper(toCoord), departureTime, null);
        double travelTime = 0;
        for (Leg leg : legs) {
            travelTime += leg.getTravelTime();
        }
        return travelTime;
    }

    class FacilityWrapper implements Facility {
        Coord coord;

        public FacilityWrapper(Coord coord) {
            this.coord = coord;
            NetworkUtils.getNearestLink(scenario.getNetwork(), coord);
        }

        @Override
        public Id getId() {
            return null;
        }

        @Override
        public Coord getCoord() {
            return null;
        }

        @Override
        public Id<Link> getLinkId() {
            return null;
        }

        @Override
        public Map<String, Object> getCustomAttributes() {
            return null;
        }
    }

    public static void main(String[] args) {
        ParseEventsAndCalcTravelTime parseEventsAndCalcTravelTime = new ParseEventsAndCalcTravelTime(args[0], args[1]);
        double transitTravelTime = parseEventsAndCalcTravelTime.getTransitTravelTime(
                CoordUtils.createCoord(360000, 142500),
                CoordUtils.createCoord(390000, 152500),
                24000
        );
        System.out.println(transitTravelTime);
    }
}
