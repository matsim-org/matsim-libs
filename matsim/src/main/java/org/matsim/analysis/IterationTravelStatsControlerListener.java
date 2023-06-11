
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.utils.io.IOUtils;

import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    @Inject
    TripsAndLegsCSVWriter.CustomTimeWriter customTimeWriter;


    @Inject
    AnalysisMainModeIdentifier mainModeIdentifier;

    Logger log = LogManager.getLogger(IterationTravelStatsControlerListener.class);

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        travelDistanceStats.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        pHbyModeCalculator.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        pkMbyModeCalculator.addIteration(event.getIteration(), experiencedPlansService.getExperiencedPlans());
        pHbyModeCalculator.writeOutput();
        pkMbyModeCalculator.writeOutput();
        final boolean writingTripsAtAll = config.controler().getWriteTripsInterval() > 0;
        final boolean regularWriteEvents = writingTripsAtAll && ((event.getIteration() > 0 && event.getIteration() % config.controler().getWriteTripsInterval() == 0) || event.isLastIteration());
        if (regularWriteEvents || (writingTripsAtAll && event.getIteration() == 0)) {
            new TripsAndLegsCSVWriter(scenario, customTripsWriterExtension, customLegsWriterExtension, mainModeIdentifier, customTimeWriter).write(experiencedPlansService.getExperiencedPlans()
                    , outputDirectoryHierarchy.getIterationFilename(event.getIteration(), Controler.DefaultFiles.tripscsv)
                    , outputDirectoryHierarchy.getIterationFilename(event.getIteration(), Controler.DefaultFiles.legscsv));

			writeActivityCSV(event.getIteration());
        }
    }


	@Override
	public void notifyShutdown(ShutdownEvent event) {
		travelDistanceStats.close();

        if (config.controler().getWriteTripsInterval() > 0) {
            writePersonsCSV();
        }
    }

    private void writePersonsCSV() {
        LogManager.getLogger(getClass()).info("Writing all Person and Attributes to " + Controler.DefaultFiles.personscsv);
        List<String> attributes = new ArrayList<>(scenario.getPopulation().getPersons().values().parallelStream().flatMap(p -> p.getAttributes().getAsMap().keySet().stream()).collect(Collectors.toSet()));
        attributes.remove("vehicles");
        List<String> header = new ArrayList<>();
        header.add("person");
        header.add("executed_score");
        header.add("first_act_x");
        header.add("first_act_y");
        header.add("first_act_type");
        header.addAll(attributes);
        try (CSVPrinter csvPrinter = new CSVPrinter(IOUtils.getBufferedWriter(outputDirectoryHierarchy.getOutputFilename(Controler.DefaultFiles.personscsv)),
                CSVFormat.DEFAULT.withDelimiter(config.global().getDefaultDelimiter().charAt(0)).withHeader(header.stream().toArray(String[]::new)))) {
            for (Person p : scenario.getPopulation().getPersons().values()) {
                if (p.getSelectedPlan() == null) {
                    log.error("Found person without a selected plan: " + p.getId().toString() + " will not be added to output_persons.csv");
                    continue;
                }
                List<String> line = new ArrayList<>();
                line.add(p.getId().toString());
                line.add(p.getSelectedPlan().getScore() == null ? "null" : p.getSelectedPlan().getScore().toString());
                String x = "";
                String y = "";
                String actType = "";
                if (p.getSelectedPlan().getPlanElements().size() > 0) {
                    Activity firstAct = (Activity) p.getSelectedPlan().getPlanElements().get(0);
                    if (firstAct.getCoord() != null) {
                        x = Double.toString(firstAct.getCoord().getX());
                        y = Double.toString(firstAct.getCoord().getY());
                    }
                    actType = firstAct.getType();
                }
                line.add(x);
                line.add(y);
                line.add(actType);
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
        LogManager.getLogger(getClass()).info("...done");
    }

	private void writeActivityCSV(int iteration) {

		List<String> attributes = experiencedPlansService.getExperiencedPlans().values().stream()
				.flatMap(p -> TripStructureUtils.getActivities(p, TripStructureUtils.StageActivityHandling.ExcludeStageActivities).stream())
				.flatMap(act -> act.getAttributes().getAsMap().keySet().stream())
				.sorted().distinct().toList();

		List<String> header = new ArrayList<>();
		header.add("person");
		header.add("activity_number");
		header.add("activity_id");
		header.add("activity_type");
		header.add("start_time");
		header.add("end_time");
		header.add("maximum_duration");
		header.add("link_id");
		header.add("facility_id");

		header.add("coord_x");
		header.add("coord_y");

		header.addAll(attributes);

		try (CSVPrinter csvPrinter = new CSVPrinter(IOUtils.getBufferedWriter(outputDirectoryHierarchy.getIterationFilename(iteration, Controler.DefaultFiles.activitiescsv)),
				CSVFormat.DEFAULT.withDelimiter(config.global().getDefaultDelimiter().charAt(0)).withHeader(header.stream().toArray(String[]::new)))) {

			for (Map.Entry<Id<Person>, Plan> e : experiencedPlansService.getExperiencedPlans().entrySet()) {

				int i = 0;
				for (Activity act : TripStructureUtils.getActivities(e.getValue(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities)) {

					List<Object> line = new ArrayList<>();

					int id = ++i;
					line.add(e.getKey());
					line.add(id);
					line.add(e.getKey().toString() + "_" + id);
					line.add(act.getType());

					line.add(act.getStartTime().isDefined() ? act.getStartTime().seconds() : "");
					line.add(act.getEndTime().isDefined() ? act.getEndTime().seconds() : "");
					line.add(act.getMaximumDuration().isDefined() ? act.getMaximumDuration().seconds() : "");
					line.add(act.getLinkId() != null ? act.getLinkId() : "");
					line.add(act.getFacilityId() != null ? act.getFacilityId(): "");

					if (act.getCoord() != null) {
						line.add(act.getCoord().getX());
						line.add(act.getCoord().getY());
					} else  {
						line.add("");
						line.add("");
					}

					for (String attribute : attributes) {
						Object value = e.getValue().getAttributes().getAttribute(attribute);
						String result = value != null ? String.valueOf(value) : "";
						line.add(result);
					}

					csvPrinter.printRecord(line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
