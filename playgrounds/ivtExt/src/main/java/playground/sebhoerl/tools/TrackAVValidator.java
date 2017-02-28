package playground.sebhoerl.tools;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

public class TrackAVValidator {
    static class TrackHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
        final private Network network;

        final private Map<Id<Person>, Id<Link>> persons = new HashMap<>();
        final private Map<Id<Vehicle>, Id<Person>> v2p = new HashMap<>();

        public TrackHandler(Network network) {
            this.network = network;
        }

        private boolean isConnecting(Id<Link> first, Id<Link> second) {
            if (first.equals(second)) return true;
            Link firstLink = network.getLinks().get(first);
            return firstLink.getToNode().getOutLinks().containsKey(second);
        }

        private boolean isRelevantPersonId(Id<Person> person) {
            return person.toString().startsWith("av");
        }

        private boolean isRelevantVehicleId(Id<Vehicle> veh) {
            return veh.toString().startsWith("av");
        }

        private void checkAndUpdate(Id<Person> person, Id<Link> newLink) {
            Id<Link> oldLink = persons.get(person);

            if (oldLink != null) {
                if (!isConnecting(oldLink, newLink)) {
                    throw new IllegalStateException();
                }
            }

            persons.put(person, newLink);
        }

        private void handlePersonChange(Id<Person> person, Id<Link> link) {
            if (isRelevantPersonId(person)) {
                checkAndUpdate(person, link);
            }
        }

        private void handleVehicleChange(Id<Vehicle> vehicleId, Id<Link> link) {
            if (isRelevantVehicleId(vehicleId) && v2p.containsKey(vehicleId)) {
                checkAndUpdate(v2p.get(vehicleId), link);
            }
        }

        @Override
        public void handleEvent(LinkEnterEvent event) {
            handleVehicleChange(event.getVehicleId(), event.getLinkId());
        }

        @Override
        public void handleEvent(ActivityEndEvent event) {
            handlePersonChange(event.getPersonId(), event.getLinkId());
        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            handleVehicleChange(event.getVehicleId(), event.getLinkId());
        }

        @Override
        public void handleEvent(ActivityStartEvent event) {
            handlePersonChange(event.getPersonId(), event.getLinkId());
        }

        @Override
        public void handleEvent(PersonArrivalEvent event) {
            handlePersonChange(event.getPersonId(), event.getLinkId());
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            handlePersonChange(event.getPersonId(), event.getLinkId());
        }

        @Override
        public void handleEvent(VehicleEntersTrafficEvent event) {
            handleVehicleChange(event.getVehicleId(), event.getLinkId());
        }

        @Override
        public void handleEvent(VehicleLeavesTrafficEvent event) {
            handleVehicleChange(event.getVehicleId(), event.getLinkId());
        }

        @Override
        public void reset(int iteration) {
            persons.clear();
            v2p.clear();
        }

        @Override
        public void handleEvent(PersonEntersVehicleEvent event) {
            if (isRelevantPersonId(event.getPersonId()) && isRelevantVehicleId(event.getVehicleId())) {
                v2p.put(event.getVehicleId(), event.getPersonId());
            }
        }

        @Override
        public void handleEvent(PersonLeavesVehicleEvent event) {
            if (isRelevantPersonId(event.getPersonId()) && isRelevantVehicleId(event.getVehicleId())) {
                v2p.remove(event.getVehicleId());
            }
        }
    }

    public void run(String eventsPath, String networkPath) {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkPath);

        EventsManager events = EventsUtils.createEventsManager();

        TrackHandler handler = new TrackHandler(network);
        events.addHandler(handler);

        new MatsimEventsReader(events).readFile(eventsPath);
    }

    static public void main(String[] args) {
        TrackAVValidator validator = new TrackAVValidator();

        String events = "/home/sebastian/ant/analysis/data/multi_400/events_2000_sub.xml.gz";
        String network = "/home/sebastian/ant/assets/network.xml";

        validator.run(events, network);
    }
}
