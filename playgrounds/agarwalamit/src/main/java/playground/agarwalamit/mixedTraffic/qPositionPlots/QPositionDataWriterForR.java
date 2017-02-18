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
package playground.agarwalamit.mixedTraffic.qPositionPlots;

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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */
public class QPositionDataWriterForR {

	private static final String outputDir ="../../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/run313/singleModes/withoutHoles/car_SW//";
	private static final String SUFFIX = "events[120]";
	private static final String eventFile = outputDir+"/events/"+SUFFIX+".xml";
	private static final String networkFile=outputDir+"/network.xml";
	
	private static Scenario scenario;
	private static QueuePositionCalculationHandler calculationHandler;
	private Map<Id<Person>,SortedMap<Double,String>> person2startTime2data;
	private Map<Id<Person>, SortedMap<Double, String>> person2StartTime2AccumulatedPos;
	private Tuple<Id<Person>, Double> lastDepartedPerson = new Tuple<>(null, 0.);
	private final boolean isWritingDataAfterDepartureOfLastPerson = false;
	
	private final static Logger LOG = Logger.getLogger(QPositionDataWriterForR.class);

	public void run(){
		scenario  = LoadMyScenarios.loadScenarioFromNetwork(networkFile);

		calculationHandler = new QueuePositionCalculationHandler(scenario);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(calculationHandler);
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile(eventFile);
		this.person2startTime2data = calculationHandler.getPerson2StartTime2PersonQPosition();
		this.person2StartTime2AccumulatedPos = calculationHandler.getPerson2StartTime2AccumulatedPosition();
		this.lastDepartedPerson = calculationHandler.getLastDepartedPersonAndTime();
		writeData(outputDir+"rData_QueuePositions_"+SUFFIX+".txt", this.person2startTime2data);
		writeData(outputDir+"rData_AccumulatedPositions_"+SUFFIX+".txt", this.person2StartTime2AccumulatedPos);
		
		LOG.info("Writing file(s) is finished.");
	}
	
	public void writeData(final String outputFile, final Map<Id<Person>, SortedMap<Double, String>> inData){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("personId \t startPositionTime \t linkId \t startPosition \t endPositionTime \t endPosition \t travelMode \n");
			for(Id<Person> p : inData.keySet()){
				for(Double d : inData.get(p).keySet()){
					if(isWritingDataAfterDepartureOfLastPerson && d <=  this.lastDepartedPerson.getSecond()) continue;
					writer.write(p+"\t"+d+"\t"+inData.get(p).get(d)+"\n");
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