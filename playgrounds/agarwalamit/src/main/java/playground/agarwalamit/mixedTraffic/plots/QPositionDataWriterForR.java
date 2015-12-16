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
package playground.agarwalamit.mixedTraffic.plots;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */
public class QPositionDataWriterForR {

	private static String outputDir ="../../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/"
			+ "run313/xtPlots/carWithoutHoles/";
	private static final String suffix = "events[400]";
	private static String eventFile = outputDir+"/events/"+suffix+".xml";
	private static String networkFile=outputDir+"/network.xml";
	
	private static Scenario scenario;
	private static QueuePositionCalculationHandler calculationHandler;
	private Map<Id<Person>,SortedMap<Double,String>> person2startTime2data;

	private final static Logger LOG = Logger.getLogger(QPositionDataWriterForR.class);

	public void run(){
		scenario  = LoadMyScenarios.loadScenarioFromNetwork(networkFile);

		calculationHandler = new QueuePositionCalculationHandler(scenario);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(calculationHandler);
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile(eventFile);
		this.person2startTime2data = calculationHandler.getPerson2StartTime2PersonPosition();
		writeData(outputDir);
		LOG.info("Writing file(s) is finished.");
	}
	
	public void writeData(final String outputFolder){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"rData_QueuePositions_"+suffix+".txt");
		try {
			writer.write("personId \t startPositionTime \t linkId \t startPosition \t endPositionTime \t endPosition \t travelMode \n");
			for(Id<Person> p : this.person2startTime2data.keySet()){
				for(Double d : this.person2startTime2data.get(p).keySet()){
					writer.write(p+"\t"+d+"\t"+this.person2startTime2data.get(p).get(d)+"\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason -"+ e);
		}
	}
	
	public static void main(String[] args) {
		new QPositionDataWriterForR().run();
	}
}