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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;
import playground.agarwalamit.mixedTraffic.plots.LinkPersonInfoContainer.PersonInfoChecker;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */
public class QPositionDataWriterForR {

	private static String outputDir ="../../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/run308/carBike/carBikePassing/";
	private static String eventFile = outputDir+"/events.xml";
	private static String networkFile=outputDir+"/network.xml";
	
	private static Scenario scenario;
	private static QueuePositionCalculationHandler calculationHandler;

	private final static Logger logger = Logger.getLogger(QPositionDataWriterForR.class);

	public void run(){
		scenario  = LoadMyScenarios.loadScenarioFromNetwork(networkFile);

		calculationHandler = new QueuePositionCalculationHandler(scenario);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(calculationHandler);
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile(eventFile);
		writeLinkEnterLeaveQueuePosDataForR();
		writeLinkEnterLeaveTimeForR();
		logger.info("Writing file(s) is finished.");
	}
	
	public static void main(String[] args) {
		new QPositionDataWriterForR().run();
	}

	private static void writeLinkEnterLeaveQueuePosDataForR(){
		List<PersonInfoChecker> qPositionData = calculationHandler.getPersonLinkEnterTimeVehiclePositionDataToWrite();
		List<PersonInfoChecker> linkEnterLeaveTimeData = calculationHandler.getPersonLinkEnterLeaveTimeDataToWrite();
		List<PersonInfoChecker> copyLinkEnterLeaveTimeData = new ArrayList<PersonInfoChecker>(linkEnterLeaveTimeData);
		
		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/rDataPersonInQueueData6.txt");
		double vehicleSpeed =0;
		try {
			writer.write("personId \t linkId \t startTimeX1 \t initialPositionY1 \t endTimeX2 \t endPositionY2 \t travelMode \n");

			for(PersonInfoChecker checker : qPositionData){
				EnteringPersonInfo enteredPerson = checker.getEnteredPersonInfo();
				String personId = enteredPerson.getPersonId().toString();
				String linkId = enteredPerson.getLink().getId().toString();
				double linkEnterTime = enteredPerson.getLinkEnterTime();
				double queuingTime = checker.getQueuingTime();
				double linkLength = enteredPerson.getLink().getLength();
				String travelMode = enteredPerson.getLegMode();
				double linkLeaveTime = checker.getLeftPersonInfo().getLinkLeaveTime();

				vehicleSpeed = MixedTrafficVehiclesUtils.getSpeed(travelMode);
				
				double initialPos = Double.valueOf(linkId) * linkLength;
				double qStartTime = queuingTime;
				double qStartDistFromFNode = initialPos + (qStartTime- linkEnterTime) * vehicleSpeed;
				if((qStartDistFromFNode-initialPos) > linkLength){
					qStartDistFromFNode=initialPos + linkLength;
				}
				double timeStepTillFreeSpeed = qStartTime;
				double endOfLink = (1+Double.valueOf(linkId)) * linkLength;

				// first line will write the distance and time for which speed was free flow speed.
				// next line will write the queue distance and link leave time.
				writer.write(personId+"\t"+linkId+"\t"+linkEnterTime+"\t"+initialPos+"\t"+timeStepTillFreeSpeed+"\t"+qStartDistFromFNode+"\t"+travelMode+"\n");
				writer.write(personId+"\t"+linkId+"\t"+timeStepTillFreeSpeed+"\t"+qStartDistFromFNode+"\t"+(Double.valueOf(linkLeaveTime))+"\t"+endOfLink+"\t"+travelMode+"\n");
				copyLinkEnterLeaveTimeData.remove(checker);
			}

			for(PersonInfoChecker checker : copyLinkEnterLeaveTimeData){
				EnteringPersonInfo enteredPerson = checker.getEnteredPersonInfo();
				String personId = enteredPerson.getPersonId().toString();
				String linkId = enteredPerson.getLink().getId().toString();
				double linkEnterTime = enteredPerson.getLinkEnterTime();
				double linkLength = enteredPerson.getLink().getLength();
				String travelMode = enteredPerson.getLegMode();
				double linkLeaveTime = checker.getLeftPersonInfo().getLinkLeaveTime();
				
				double initialPos = Double.valueOf(linkId) * linkLength;
				double finalPos = ( 1 + Double.valueOf(linkId))* linkLength;
				writer.write(personId+"\t"+linkId+"\t"+linkEnterTime+"\t"+initialPos+"\t"+linkLeaveTime+"\t"+finalPos+"\t"+travelMode+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written in file. Reason : "+e);
		}
	}

	private static void writeLinkEnterLeaveTimeForR(){

		List<PersonInfoChecker> linkEnterLeaveTimeDataList = calculationHandler.getPersonLinkEnterLeaveTimeDataToWrite();
		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/rDataPersonLinkEnterLeave.txt");
		try {
			writer.write("personId \t linkId \t linkEnterTimeX1 \t initialPositionY1 \t linkLeaveTimeX2 \t endPositionY2 \t travelMode \n");
			for(PersonInfoChecker checker : linkEnterLeaveTimeDataList){
				EnteringPersonInfo enteredPerson = checker.getEnteredPersonInfo();
				String personId = enteredPerson.getPersonId().toString();
				String linkId = enteredPerson.getLink().getId().toString();
				double linkEnterTime = enteredPerson.getLinkEnterTime();
				double linkLength = enteredPerson.getLink().getLength();
				String travelMode = enteredPerson.getLegMode();
				double linkLeaveTime = checker.getLeftPersonInfo().getLinkLeaveTime();

				double initialPos = Double.valueOf(linkId)*Double.valueOf(linkLength);
				double finalPos = (1+Double.valueOf(linkId))*Double.valueOf(linkLength);
				writer.write(personId+"\t"+linkId+"\t"+linkEnterTime+"\t"+initialPos+"\t"+linkLeaveTime+"\t"+finalPos+"\t"+travelMode+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written in file. Reason : "+e);
		}
	}
}