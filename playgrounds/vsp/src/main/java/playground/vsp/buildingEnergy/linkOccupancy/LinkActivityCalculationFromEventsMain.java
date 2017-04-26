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
package playground.vsp.buildingEnergy.linkOccupancy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;


/**
 * Simple main-routine that parse an eventsfile, using {@link LinkActivityOccupancyCounter}. 
 * The generated data is written to csv-tables.
 * @author droeder
 *
 */
public final class LinkActivityCalculationFromEventsMain {

	private static final Logger log = Logger
			.getLogger(LinkActivityCalculationFromEventsMain.class);

	private LinkActivityCalculationFromEventsMain() {
	}

	private static final String DIR = "E:\\VSP\\svn\\shared-svn\\studies\\droeder\\buildingEnergy\\runs\\";
	private static final String RUN = "2kW.15";
	
	private static String[] ARGS = new String[]{
			DIR + RUN + "\\" + RUN + ".output_network.xml.gz",
			DIR + RUN + "\\ITERS\\it.1000\\" + RUN + ".1000.plans.xml.gz",
			DIR + RUN + "\\ITERS\\it.1000\\" + RUN + ".1000.events.xml.gz",
			DIR + RUN + "\\",
			"900",
			"86400",
			RUN
	};
	
	private static Counter he= new Counter("home events # ");
	private static Counter we = new Counter("work events # ");
	private static Counter hp= new Counter("home plans # ");
	private static Counter wp = new Counter("work plans # ");
	
	private static final String PREFIX = "activityCountOnLinks";
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length == 0) args = ARGS;
		if(args.length != 7){
			throw new IllegalArgumentException("expecting 6 arguments {networkFile, plansFile, eventsFile, outputPath, timeSliceSize, tmax, runId");
		}
		String networkFile = args[0];
		String plansFile = args[1];
		String eventsFile = args[2];
		String outputPath = new File(args[3]).getAbsolutePath() + System.getProperty("file.separator");
		int td = Integer.parseInt(args[4]);
		int tmax = Integer.parseInt(args[5]);
		String runId = args[6];
		//catch logEntries
		OutputDirectoryLogging.initLogging(new OutputDirectoryHierarchy(outputPath, runId + "." + PREFIX, OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles, false));
		OutputDirectoryLogging.catchLogEntries();
		Gbl.enableThreadCpuTimeMeasurement();
		// dump input-parameters
		log.info("running class: " + System.getProperty("sun.java.command"));
