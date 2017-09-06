package playground.clruch.trb18.analysis.detail.events;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.vehicles.Vehicle;

import com.google.common.util.concurrent.AtomicDouble;

import playground.clruch.trb18.analysis.detail.Utils;

public class LegCollector implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TeleportationArrivalEventHandler, LinkEnterEventHandler {
    final private Network network;
    final private Collection<Handler> handlers = new HashSet<>();

    final private Map<Id<Person>, Double> departureTimes = new HashMap<>();
    final private Map<Id<Person>, Double> enterVehicleTimes = new HashMap<>();

    final private Map<Id<Vehicle>, Id<Person>> vehicle2person = new HashMap<>();
    final private Map<Id<Person>, AtomicDouble> distances = new HashMap<>();

    interface Handler {
        void handleLeg(Leg leg);
    }

    public void addHandler(Handler handler) {
        this.handlers.add(handler);
    }

    public class Leg {
        public Id<Person> agentId;

        public String mode;
        public double distance = Double.NaN;
        public double travelTime = Double.NaN;
        public double waitingTime = Double.NaN;
    }

    public LegCollector(Network network) {
        this.network = network;
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (Utils.isValidAgent(event.getPersonId())) {
            Double departureTime = departureTimes.remove(event.getPersonId());
            Double enterVehicleTime = enterVehicleTimes.remove(event.getPersonId());
            AtomicDouble distance = distances.remove(event.getPersonId());

            if (departureTime != null) {
                Leg leg = new Leg();
                leg.agentId = event.getPersonId();
                leg.mode = event.getLegMode();
                leg.distance = distance.doubleValue();
                leg.travelTime = event.getTime() - departureTime;

                if (enterVehicleTime != null) {
                    leg.waitingTime = enterVehicleTime - departureTime;
                }

                for (Handler handler : handlers) {
                    handler.handleLeg(leg);
                }
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (Utils.isValidAgent(event.getPersonId())) {
            departureTimes.put(event.getPersonId(), event.getTime());
            distances.put(event.getPersonId(), new AtomicDouble(0.0));
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (Utils.isValidAgent(event.getPersonId())) {
            enterVehicleTimes.put(event.getPersonId(), event.getTime());
            vehicle2person.put(event.getVehicleId(), event.getPersonId());
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (Utils.isValidAgent(event.getPersonId())) {
            vehicle2person.remove(event.getVehicleId());
        }
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        if (Utils.isValidAgent(event.getPersonId())) {
            distances.get(event.getPersonId()).addAndGet(event.getDistance());
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Person> personId = vehicle2person.get(event.getVehicleId());

        if (personId != null) {
            distances.get(personId).addAndGet(network.getLinks().get(event.getLinkId()).getLength());
        }
    }

    @Override
    public void reset(int iteration) {}
}
