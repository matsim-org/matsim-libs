/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyServer.java
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

package org.matsim.vis.otfvis;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.otfvis.io.OTFLaneWriter;
import org.matsim.pt.otfvis.FacilityDrawer;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.otfvis.io.OTFSignalWriter;
import org.matsim.signalsystems.otfvis.io.SignalGroupStateChangeTracker;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFQueryRemote;
import org.matsim.vis.otfvis.opengl.queries.AbstractQuery;
import org.matsim.vis.snapshots.writers.VisMobsimFeature;

/**
 * OnTheFlyServer is the live server of the OTFVis.
 * it is injected into a running QueueSimulation by either overloading the
 * simulation's afterSimStep() method or via the external trigger events.
 * The simulation should call on every tick the method updateStatus().
 * Then the server can stop the simulation run whenever wanted.
 * The interface to the client is given by the OTFLiveServerRemote interface.
 *
 * @author dstrippgen
 *
 */
public class OnTheFlyServer implements OTFLiveServerRemote {

	private static final long serialVersionUID = -4012748585344947013L;

	private static final Logger log = Logger.getLogger(OnTheFlyServer.class);

	private enum Status {
		UNCONNECTED, PAUSE, PLAY, STEP;
	}

	private volatile Status status = Status.UNCONNECTED;

	private final Object paused = new Object();

	private final Object stepDone = new Object();

	private final Object updateFinished = new Object();

	private volatile int localTime = 0;

	private OTFServerQuadTree quad;

	private final List<OTFDataWriter<?>> additionalElements= new LinkedList<OTFDataWriter<?>>();

	private EventsManager events;

	private Collection<AbstractQuery> activeQueries = new ArrayList<AbstractQuery>();

	private final ByteBuffer buf = ByteBuffer.allocate(80000000);

	private volatile double stepToTime = 0;

	private VisMobsimFeature otfVisQueueSimFeature;

	private Semaphore accessToQNetwork = new Semaphore(1);

	public OnTheFlyServer(EventsManager events) {
		this.events = events; 
	}

	public static OnTheFlyServer createInstance(EventsManager events) {
		OnTheFlyServer instance = new OnTheFlyServer(events);
		return instance;
	}

	public void reset() {
		status = Status.PAUSE;
		localTime = 0;
		synchronized (paused) {
			paused.notifyAll();
		}
		if(stepToTime != 0) {
			status = Status.STEP;
		} else {
			synchronized (stepDone) {
				stepDone.notifyAll();
			}
		}
	}

	public void updateStatus(double time) {
		localTime = (int) time;
		if (status == Status.STEP) {
			// Time and Iteration reached?
			if (stepToTime <= localTime) {
				synchronized (stepDone) {
					stepDone.notifyAll();
					status = Status.PAUSE;
				}
			}
		}
		if (status == Status.PAUSE) {
			synchronized(paused) {
				try {
					paused.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean requestNewTime(final int time, final TimePreference searchDirection) {
		if( ((searchDirection == TimePreference.RESTART) && (time < localTime))) { 
			doStep(time);
			return true;
		} else if (time < localTime) {
			// if requested time lies in the past, sorry we cannot do that right now
			stepToTime = 0;
			// if forward search is OK, then the actual timestep is the BEST fit
			return (searchDirection != TimePreference.EARLIER);
		} else if (time == localTime) {
			stepToTime = 0;
			return true;
		} else {
			doStep(time);
			return true;
		}
	}

	private void doStep(int stepcounter) {
		// leave Status on pause but let one step run (if one is waiting)
		synchronized(paused) {
			stepToTime = stepcounter;
			status = Status.STEP;
			paused.notifyAll();
		}
		synchronized (stepDone) {
			if (status == Status.PAUSE) return;
			try {
				stepDone.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void pause(){
		synchronized (updateFinished) {
			status = Status.PAUSE;
		}
	}

	@Override
	public void play() {
		synchronized(paused) {
			status = Status.PLAY;
			paused.notifyAll();
		}
	}


	@Override
	public int getLocalTime() {
		return localTime;
	}

	@Override
	public boolean isLive() {
		return true;
	}

	@Override
	public OTFServerQuadTree getQuad(OTFConnectionManager connect) {
		if (quad != null) {
			return quad;
		} else {
			quad = new LiveServerQuadTree(this.otfVisQueueSimFeature.getVisMobsim().getVisNetwork());
			quad.initQuadTree(connect);
			for(OTFDataWriter<?> writer : additionalElements) {
				log.info("Adding additional element: " + writer.getClass().getName());
				quad.addAdditionalElement(writer);
			}
			return quad;
		}
	}

	@Override
	public void toggleShowParking() {
		OTFLinkAgentsHandler.showParked = !OTFLinkAgentsHandler.showParked;
	}

	@Override
	public byte[] getQuadConstStateBuffer() {
		try {
			accessToQNetwork.acquire();
			byte[] result;
			buf.position(0);
			quad.writeConstData(buf);
			int pos = buf.position();
			result = new byte[pos];
			buf.position(0);
			buf.get(result);
			return result;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			accessToQNetwork.release();
		}
	}

	@Override
	public byte[] getQuadDynStateBuffer(final QuadTree.Rect bounds) {
		try {
			accessToQNetwork.acquire();
			byte[] result;
			buf.position(0);
			quad.writeDynData(bounds, buf);
			int pos = buf.position();
			result = new byte[pos];
			buf.position(0);
			buf.get(result);
			return result;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			accessToQNetwork.release();
		}
	}

	@Override
	public OTFQueryRemote answerQuery(AbstractQuery query) {
		query.installQuery(otfVisQueueSimFeature, events, quad);
		activeQueries.add(query);
		return query;
	}

	@Override
	public void removeQueries() {
		for (AbstractQuery query : activeQueries) {
			query.uninstall();
		}
		activeQueries.clear();
	}

	@Override
	public Collection<Double> getTimeSteps() {
		// There are no timesteps implemented here right now, so we return null instead
		return null;
	}

	public void addAdditionalElement(OTFDataWriter<?> element) {
		this.additionalElements.add(element);
	}

	public void setSimulation(VisMobsimFeature otfVisQueueSimFeature) {
		this.otfVisQueueSimFeature = otfVisQueueSimFeature;
	}

	@Override
	public OTFVisConfigGroup getOTFVisConfig() {
		OTFVisConfigGroup otfVisConfig = this.otfVisQueueSimFeature.getVisMobsim().getScenario().getConfig().otfVis();
		if (otfVisConfig == null) {
			otfVisConfig = new OTFVisConfigGroup();
		}
		double effLaneWidth = this.otfVisQueueSimFeature.getVisMobsim().getVisNetwork().getNetwork().getEffectiveLaneWidth() ;
		if ( Double.isNaN(effLaneWidth) ) {
			otfVisConfig.setEffectiveLaneWidth( null ) ;
		} else {
			otfVisConfig.setEffectiveLaneWidth( effLaneWidth ) ;
		}

		return otfVisConfig ;
	}

	public void blockUpdates() {
		try {
			accessToQNetwork.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void unblockUpdates() {
		accessToQNetwork.release();
	}

}