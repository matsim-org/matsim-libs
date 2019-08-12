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
import commercialtraffic.jobGeneration.CommercialJobUtils;
import commercialtraffic.scoring.ScoreCommercialServices;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.misc.Time;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;


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
    private Map<String, Map<Id<Carrier>, Map<Integer, Double>>> carrierShareHistories = new HashMap<>();

    DecimalFormat format = new DecimalFormat();


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        format.setMinimumIntegerDigits(1);
        format.setMaximumFractionDigits(2);
        format.setGroupingUsed(false);

        writeIterationCarrierStats(event);
        writeDeliveryStats(services.getControlerIO().getIterationFilename(event.getIteration(), "deliveryStats.csv"));
        analyzeCarrierMarketShares(event.getIteration());
        
        firstIteration = false;

    }

    private void analyzeCarrierMarketShares(int iteration) {


        Map<String, Set<Id<Carrier>>> carriersSplitByMarket = CommercialJobUtils.splitCarriersByMarket(carriers);


        for (Map.Entry<String, Set<Id<Carrier>>> entry : carriersSplitByMarket.entrySet()) {
            Map<Id<Carrier>, Map<Integer, Double>> marketShareHistory = carrierShareHistories.getOrDefault(entry.getKey(), new HashMap<>());
            carrierShareHistories.put(entry.getKey(), marketShareHistory);

            Map<Id<Carrier>, Long> carrierDeliveries = new TreeMap<>();
            for (Id<Carrier> carrierId : entry.getValue()) {
                carrierDeliveries.put(carrierId, scoreCommercialServices.getLogEntries().stream()
                        .filter(k -> k.getCarrierId().equals(carrierId))
                        .count());
            }

            try (CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(services.getControlerIO().getOutputFilename("output_marketshare_" + entry.getKey() + ".csv")), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND), CSVFormat.DEFAULT.withDelimiter(sep.charAt(0)))) {
                if (firstIteration) {
                    csvPrinter.printRecord(carrierDeliveries.keySet());
                }
                double sum = carrierDeliveries.values().stream().mapToDouble(Long::doubleValue).sum();
                for (Map.Entry<Id<Carrier>, Long> del : carrierDeliveries.entrySet()) {
                    Map<Integer, Double> history = marketShareHistory.getOrDefault(del.getKey(), new HashMap<>());
                    marketShareHistory.put(del.getKey(), history);
                    final double value = sum > 0 ? del.getValue() / sum : 0.0;
                    history.put(iteration, value);
                    csvPrinter.print(format.format(value));
                }
                csvPrinter.println();


            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            XYLineChart chart = new XYLineChart(entry.getKey() + " Carrier market Share Statistics", "iteration", "mode");
            for (Map.Entry<Id<Carrier>, Map<Integer, Double>> idMapEntry : marketShareHistory.entrySet()) {
                String carrier = idMapEntry.getKey().toString();
                Map<Integer, Double> history = idMapEntry.getValue();
                chart.addSeries(carrier, history);
            }
            chart.addMatsimLogo();
            chart.saveAsPng(services.getControlerIO().getOutputFilename("output_marketshare_" + entry.getKey() + ".png"), 800, 600);

        }


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
                    .filter(entry -> CommercialJobUtils.getCarrierIdFromDriver(entry.getKey()).equals(carrier.getId()))
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

            try (CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(services.getControlerIO().getOutputFilename("carrierStats." + carrier.getId() + ".csv")), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND), CSVFormat.DEFAULT.withDelimiter(sep.charAt(0)))) {
                if (firstIteration) {
                    csvPrinter.printRecord("Iteration", "Carrier", "Tours", "Total Distance (km)", "Average Tour Distance (km)", "Deliveries", "AverageScore", "TotalScore", "AverageDelay", "MaximumDelay");
                }
                csvPrinter.printRecord(event.getIteration(),
                        carrier.getId(),
                        distances.getCount(),
                        format.format(distances.getSum() / 1000.),
                        format.format(distances.getAverage() / 1000.),
                        scores.getCount(),
                        format.format(scores.getAverage()),
                        format.format(scores.getSum()),
                        format.format(timeDerivations.getAverage()),
                        timeDerivations.getMax());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
