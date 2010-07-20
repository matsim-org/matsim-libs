package org.matsim.vis.otfvis.gui;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFVisControlerListener;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;

public class OTFHostControl {

	private static Logger log = Logger.getLogger(OTFHostControl.class);

	private final OTFHostConnectionManager masterHostConnectionManager;

	private final List <OTFHostConnectionManager> hostControls = new ArrayList<OTFHostConnectionManager>();

	private int simTime = 0;

	private int loopStart = 0;

	private int loopEnd = Integer.MAX_VALUE;

	private int controllerStatus = 0;

	private MovieTimer movieTimer = null;

	private OTFHostControlBar hostControlBar;

	public OTFHostControl(OTFHostConnectionManager masterHostConnectionManager, OTFHostControlBar hostControlBar) {
		this.masterHostConnectionManager = masterHostConnectionManager;
		this.hostControlBar = hostControlBar;
	}

	public void toStart() {
		stopMovie();
		if(isLive()) {
			cancel();
			requestTimeStep(0, OTFServerRemote.TimePreference.LATER);
			simTime = 0;
		} else {
			requestTimeStep(loopStart, OTFServerRemote.TimePreference.LATER);
			log.debug("To start...");
		}
	}

	private void cancel() {
		try {
			((OTFLiveServerRemote) this.masterHostConnectionManager.getOTFServer()).requestControllerStatus(OTFVisControlerListener.CANCEL);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void stopMovie() {
		if (movieTimer != null) {
			movieTimer.terminate();
			movieTimer = null;
		}
	}

	/**
	 * Called when user clicks on the time line displayed when playing movies.
	 *
	 * @param newTime_s
	 */
	public void setNEWTime(int newTime_s) {
		if (newTime_s != getSimTime()) {
			stopMovie();
			gotoTime(newTime_s, null);
		}
	}


	void gotoTime(int gotoTime, OTFAbortGoto progressBar) {
		boolean restart = gotoTime < getSimTime();
		if (restart){
			requestTimeStep(gotoTime, OTFServerRemote.TimePreference.RESTART);
		} else if (!requestTimeStep(gotoTime, OTFServerRemote.TimePreference.EARLIER)) {
			requestTimeStep(gotoTime, OTFServerRemote.TimePreference.LATER);
		}
		if (progressBar != null) {
			progressBar.terminate = true;
		}
		fetchTimeAndStatus();
		hostControlBar.updateTimeLabel();
	}

	void fetchTimeAndStatus() {
		try {
			simTime = this.masterHostConnectionManager.getOTFServer().getLocalTime();
			if(controllerStatus != OTFVisControlerListener.NOCONTROL){
				controllerStatus = ((OTFLiveServerRemote) this.masterHostConnectionManager.getOTFServer()).getControllerStatus();
			}
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	boolean requestTimeStep(int newTime, OTFServerRemote.TimePreference prefTime) {
		try {
			if (requestNewTime(newTime, prefTime)) {
				simTime = masterHostConnectionManager.getOTFServer().getLocalTime();
				for(OTFHostConnectionManager slave : hostControls) {
					if (!slave.equals(this.masterHostConnectionManager))
						slave.getOTFServer().requestNewTime(newTime, prefTime);
				}
				return true;
			} else {
				log.info("No such timestep found.");
				return false;
			}
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	boolean requestNewTime(int newTime, OTFServerRemote.TimePreference prefTime) {
		try {
			return masterHostConnectionManager.getOTFServer().requestNewTime(newTime, prefTime);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isLive() {
		return masterHostConnectionManager.isLiveHost();
	}

	public int getSimTime() {
		return simTime;
	}

	void setSimTime(int simTime) {
		this.simTime = simTime;
	}

	int getControllerStatus() {
		return controllerStatus;
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
		if (masterHostConnectionManager.isLiveHost()) {
			pressPauseOnServer();
		}
		stopMovie();
	}


	public Collection<Double> getTimeStepsdrawer() {
		try {
			return this.masterHostConnectionManager.getOTFServer().getTimeSteps();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}


	private void pressPlayOnServer() {
		try {
			((OTFLiveServerRemote) masterHostConnectionManager.getOTFServer()).play();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	void pressPauseOnServer() {
		try {
			((OTFLiveServerRemote) masterHostConnectionManager.getOTFServer()).pause();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	void updateSyncPlay(boolean synchronizedPlay) {
		if (!masterHostConnectionManager.isLiveHost()) {
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
			int actTime = 0;
			while (!terminate) {
				try {
					delay = OTFClientControl.getInstance().getOTFVisConfig().getDelay_ms();
					sleep(delay);
					if (hostControlBar.isSynchronizedPlay() && ((getSimTime() >= loopEnd) || !requestNewTime(getSimTime() + 1, OTFServerRemote.TimePreference.LATER))) {
						requestNewTime(loopStart, OTFServerRemote.TimePreference.LATER);
					}
					actTime = getSimTime();
					simTime = masterHostConnectionManager.getOTFServer().getLocalTime();
					for (OTFHostConnectionManager slave : hostControls) {
						if (!slave.equals(masterHostConnectionManager)) {
							slave.getOTFServer().requestNewTime(simTime, OTFServerRemote.TimePreference.LATER);
						}
					}
					hostControlBar.updateTimeLabel();
					if (simTime != actTime) {
						hostControlBar.repaint();
						hostControlBar.invalidateDrawers();
					}
				} catch (RemoteException e) {
					throw new RuntimeException(e);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

}
