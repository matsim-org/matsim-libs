/* *********************************************************************** *
 * project: org.matsim.*
 * OTFAbortGoto.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.gui;

import javax.swing.ProgressMonitor;

import org.matsim.core.utils.misc.Time;
import org.matsim.vis.otfvis.interfaces.OTFServer;


/**
 * A Thread implementation responsible for showing the "MobSim running to ..." Dialog when the user selects
 * a new time in the input field for the time.
 *
 * @author dstrippgen
 */
public class OTFAbortGoto extends Thread  {
	public boolean terminate = false;
	private final OTFServer host;
	private final int toTime;

	public OTFAbortGoto(OTFServer host, int toTime) {
		this.toTime = toTime;
		this.host = host;
	}

	@Override
	public void run() {
		int actTime = host.getLocalTime();
		int from = actTime;
		int to = toTime;
		// this is a reset! start from 00:00:00
		if(from > to) from = 0;

		ProgressMonitor progressMonitor = new ProgressMonitor(null,
				"Running Simulation forward to " + Time.writeTime(toTime),
				"hat", from, to);

		while (!terminate) {
			try {
				sleep(500);
				int lastTime = actTime;
				actTime = host.getLocalTime();
				if(((lastTime > actTime) || (actTime == -1)) && (host.isLive())){
					if(actTime == -1) actTime = 0;
				}

				String message = "Completed to Time: "+ Time.writeTime(actTime);
				progressMonitor.setNote(message);
				double pastMidnight = (actTime > 24*3600) ? (actTime -24*3600)/3600. : 0;
				int progress = (pastMidnight>0 ? 24*3600 + (int)(5*3600*(pastMidnight/(pastMidnight+1))) : actTime);
				progressMonitor.setProgress(progress);
				if ( (actTime >= toTime) || progressMonitor.isCanceled()) {
					terminate = true;
				}
				if ((actTime < toTime) && terminate) {
					host.requestNewTime(actTime);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		progressMonitor.close();
	}

}
