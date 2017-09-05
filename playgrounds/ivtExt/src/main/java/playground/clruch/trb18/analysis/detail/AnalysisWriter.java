package playground.clruch.trb18.analysis.detail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;

import playground.clruch.trb18.analysis.detail.events.LegCollector;
import playground.clruch.trb18.analysis.detail.events.PairCollector;
import playground.clruch.trb18.analysis.detail.events.TripCollector;

public class AnalysisWriter implements PairCollector.Handler {
    final private BufferedWriter writer;
    final private Network network;

    final private List<String> mainModes = Arrays.asList("car", "pt", "walk", "bike", "transit_walk", "av");

    final private String SEPARATOR = "\t";

    public AnalysisWriter(File outputPath, Network network) throws FileNotFoundException {
        this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));
        this.network = network;

        List<String> referenceHeader = buildHeaderRow("REFERENCE");
        List<String> scenarioHeader = buildHeaderRow("SCENARIO");
        writeRow(referenceHeader, scenarioHeader);
    }

    private void writeRow(List<String> firstElements, List<String> secondElements) {
        List<String> row = new LinkedList<>();
        row.addAll(firstElements);
        row.addAll(secondElements);
        writeRow(row);
    }

    private void writeRow(List<String> elements) {
        try {
            writer.write(String.join(SEPARATOR, elements) + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public void handlePair(TripCollector.Trip referenceTrip, TripCollector.Trip scenarioTrip) {
        List<String> referenceData = buildDataRow(referenceTrip);
        List<String> scenarioData = buildDataRow(scenarioTrip);
        writeRow(referenceData, scenarioData);
    }

    private List<String> buildHeaderRow(String suffix) {
        return Arrays.asList(
                "AGENT_ID_" + suffix,
                "ORIGIN_X_" + suffix,
                "ORIGIN_Y_" + suffix,
                "DESTINATION_X_" + suffix,
                "DESTINATION_Y_" + suffix,
                "ORIGIN_TYPE_" + suffix,
                "DESTINATION_TYPE_" + suffix,
                "DISTANCE_" + suffix,
                "TRAVEL_TIME_" + suffix,
                "WAITING_TIME_" + suffix,
                "MODE_" + suffix,
                "START_TIME_" + suffix
        );
    }

    private List<String> buildDataRow(TripCollector.Trip trip) {
        Coord originCoord = network.getLinks().get(trip.originActivity.linkId).getCoord();
        Coord destinationCoord = network.getLinks().get(trip.destinationActivity.linkId).getCoord();

        return Arrays.asList(
                trip.agentId.toString(),
                String.valueOf(originCoord.getX()), String.valueOf(originCoord.getY()),
                String.valueOf(destinationCoord.getX()), String.valueOf(destinationCoord.getY()),
                trip.originActivity.type, trip.destinationActivity.type,
                String.valueOf(computeDistance(trip)),
                String.valueOf(computeTravelTime(trip)),
                String.valueOf(computeWaitingTime(trip)),
                computeMode(trip),
                String.valueOf(trip.originActivity.endTime)
        );
    }

    private double computeDistance(TripCollector.Trip trip) {
        return trip.legs.stream().mapToDouble(l -> l.distance).sum();
    }

    private double computeTravelTime(TripCollector.Trip trip) {
        return trip.destinationActivity.startTime - trip.originActivity.endTime;
    }

    private double computeWaitingTime(TripCollector.Trip trip) {
        return trip.legs.stream().filter(l -> !Double.isNaN(l.waitingTime)).mapToDouble(l -> l.waitingTime).sum();
    }

    private String computeMode(TripCollector.Trip trip) {
        Optional<LegCollector.Leg> any = trip.legs.stream().filter(l -> mainModes.contains(l.mode)).findAny();

        if (!any.isPresent()) {
            System.out.println(trip.agentId);
            return "NULL";
        }

        if (any.get().mode.equals("transit_walk")) {
            return "pt";
        }

        return any.get().mode;
    }
}
