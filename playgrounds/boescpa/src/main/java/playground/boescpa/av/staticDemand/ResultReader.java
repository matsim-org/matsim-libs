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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * What is it for?
 *
 * @author boescpa
 */
public class ResultReader {
    private static Logger log = Logger.getLogger(ResultReader.class);
    private final static String DELIMITER = "; ";

    private static String defaultFileName_Prefix;
    private static String defaultFileName_Postfix;

    public static void main(String[] args) {
        defaultFileName_Prefix = args[0];
        defaultFileName_Postfix = args[1];
        final String outputFile = args[2];

        List<String> results = readFiles();
        writeSummary(results, outputFile);
    }

    private static void writeSummary(List<String> results, String outputFile) {
        try {
            BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
            for (String line : results) {
                writer.write(line);
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    protected static List<String> readFiles() {
        List<String> results = new ArrayList<>();
        results.add(
                "ShareOfAgents" + DELIMITER
                + "ShareOfFleet" + DELIMITER
                + "TotalNumberAgents" + DELIMITER
                + "TotalDemand" + DELIMITER
                + "TotalNumberOfAVs" + DELIMITER
                + "MetDemand" + DELIMITER
                + "AverageWaitingTimeAssignmentMetDemand" + DELIMITER
                + "MaxWaitingTimeAssignmentMetDemand" + DELIMITER
                + "AverageResponseTimeMetDemand" + DELIMITER
                + "MaxResponseTimeMetDemand" + DELIMITER
                + "AverageTravelTimeMetDemand" + DELIMITER
                + "MinTravelTimeMetDemand" + DELIMITER
                + "MaxTravelTimeMetDemand" + DELIMITER
                + "AverageTravelDistanceMetDemand" + DELIMITER
                + "LateMetDemand" + DELIMITER
                + "AverageWaitingTimeForAssignmentLateMetDemand" + DELIMITER
                + "MaxWaitingTimeForAssignmentLateMetDemand" + DELIMITER
                + "AverageResponseTimeLateMetDemand" + DELIMITER
                + "MaxResponseTimeLateMetDemand" + DELIMITER
                + "AverageWaitingTimeForLateCar" + DELIMITER
                + "MaxWaitingTimeForLateCar" + DELIMITER
                + "AverageTravelTimeLateMetDemand" + DELIMITER
                + "MinTravelTimeLateMetDemand" + DELIMITER
                + "MaxTravelTimeLateMetDemand" + DELIMITER
                + "AverageTravelDistanceLateMetDemand" + DELIMITER
                + "UnmetDemand"
		);

        for (int i = 1; i <= 10; i++) {
			double shareOfAgents = i/10.;
            for (int j = 1; j <= 10; j++) {
				double shareOfFleet;
				String fileSummary;
				if (j <= 4) {
					shareOfFleet = ((10*j) - 5)/100.;
					fileSummary = readFile(shareOfAgents, shareOfFleet);
					if (fileSummary != null) {
						results.add(fileSummary);
					}
				}
				shareOfFleet = j/10.;
				fileSummary = readFile(shareOfAgents, shareOfFleet);
                if (fileSummary != null) {
                    results.add(fileSummary);
                }
            }
        }

        return results;
    }

    private static String readFile(double shareOfAgents, double shareOfFleet) {
        String fileName = defaultFileName_Prefix
                + "_A" + shareOfAgents
                + "_F" + shareOfFleet
                + defaultFileName_Postfix;
        try {
			BufferedReader reader = IOUtils.getBufferedReader(fileName);
			reader.readLine();
			int numberOfAgents = Integer.parseInt(reader.readLine().split(":")[1].trim());
			int totalDemand = Integer.parseInt(reader.readLine().split(":")[1].trim());
			int numberOfAVs = Integer.parseInt(reader.readLine().split(":")[1].trim());
			reader.readLine();
			int metDemand = Integer.parseInt(reader.readLine().split(":")[1].trim());
			double averageWaitingTimeAssignmentMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double maxWaitingTimeAssignmentMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double averageResponseTimeMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double maxResponseTimeMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double averageTravelTimeMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double minTravelTimeMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double maxTravelTimeMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double averageTravelDistanceMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			reader.readLine();
			int lateMetDemand = Integer.parseInt(reader.readLine().split(":")[1].trim());
			double averageWaitingTimeAssignmentLateMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double maxWaitingTimeAssignmentLateMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double averageResponseTimeLateMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double maxResponseTimeLateMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double averageWaitingTimeForLateCar = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double maxWaitingTimeForLateCar = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double averageTravelTimeLateMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double minTravelTimeLateMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double maxTravelTimeLateMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			double averageTravelDistanceLateMetDemand = Double.parseDouble(reader.readLine().split(":")[1].split(" ")[1].trim());
			reader.readLine();
			int unmetDemand = Integer.parseInt(reader.readLine().split(":")[1].trim());
			reader.close();
			return shareOfAgents + DELIMITER
					+ shareOfFleet + DELIMITER
					+ numberOfAgents + DELIMITER
					+ totalDemand + DELIMITER
					+ numberOfAVs + DELIMITER
					+ metDemand + DELIMITER
					+ averageWaitingTimeAssignmentMetDemand + DELIMITER
					+ maxWaitingTimeAssignmentMetDemand + DELIMITER
					+ averageResponseTimeMetDemand + DELIMITER
					+ maxResponseTimeMetDemand + DELIMITER
					+ averageTravelTimeMetDemand + DELIMITER
					+ minTravelTimeMetDemand + DELIMITER
					+ maxTravelTimeMetDemand + DELIMITER
					+ averageTravelDistanceMetDemand + DELIMITER
					+ lateMetDemand + DELIMITER
					+ averageWaitingTimeAssignmentLateMetDemand + DELIMITER
					+ maxWaitingTimeAssignmentLateMetDemand + DELIMITER
					+ averageResponseTimeLateMetDemand + DELIMITER
					+ maxResponseTimeLateMetDemand + DELIMITER
					+ averageWaitingTimeForLateCar + DELIMITER
					+ maxWaitingTimeForLateCar + DELIMITER
					+ averageTravelTimeLateMetDemand + DELIMITER
					+ minTravelTimeLateMetDemand + DELIMITER
					+ maxTravelTimeLateMetDemand + DELIMITER
					+ averageTravelDistanceLateMetDemand + DELIMITER
					+ unmetDemand;
		} catch (IOException e) {
			log.error(e.getMessage());
            return null;
		}
    }

}
