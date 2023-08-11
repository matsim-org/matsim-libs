/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingsearch.evaluation;


import com.google.inject.Inject;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;


import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author  jbischoff
 *
 */

public class ParkingListener implements IterationEndsListener {

	@Inject
	ParkingSearchManager manager;
	@Inject
	OutputDirectoryHierarchy output;

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationEndsListener#notifyIterationEnds(org.matsim.core.controler.events.IterationEndsEvent)
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		writeStats(manager.produceStatistics(), event.getIteration());
		writeStatsByTimesteps(((FacilityBasedParkingManager)manager).produceTimestepsStatistics(), event.getIteration());
		manager.reset(event.getIteration());
	}

	private void writeStatsByTimesteps(List<String> produceBeneStatistics, int iteration) {
		BufferedWriter bw = IOUtils.getBufferedWriter(output.getIterationFilename(iteration, "parkingStatsPerTimeSteps.csv"));
		try {

			String header = "time;rejectedReservations;foundParking;unpark";
			bw.write(header);
			bw.newLine();
			for (String s : produceBeneStatistics){
				bw.write(s);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * @param produceStatistics
	 */
	private void writeStats(List<String> produceStatistics, int iteration) {
		BufferedWriter bw = IOUtils.getBufferedWriter(output.getIterationFilename(iteration, "parkingStats.csv"));
		try {

			String header = "linkId;X;Y;parkingFacility;capacity;EndOccupation;reservationsRequests;numberOfParkedVehicles;rejectedReservations";
			bw.write(header);
			bw.newLine();
			for (String s : produceStatistics){
				bw.write(s);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

}
