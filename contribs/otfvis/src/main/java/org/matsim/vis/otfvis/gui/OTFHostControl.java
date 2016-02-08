/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import java.util.Collection;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.interfaces.OTFLiveServer;
import org.matsim.vis.otfvis.interfaces.OTFServer;

public class OTFHostControl {

	private static Logger log = Logger.getLogger(OTFHostControl.class);

	private BoundedRangeModel simTime;

	private int loopStart = 0;

	private int loopEnd = Integer.MAX_VALUE;

	private MovieTimer movieTimer = null;

	private OTFHostControlBar hostControlBar;

	private OTFServer server;

	public OTFHostControl(OTFServer server, OTFHostControlBar hostControlBar) {
		this.server = server;
		this.hostControlBar = hostControlBar;
		Collection<Double> steps = getTimeStepsdrawer();
		if (steps != null) {
			// Movie mode with timesteps
			Double[] dsteps = steps.toArray(new Double[steps.size()]);
			int min = dsteps[0].intValue();
			int max = dsteps[dsteps.length-1].intValue();
			simTime = new DefaultBoundedRangeModel(min, 0 /* extent */, min, max);
		} else {
			// Live mode without timesteps
			simTime = new DefaultBoundedRangeModel(0 /* value */, 0 /* extent */, 0 /* value */, Integer.MAX_VALUE /* max */);
		}

		simTime.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				invalidateDrawers();
				OTFHostControl.this.hostControlBar.updateTimeLabel();
			}

		});
	}

	public void toStart() {
		stopMovie();
		if(isLive()) {
			cancel();
			requestTimeStep(0, OTFServer.TimePreference.LATER);
			simTime.setValue(0);
		} else {
			requestTimeStep(loopStart, OTFServer.TimePreference.LATER);
			log.debug("To start...");
		}
	}

	private void cancel() {
		throw new RuntimeException("Can't do that at the moment.");
	}

	public void stopMovie() {
		if (movieTimer != null) {
			movieTimer.terminate();
			movieTimer = null;
		}
	}

	/**
	 * Called when user clicks on the time line displayed when playing movies.
	 */
	public void setNEWTime() {
		gotoTime(getSimTime(), null);
	}


	void gotoTime(int gotoTime, OTFAbortGoto progressBar) {
		// yy I have no clear idea what the following means.  Going backwards is only possible in file mode, 
		// and in consequence there may have been the idea to go backwards in live situations by restarting
		// at the beginning.  However, given our typically sizes this clearly does not make sense and
		// thus was never implemented.  In consequence, TimePreference.RESTART is only used
		// at two locations, one of them being here.  Where it is finally caught (in OnTheFlyServer), 
		// the result with earlier times is the same as when called with another time preference except
		// that the returned boolean may different, but I don't think that that is used in a consistent
		// way.  
		// I guess that the more complicated mechanics is necessary to "loop around" in file mode.
		// kai, jan'16
		
		if (gotoTime < getSimTime()){
//			requestTimeStep(gotoTime, OTFServer.TimePreference.RESTART);
			// I don't think that this is doing anything at all. ??? kai, jan'16
		} else if (!requestTimeStep(gotoTime, OTFServer.TimePreference.EARLIER)) {
			requestTimeStep(gotoTime, OTFServer.TimePreference.LATER);
		}
		if (progressBar != null) {
			progressBar.terminate = true;
		}
		fetchTimeAndStatus();
		hostControlBar.updateTimeLabel();
	}

	public void fetchTimeAndStatus() {
		int localTime = server.getLocalTime();
		simTime.setValue(localTime);
	}

	boolean requestTimeStep(int newTime, OTFServer.TimePreference prefTime) {
		if (requestNewTime(newTime, prefTime)) {
			simTime.setValue(server.getLocalTime());
			return true;
		} else {
			log.info("No such timestep found.");
			return false;
		}
	}

	boolean requestNewTime(int newTime, OTFServer.TimePreference prefTime) {
		boolean requestNewTime = server.requestNewTime(newTime, prefTime);
		return requestNewTime;
	}

	public boolean isLive() {
		return server.isLive();
	}

	public int getSimTime() {
		return simTime.getValue();
	}

	public BoundedRangeModel getSimTimeModel() {
		return simTime;
	}

	void setSimTime(int simTime) {
		this.simTime.setValue(simTime);
	}

	public void play(boolean synchronizedPlay) {
		log.debug("Pressed PLAY, creating movie timer.");
		movieTimer = new MovieTimer();
		movieTimer.start();
		if (!synchronizedPlay) {
			pressPlayOnServer();
		}
	}

	public void pause() {
		if (server.isLive()) {
			pressPauseOnServer();
		}
		stopMovie();
	}


	public Collection<Double> getTimeStepsdrawer() {
		return server.getTimeSteps();
	}


	private void pressPlayOnServer() {
		((OTFLiveServer) server).play();
	}

	private void pressPauseOnServer() {
		((OTFLiveServer) server).pause();
	}

	void updateSyncPlay(boolean synchronizedPlay) {
		if (!server.isLive()) {
			return;
		}
		if (synchronizedPlay) {
			pressPauseOnServer();
		} else {
			pressPlayOnServer();
		}
		fetchTimeAndStatus();
	}

	/**
	 *  sets the loop that the movieplayer should loop
	 * @param min either sec for startloop or -1 for leave unchanged default =0
	 * @param max either sec for endloop or -1 for leave unchanged default = Integer.MAX_VALUE
	 */
	public void setLoopBounds(int min, int max) {
		if (min != -1) {
			loopStart = min;
		}
		if (max != -1) {
			loopEnd = max;
		}
	}

	public void invalidateDrawers() {
		hostControlBar.redrawDrawers();
	}

	class MovieTimer extends Thread {

		private boolean terminate = false;

		public MovieTimer() {
			setDaemon(true);
		}

		public void terminate() {
			this.terminate = true;
		}

		@Override
		public void run() {
			int delay = 30;
			while (!terminate) {
				try {
					delay = OTFClientControl.getInstance().getOTFVisConfig().getDelay_ms();
					sleep(delay);
					if (hostControlBar.isSynchronizedPlay() && ((getSimTime() >= loopEnd) || !requestNewTime(getSimTime() + 1, OTFServer.TimePreference.LATER))) {
						requestNewTime(loopStart, OTFServer.TimePreference.LATER);
					}
					simTime.setValue(server.getLocalTime());
					SwingUtilities.invokeLater(new Runnable() { // This is important. Code below modifies stuff which is "owned" by swing, so it must run on the Swing thread! 
						
						int actTime = 0;
						
						@Override
						public void run() {
							actTime = getSimTime();
							hostControlBar.updateTimeLabel();
							if (simTime.getValue() != actTime) {
								hostControlBar.repaint();
							}
						}
					});
					
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

}
