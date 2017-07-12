package playground.clruch.trb18.analysis.spatial;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import playground.clruch.trb18.analysis.ReferenceReader;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class SpatialHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler {
    final private Map<Id<Person>, Queue<ReferenceReader.ReferenceTrip>> referenceTravelTimes;
    final private Map<Id<Person>, Sample> samples = new HashMap<>();
    final private Map<Id<Person>, Double> departures = new HashMap<>();
    final private Network network;

    public SpatialHandler(Network network, Map<Id<Person>, Queue<ReferenceReader.ReferenceTrip>> referenceTravelTimes) {
        this.referenceTravelTimes = referenceTravelTimes;
        this.network = network;
    }

    private Sample getSample(Id<Person> personId) {
        if (!samples.containsKey(personId)) {
            samples.put(personId, new Sample());
        }

        return samples.get(personId);
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        Double departureTime = departures.remove(event.getPersonId());
        Queue<ReferenceReader.ReferenceTrip> trip = referenceTravelTimes.get(event.getPersonId());

        if (trip != null) {
            if (departureTime != null) {
                double travelTime = event.getTime() - departureTime;
                ReferenceReader.ReferenceTrip originalTrip = trip.poll();

                if (originalTrip != null) {
                    Sample sample = getSample(event.getPersonId());
                    sample.totalTravelTimeDelay += travelTime - originalTrip.travelTime;
                }
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals("av")) {
            departures.put(event.getPersonId(), event.getTime());

            Sample sample = getSample(event.getPersonId());
            sample.numberOfTrips++;
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Double departureTime = departures.get(event.getPersonId());

        if (departureTime != null) {
            Sample sample = getSample(event.getPersonId());
            sample.totalWaitingTime += event.getTime() - departureTime;
        }
    }

    @Override
    public void reset(int iteration) {

    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (event.getActType().startsWith("home")) {
            Sample sample = getSample(event.getPersonId());
            sample.homeCoord = network.getLinks().get(event.getLinkId()).getCoord();
        }
    }

    public void write(File outputPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        String[] columns = {
                "person_id",
                "home_x",
                "home_y",
                "totalTravelTimeDelay",
                "totalWaitingTime",
                "numberOfTrips"
        };

        writer.write(String.join("\t", columns) + "\n");
        writer.flush();

        for (Map.Entry<Id<Person>, Sample> entry : samples.entrySet()) {
            if (entry.getValue().homeCoord == null) continue;

            String[] row = {
                    entry.getKey().toString(),
                    String.valueOf(entry.getValue().homeCoord.getX()),
                    String.valueOf(entry.getValue().homeCoord.getY()),
                    String.valueOf(entry.getValue().totalTravelTimeDelay),
                    String.valueOf(entry.getValue().totalWaitingTime),
                    String.valueOf(entry.getValue().numberOfTrips)
            };

            writer.write(String.join("\t", row) + "\n");
            writer.flush();
        }

        writer.flush();
        writer.close();
    }
}
