package org.matsim.contrib.emissions.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkUtils;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestUtils {

    public static void writeWarmEventsToFile(Path eventsFile, Network network, Pollutant pollutant, double pollutionPerEvent, int fromTime, int toTime) {

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EventWriter writer = new EventWriterXML(eventsFile.toString());
        eventsManager.addHandler(writer);
        eventsManager.initProcessing();

        network.getLinks().values().forEach(link -> {
            for (int i = fromTime; i <= toTime; i++) {
                eventsManager.processEvent(createWarmEmissionEvent(i, link, pollutant, pollutionPerEvent));
            }
        });
        eventsManager.finishProcessing();
        writer.closeFile();
    }

    public static WarmEmissionEvent createWarmEmissionEvent(double time, Link link, Pollutant pollutant, double pollutionPerEvent) {
        Map<Pollutant, Double> emissions = new HashMap<>();
        emissions.put(pollutant, pollutionPerEvent);
        return new WarmEmissionEvent(time, link.getId(), Id.createVehicleId(UUID.randomUUID().toString()), emissions);
    }

    public static void writeColdEventsToFile(Path eventsFile, Network network, Pollutant pollutant, double pollutionPerEvent, int fromTime, int toTime) {

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EventWriter writer = new EventWriterXML(eventsFile.toString());
        eventsManager.addHandler(writer);
        eventsManager.initProcessing();

        network.getLinks().values().forEach(link -> {
            for (int i = fromTime; i <= toTime; i++) {
                eventsManager.processEvent(createColdEmissionEvent(i, link, pollutant, pollutionPerEvent));
            }
        });
        eventsManager.finishProcessing();
        writer.closeFile();
    }

    public static ColdEmissionEvent createColdEmissionEvent(double time, Link link, Pollutant pollutant, double pollutionPerEvent) {
        Map<Pollutant, Double> emissions = new HashMap<>();
        emissions.put(pollutant, pollutionPerEvent);
        return new ColdEmissionEvent(time, link.getId(), Id.createVehicleId(UUID.randomUUID().toString()), emissions);
    }

    public static Network createRandomNetwork(int numberOfLinks, double maxX, double maxY) {

        Network network = NetworkUtils.createNetwork(new NetworkConfigGroup());

        for (long i = 0; i < numberOfLinks; i++) {

            Link link = createRandomLink(network.getFactory(), maxX, maxY);
            network.addNode(link.getFromNode());
            network.addNode(link.getToNode());
            network.addLink(link);
        }
        return network;
    }

    private static Link createRandomLink(NetworkFactory factory, double maxX, double maxY) {
        Node fromNode = createRandomNode(factory, maxX, maxY);
        Node toNode = createRandomNode(factory, maxX, maxY);
        return factory.createLink(Id.createLinkId(UUID.randomUUID().toString()), fromNode, toNode);
    }

    private static Node createRandomNode(NetworkFactory factory, double maxX, double maxY) {
        Coord coord = new Coord(getRandomValue(maxX), getRandomValue(maxY));
        return factory.createNode(Id.createNodeId(UUID.randomUUID().toString()), coord);
    }

    private static double getRandomValue(double upperBounds) {
        return Math.random() * upperBounds;
    }

}