//			LinkActivityCalculationFromEventsMain.class.getCanonicalName().toString());
		log.info("networkFile: " + networkFile);
		log.info("plansFile: " + plansFile);
		log.info("eventsFile: " + eventsFile);
		log.info("outputPath: " + outputPath);
		log.info("timeslice-duration: " + td);
		log.info("tmax: " + tmax);

		Scenario sc = prepareScenario(plansFile, networkFile);
		log.info("init occupancy-counter.");
		Map<String, LinkActivityOccupancyCounter> home = initOccupancyCounter("home", td, tmax, sc.getPopulation());
		Map<String, LinkActivityOccupancyCounter> work= initOccupancyCounter("work", td, tmax, sc.getPopulation());
		log.info("finished (init occupancy-counter).");
		parseEvents(home, work, eventsFile);
		hp.printCounter();
		wp.printCounter();
		he.printCounter();
		we.printCounter();
		finishHandler(home,work);
		// not sure if this is necessary, but anyway, sort link-ids so we have a predictable order.
		List<Id> linkIds = new ArrayList<Id>(sc.getNetwork().getLinks().keySet());
		Collections.sort(linkIds);
		dump(outputPath, "home", home, linkIds, tmax, td, runId);
		dump(outputPath, "work", work, linkIds, tmax, td, runId);
		Gbl.printCurrentThreadCpuTime();
		log.info("finished");
	}

	/**
	 * @param eventsFile 
	 * @param work 
	 * @param home 
	 * 
	 */
	private static void parseEvents(Map<String, LinkActivityOccupancyCounter> home, Map<String, LinkActivityOccupancyCounter> work, String eventsFile) {
		log.info("parsing events.");
		EventsManager manager = EventsUtils.createEventsManager();
		for(LinkActivityOccupancyCounter v: home.values()){
			manager.addHandler(v);
		}
		for(LinkActivityOccupancyCounter v: work.values()){
			manager.addHandler(v);
		}
		
		manager.addHandler(new ActivityStartEventHandler() {
			
			@Override
			public void reset(int iteration) {
				
			}
			
			@Override
			public void handleEvent(ActivityStartEvent event) {
				if(event.getActType().equals("home")) he.incCounter();
				if(event.getActType().equals("work")) we.incCounter();
			}
		});
		new MatsimEventsReader(manager).readFile(eventsFile);
		log.info("finished (parsing events).");		
	}

	/**
	 * @param string
	 * @param td
	 * @param tmax
	 * @return
	 */
	private static Map<String, LinkActivityOccupancyCounter> initOccupancyCounter(
			String string, int td, int tmax, Population p) {
		Map<String, LinkActivityOccupancyCounter> map = new HashMap<String, LinkActivityOccupancyCounter>();
		for(int i = 0; i < tmax ; i += td){
			map.put(String.valueOf(i), new LinkActivityOccupancyCounter(p, i, i + td , string));
		}
		map.put(">"+String.valueOf(tmax), new LinkActivityOccupancyCounter(p, tmax, Integer.MAX_VALUE, string));
		map.put("all", new LinkActivityOccupancyCounter(p, 0, Integer.MAX_VALUE, string));
		return map;
	}

	/**
	 * @param plansFile
	 * @return
	 */
	private static Scenario prepareScenario(String plansFile, String networkFile) {
		log.info("load scenario-data.");
		MutableScenario sc = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
//		final Population population = (Population) sc.getPopulation();
		StreamingPopulationReader reader = new StreamingPopulationReader( sc ) ;
		reader.addAlgorithm(new PersonAlgorithm() {
			
			@Override
			public void run(Person person) {
				for(int i = 1; i < person.getSelectedPlan().getPlanElements().size(); i++){
					if(person.getSelectedPlan().getPlanElements().get(i) instanceof Activity){
						Activity a = (Activity) person.getSelectedPlan().getPlanElements().get(i);
						if(a.getType().equals("home")) hp.incCounter();
						if(a.getType().equals("work")) wp.incCounter();
					}
				}
			}
		});
//		new PopulationReader(sc).readFile(plansFile);
//		reader.runAlgorithms();
		reader.readFile(plansFile);
		log.info("finished (load scenario-data).");
		return sc;
	}

	/**
	 * @param home
	 * @param work
	 */
	private static void finishHandler(Map<String, LinkActivityOccupancyCounter> home,
			Map<String, LinkActivityOccupancyCounter> work) {
		for(LinkActivityOccupancyCounter laoc : home.values()){
			laoc.finish();
		}
		for(LinkActivityOccupancyCounter laoc : work.values()){
			laoc.finish();
		}
	}

	/**
	 * @param outputPath
	 * @param values
	 * @param linkIds
	 */
	private static void dump(String outputPath, String name,
			Map<String, LinkActivityOccupancyCounter> values, List<Id> linkIds, int tmax , int td, String runId) {
		log.info("writing " + name + "-activities.");
		BufferedWriter w = IOUtils.getBufferedWriter(outputPath + runId + "." + PREFIX + "." + name + ".csv.gz");
		try {
			//write header
			w.write("linkId;");
			for(int i = 0; i < tmax ; i += td){
				w.write(String.valueOf(i) + ";");
			}
			w.write(">" + String.valueOf(tmax) + ";all;");
			w.write("\n");
			//write data
			for(Id id: linkIds){
				w.write(id.toString() + ";");
				for(int i = 0; i < tmax ; i += td){
					w.write(values.get(String.valueOf(i)).getMaximumOccupancy(id) + ";");
				}
				w.write(values.get(">" + String.valueOf(tmax)).getMaximumOccupancy(id) + ";");
				w.write(values.get("all").getMaximumOccupancy(id) + ";");
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

