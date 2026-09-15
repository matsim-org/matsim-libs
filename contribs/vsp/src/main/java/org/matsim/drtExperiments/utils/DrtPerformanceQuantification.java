package org.matsim.drtExperiments.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.contrib.drt.extension.preplanned.optimizer.WaitForStopTask;
import org.matsim.contrib.drt.util.DrtEventsReaders;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.matsim.application.ApplicationUtils.globFile;
//TODO clean this script!!!
public class DrtPerformanceQuantification {
    private final VehicleDrivingTimeStatistics vehicleDrivingTimeStatistics = new VehicleDrivingTimeStatistics();
    private final RejectionStatistics rejectionStatistics = new RejectionStatistics();
    private String computationalTimeString = "unknown";
    private String iterationsString = "unknown";
    private String horizonString = "not_applicable";
    private String intervalString = "not_applicable";

    /**
     * Offline post analysis, to be called from the main method of this class.
     */
    private void analyze(String eventsFilePathString) {
        vehicleDrivingTimeStatistics.reset(0);
        rejectionStatistics.reset(0);

        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(vehicleDrivingTimeStatistics);
        eventsManager.addHandler(rejectionStatistics);
        MatsimEventsReader eventsReader = DrtEventsReaders.createEventsReader(eventsManager, WaitForStopTask.TYPE);
        eventsReader.readFile(eventsFilePathString);
    }

    /**
     * Online post analysis, to be attached to the run script
     */
    public void analyze(Path outputDirectory, long computationalTime, String iterations) {
        vehicleDrivingTimeStatistics.reset(0);
        rejectionStatistics.reset(0);

        computationalTimeString = Long.toString(computationalTime);
        iterationsString = iterations;
        Path eventPath = globFile(outputDirectory, "*output_events.*");
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(vehicleDrivingTimeStatistics);
        eventsManager.addHandler(rejectionStatistics);
        MatsimEventsReader eventsReader = DrtEventsReaders.createEventsReader(eventsManager, WaitForStopTask.TYPE);
        eventsReader.readFile(eventPath.toString());
    }

    /**
     * Online post analysis for Rolling Horizon optimizer, to be attached to the run script
     */
    public void analyzeRollingHorizon(Path outputDirectory, long computationalTime, String iterations, String horizon, String interval) {
        vehicleDrivingTimeStatistics.reset(0);
        rejectionStatistics.reset(0);

        computationalTimeString = Long.toString(computationalTime);
        this.iterationsString = iterations;
        this.intervalString = interval;
        this.horizonString = horizon;
        Path eventPath = globFile(outputDirectory, "*output_events.*");
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(vehicleDrivingTimeStatistics);
        eventsManager.addHandler(rejectionStatistics);
        MatsimEventsReader eventsReader = DrtEventsReaders.createEventsReader(eventsManager, WaitForStopTask.TYPE);
        eventsReader.readFile(eventPath.toString());
    }

    public double getTotalDrivingTime() {
        return vehicleDrivingTimeStatistics.getTotalDrivingTime();
    }

    public int getRejections() {
        return rejectionStatistics.getRejectedRequests();
    }

