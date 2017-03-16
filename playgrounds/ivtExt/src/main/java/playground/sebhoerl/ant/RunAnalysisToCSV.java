package playground.sebhoerl.ant;

import com.sun.javafx.binding.StringFormatter;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;
import playground.sebhoerl.ant.handlers.*;
import playground.sebhoerl.av_paper.BinCalculator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RunAnalysisToCSV {
    public static void main(String[] args) throws IOException {
        double START_TIME = 0.0;
        double END_TIME = 30.0 * 3600.0;
        double INTERVAL = 300.0;
        String RELEVANT_OPERATOR = "*";

        String NETWORK_PATH = args[0];
        String EVENTS_PATH = args[1];
        String OUTPUT_PATH = args[2];

        if (args.length > 3) {
            RELEVANT_OPERATOR = args[3];
        }

        if (args.length > 4) {
            START_TIME = Double.parseDouble(args[4]);
            END_TIME = Double.parseDouble(args[5]);
            INTERVAL = Double.parseDouble(args[6]);
        }

        if (RELEVANT_OPERATOR.equals("*")) {
            RELEVANT_OPERATOR = null;
        }

        BinCalculator binCalculator = BinCalculator.createByInterval(START_TIME, END_TIME, INTERVAL);

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(NETWORK_PATH);

        EventsManager events = EventsUtils.createEventsManager();
        MatsimEventsReader reader = new MatsimEventsReader(events);

        DataFrame dataFrame = new DataFrame(binCalculator, RELEVANT_OPERATOR);
        events.addHandler(new CountsHandler(dataFrame));
        events.addHandler(new DistanceHandler(dataFrame, network));
        events.addHandler(new IdleHandler(dataFrame));
        events.addHandler(new OccupancyHandler(dataFrame));
        events.addHandler(new TimeHandler(dataFrame));
        events.addHandler(new LegChainHandler(dataFrame));

        reader.readFile(EVENTS_PATH);
        events.resetHandlers(0);

        new File(OUTPUT_PATH).mkdirs();

        String operatorPrefix = RELEVANT_OPERATOR == null ? "" : RELEVANT_OPERATOR + "_";

        writeCountsByModeAndBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "departures.csv"), dataFrame, dataFrame.departureCount);
        writeCountsByModeAndBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "arrivals.csv"), dataFrame, dataFrame.arrivalCount);
        writeCountsByModeAndBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "travelling.csv"), dataFrame, dataFrame.travellerCount);

        writeSingleCountByBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "waiting_customers.csv"), "WAITING", dataFrame, dataFrame.waitingCount);
        writeSingleCountByBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "idle_avs.csv"), "IDLE", dataFrame, dataFrame.idleAVs);

        writeTimesByBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "av_waiting_times.csv"), dataFrame, dataFrame.waitingTimes);
        writeTimesByBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "av_travel_times.csv"), dataFrame, dataFrame.travelTimes);

        writeDistances(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "av_distances.txt"), dataFrame);
        writeInfo(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "info.txt"), dataFrame);

        writeOccupancy(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "occupancy.csv"), dataFrame);

        writeLegChains(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "chains.csv"), dataFrame);
    }

    private static <T extends Number> void writeCountsByModeAndBin(BufferedWriter writer, DataFrame dataFrame, Map<String, List<T>> data) throws IOException {
        List<String> elements = new LinkedList<>();

        elements.add("BIN");
        elements.addAll(dataFrame.modes);
        writer.write(String.join(";", elements) + "\n");
        elements.clear();

        for (int i = 0; i < dataFrame.binCalculator.getBins(); i++) {
            elements.add(String.valueOf(i));

            for (String mode : dataFrame.modes) {
                elements.add(data.get(mode).get(i).toString());
            }

            writer.write(String.join(";", elements) + "\n");
            elements.clear();
        }

        writer.close();
    }

    private static <T extends Number> void writeSingleCountByBin(BufferedWriter writer, String topic, DataFrame dataFrame, List<T> data) throws IOException {
        List<String> elements = new LinkedList<>();

        elements.add("BIN");
        elements.add(topic);
        writer.write(String.join(";", elements) + "\n");
        elements.clear();

        for (int i = 0; i < dataFrame.binCalculator.getBins(); i++) {
            elements.add(String.valueOf(i));
            elements.add(data.get(i).toString());

            writer.write(String.join(";", elements) + "\n");
            elements.clear();
        }

        writer.close();
    }

    private static <T extends Number> void writeTimesByBin(BufferedWriter writer, DataFrame dataFrame, List<List<T>> data) throws IOException {
        List<String> elements = new LinkedList<>();

        elements.add("BIN");
        elements.add("TIMES...");
        writer.write(String.join(";", elements) + "\n");
        elements.clear();

        for (int i = 0; i < dataFrame.binCalculator.getBins(); i++) {
            elements.add(String.valueOf(i));

            for (int j = 0; j < data.get(i).size(); j++) {
                elements.add(data.get(i).get(j).toString());
            }

            writer.write(String.join(";", elements) + "\n");
            elements.clear();
        }

        writer.close();
    }

    private static <T extends Number> void writeDistances(BufferedWriter writer, DataFrame dataFrame) throws IOException {
        List<String> elements = new LinkedList<>();

        for (Double distance : dataFrame.avDistances) {
            elements.add(distance.toString());
        }

        writer.write(String.join(";", elements));
        writer.close();
    }

    private static void writeOccupancy(BufferedWriter writer, DataFrame dataFrame) throws IOException {
        List<String> elements = new LinkedList<>();

        elements.add("BIN");
        for (int pax = 0; pax < 5; pax++) {
            elements.add(String.valueOf(pax) + "PAX");
        }
        writer.write(String.join(";", elements) + "\n");
        elements.clear();

        for (int i = 0; i < dataFrame.binCalculator.getBins(); i++) {
            elements.add(String.valueOf(i));

            for (int pax = 0; pax < 5; pax++) {
                elements.add(dataFrame.occupancy.get(pax).get(i).toString());
            }

            writer.write(String.join(";", elements) + "\n");
            elements.clear();
        }
        writer.close();

    }

    private static void writeInfo(BufferedWriter writer, DataFrame dataFrame) throws IOException {
        writer.write(String.format("vehicleDistance = %f\n", dataFrame.vehicleDistance));
        writer.write(String.format("passengerDistance = %f\n", dataFrame.passengerDistance));
        writer.write(String.format("avVehicleDistance = %f\n", dataFrame.avVehicleDistance));
        writer.write(String.format("avPassengerDistance = %f\n", dataFrame.avPassengerDistance));
        writer.write(String.format("avEmptyRideDistance = %f\n", dataFrame.avEmptyRideDistance));
        writer.close();
    }

    private static void writeLegChains(BufferedWriter writer, DataFrame dataFrame) throws IOException {
        List<String> chainKeys = new LinkedList<>();
        chainKeys.addAll(dataFrame.chainCounts.keySet());
        Collections.sort(chainKeys);

        for (String chain : chainKeys) {
            writer.write(String.format("%s;%d\n", chain, dataFrame.chainCounts.get(chain)));
        }

        writer.close();
    }
}
