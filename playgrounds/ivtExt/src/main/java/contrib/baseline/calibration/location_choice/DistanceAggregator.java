package contrib.baseline.calibration.location_choice;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.Vehicle;

import java.io.*;
import java.util.*;

public class DistanceAggregator implements ActivityStartEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkLeaveEventHandler, TeleportationArrivalEventHandler {
    final private Map<Id<Person>, Double> personDistances = new HashMap<>();
    final private Map<Id<Vehicle>, Set<Id<Person>>> vehicle2person = new HashMap<>();
    final private Map<String, Set<Double>> activityDistances = new HashMap<>();

    final private Network network;

    static public void main(String args[]) throws IOException {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(args[0]);

        EventsManager eventsManager = EventsUtils.createEventsManager();
        DistanceAggregator aggregator = new DistanceAggregator(network);
        eventsManager.addHandler(aggregator);

        new MatsimEventsReader(eventsManager).readFile(args[1]);

        aggregator.write(args[2]);
    }

    public DistanceAggregator(Network network) {
        this.network = network;
    }

    @Override
    public void reset(int iteration) {
        personDistances.clear();
        vehicle2person.clear();
        activityDistances.clear();
    }

    public Map<String, Double> getMeanDistances() {
        Map<String, Double> result = new HashMap<>();

        activityDistances.forEach((activityType, values) -> result.put(
                activityType,
                new DescriptiveStatistics(values.stream().mapToDouble(Double::doubleValue).toArray()).getMean() / 1000.0
        ));

        return result;
    }

    public Map<String, Double> getMedianDistances() {
        Map<String, Double> result = new HashMap<>();

        activityDistances.forEach((activityType, values) -> result.put(
                activityType,
                new DescriptiveStatistics(values.stream().mapToDouble(Double::doubleValue).toArray()).getPercentile(0.5) / 1000.0
        ));

        return result;
    }

    public void write(String targetPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetPath)));

        writer.write("purpose\tmean_distance\tmedian_distance\n");

        for (String activityType : activityDistances.keySet()) {
            double[] distances = new double[activityDistances.get(activityType).size()];

            Iterator<Double> iterator = activityDistances.get(activityType).iterator();
            for (int i = 0; i < distances.length; i++) distances[i] = iterator.next();

            DescriptiveStatistics stats = new DescriptiveStatistics(distances);
            writer.write(String.format("%s\t%f\t%f\n", activityType, stats.getMean() / 1000.0, stats.getPercentile(0.5) / 100.0));
        }

        writer.flush();
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (isRelevantAgent(event.getPersonId()) && isRelevantActivity(event.getActType())) {
            ensureActivity(event.getActType());
            ensurePerson(event.getPersonId());

            activityDistances.get(event.getActType()).add(personDistances.get(event.getPersonId()));
            personDistances.put(event.getPersonId(), 0.0);
        }
    }

    private boolean isRelevantActivity(String activityType) {
        if (activityType.equals("pt interaction")) return false;
        return true;
    }

    private void ensureActivity(String activityType) {
        if (!activityDistances.containsKey(activityType)) {
            activityDistances.put(activityType, new HashSet<>());
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (vehicle2person.containsKey(event.getVehicleId())) {
            for (Id<Person> personId : vehicle2person.get(event.getVehicleId())) {
                ensurePerson(personId);
                personDistances.put(personId, personDistances.get(personId) + network.getLinks().get(event.getLinkId()).getLength());
            }
        }
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        if (isRelevantAgent(event.getPersonId())) {
            ensurePerson(event.getPersonId());
            personDistances.put(event.getPersonId(), personDistances.get(event.getPersonId()) + event.getDistance());
        }
    }

    private void ensurePerson(Id<Person> personId) {
        if (!personDistances.containsKey(personId)) {
            personDistances.put(personId, 0.0);
        }
    }

    private void ensureVehicle(Id<Vehicle> vehicleId) {
        if (!vehicle2person.containsKey(vehicleId)) {
            vehicle2person.put(vehicleId, new HashSet<>());
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (isRelevantAgent(event.getPersonId())) {
            ensureVehicle(event.getVehicleId());
            vehicle2person.get(event.getVehicleId()).add(event.getPersonId());
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (isRelevantAgent(event.getPersonId())) {
            ensureVehicle(event.getVehicleId());
            vehicle2person.get(event.getVehicleId()).remove(event.getPersonId());
        }
    }

    private boolean isRelevantAgent(Id<Person> personId) {
        if (personId.toString().contains("pt")) return false;
        return true;
    }
}
