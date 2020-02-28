
/* *********************************************************************** *
 * project: org.matsim.*
 * TravelDistanceStatsControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.analysis;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.utils.io.IOUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class IterationTravelStatsControlerListener implements IterationEndsListener, ShutdownListener {

    @Inject
    Config config;

    @Inject
    Scenario scenario;
	@Inject
	private ExperiencedPlansService experiencedPlansService;

	@Inject
	private TravelDistanceStats travelDistanceStats;

	@Inject
    private PHbyModeCalculator pHbyModeCalculator;
	
	@Inject
    private PKMbyModeCalculator pkMbyModeCalculator;
    @Inject
    OutputDirectoryHierarchy outputDirectoryHierarchy;

    @Inject
    TripsAndLegsCSVWriter.CustomTripsWriterExtension customTripsWriterExtension;
    @Inject
    TripsAndLegsCSVWriter.CustomLegsWriterExtension customLegsWriterExtension;

	@Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        travelDistanceStats.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        pHbyModeCalculator.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        pkMbyModeCalculator.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        final boolean writingTripsAtAll = config.controler().getWriteTripsInterval() > 0;
        final boolean regularWriteEvents = writingTripsAtAll && (event.getIteration() > 0 && event.getIteration() % config.controler().getWriteTripsInterval() == 0);
        if (regularWriteEvents || (writingTripsAtAll && event.getIteration() == 0)) {
            new TripsAndLegsCSVWriter(scenario, customTripsWriterExtension, customLegsWriterExtension).write(experiencedPlansService.getExperiencedPlans()
                    , outputDirectoryHierarchy.getIterationFilename(event.getIteration(), Controler.DefaultFiles.tripscsv)
                    , outputDirectoryHierarchy.getIterationFilename(event.getIteration(), Controler.DefaultFiles.legscsv));
        }
    }


	@Override
	public void notifyShutdown(ShutdownEvent event) {
		travelDistanceStats.close();
		// TODO: this way the statistics are written only at the end. Better after each iteration?
        pHbyModeCalculator.writeOutput();
        pkMbyModeCalculator.writeOutput();
        if (config.controler().getWriteTripsInterval() > 0) {
            writePersonsCSV();
        }
    }

    private void writePersonsCSV() {
        Logger.getLogger(getClass()).info("Writing all Person and Attributes to " + Controler.DefaultFiles.personscsv);
        List<String> attributes = new ArrayList<>(scenario.getPopulation().getPersons().values().parallelStream().flatMap(p -> p.getAttributes().getAsMap().keySet().stream()).collect(Collectors.toSet()));
        attributes.remove("vehicles");
        List<String> header = new ArrayList<>();
        header.add("person");
        header.add("first_act_x");
        header.add("first_act_y");
        header.addAll(attributes);
        try (CSVPrinter csvPrinter = new CSVPrinter(IOUtils.getBufferedWriter(outputDirectoryHierarchy.getOutputFilename(Controler.DefaultFiles.personscsv)),
                CSVFormat.DEFAULT.withDelimiter(config.global().getDefaultDelimiter().charAt(0)).withHeader(header.stream().toArray(String[]::new)))) {
            for (Person p : scenario.getPopulation().getPersons().values()) {
                List<String> line = new ArrayList<>();
                line.add(p.getId().toString());
                String x = "";
                String y = "";
                if (p.getSelectedPlan().getPlanElements().size() > 0) {
                    Activity firstAct = (Activity) p.getSelectedPlan().getPlanElements().get(0);
                    if (firstAct.getCoord() != null) {
                        x = Double.toString(firstAct.getCoord().getX());
                        y = Double.toString(firstAct.getCoord().getY());
                    }
                }
                line.add(x);
                line.add(y);
                for (String attribute : attributes) {
                    Object value = p.getAttributes().getAttribute(attribute);
                    String result = value != null ? String.valueOf(value) : "";
                    line.add(result);
                }
                csvPrinter.printRecord(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Logger.getLogger(getClass()).info("...done");
    }
}
