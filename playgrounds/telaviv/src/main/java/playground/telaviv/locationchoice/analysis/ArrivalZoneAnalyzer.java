/* *********************************************************************** *
 * project: org.matsim.*
 * ArrivalZoneAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.locationchoice.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.telaviv.zones.ZoneMapping;

public class ArrivalZoneAnalyzer implements AgentArrivalEventHandler {

	private static final Logger log = Logger.getLogger(ArrivalZoneAnalyzer.class);
	
	private static String basePath = "../../matsim/mysimulations/telaviv/";
//	private static String runPath = "output_without_location_choice_0.10/";
//	private static String runPath = "output_JDEQSim_with_location_choice/";
	private static String runPath = "output_JDEQSim_with_location_choice_without_TravelTime/";
	
	private static String networkFile = basePath + "input/network.xml";
//	private static String populationFile = basePath + "input/plans_10.xml.gz";
//	private static String populationFile = basePath + runPath + "ITERS/it.0/0.plans.xml.gz";
	private static String populationFile = basePath + runPath + "ITERS/it.100/100.plans.xml.gz";
	
//	private String eventsFile = basePath + runPath + "/ITERS/it.0/0.events.txt.gz";
	private String eventsFile = basePath + runPath + "/ITERS/it.100/100.events.txt.gz";

	private String shoppingOutFile = basePath + runPath + "100.shoppingArrivalsCounter.txt";
	private String otherOutFile = basePath + runPath + "100.otherArrivalsCounter.txt";
	private String workOutFile = basePath + runPath + "100.workArrivalsCounter.txt";
	private String educationOutFile = basePath + runPath + "100.educationArrivalsCounter.txt";

	private String delimiter = "\t";
	private String lineBreak = "\n";
	private Charset charset = Charset.forName("UTF-8");
	
	private Scenario scenario;
	private ZoneMapping zoneMapping;

	private int maxHour = 48;
	private Map<Id, Integer> activityCounter;	// <PersonId, currently performed Activity Index>
	private Map<Integer, Map<Integer, Integer>> shoppingArrivals;	// Map<ZoneId, Map<Hour, ArrivalCount>>
	private Map<Integer, Map<Integer, Integer>> otherArrivals;	// Map<ZoneId, Map<Hour, ArrivalCount>>
	private Map<Integer, Map<Integer, Integer>> workArrivals;	// Map<ZoneId, Map<Hour, ArrivalCount>>
	private Map<Integer, Map<Integer, Integer>> educationArrivals;	// Map<ZoneId, Map<Hour, ArrivalCount>>
		
	public static void main(String[] args) {
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		// load network
		new MatsimNetworkReader(scenario).readFile(networkFile);
			
		// load population
		new MatsimPopulationReader(scenario).readFile(populationFile);

		new ArrivalZoneAnalyzer(scenario);
	}
	
	public ArrivalZoneAnalyzer(Scenario scenario) {
		this.scenario = scenario;
		
		log.info("Creating ZoneMapping...");
		zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84"));
		log.info("done.");

		log.info("Initialize data structure...");
		initMap();
		log.info("done.");
		
		TravelTimeCalculator travelTime = new TravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		if (eventsFile != null) {
			// We use a new EventsManager where we only register the TravelTimeCalculator.
			EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();
			eventsManager.addHandler(this);
			
			log.info("Processing events file to get initial travel times...");
			EventsReaderTXTv1 reader = new EventsReaderTXTv1(eventsManager);
			reader.readFile(eventsFile);
			
			eventsManager.removeHandler(travelTime);
			eventsManager = null;
		}	
		
		log.info("Writing data to file...");
		writeFile(shoppingOutFile, shoppingArrivals);
		writeFile(otherOutFile, otherArrivals);
		writeFile(workOutFile, workArrivals);
		writeFile(educationOutFile, educationArrivals);
		log.info("done.");
	}
	
	private void initMap() {
		shoppingArrivals = new HashMap<Integer, Map<Integer, Integer>>();
		otherArrivals = new HashMap<Integer, Map<Integer, Integer>>();
		workArrivals = new HashMap<Integer, Map<Integer, Integer>>();
		educationArrivals = new HashMap<Integer, Map<Integer, Integer>>();
		
		for (Integer TAZ : zoneMapping.getParsedZones().keySet()) {
			
			Map<Integer, Integer> shoppingHourMap = new HashMap<Integer, Integer>();
			Map<Integer, Integer> otherHourMap = new HashMap<Integer, Integer>();
			Map<Integer, Integer> workHourMap = new HashMap<Integer, Integer>();
			Map<Integer, Integer> educationHourMap = new HashMap<Integer, Integer>();
			for (int hour = 0; hour < maxHour; hour++) {
				shoppingHourMap.put(hour, 0);
				otherHourMap.put(hour, 0);
				workHourMap.put(hour, 0);
				educationHourMap.put(hour, 0);
			}
			
			shoppingArrivals.put(TAZ, shoppingHourMap);
			otherArrivals.put(TAZ, otherHourMap);
			workArrivals.put(TAZ, workHourMap);
			educationArrivals.put(TAZ, educationHourMap);
		}
		
		activityCounter = new HashMap<Id, Integer>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			activityCounter.put(person.getId(), 0);
		}
	}
	
	private void writeFile(String outFile, Map<Integer, Map<Integer, Integer>> arrivals) {
		FileOutputStream fos = null; 
		OutputStreamWriter osw = null; 
	    BufferedWriter bw = null;
		
	    try {
			fos = new FileOutputStream(outFile);
			osw = new OutputStreamWriter(fos, charset);
			bw = new BufferedWriter(osw);
			
			// write Header
			StringBuffer header = new StringBuffer();
			header.append("TAZ");
			header.append(delimiter);
			header.append("hour");
			header.append(delimiter);
			header.append("count");
			header.append(lineBreak);
			bw.write(header.toString());
			
			for (Integer TAZ : zoneMapping.getParsedZones().keySet()) {
				Map<Integer, Integer> hourMap = arrivals.get(TAZ);
				for (int hour = 0; hour < maxHour; hour++) {
					int count = hourMap.get(hour);
					
					StringBuffer dataLine = new StringBuffer();
					dataLine.append(TAZ);
					dataLine.append(delimiter);
					dataLine.append(hour);
					dataLine.append(delimiter);
					dataLine.append(count);
					dataLine.append(lineBreak);
					bw.write(dataLine.toString());
				}
			}
			
			bw.close();
			osw.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Person person = scenario.getPopulation().getPersons().get(event.getPersonId()); 
		int activityNum = activityCounter.get(event.getPersonId());
		activityNum = activityNum + 2;
		activityCounter.put(event.getPersonId(), activityNum);
		Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(activityNum);
		String activityType = activity.getType();
		
		Map<Integer, Map<Integer, Integer>> arrivals = null;
		if (activityType.contains("work")) arrivals = workArrivals;
		else if (activityType.contains("leisure")) arrivals = otherArrivals;
		else if (activityType.contains("shopping")) arrivals = shoppingArrivals;
		else if (activityType.contains("education")) arrivals = educationArrivals;
		else if (activityType.contains("tta")) return;	// it is a "home" arrivals - nothing more to count
		else if (activityType.contains("home")) return;	// it is a "home" arrivals - nothing more to count
		else log.info(activityType);
		
		Feature zone = zoneMapping.getLinkMapping().get(event.getLinkId());
		
		/*
		 * TTA agents from outside the Tel-Aviv area have no from zone - therefore
		 * we cannot find a value in the map.
		 * Those agents are marked by adding "tta" to their Id. If it is such an agent,
		 * we skip the event. Otherwise we process it and create a NullPointerException, 
		 * which should not occur... 
		 */
		if (zone == null) {
			if (event.getPersonId().toString().contains("tta")) return;
		}
		
		
		int TAZ = (Integer) zone.getAttribute(3);
		
		Map<Integer, Integer> zoneMap = arrivals.get(TAZ);
		int hour = (int) Math.ceil(event.getTime() / 3600);
		int count = zoneMap.get(hour);
		count++;
		zoneMap.put(hour, count);
	}

	@Override
	public void reset(int iteration) {
		
	}
}