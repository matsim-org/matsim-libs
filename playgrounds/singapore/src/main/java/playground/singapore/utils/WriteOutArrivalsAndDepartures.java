package playground.singapore.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.Departure;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fouriep on 24/10/16.
 */
public class WriteOutArrivalsAndDepartures {
    private final Scenario scenario;
    private final EventsManager eventsManager;
    private final BufferedWriter output;
    private final Map<Id, Id[]> vehiclesIdsToDepartures = new HashMap<>();


    public WriteOutArrivalsAndDepartures(String configFileName, String outputFileName) {
        scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig(configFileName));
        eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(new RecordDepartureDetails());
        eventsManager.addHandler(new Arrival());
        eventsManager.addHandler(new Departure());

        output = IOUtils.getBufferedWriter(outputFileName);
    }

    public void run(String eventsFileName) {
        try {
            output.write(String.format("departureId,lineId,routeId,stopId,eventType,time\n"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        (new MatsimEventsReader(eventsManager)).readFile(eventsFileName);
        try {
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        WriteOutArrivalsAndDepartures parseEventsAndCalcTravelTime = new WriteOutArrivalsAndDepartures(args[0], args[2]);
        parseEventsAndCalcTravelTime.run(args[1]);
    }

    private class RecordDepartureDetails implements TransitDriverStartsEventHandler {

        @Override
        public void handleEvent(TransitDriverStartsEvent event) {
            vehiclesIdsToDepartures.put(event.getVehicleId(), new Id[]{event.getDepartureId(), event.getTransitLineId(), event.getTransitRouteId()});
        }

        @Override
        public void reset(int iteration) {

        }
    }

    private class Arrival implements VehicleArrivesAtFacilityEventHandler {

        @Override
        public void handleEvent(VehicleArrivesAtFacilityEvent event) {
            String departureId = vehiclesIdsToDepartures.get(event.getVehicleId())[0].toString();
            String lineId = vehiclesIdsToDepartures.get(event.getVehicleId())[1].toString();
            String routeId = vehiclesIdsToDepartures.get(event.getVehicleId())[2].toString();
            try {
                output.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"arrival\",%d\n", departureId, lineId, routeId, event.getFacilityId().toString(), (int)event.getTime()
                ));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        }

        @Override
        public void reset(int iteration) {

        }
    }

    private class Departure implements VehicleDepartsAtFacilityEventHandler {
        @Override
        public void handleEvent(VehicleDepartsAtFacilityEvent event) {
            String departureId = vehiclesIdsToDepartures.get(event.getVehicleId())[0].toString();
            String lineId = vehiclesIdsToDepartures.get(event.getVehicleId())[1].toString();
            String routeId = vehiclesIdsToDepartures.get(event.getVehicleId())[2].toString();
            try {
                output.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"departure\",%d\n", departureId, lineId, routeId, event.getFacilityId().toString(), (int)event.getTime()
                ));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        }

        @Override
        public void reset(int iteration) {

        }
    }
}
