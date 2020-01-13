/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
 * *********************************************************************** */
package org.matsim.contrib.travelsummary.events2traveldiaries;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;

import java.io.File;
import java.io.IOException;

/**
 * @author pieterfourie
 * <p>
 * Running this class with or without arguments prints out instructions for use.
 * </p>
 */
public class RunEventsToTravelDiaries {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String eventsFileName = null;
        Config config = null;
        String appendage = "";
        String outputDirectory = null;
        printHelp();
        try {
            config = ConfigUtils.loadConfig(args[0]);
            outputDirectory = config.controler().getOutputDirectory();

            eventsFileName = args[1];

            if (args.length > 2) {
                appendage = args[2];
                if (!(appendage.matches("[_]*[a-zA-Z0-9]*") ||
                        appendage.matches("[a-zA-Z0-9]*[_]*") ||
                        appendage.matches("[a-zA-Z0-9]*"))) {
                    System.err.println("Only alphanumeric and " +
                            "leading OR trailing underscore characters allowed in " +
                            "prefix/suffix.");
                    System.exit(1);
                }
            }

            if (args.length > 3) {

                outputDirectory = args[3];

            } else {
                System.err.println("No output directory specified. " +
                        "Output directory from config will be used.");

            }
            //test the output directory first before running the whole analysis
            File f = new File(outputDirectory);
            System.out.println("Writing files to " + f.getAbsolutePath());
            if (!f.exists() && f.canWrite()) {
                System.err.println("Cannot write to output directory. " +
                        "Check for existence and permissions");
                System.exit(1);
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            System.exit(1);
        }
        Scenario scenario = ScenarioUtils.createScenario(config);

        new MatsimNetworkReader(scenario.getNetwork()).parse(config.network().getInputFileURL(config.getContext()));

        if (config.transit().isUseTransit()) {

            new TransitScheduleReader(scenario)
                    .readFile(config.transit().getTransitScheduleFile());

            new VehicleReaderV1(scenario.getTransitVehicles())
                    .readFile(config.transit().getVehiclesFile());

        }


        EventsToTravelDiaries handler =
                new EventsToTravelDiaries(scenario);

        EventsManager events = new EventsManagerImpl();

        events.addHandler(handler);

        new MatsimEventsReader(events).readFile(eventsFileName);

        try {
            handler.writeSimulationResultsToTabSeparated(outputDirectory, appendage);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void printHelp() {
        System.out.println(
                "This class generates tab-separated travel diary tables from MATSim events.\n" +
                        "It currently works with scenarios that both teleport and fully simulate public\n" +
                        "transportation.\n\n" +
                        "It takes the following inputs, in strict order:\n\n" +
                        "0 (REQUIRED): A config file, specifying the network file, and transit data (if\n" +
                        "              simulating transit).\n\n" +
                        "1 (REQUIRED): An events file.\n\n" +
                        "2 (OPTIONAL): An ALPHANUMERIC prefix or suffix to prepend or append to the beginning or\n" +
                        "              end of the output file names. Prefixes have a trailing under-\n" +
                        "              score, suffixes have a leading underscore, e.g. pre_ will\n" +
                        "              produce pre_matsim_table.txt, and _post will produce\n" +
                        "              matsim_table_post.txt. Useful when comparing multiple diaries.\n\n" +
                        "3 (OPTIONAL): An output directory, where the travel diary tables will be\n" +
                        "              written to, otherwise the output directory from the config\n" +
                        "              is used.\n\n" +
                        "4 (OPTIONAL): A maximum number of events to process (for diagnostics).\n\n"
        );
    }
}

