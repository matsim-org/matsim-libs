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

import java.rmi.RemoteException;

import javax.swing.ProgressMonitor;

import org.matsim.core.utils.misc.Time;
import org.matsim.vis.otfvis.executables.OTFVisController;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote.TimePreference;


/**
 * A Thread implementation responsible for showing the "MobSim running to ..." Dialog when the user selects 
 * a new time in the input field for the time.
 * 
 * @author dstrippgen
 *
 */
public class OTFAbortGoto extends Thread  {
	public boolean terminate = false;
	private final OTFServerRemote host;
	private final int toTime;
	private int toIter = 0;
	private ProgressMonitor progressMonitor;
	private int actStatus = 0;
	private int actIter = 0;
	
	public OTFAbortGoto(OTFServerRemote host, int toTime, int toIter) {
		this.toTime = toTime;
		this.toIter = toIter;
		this.host = host;
	}

	@Override
	public void run() {
		int actTime = 0;
		try {
			actTime = host.getLocalTime();
			if(host.isLive()) {
				actStatus = ((OTFLiveServerRemote)host).getControllerStatus();
				actIter = OTFVisController.getIteration(actStatus);
			}
			
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		progressMonitor = new ProgressMonitor(null,
                "Running Simulation forward to " + Time.writeTime(toTime),
                "hat", actTime+3600*30*actIter, toTime+ 3600*30*toIter);

		while (!terminate) {
			try {
				sleep(500);
				int lastTime = actTime;
				actTime = host.getLocalTime();
				if(((lastTime > actTime) || (actTime == -1)) && (host.isLive())){
					actStatus = ((OTFLiveServerRemote)host).getControllerStatus();
					actIter = OTFVisController.getIteration(actStatus);
					actStatus = OTFVisController.getStatus(actStatus);
					if(actTime == -1) actTime = 0;
				}
				
				String message = String.format("Completed to Time: "+ Time.writeTime(actTime));
				if(actStatus == OTFVisController.RUNNING){
					message = String.format("Completed to Time: "+ actIter + "#" + Time.writeTime(actTime));
				} else if( actStatus == OTFVisController.REPLANNING){
					message = String.format("Completed to Iteration: "+ actIter + ": REPLANNING");
				} else {
					
				}
				progressMonitor.setNote(message);
				double pastMidnight = (actTime > 24*3600) ? (actTime -24*3600)/3600. : 0;
				int progress = (pastMidnight>0 ? 24*3600 + (int)(5*3600*(pastMidnight/(pastMidnight+1))) : actTime) + 3600*30*actIter;
				progressMonitor.setProgress(progress);
				if ( ((actIter >= toIter) && (actTime >= toTime)) || progressMonitor.isCanceled()) {
					terminate = true;
				}
				//System.out.println("Loc time " + actTime);
				if ((actTime < toTime) && terminate == true) {
					host.requestNewTime(actTime, TimePreference.EARLIER);
				}
			} catch (RemoteException e) {
				terminate = true;;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		progressMonitor.close();
	}

}
