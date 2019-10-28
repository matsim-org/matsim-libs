/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.vsp.andreas.utils.ana.filterStuckScores;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.andreas.utils.stats.RecursiveStatsContainer;


public class FilterStuckScores implements PersonStuckEventHandler{

	private final static Logger log = Logger.getLogger(FilterStuckScores.class);

	private Set<String> stuckAgentIds = new TreeSet<String>();
	private Set<String> tempIds;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFile = args[0];
		String eventsFilenamesList = args[1];
		String plansFilenamesList = args[2];
		String outputFolder = args[3];

		FilterStuckScores.filterStuckScores(networkFile, eventsFilenamesList, plansFilenamesList, outputFolder);

	}

	private static void filterStuckScores(String networkFile, String eventsFilenamesList, String plansFilenamesList, String outputFolder) {
		FilterStuckScores fsc = new FilterStuckScores();
		fsc.parseEventsForStuckAgentIds(eventsFilenamesList, outputFolder + "/");
		fsc.parsePopulationsForStuckAgentsScore(networkFile, plansFilenamesList, outputFolder + "/");
	}


	private void parsePopulationsForStuckAgentsScore(String networkFile, String plansFilenamesList, String outputFolder) {
		List<String> plansFiles = ReadFilenames.readFilenames(plansFilenamesList);

		BufferedWriter results = IOUtils.getBufferedWriter(outputFolder + "_scores_total.txt");

		try {
			results.write("# N stuck; avg. stuck; SD stuck; N not stuck; avg. not stuck; SD not stuck; filename");

			for (String planFile : plansFiles) {
				String filenameOnly = planFile.substring(planFile.lastIndexOf("/") + 1);

				BufferedWriter writerStuck = IOUtils.getBufferedWriter(outputFolder + filenameOnly + "_scores_stuck.txt");
				BufferedWriter writerNotStuck = IOUtils.getBufferedWriter(outputFolder + filenameOnly + "_scores_notStuck.txt");

				//header
				writerStuck.write("# Agent Id; Score - Stucks read from " + planFile);
				writerNotStuck.write("# Agent Id; Score - NotStuck read from " + planFile);

				RecursiveStatsContainer statsStuck = new RecursiveStatsContainer();
				RecursiveStatsContainer statsNotStuck = new RecursiveStatsContainer();

				Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
				new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
				new PopulationReader(sc).readFile(planFile);

				for (Person person : sc.getPopulation().getPersons().values()) {
					double score = person.getSelectedPlan().getScore().doubleValue();
					if (this.stuckAgentIds.contains(person.getId().toString())) {
						// person is stuck in at least one run
						statsStuck.handleNewEntry(score);
						writerStuck.newLine();
						writerStuck.write(person.getId() + "; " + score);
					} else {
						// person never stucks
						statsNotStuck.handleNewEntry(score);
						writerNotStuck.newLine();
						writerNotStuck.write(person.getId() + "; " + score);
					}
				}

				writerStuck.flush();
				writerNotStuck.flush();

				writerStuck.close();
				writerNotStuck.close();
				
				// write results to file
				results.newLine();
				results.write("" + statsStuck.getNumberOfEntries());
				results.write("; " + statsStuck.getMean());
				results.write("; " + statsStuck.getStdDev());
				results.write("; " + statsNotStuck.getNumberOfEntries());
				results.write("; " + statsNotStuck.getMean());
				results.write("; " + statsNotStuck.getStdDev());
				results.write("; " + filenameOnly);
			}
			
			results.flush();
			results.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void parseEventsForStuckAgentIds(String eventsFilenamesList, String outputFolder) {
		List<String> eventsFiles = ReadFilenames.readFilenames(eventsFilenamesList);
		
		for (String eventFile : eventsFiles) {
			EventsManager manager = EventsUtils.createEventsManager();
			manager.addHandler(this);
			this.reset(0);
			new MatsimEventsReader(manager).readFile(eventFile);
			
			String filenameOnly = eventFile.substring(eventFile.lastIndexOf("/") + 1);
			String filename = outputFolder + filenameOnly + "_stuckIds.txt";
			
			this.writeIdsToFile(filename, eventFile);

			// store them away
			this.stuckAgentIds.addAll(this.tempIds);
		}

		String filename = outputFolder + "_stuckIds_total.txt";
		this.tempIds = this.stuckAgentIds;
		this.writeIdsToFile(filename, "total");
	}

	@Override
	public void reset(int iteration) {
		this.tempIds = new TreeSet<String>();
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.tempIds.add(event.getPersonId().toString());
	}
	
	private void writeIdsToFile(String filename, String eventFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		
		try {
			//header
			writer.write("# Read " + this.tempIds.size() + " Ids from " + eventFile);

			//content
			for(String id: this.tempIds){
				writer.newLine();
				writer.write(id);
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