    /**
     * Write the title and result in the output directory
     */
    public void writeResults(Path outputDirectory) throws IOException {
        Path outputStatsPath = Path.of(outputDirectory + "/drt-result-quantification.tsv");
        CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(outputStatsPath.toString()), CSVFormat.TDF);
        List<String> titleRow = Arrays.asList("iterations", "total_driving_time", "rejections", "computational_time");
        tsvWriter.printRecord(titleRow);
        tsvWriter.printRecord(Arrays.asList(iterationsString, Double.toString(getTotalDrivingTime()), Long.toString(getRejections()), computationalTimeString));
        tsvWriter.close();
        System.out.println("No. iterations = " + iterationsString);
        System.out.println("Computational time = " + computationalTimeString);
        System.out.println("Total driving time = " + getTotalDrivingTime());
        System.out.println("Number of rejections = " + getRejections());
    }

    /**
     * Write the title and result in the output directory for Rolling Horizon run
     */
    public void writeResultsRollingHorizon(Path outputDirectory) throws IOException {
        Path outputStatsPath = Path.of(outputDirectory + "/drt-result-quantification.tsv");
        CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(outputStatsPath.toString()), CSVFormat.TDF);
        List<String> titleRow = Arrays.asList("horizon", "interval", "iterations", "total_driving_time", "rejections", "computational_time");
        tsvWriter.printRecord(titleRow);
        tsvWriter.printRecord(Arrays.asList(horizonString, intervalString, iterationsString,
                Double.toString(getTotalDrivingTime()), Long.toString(getRejections()), computationalTimeString));
        tsvWriter.close();
        System.out.println("Horizon = " + horizonString + ", Interval = " + intervalString + ", Iterations = " + iterationsString);
        System.out.println("Computational time = " + computationalTimeString);
        System.out.println("Total driving time = " + getTotalDrivingTime());
        System.out.println("Number of rejections = " + getRejections());
    }

    /**
     * Print title row (for sequential runs)
     */
    public void writeTitle(Path outputDirectory) throws IOException {
        Path outputStatsPath = Path.of(outputDirectory + "/drt-result-quantification.tsv");
        CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(outputStatsPath.toString()), CSVFormat.TDF);
        List<String> titleRow = Arrays.asList("iterations", "total_driving_time", "rejections", "computational_time");
        tsvWriter.printRecord(titleRow);
        tsvWriter.close();
    }

    /**
     * Print title row (for sequential runs, Rolling Horizon)
     */
    public void writeTitleForRollingHorizon(Path outputDirectory) throws IOException {
        Path outputStatsPath = Path.of(outputDirectory + "/drt-result-quantification.tsv");
        CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(outputStatsPath.toString()), CSVFormat.TDF);
        List<String> titleRow = Arrays.asList("horizon", "interval", "iterations", "total_driving_time", "rejections", "computational_time");
        tsvWriter.printRecord(titleRow);
        tsvWriter.close();
    }

    /**
     * Print single entry (for sequential runs)
     */
    public void writeResultEntry(Path outputDirectory) throws IOException {
        Path outputStatsPath = Path.of(outputDirectory + "/drt-result-quantification.tsv");
        CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(outputStatsPath.toString(), true), CSVFormat.TDF);
        tsvWriter.printRecord(Arrays.asList(iterationsString,
                Double.toString(getTotalDrivingTime()), Long.toString(getRejections()), computationalTimeString));
        tsvWriter.close();
        System.out.println("No. iterations = " + iterationsString);
        System.out.println("Computational time = " + computationalTimeString);
        System.out.println("Total driving time = " + getTotalDrivingTime());
        System.out.println("Number of rejections = " + getRejections());
    }

    /**
     * Print single entry (for sequential runs, Rolling Horizon)
     */
    public void writeResultEntryRollingHorizon(Path outputDirectory) throws IOException {
        Path outputStatsPath = Path.of(outputDirectory + "/drt-result-quantification.tsv");
        CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(outputStatsPath.toString(), true), CSVFormat.TDF);
        tsvWriter.printRecord(Arrays.asList(horizonString, intervalString, iterationsString,
                Double.toString(getTotalDrivingTime()), Long.toString(getRejections()), computationalTimeString));
        tsvWriter.close();
        System.out.println("Horizon = " + horizonString + ", Interval = " + intervalString + ", Iterations = " + iterationsString);
        System.out.println("Computational time = " + computationalTimeString);
        System.out.println("Total driving time = " + getTotalDrivingTime());
        System.out.println("Number of rejections = " + getRejections());
    }

    public static void main(String[] args) {
        String eventPath = args[0];
        DrtPerformanceQuantification drtPerformanceQuantification = new DrtPerformanceQuantification();
        drtPerformanceQuantification.analyze(eventPath);
        System.out.println("Total driving time is " + drtPerformanceQuantification.getTotalDrivingTime() + " seconds");
        System.out.println("There are " + drtPerformanceQuantification.getRejections() + " rejected requests");
    }

    /**
     * Read total fleet driving time from the event file
     */
    static class VehicleDrivingTimeStatistics implements VehicleEntersTrafficEventHandler,
            VehicleLeavesTrafficEventHandler {
        private double totalDrivingTime;

        @Override
        public void reset(int iteration) {
            totalDrivingTime = 0;
        }

        @Override
        public void handleEvent(VehicleEntersTrafficEvent vehicleEntersTrafficEvent) {
            double enterTime = vehicleEntersTrafficEvent.getTime();
            totalDrivingTime -= enterTime;
        }

        @Override
        public void handleEvent(VehicleLeavesTrafficEvent vehicleLeavesTrafficEvent) {
            double leavingTime = vehicleLeavesTrafficEvent.getTime();
            totalDrivingTime += leavingTime;
        }

        public double getTotalDrivingTime() {
            return totalDrivingTime;
        }
    }

    static class RejectionStatistics implements PassengerRequestRejectedEventHandler {
        private int rejectedRequests = 0;

        @Override
        public void handleEvent(PassengerRequestRejectedEvent passengerRequestRejectedEvent) {
            rejectedRequests++;
        }

        @Override
        public void reset(int iteration) {
            rejectedRequests = 0;
        }

        public int getRejectedRequests() {
            return rejectedRequests;
        }
    }


}
