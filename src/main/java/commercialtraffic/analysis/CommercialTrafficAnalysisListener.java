/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package commercialtraffic.analysis;/*
 * created by jbischoff, 19.06.2019
 */

import com.google.inject.Inject;
import commercialtraffic.deliveryGeneration.PersonDelivery;
import commercialtraffic.scoring.ScoreCommercialServices;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.Locale;


public class CommercialTrafficAnalysisListener implements IterationEndsListener {

    @Inject
    MatsimServices services;

    @Inject
    Carriers carriers;

    @Inject
    ScoreCommercialServices scoreCommercialServices;

    @Inject
    TourLengthAnalyzer tourLengthAnalyzer;

    private boolean firstIteration = true;
    private String sep = ";";

    DecimalFormat format = new DecimalFormat();


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        format.setMinimumIntegerDigits(1);
        format.setMaximumFractionDigits(2);
        format.setGroupingUsed(false);

        writeIterationCarrierStats(event);
        writeDeliveryStats(services.getControlerIO().getIterationFilename(event.getIteration(), "deliveryStats.csv"));

        firstIteration = false;

    }

    private void writeDeliveryStats(String filename) {
        Collections.sort(scoreCommercialServices.getLogEntries(), Comparator.comparing(ScoreCommercialServices.DeliveryLogEntry::getTime));
        try (CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(filename)), CSVFormat.DEFAULT.withDelimiter(sep.charAt(0)).withHeader("CarrierId"
                , "PersonId", "Time", "Score", "LinkId", "TimeDerivation", "DriverId"))) {
            for (ScoreCommercialServices.DeliveryLogEntry entry : scoreCommercialServices.getLogEntries()) {
                csvPrinter.print(entry.getCarrierId());
                csvPrinter.print(entry.getPersonId());
                csvPrinter.print(Time.writeTime(entry.getTime()));
                csvPrinter.print(entry.getScore());
                csvPrinter.print(entry.getLinkId());
                csvPrinter.print(entry.getTimeDifference());
                csvPrinter.print(entry.getDriverId());
                csvPrinter.println();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void writeIterationCarrierStats(IterationEndsEvent event) {
        for (Carrier carrier : carriers.getCarriers().values()) {
            DoubleSummaryStatistics distances = tourLengthAnalyzer.getDeliveryAgentDistances().entrySet().stream()
                    .filter(entry -> PersonDelivery.getCarrierIdFromDriver(entry.getKey()).equals(carrier.getId()))
                    .mapToDouble(e -> e.getValue())
                    .summaryStatistics();
            DoubleSummaryStatistics scores = scoreCommercialServices.getLogEntries().stream()
                    .filter(deliveryLogEntry -> deliveryLogEntry.getCarrierId().equals(carrier.getId()))
                    .mapToDouble(ScoreCommercialServices.DeliveryLogEntry::getScore)
                    .summaryStatistics();
            DoubleSummaryStatistics timeDerivations = scoreCommercialServices.getLogEntries().stream()
                    .filter(deliveryLogEntry -> deliveryLogEntry.getCarrierId().equals(carrier.getId()))
                    .mapToDouble(ScoreCommercialServices.DeliveryLogEntry::getTimeDifference)
                    .summaryStatistics();
            BufferedWriter bw = IOUtils.getAppendingBufferedWriter(services.getControlerIO().getOutputFilename("carrierStats." + carrier.getId() + ".csv"));
            try {
                if (firstIteration) {
                    bw.write("Iteration" + sep + "Carrier" + sep + "Tours" + sep + "Total Distance (km)" + sep + "Average Tour Distance (km)" + sep + "Deliveries" + sep + "AverageScore" + sep + "TotalScore" + sep + "AverageDelay" + sep + "MaximumDelay");
                }
                bw.newLine();
                bw.write(event.getIteration() + sep +
                        carrier.getId() + sep +
                        distances.getCount() + sep +
                        format.format(distances.getSum() / 1000.) + sep +
                        format.format(distances.getAverage() / 1000.) + sep +
                        scores.getCount() + sep +
                        format.format(scores.getAverage()) + sep +
                        format.format(scores.getSum()) + sep +
                        format.format(timeDerivations.getAverage()) + sep +
                        timeDerivations.getMax());
                bw.flush();
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
