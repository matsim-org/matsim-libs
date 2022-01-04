/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleStatsPerVehicleType.java
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

/**
 * @author vsp-gleich
 */
public class VehicleStatsPerVehicleType {

    private final LinkPaxVolumesAnalysis linkPaxVolumesAnalysis;
    private final Network network;
    private final String columnSeparator;

    public VehicleStatsPerVehicleType(LinkPaxVolumesAnalysis linkPaxVolumesAnalysis, Network network, String sep) {
        this.linkPaxVolumesAnalysis = linkPaxVolumesAnalysis;
        this.network = network;
        this.columnSeparator = sep;
    }

    public void writeOperatingStatsPerVehicleType(String fileName) {
        // have results sorted
        SortedSet<Id<VehicleType>> vehicleIdsSorted = new TreeSet(linkPaxVolumesAnalysis.getVehicleTypes());

        String[] header = {"vehicleType", "vehicleKm", "passengerKm", "vehicleHoursOnNetwork", "numberVehiclesUsed"};

        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(fileName),
                CSVFormat.DEFAULT.withDelimiter(columnSeparator.charAt(0)).withHeader(header))
        ) {
            for (Id<VehicleType> vehicleTypeId : vehicleIdsSorted) {
                double sumVehicleKm = 0.0;
                double sumPaxKm = 0.0;
                for (Id<Link> linkId : linkPaxVolumesAnalysis.getLinkIds()) {
                    double linkLengthKm = network.getLinks().get(linkId).getLength() / 1000;

                    int[] vehicleVolumes = linkPaxVolumesAnalysis.getVehicleVolumesForLinkPerVehicleType(linkId, vehicleTypeId);
                    if (vehicleVolumes != null) {
                        sumVehicleKm += linkPaxVolumesAnalysis.getVolumePerDayFromTimeBinArray(vehicleVolumes) * linkLengthKm;
                    }

                    int[] passengerVolumes = linkPaxVolumesAnalysis.getPaxVolumesForLinkPerVehicleType(linkId, vehicleTypeId);
                    if (passengerVolumes != null) {
                        sumPaxKm += linkPaxVolumesAnalysis.getVolumePerDayFromTimeBinArray(passengerVolumes) * linkLengthKm;
                    }

                }
                printer.print(vehicleTypeId);
                printer.print(sumVehicleKm);
                printer.print(sumPaxKm);
                printer.print(linkPaxVolumesAnalysis.getVehicleType2timeOnNetwork().get(vehicleTypeId) / 3600);
                printer.print(linkPaxVolumesAnalysis.getVehicleType2numberSeen().get(vehicleTypeId));
                printer.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
