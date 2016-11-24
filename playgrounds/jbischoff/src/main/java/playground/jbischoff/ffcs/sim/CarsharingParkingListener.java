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

/**
 * 
 */
package playground.jbischoff.ffcs.sim;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;

import playground.jbischoff.ffcs.manager.FreefloatingCarsharingManager;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CarsharingParkingListener implements IterationEndsListener {

	@Inject
	ParkingSearchManager manager;
	@Inject
	OutputDirectoryHierarchy output;
	@Inject 
	FreefloatingCarsharingManager ffmanager;

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationEndsListener#notifyIterationEnds(org.matsim.core.controler.events.IterationEndsEvent)
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		writeStats(manager.produceStatistics(), event.getIteration());
		ffmanager.reset(event.getIteration());
		manager.reset(event.getIteration());
	}

	/**
	 * @param produceStatistics
	 */
	private void writeStats(List<String> produceStatistics, int iteration) {
		BufferedWriter bw = IOUtils.getBufferedWriter(output.getIterationFilename(iteration, "parkingStats.csv"));
		try {
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
