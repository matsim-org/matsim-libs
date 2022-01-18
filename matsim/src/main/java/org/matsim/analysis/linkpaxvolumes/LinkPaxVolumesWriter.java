/* *********************************************************************** *
 * project: org.matsim.*
 * LinkPaxVolumesWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.analysis.linkpaxvolumes;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

public class LinkPaxVolumesWriter {
    private final LinkPaxVolumesAnalysis linkPaxVolumesAnalysis;
    private final Network network;
    private final String columnSeparator;
    private final int numberOfHours;

    public LinkPaxVolumesWriter(LinkPaxVolumesAnalysis linkPaxVolumesAnalysis, Network network, String sep) {
        this.linkPaxVolumesAnalysis = linkPaxVolumesAnalysis;
        this.network = network;
        this.columnSeparator = sep;
        this.numberOfHours = linkPaxVolumesAnalysis.getNumberOfHours();
    }

    public void writeLinkVehicleAndPaxVolumesAllPerDayCsv(String fileName) {
        // have results sorted
        SortedSet<Id<Link>> linkIdsSorted = new TreeSet(network.getLinks().keySet());

        String[] header = {"link", "vehicles", "passengersInclDriver"};

        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(fileName),
                CSVFormat.DEFAULT.withDelimiter(columnSeparator.charAt(0)).withHeader(header))
        ) {
            for (Id<Link> linkId : linkIdsSorted) {
                printer.print(linkId);
                printer.print(calcDailyValueOrZero(linkPaxVolumesAnalysis.getVehicleVolumesForLink(linkId)));
                printer.print(calcDailyValueOrZero(linkPaxVolumesAnalysis.getPaxVolumesForLink(linkId)));
                printer.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeLinkVehicleAndPaxVolumesPerNetworkModePerHourCsv(String fileName) {
        // have results sorted
        SortedSet<Id<Link>> linkIdsSorted = new TreeSet(network.getLinks().keySet());
        SortedSet<String> networkModesSorted = new TreeSet(linkPaxVolumesAnalysis.getNetworkModes());

        String[] header = {"link", "networkMode", "hour", "vehicles", "passengersInclDriver"};

        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(fileName),
                CSVFormat.DEFAULT.withDelimiter(columnSeparator.charAt(0)).withHeader(header))
        ) {
            for (Id<Link> linkId : linkIdsSorted) {
                for (String networkMode : networkModesSorted) {
                    int[] hourlyVehicleVolumes = calcHourlyValuesOrZero(linkPaxVolumesAnalysis.
                            getVehicleVolumesForLinkPerNetworkMode(linkId, networkMode));
                    int[] hourlyPassengerVolumes = calcHourlyValuesOrZero(linkPaxVolumesAnalysis.
                            getPaxVolumesForLinkPerNetworkMode(linkId, networkMode));
                    for (int hour = 0; hour < numberOfHours; hour++) {
                        printer.print(linkId);
                        printer.print(networkMode);
                        printer.print(hour);
                        printer.print(hourlyVehicleVolumes[hour]);
                        printer.print(hourlyPassengerVolumes[hour]);
                        printer.println();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Please note that the same vehicle is counted for each passenger mode it is serving here. E.g. a pt vehicle
     * will be counted with the driver's passenger mode and with the passenger's passenger mode, i.e. multiple times.
     */
    public void writeLinkVehicleAndPaxVolumesPerPassengerModePerHourCsv(String fileName) {
        // have results sorted
        SortedSet<Id<Link>> linkIdsSorted = new TreeSet(network.getLinks().keySet());
        SortedSet<String> passengerModesSorted = new TreeSet(linkPaxVolumesAnalysis.getPassengerModes());

        String[] header = {"link", "passengerMode", "hour", "vehicles", "passengersPossiblyInclDriver"};

        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(fileName),
                CSVFormat.DEFAULT.withDelimiter(columnSeparator.charAt(0)).withHeader(header))
        ) {
            for (Id<Link> linkId : linkIdsSorted) {
                for (String passengerMode : passengerModesSorted) {
                    int[] hourlyVehicleVolumes = calcHourlyValuesOrZero(linkPaxVolumesAnalysis.
                            getVehicleVolumesForLinkPerPassengerMode(linkId, passengerMode));
                    int[] hourlyPassengerVolumes = calcHourlyValuesOrZero(linkPaxVolumesAnalysis.
                            getPaxVolumesForLinkPerPassengerMode(linkId, passengerMode));
                    for (int hour = 0; hour < numberOfHours; hour++) {
                        printer.print(linkId);
                        printer.print(passengerMode);
                        printer.print(hour);
                        printer.print(hourlyVehicleVolumes[hour]);
                        printer.print(hourlyPassengerVolumes[hour]);
                        printer.println();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeLinkVehicleAndPaxVolumesPerVehicleTypePerHourCsv(String fileName) {
        // have results sorted
        SortedSet<Id<Link>> linkIdsSorted = new TreeSet(network.getLinks().keySet());
        SortedSet<Id<VehicleType>> vehicleIdsSorted = new TreeSet(linkPaxVolumesAnalysis.getVehicleTypes());

        String[] header = {"link", "vehicleType", "hour", "vehicles", "passengersInclDriver"};

        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(fileName),
                CSVFormat.DEFAULT.withDelimiter(columnSeparator.charAt(0)).withHeader(header))
        ) {
            for (Id<Link> linkId : linkIdsSorted) {
                for (Id<VehicleType> vehicleTypeId : vehicleIdsSorted) {
                    int[] hourlyVehicleVolumes = calcHourlyValuesOrZero(linkPaxVolumesAnalysis.
                            getVehicleVolumesForLinkPerVehicleType(linkId, vehicleTypeId));
                    int[] hourlyPassengerVolumes = calcHourlyValuesOrZero(linkPaxVolumesAnalysis.
                            getPaxVolumesForLinkPerVehicleType(linkId, vehicleTypeId));
                    for (int hour = 0; hour < numberOfHours; hour++) {
                        printer.print(linkId);
                        printer.print(vehicleTypeId);
                        printer.print(hour);
                        printer.print(hourlyVehicleVolumes[hour]);
                        printer.print(hourlyPassengerVolumes[hour]);
                        printer.println();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int[] calcHourlyValuesOrZero(int[] volumes) {
        if (volumes != null) {
            return linkPaxVolumesAnalysis.getVolumePerHourFromTimeBinArray(volumes);
        } else {
            return new int[numberOfHours];
        }
    }

    private int calcDailyValueOrZero(int[] volumes) {
        if (volumes != null) {
            return linkPaxVolumesAnalysis.getVolumePerDayFromTimeBinArray(volumes);
        } else {
            return 0;
        }
    }
}
