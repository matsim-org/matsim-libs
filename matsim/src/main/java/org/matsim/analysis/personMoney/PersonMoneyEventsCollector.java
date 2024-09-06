/* *********************************************************************** *
 * project: org.matsim.*
 * PersonMoneyEventsCollector.java
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
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects all {@link PersonMoneyEvent}s and writes them out into a tsv file.
 *
 * @author vsp-gleich
 */
public class PersonMoneyEventsCollector implements PersonMoneyEventHandler {

    private final List<PersonMoneyEvent> personMoneyEventList = new ArrayList<>();
    private final String DEL;

    @Inject
    PersonMoneyEventsCollector (GlobalConfigGroup globalConfigGroup) {
        this.DEL = globalConfigGroup.getDefaultDelimiter();
    }

    @Override
    public void reset(int iteration) {
        personMoneyEventList.clear();
    }

    @Override
    public void handleEvent(PersonMoneyEvent event) {
        personMoneyEventList.add(event);
    }

    void writeAllPersonMoneyEvents(String outputFilename) {
        try (CSVPrinter csvPrinter = new CSVPrinter(IOUtils.getBufferedWriter(outputFilename),
                CSVFormat.DEFAULT.withDelimiter(DEL.charAt(0)))) {
            csvPrinter.printRecord("time", "person", "amount", "purpose", "transactionPartner", "reference");

            for (PersonMoneyEvent personMoneyEvent: personMoneyEventList) {
                csvPrinter.printRecord(personMoneyEvent.getTime(), personMoneyEvent.getPersonId(),
                        personMoneyEvent.getAmount(), personMoneyEvent.getPurpose(),
                        personMoneyEvent.getTransactionPartner(), personMoneyEvent.getReference());
            }
        } catch (IOException e) {
            LogManager.getLogger(getClass()).error("Could not write " + outputFilename + ".");
        }
    }
}
