/* *********************************************************************** *
 * project: org.matsim.*
 * PersonMoneyEventsAggregator.java
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

package org.matsim.analysis.personMoney;

import com.google.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.core.config.groups.GlobalConfigGroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Sums up the amount of personMoneyEvents by purpose and transactionPartner
 *
 * @author vsp-gleich
 */
public class PersonMoneyEventsAggregator implements PersonMoneyEventHandler {

    private final Map<String, Map<String, Double>> personMoneyAmountSumByPurposeAndTransactionPartner = new HashMap<>();
    private final String DEL;

    @Inject
    PersonMoneyEventsAggregator (GlobalConfigGroup globalConfigGroup) {
        this.DEL = globalConfigGroup.getDefaultDelimiter();
    }

    @Override
    public void reset(int iteration) {
        personMoneyAmountSumByPurposeAndTransactionPartner.clear();
    }

    @Override
    public void handleEvent(PersonMoneyEvent event) {
        String purpose = event.getPurpose() == null ? "null" : event.getPurpose();
        String transactionPartner = event.getTransactionPartner() == null ? "null" : event.getTransactionPartner();
        Map<String, Double> transactionPartnerMap = personMoneyAmountSumByPurposeAndTransactionPartner.computeIfAbsent(purpose, k -> new HashMap<>());
        transactionPartnerMap.put(transactionPartner, event.getAmount() + transactionPartnerMap.getOrDefault(transactionPartner, 0.0));
    }

    void writeOutput(String outputFilename) {
        try (CSVPrinter csvPrinter = new CSVPrinter(
                Files.newBufferedWriter(Paths.get(outputFilename)),
                CSVFormat.DEFAULT.withDelimiter(DEL.charAt(0)))) {
            csvPrinter.printRecord("purpose", "transactionPartner", "sumAmount");

            // sort output
            Map<String, Map<String, Double>> purpose2EntriesSorted = new TreeMap<>(personMoneyAmountSumByPurposeAndTransactionPartner);

            for (Map.Entry<String, Map<String, Double>> entryPurposeLevel: purpose2EntriesSorted.entrySet()) {
                Map<String, Double> transactionPartner2EntriesSorted = new TreeMap<>(entryPurposeLevel.getValue());
                for (Map.Entry<String, Double> entryTransactionPartnerLevel: transactionPartner2EntriesSorted.entrySet()) {
                    csvPrinter.printRecord(entryPurposeLevel.getKey(), entryTransactionPartnerLevel.getKey(), entryTransactionPartnerLevel.getValue());
                }
            }
        } catch (IOException e) {
            LogManager.getLogger(getClass()).error("Could not write " + outputFilename + ".");
        }
    }
}
