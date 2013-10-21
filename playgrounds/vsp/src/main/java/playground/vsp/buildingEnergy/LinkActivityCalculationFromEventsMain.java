/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.buildingEnergy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * Simple main-routine that parse an eventsfile, using {@link LinkActivityOccupancyCounter}
 * (one per half hour). The generated data is written to csv-tables.
 * @author droeder
 *
 */
public final class LinkActivityCalculationFromEventsMain {

	private static final Logger log = Logger
			.getLogger(LinkActivityCalculationFromEventsMain.class);

	private LinkActivityCalculationFromEventsMain() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 4){
			throw new IllegalArgumentException("expecting 4 arguments {networkFile, plansFile, eventsFile, outputPath");
		}
		String networkFile = args[0];
		String plansFile = args[1];
		String eventsFile = args[2];
		String outputPath = new File(args[3]).getAbsolutePath() + System.getProperty("file.separator");
		//catch logEntries
		OutputDirectoryLogging.initLogging(new OutputDirectoryHierarchy(outputPath, "activityOnLinks", true, false));
		OutputDirectoryLogging.catchLogEntries();
		// dump input-parameters
		log.info("running class: " + LinkActivityCalculationFromEventsMain.class.getCanonicalName().toString());
		log.info("networkFile: " + networkFile);
		log.info("plansFile: " + plansFile);
		log.info("eventsFile: " + eventsFile);
		log.info("outputPath: " + outputPath);
		// load data
		log.info("load scenario-data.");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(networkFile);
		new MatsimPopulationReader(sc).readFile(plansFile);
		log.info("finished (load scenario-data).");
		// create occupancyCounters
		log.info("init occupancy-counter.");
		Map<String, LinkActivityOccupancyCounter> home = new HashMap<String, LinkActivityOccupancyCounter>();
		Map<String, LinkActivityOccupancyCounter> work= new HashMap<String, LinkActivityOccupancyCounter>();
		for(int i = 0; i < 24 * 3600; i += 1800){
			home.put(String.valueOf(i), new LinkActivityOccupancyCounter(sc.getPopulation(), 0, i + 1800 - 1, "???"));
			work.put(String.valueOf(i), new LinkActivityOccupancyCounter(sc.getPopulation(), 0, i + 1800 - 1, "???"));
		}
		log.info("finished (init occupancy-counter).");
		//init eventhandler
		EventsManager manager = EventsUtils.createEventsManager();
		for(LinkActivityOccupancyCounter v: home.values()){
			manager.addHandler(v);
		}
		for(LinkActivityOccupancyCounter v: work.values()){
			manager.addHandler(v);
		}
		//parse events
		log.info("start parsing events.");
		new MatsimEventsReader(manager).readFile(eventsFile);
		log.info("finished (start parsing events).");
		// not sure if this is necessary, but anyway, sort links so we have a predictable order.
		List<Id> linkIds = new ArrayList<Id>(sc.getNetwork().getLinks().keySet());
		Collections.sort(linkIds);
		dump(outputPath, "home", home, linkIds);
		dump(outputPath, "work", work, linkIds);
		log.info("finished");
	}

	/**
	 * @param outputPath
	 * @param values
	 * @param linkIds
	 */
	private static void dump(String outputPath, String name,
			Map<String, LinkActivityOccupancyCounter> values, List<Id> linkIds) {
		log.info("writing " + name + "-activities.");
		BufferedWriter w = IOUtils.getBufferedWriter(outputPath + "activityCount." + name + "csv.gz");
		try {
			//write header
			w.write("timeBin;");
			for(Id id: linkIds){
				w.write(id.toString() + ";");
			}
			w.write("\n");
			//write data
			for(Entry<String,LinkActivityOccupancyCounter> e: values.entrySet()){
				w.write(e.getKey().toString() + ";");
				for(Id id: linkIds){
					w.write(id.toString() + ";");
				}
				w.write("\n");
			}
			w.flush();
			w.close();
			log.info("finished (writing " + name + "-activities.)");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}

