/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionDataGrabber.java
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

package playground.christoph.evacuation.mobsim.decisiondata;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.io.IOUtils;

public class DecisionDataWriter implements AfterMobsimListener {

	public static final String personDecisionDataFile = "personDecisionData.txt.gz";
	public static final String householdDecisionDataFile = "householdDecisionData.txt.gz";

	public static final String newLine = "\n";
	public static final String delimiter = "\t";
	
	private final DecisionDataProvider decisionDataProvider;
	
	public DecisionDataWriter(DecisionDataProvider decisionDataProvider) {
		this.decisionDataProvider = decisionDataProvider;
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		String personFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), personDecisionDataFile);
		this.writePersonDecisionsToFile(personFile);
		
		String householdFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), householdDecisionDataFile);
		this.writeHouseholdDecisionsToFile(householdFile);
	}

	private void writePersonDecisionsToFile(String file) {
		
		try {
			BufferedWriter modelWriter = IOUtils.getBufferedWriter(file);
			
			writePersonHeader(modelWriter);
			writePersonRows(modelWriter);
			
			modelWriter.flush();
			modelWriter.close();			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writePersonHeader(BufferedWriter modelWriter) throws IOException {
		modelWriter.write("personId");
		modelWriter.write(delimiter);
		modelWriter.write("householdId");
		modelWriter.write(delimiter);
		modelWriter.write("isAffected");
		modelWriter.write(delimiter);
		modelWriter.write("hasChildren");
		modelWriter.write(delimiter);
		modelWriter.write("inPanic");
		modelWriter.write(delimiter);
		modelWriter.write("participating");
		modelWriter.write(delimiter);
		modelWriter.write("pickupDecision");
		modelWriter.write(delimiter);
		modelWriter.write("agentReturnHomeTime");
		modelWriter.write(delimiter);
		modelWriter.write("agentDirectEvacuationTime");
		modelWriter.write(delimiter);
		modelWriter.write("agentTransportMode");
		modelWriter.write(delimiter);
		modelWriter.write("agentReturnHomeVehicleId");
		modelWriter.write(newLine);
	}
	
	private void writePersonRows(BufferedWriter modelWriter) throws IOException {
		
		for (PersonDecisionData pdd : this.decisionDataProvider.getPersonDecisionData()) {		
			if (pdd.getPersonId() != null) modelWriter.write(pdd.getPersonId().toString());
			else modelWriter.write("null");
			modelWriter.write(delimiter);
			
			if (pdd.getHouseholdId() != null) modelWriter.write(pdd.getHouseholdId().toString());
			else modelWriter.write("null");
			modelWriter.write(delimiter);
			
			modelWriter.write(String.valueOf(pdd.isAffected()));
			modelWriter.write(delimiter);
			
			modelWriter.write(String.valueOf(pdd.hasChildren()));
			modelWriter.write(delimiter);
			
			modelWriter.write(String.valueOf(pdd.isInPanic()));
			modelWriter.write(delimiter);
			
			if (pdd.getParticipating() != null) modelWriter.write(pdd.getParticipating().toString());
			else modelWriter.write("null");
			modelWriter.write(delimiter);
			
			if (pdd.getPickupDecision() != null) modelWriter.write(pdd.getPickupDecision().toString());
			else modelWriter.write("null");
			modelWriter.write(delimiter);
			
			modelWriter.write(String.valueOf(pdd.getAgentReturnHomeTime()));
			modelWriter.write(delimiter);
			
			modelWriter.write(String.valueOf(pdd.getAgentDirectEvacuationTime()));
			modelWriter.write(delimiter);
			
			if (pdd.getAgentTransportMode() != null) modelWriter.write(pdd.getAgentTransportMode());
			else modelWriter.write("null");
			modelWriter.write(delimiter);
			
			if (pdd.getAgentReturnHomeVehicleId() != null) modelWriter.write(pdd.getAgentReturnHomeVehicleId().toString());
			else modelWriter.write("null");
			modelWriter.write(newLine);
		}
	}

	private void writeHouseholdDecisionsToFile(String file) {
		
		try {
			BufferedWriter modelWriter = IOUtils.getBufferedWriter(file);
			
			writeHouseholdHeader(modelWriter);
			writeHouseholdRows(modelWriter);
			
			modelWriter.flush();
			modelWriter.close();			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void writeHouseholdHeader(BufferedWriter modelWriter) throws IOException {
		modelWriter.write("householdId");
		modelWriter.write(delimiter);
		modelWriter.write("homeLinkId");
		modelWriter.write(delimiter);
		modelWriter.write("homeFacilityId");
		modelWriter.write(delimiter);
		modelWriter.write("meetingFacilityId");
		modelWriter.write(delimiter);
//		modelWriter.write("householdPosition");
//		modelWriter.write(delimiter);
		modelWriter.write("hasChildren");
		modelWriter.write(delimiter);
		modelWriter.write("participating");
		modelWriter.write(delimiter);
		modelWriter.write("homeFacilityIsAffected");
		modelWriter.write(delimiter);
		modelWriter.write("latestAcceptedLeaveTime");
		modelWriter.write(delimiter);
		modelWriter.write("householdReturnHomeTime");
		modelWriter.write(delimiter);
		modelWriter.write("householdEvacuateFromHomeTime");
		modelWriter.write(delimiter);
		modelWriter.write("householdDirectEvacuationTime");
		modelWriter.write(delimiter);
		modelWriter.write("departureTimeDelay");
		modelWriter.write(newLine);
	}
	
	private void writeHouseholdRows(BufferedWriter modelWriter) throws IOException {
		
		for (HouseholdDecisionData hdd : this.decisionDataProvider.getHouseholdDecisionData()) {
			if (hdd.getHouseholdId() != null) modelWriter.write(hdd.getHouseholdId().toString());
			else modelWriter.write("null");
			modelWriter.write(delimiter);
			
			if (hdd.getHomeLinkId() != null) modelWriter.write(hdd.getHomeLinkId().toString());
			else modelWriter.write("null");
			modelWriter.write(delimiter);
			
			if (hdd.getHomeFacilityId() != null) modelWriter.write(hdd.getHomeFacilityId().toString());
			else modelWriter.write("null");
			modelWriter.write(delimiter);
			
			if (hdd.getMeetingPointFacilityId() != null) modelWriter.write(hdd.getMeetingPointFacilityId().toString());
			else modelWriter.write("null");
			modelWriter.write(delimiter);

//			if (hdd.getHouseholdPosition() != null) modelWriter.write(hdd.getHouseholdPosition().toString());
//			else modelWriter.write("null");
//			modelWriter.write(delimiter);
						
			modelWriter.write(String.valueOf(hdd.hasChildren()));
			modelWriter.write(delimiter);
			
			if (hdd.getParticipating() != null) modelWriter.write(hdd.getParticipating().toString());
			else modelWriter.write("null");
			modelWriter.write(delimiter);
			
			modelWriter.write(String.valueOf(hdd.isHomeFacilityIsAffected()));
			modelWriter.write(delimiter);
			
			
			modelWriter.write(String.valueOf(hdd.getLatestAcceptedLeaveTime()));
			modelWriter.write(delimiter);
			
			modelWriter.write(String.valueOf(hdd.getHouseholdReturnHomeTime()));
			modelWriter.write(delimiter);
			
			modelWriter.write(String.valueOf(hdd.getHouseholdEvacuateFromHomeTime()));
			modelWriter.write(delimiter);
			
			modelWriter.write(String.valueOf(hdd.getHouseholdDirectEvacuationTime()));
			modelWriter.write(delimiter);
			
			modelWriter.write(String.valueOf(hdd.getDepartureTimeDelay()));
			modelWriter.write(newLine);
		}
	}
}
