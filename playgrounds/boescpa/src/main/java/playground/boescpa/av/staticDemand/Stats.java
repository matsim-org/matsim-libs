/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package playground.boescpa.av.staticDemand;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import playground.boescpa.analysis.trips.Trip;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class Stats {
    private static Logger log = Logger.getLogger(Stats.class);

    public static final String delimiter = "; ";

    private final List<Trip> demand;
    private final List<AutonomousVehicle> fleet;
    private List<Request> requestStats = new ArrayList<>();
    private List<String> simStats = new ArrayList<>();
    private List<String> simResults = new ArrayList<>();

    private long totalDemand = 0;
    private long unmetDemand = 0;
    private long lateMetDemand = 0;
    private long metDemand = 0;
    private double responseTimeMetDemand = 0; // seconds
    private double responseTimeLateMetDemand = 0; // seconds
    private double maxResponseTimeMetDemand = 0; // seconds
    private double maxResponseTimeLateMetDemand = 0; // seconds
    private double minTravelTimeMetDemand = 0; // minutes
    private double maxTravelTimeMetDemand = 0; // minutes
    private double totalTravelTimeMetDemand = 0; // minutes
    private double travelDistanceMetDemand = 0; // m
    private double minTravelTimeLateMetDemand = 0; // minutes
    private double maxTravelTimeLateMetDemand = 0; // minutes
    private double totalTravelTimeLateMetDemand = 0; // minutes
    private double travelDistanceLateMetDemand = 0; // m

    private double totalWaitingTimeForAssignmentMetDemand = 0; // seconds
    private double maxWaitingTimeForAssignmentMetDemand = 0; // seconds
    private double totalWaitingTimeForAssignmentLateMetDemand = 0; // seconds
    private double maxWaitingTimeForAssignmentLateMetDemand = 0; // seconds
    private double totalWaitingTimeAgents = 0; // seconds
    private double maxWaitingTimeAgents = 0; // seconds

    private int numberOfAgents;
    private int numberOfAVs;

    public Stats(StaticAVSim.SimBase simBase) {
        demand = Collections.unmodifiableList(simBase.getDemand());
        fleet = Collections.unmodifiableList(simBase.getFleet());
        numberOfAgents = StaticDemand.getAllAgents(demand).size();
        numberOfAVs = fleet.size();

        writeRecordStatsHeader();
    }

    public void incTotalDemand() {
        totalDemand++;
    }

    public void incMetDemand() {
        metDemand++;
    }

    public void incLateMetDemand() {
        lateMetDemand++;
    }

    public void incUnmetDemand() {
        unmetDemand++;
    }

    public void incResponseTimeMetDemand(double responseTime) {
        if (responseTime >= 0) {
            responseTimeMetDemand += responseTime;
        } else {
            throw new IllegalArgumentException("Negative response time!");
        }

        if (responseTime > maxResponseTimeMetDemand) {
            maxResponseTimeMetDemand = responseTime;
        }
    }

    public void incResponseTimeLateMetDemand(double responseTime) {
        if (responseTime >= 0) {
            responseTimeLateMetDemand += responseTime;
        } else {
            throw new IllegalArgumentException("Negative late reponse time!");
        }

        if (responseTime > maxResponseTimeLateMetDemand) {
            maxResponseTimeLateMetDemand = responseTime;
        }
    }

    public void incTravelTimeMetDemand(double travelTimeMetDemand) {
        double localTravelTime = travelTimeMetDemand / 60; // Conversion to minutes...

        if (localTravelTime >= 0) {
            totalTravelTimeMetDemand += localTravelTime;
        } else {
            throw new IllegalArgumentException("Negative travel time!");
        }

        if (localTravelTime < minTravelTimeMetDemand) {
            minTravelTimeMetDemand = localTravelTime;
        }
        if (localTravelTime > maxTravelTimeMetDemand) {
            maxTravelTimeMetDemand = localTravelTime;
        }
    }

    public void incTravelDistanceMetDemand(double distance) {
        if (distance >= 0) {
            travelDistanceMetDemand += distance;
        } else {
            throw new IllegalArgumentException("Negative travel distance!");
        }
    }

    public void incTravelTimeLateMetDemand(double travelTimeLateMetDemand) {
        double localTravelTime = travelTimeLateMetDemand / 60; // Conversion to minutes...

        if (localTravelTime >= 0) {
            totalTravelTimeLateMetDemand += localTravelTime;
        } else {
            throw new IllegalArgumentException("Negative travel time!");
        }

        if (localTravelTime < minTravelTimeLateMetDemand) {
            minTravelTimeLateMetDemand = localTravelTime;
        }
        if (localTravelTime > maxTravelTimeLateMetDemand) {
            maxTravelTimeLateMetDemand = localTravelTime;
        }
    }

    public void incTravelDistanceLateMetDemand(double distance) {
        if (distance >= 0) {
            travelDistanceLateMetDemand += distance;
        } else {
            throw new IllegalArgumentException("Negative travel distance!");
        }
    }

    public void incWaitingTimeForAssignmentMetDemand(double waitingTimeForAssignment) {
        if (waitingTimeForAssignment >= 0) {
            totalWaitingTimeForAssignmentMetDemand += waitingTimeForAssignment;
            if (waitingTimeForAssignment > maxWaitingTimeForAssignmentMetDemand) {
                maxWaitingTimeForAssignmentMetDemand = waitingTimeForAssignment;
            }
        } else {
            throw new IllegalArgumentException("Negative waiting time for assignment!");
        }
    }

    public void incWaitingTimeForAssignmentLateMetDemand(double waitingTimeForAssignment) {
        if (waitingTimeForAssignment >= 0) {
            totalWaitingTimeForAssignmentLateMetDemand += waitingTimeForAssignment;
            if (waitingTimeForAssignment > maxWaitingTimeForAssignmentLateMetDemand) {
                maxWaitingTimeForAssignmentLateMetDemand = waitingTimeForAssignment;
            }
        } else {
            throw new IllegalArgumentException("Negative waiting time for assignment!");
        }
    }

    public void incWaitingTime(double waitingTimeAgents) {
        if (waitingTimeAgents >= 0) {
            totalWaitingTimeAgents += waitingTimeAgents;
            if (waitingTimeAgents > maxWaitingTimeAgents) {
                maxWaitingTimeAgents = waitingTimeAgents;
            }
        } else {
            throw new IllegalArgumentException("Negative waiting times!");
        }
    }

    public void addRequest(Request request) {
        requestStats.add(request);
    }

    private void writeRecordStatsHeader() {
        simStats.add("time [min]"
                        + delimiter + "pendingRequests"
                        + delimiter + "vehiclesInUse"
                        + delimiter + "availableVehicles"
                        //+ delimiter + "redistributingVehicles"
                        //+ delimiter + "redistCacheSize"
                        + delimiter + "quickServedRequests"
                        + delimiter + "servedRequests"
                        + delimiter + "lateServedRequests"
                        + delimiter + "unservedRequests"
        );
    }

    public void recordStats(int time, int pendingRequests, int vehiclesInUse, int redistributingVehicles,
                            int availableVehicles, int redistCacheSize, int quickServedRequests, int servedRequests,
                            int lateServedRequests, int unservedRequests) {
        simStats.add(time / 60
                        + delimiter + pendingRequests
                        + delimiter + vehiclesInUse
                        + delimiter + availableVehicles
                        //+ delimiter + redistributingVehicles
                        //+ delimiter + redistCacheSize
                        + delimiter + quickServedRequests
                        + delimiter + servedRequests
                        + delimiter + lateServedRequests
                        + delimiter + unservedRequests
        );

    }

    public void printResults(String pathToOutput) {
        composeResults();
        log.info("");
        for (String result : simResults) {
            log.info(result);
        }
        log.info("");
        writeResultsToFile(pathToOutput);
    }

    private void composeResults() {
        simResults.add("RESULTS:");
        simResults.add(" - Total number of agents: " + numberOfAgents);
        simResults.add(" - Total demand: " + totalDemand);
        simResults.add(" - Total number of AVs: " + numberOfAVs);
        //simResults.add(" - Average response time met and late met demand: " + 0.01 * (Math.round(100 * ((responseTimeMetDemand + responseTimeLateMetDemand) / (metDemand + lateMetDemand) / 60))) + " min");
        //simResults.add(" - Max response time met and late met demand: " + 0.01 * (Math.round(100 * (Math.max(maxResponseTimeMetDemand, maxResponseTimeLateMetDemand) / 60))) + " min");
        // Met demand:
        simResults.add("   ...........");
        simResults.add(" - Met demand: " + metDemand);
        simResults.add(" - Average waiting time for assignment met demand: " + 0.01 * (Math.round(100 * (totalWaitingTimeForAssignmentMetDemand / metDemand / 60))) + " min");
        simResults.add(" - Max waiting time for assignment met demand: " + 0.01 * (Math.round(100 * (maxWaitingTimeForAssignmentMetDemand / 60))) + " min");
        simResults.add(" - Average response time met demand: " + 0.01 * (Math.round(100 * (responseTimeMetDemand / metDemand / 60))) + " min");
        simResults.add(" - Max response time met demand: " + 0.01 * (Math.round(100 * (maxResponseTimeMetDemand / 60))) + " min");
        simResults.add(" - Average travel time met demand: " + 0.01 * (Math.round(100 * (totalTravelTimeMetDemand / metDemand))) + " min");
        simResults.add(" - Min travel time met demand: " + 0.01 * (Math.round(100 * (minTravelTimeMetDemand))) + " min");
        simResults.add(" - Max travel time met demand: " + 0.01 * (Math.round(100 * (maxTravelTimeMetDemand))) + " min");
        simResults.add(" - Average travel distance met demand: " + (travelDistanceMetDemand / metDemand / 1000) + " km");
        // Late met demand:
        simResults.add("   ...........");
        simResults.add(" - Late met demand: " + lateMetDemand);
        simResults.add(" - Average waiting time for assignment late met demand: " + 0.01 * (Math.round(100 * (totalWaitingTimeForAssignmentLateMetDemand / lateMetDemand / 60))) + " min");
        simResults.add(" - Max waiting time for assignment late met demand: " + 0.01 * (Math.round(100 * (maxWaitingTimeForAssignmentLateMetDemand / 60))) + " min");
        simResults.add(" - Average response time late met demand: " + 0.01 * (Math.round(100 * (responseTimeLateMetDemand / lateMetDemand / 60))) + " min");
        simResults.add(" - Max response time late met demand: " + 0.01 * (Math.round(100 * (maxResponseTimeLateMetDemand / 60))) + " min");
        simResults.add(" - Average waiting time for late car: " + 0.01 * (Math.round(100 * (totalWaitingTimeAgents / lateMetDemand / 60))) + " min");
        simResults.add(" - Max waiting time for late car: " + 0.01 * (Math.round(100 * (maxWaitingTimeAgents / 60))) + " min");
        simResults.add(" - Average travel time late met demand: " + 0.01 * (Math.round(100 * (totalTravelTimeLateMetDemand / lateMetDemand))) + " min");
        simResults.add(" - Min travel time late met demand: " + 0.01 * (Math.round(100 * (minTravelTimeLateMetDemand))) + " min");
        simResults.add(" - Max travel time late met demand: " + 0.01 * (Math.round(100 * (maxTravelTimeLateMetDemand))) + " min");
        simResults.add(" - Average travel distance late met demand: " + (travelDistanceLateMetDemand / lateMetDemand / 1000) + " km");
        // Unmet demand:
        simResults.add("   ...........");
        simResults.add(" - Unmet demand: " + unmetDemand);
    }

    private void writeResultsToFile(String pathToOutput) {
        final String outputFileStats = pathToOutput.substring(0, pathToOutput.lastIndexOf("."))
                + "_Stats" + pathToOutput.substring(pathToOutput.lastIndexOf("."));
        final String outputFileResults = pathToOutput.substring(0, pathToOutput.lastIndexOf("."))
                + "_Results.txt";
        final String outputFileVehicles = pathToOutput.substring(0, pathToOutput.lastIndexOf("."))
                + "_Vehicles" + pathToOutput.substring(pathToOutput.lastIndexOf("."));
        final String outputFileRequests = pathToOutput.substring(0, pathToOutput.lastIndexOf("."))
                + "_Requests" + pathToOutput.substring(pathToOutput.lastIndexOf("."));

        try {
            final BufferedWriter outStats = IOUtils.getBufferedWriter(outputFileStats);
            final BufferedWriter outResults = IOUtils.getBufferedWriter(outputFileResults);
            final BufferedWriter outVehicles = IOUtils.getBufferedWriter(outputFileVehicles);
            final BufferedWriter outRequests = IOUtils.getBufferedWriter(outputFileRequests);
            log.info("Writing stats file...");
            for (String line : simStats) {
                outStats.write(line);
                outStats.newLine();
            }
            outStats.close();
            log.info("Writing stats file...done.");
            log.info("Writing results file...");
            for (String line : simResults) {
                outResults.write(line);
                outResults.newLine();
            }
            outResults.close();
            log.info("Writing results file...done.");
            log.info("Writing vehicle file...");
            outVehicles.write(AutonomousVehicle.getStatsDescr());
            outVehicles.newLine();
            for (AutonomousVehicle vehicle : fleet) {
                outVehicles.write(vehicle.getStats());
                outVehicles.newLine();
            }
            outVehicles.close();
            log.info("Writing vehicle file...done.");
            log.info("Writing request file...");
            outRequests.write(Request.getStatsDescr());
            outRequests.newLine();
            for (Request request : requestStats) {
                outRequests.write(request.getStats());
                outRequests.newLine();
            }
            outRequests.close();
            log.info("Writing request file...done.");
        } catch (IOException e) {
            log.info("Given trip-file-path not valid. Print trips not successfully executed.");
        }
    }
}
