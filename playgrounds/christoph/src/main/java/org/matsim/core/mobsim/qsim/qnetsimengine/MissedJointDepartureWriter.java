/* *********************************************************************** *
 * project: org.matsim.*
 * JointDepartureWriter.java
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.io.IOUtils;

public class MissedJointDepartureWriter implements AfterMobsimListener {

	private static final String newLine = "\n";
	private static String missedJointDeparturesFile = "missedJointDepartures.txt.gz";
		
	private final JointDepartureOrganizer jointDepartureOrganizer;
	private BufferedWriter bufferedWriter;
	
	public MissedJointDepartureWriter(JointDepartureOrganizer jointDepartureOrganizer) {
		this.jointDepartureOrganizer = jointDepartureOrganizer;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		// Write file containing all departures which have not been processed.
		Map<Id, JointDeparture> missedDepartures = new TreeMap<Id, JointDeparture>(this.jointDepartureOrganizer.scheduledDepartures);
		try {
			String file = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
					missedJointDeparturesFile);
			bufferedWriter = IOUtils.getBufferedWriter(file);
			for(JointDeparture jointDeparture : missedDepartures.values()) {
				bufferedWriter.write(jointDeparture.toString());
				bufferedWriter.write(newLine);
			}
			bufferedWriter.flush();
			bufferedWriter.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
