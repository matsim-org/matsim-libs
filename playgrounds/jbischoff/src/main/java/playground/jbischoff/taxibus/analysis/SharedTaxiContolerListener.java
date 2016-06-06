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

package playground.jbischoff.taxibus.analysis;

import javax.inject.Inject;

import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;

/**
 * @author  jbischoff
 *
 */
public class SharedTaxiContolerListener implements AfterMobsimListener, ShutdownListener{
	
	private OutputDirectoryHierarchy controlerIO;
	private SharedTaxiTripAnalyzer sharedTaxiTripAnalyzer;

	@Inject
	public SharedTaxiContolerListener(OutputDirectoryHierarchy controlerIO, SharedTaxiTripAnalyzer sharedTaxiTripAnalyzer)  {
		this.controlerIO  = controlerIO;
		this.sharedTaxiTripAnalyzer = sharedTaxiTripAnalyzer;
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		String filename = this.controlerIO.getOutputFilename("sharedTaxiStats.csv");
		sharedTaxiTripAnalyzer.writeAverageStats(filename);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		String filename = this.controlerIO.getIterationFilename(event.getIteration(), "sharedTaxiTrips.csv");
		
		sharedTaxiTripAnalyzer.writeStats(filename);
		sharedTaxiTripAnalyzer.aggregateRideTimes(event.getIteration());
	}

}
