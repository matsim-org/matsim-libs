package org.matsim.utils.vis.otfivs.gui;

import java.rmi.RemoteException;

import javax.swing.ProgressMonitor;

import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.otfivs.interfaces.OTFServerRemote;
import org.matsim.utils.vis.otfivs.interfaces.OTFServerRemote.TimePreference;


public class OTFAbortGoto extends Thread  {
	public boolean terminate = false;
	private final OTFServerRemote host;
	private final int toTime;
	private ProgressMonitor progressMonitor;
	
	public OTFAbortGoto(OTFServerRemote host, int toTime) {
		this.toTime = toTime;
		this.host = host;
	}

	@Override
	public void run() {
		int actTime = 0;
		try {
			actTime = host.getLocalTime();
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		progressMonitor = new ProgressMonitor(null,
                "Running Simulation forward to " + Time.writeTime(toTime),
                "hat", actTime, toTime);

		while (!terminate) {
			try {
				sleep(500);

				actTime = host.getLocalTime();
				String message = String.format("Completed to Time: "+ Time.writeTime(actTime));
				progressMonitor.setNote(message);
				progressMonitor.setProgress(actTime);
				if ( actTime >= toTime || progressMonitor.isCanceled()) terminate = true;
				//System.out.println("Loc time " + actTime);
				if (host.getLocalTime() < toTime && terminate == true) {
					host.requestNewTime(actTime, TimePreference.EARLIER);
				}
			} catch (Exception e) {
				terminate = true;;
			}
		}
		progressMonitor.close();
	}

}
