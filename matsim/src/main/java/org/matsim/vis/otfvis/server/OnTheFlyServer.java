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

package org.matsim.vis.otfvis.server;

import java.nio.ByteBuffer;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.vis.otfvis.OTFVisQueueSimFeature;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFServerQuadI;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFQSimServerQuadBuilder;
import org.matsim.vis.otfvis.executables.OTFVisController;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFQueryRemote;
import org.matsim.vis.otfvis.opengl.queries.AbstractQuery;

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
public class OnTheFlyServer extends UnicastRemoteObject implements OTFLiveServerRemote {

	private static class QuadStorage {
		public OTFServerQuad2 quad;
		public byte [] buffer;
		public QuadStorage(String id, OTFServerQuad2 quad, byte[] buffer) {
			this.quad = quad;
			this.buffer = buffer;
		}
	}

	private static final long serialVersionUID = -4012748585344947013L;

	private static final Logger log = Logger.getLogger(OnTheFlyServer.class);
	
	public static final int UNCONNECTED = 0;
	public static final int PAUSE = 1;
	public static final int PLAY = 2;
	public static final int STEP = 3;

	private static Registry registry;

	private int status = UNCONNECTED;

	private final Object paused = new Object();
	private final Object stepDone = new Object();
	private final Object updateFinished = new Object();
	private int localTime = 0;

	private final Map<String, QuadStorage> quads = new HashMap<String, QuadStorage>();
	private final Set<String> updateThis = new HashSet<String>();
	private final List<OTFDataWriter<?>> additionalElements= new LinkedList<OTFDataWriter<?>>();

	private int controllerStatus = OTFVisController.NOCONTROL;
	private int controllerIteration = 0;
	private int stepToIteration = 0;
	private int requestStatus = 0;

	private EventsManager events;

	private OTFQSimServerQuadBuilder quadBuilder;
	
	private Collection<AbstractQuery> activeQueries = new ArrayList<AbstractQuery>();

	private final ByteBuffer buf = ByteBuffer.allocate(20000000);

	private double stepToTime = 0;

	private OTFVisQueueSimFeature otfVisQueueSimFeature;

	private OnTheFlyServer(RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory) throws RemoteException {
		super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
		OTFDataWriter.setServer(this);
	}

	private OnTheFlyServer() throws RemoteException {
		super(0);
	}
	
	private void init(QueueNetwork network, Population population, EventsManager events){
		this.quadBuilder = new OTFQSimServerQuadBuilder(network);
		this.setEvents(events);
	}
	
	
	public static OnTheFlyServer createInstance(String readableName, QueueNetwork network, Population population, EventsManager events, boolean useSSL) {
		registry = getRegistry(useSSL);
		try {
			// Register with RMI to be seen from client
			OnTheFlyServer instance;
			if (useSSL) {
				instance = new OnTheFlyServer(new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
			} else {
				instance = new OnTheFlyServer();
			}
			instance.init(network, population, events);
			registry.bind(readableName, instance);
			log.info("OTFServer bound in RMI registry");
			return instance;
		} catch (AlreadyBoundException e) {
			throw new RuntimeException(e);
		} catch (AccessException e) {
			throw new RuntimeException(e);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	private static Registry getRegistry(boolean useSSL) {
		Registry registry;
		if (useSSL) {
			System.setProperty("javax.net.ssl.keyStore", "input/keystore");
			System.setProperty("javax.net.ssl.keyStorePassword", "vspVSP");
			System.setProperty("javax.net.ssl.trustStore", "input/truststore");
			System.setProperty("javax.net.ssl.trustStorePassword", "vspVSP");
			try {
				registry = LocateRegistry.createRegistry(4019, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		} else {
			try {
				registry = LocateRegistry.getRegistry(4019);
				// THIS Line is important, as this checks, if registry is REALLY
				// connected "late binding"
				registry.list();
			} catch (RemoteException e) {
				try {
					registry = LocateRegistry.createRegistry(4019);
				} catch (RemoteException e1) {
					throw new RuntimeException(e);
				}
			}
		}
		return registry;
	}

	public void reset() {
		status = PAUSE;
		localTime = 0;
		controllerStatus = OTFVisController.RUNNING | controllerIteration;
		stepToIteration = 0;
		requestStatus = 0;
		synchronized (paused) {
			paused.notifyAll();
		}
		if(stepToTime != 0) {
			status = STEP;
		}else 		synchronized (stepDone) {
			stepDone.notifyAll();
		};
	}
	
	public void cleanup() {
		try {
			//Naming.unbind("DSOTFServer_" + UserReadableName);
			UnicastRemoteObject.unexportObject(this, true);
			UnicastRemoteObject.unexportObject(registry, true);
			registry = null;
		} catch (NoSuchObjectException e) {
			e.printStackTrace();
		}
	}

	public int updateStatus(double time) {
		localTime = (int) time;
		if (status == STEP) {
			// Time and Iteration reached?
			if( (stepToIteration <= controllerIteration) && (stepToTime <= localTime) ) {
				synchronized (stepDone) {
					stepDone.notifyAll();
					status = PAUSE;
				}
			}
		}
		if (status == PAUSE) {
			synchronized(paused) {
				try {
					paused.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return status;
	}

	public boolean requestNewTime(int time, final TimePreference searchDirection) throws RemoteException {
		if( ((searchDirection == TimePreference.RESTART) && (time < localTime))){
			requestStatus = OTFVisController.CANCEL;
			doStep(time);
			return true;
		}
		// if requested time lies in the past, sorry we cannot do that right now
		if ((stepToIteration < controllerIteration) || ((stepToIteration == controllerIteration) && (time < localTime)) ) {
			time = localTime;
			stepToTime = 0;
			// if forward search is OK, then the actual timestep is the BEST fit
			return (searchDirection != TimePreference.EARLIER);
		}
		if ((stepToIteration == controllerIteration) && (time == localTime)) {
			stepToTime = 0;
			return true;
		}
		doStep(time);
		return true;
	}

	private void doStep(int stepcounter) {
		// leave Status on pause but let one step run (if one is waiting)
		synchronized(paused) {
			stepToTime = stepcounter;
			status = STEP;
			paused.notifyAll();
		}
		synchronized (stepDone) {
			if (status == PAUSE) return;
			try {
				stepDone.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void pause()  throws RemoteException{
		synchronized (updateFinished) {
			status = PAUSE;
		}
	}

	public void play()  throws RemoteException{
		synchronized(paused) {
			status = PLAY;
			paused.notifyAll();
		}
	}


	public int getLocalTime() throws RemoteException {
		return localTime;
	}

	public boolean isLive() {
		return true;
	}

	public OTFServerQuadI getQuad(String id, OTFConnectionManager connect) throws RemoteException {
		if (quads.containsKey(id)) return quads.get(id).quad;
		OTFServerQuad2 quad = this.quadBuilder.createAndInitOTFServerQuad(connect);
		quad.initQuadTree(connect);
		for(OTFDataWriter<?> writer : additionalElements) {
			log.info("Adding additional element: " + writer.getClass().getName());
			quad.addAdditionalElement(writer);
		}
		quads.put(id, new QuadStorage(id, quad, null));
		OTFDataWriter.setServer(this);
		return quad;
	}

	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		buf.position(0);
		quads.get(id).quad.writeConstData(buf);
		byte [] result;
		synchronized (buf) {
			int pos = buf.position();
			result = new byte[pos];
			buf.position(0);
			buf.get(result);
		}
		return result;
	}

	@Override
	public void toggleShowParking() throws RemoteException {
		OTFLinkAgentsHandler.showParked = !OTFLinkAgentsHandler.showParked;
	}

	public byte[] getQuadDynStateBuffer(String id, QuadTree.Rect bounds) throws RemoteException {
		byte[] result;
		QuadStorage updateQuad = quads.get(id);
		if (status == PAUSE) {
			synchronized (buf) {
				buf.position(0);
				updateQuad.quad.writeDynData(bounds, buf);
				int pos = buf.position();
				result = new byte[pos];
				buf.position(0);
				buf.get(result);
				return result;
			}
		}
		// otherwise == PLAY, we need to sort this into the array of demanding quads and then they will be fillesd next in getStatus is called
		try {
			synchronized (updateFinished) {
				updateThis.add(id);
				updateFinished.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return updateQuad.buffer;
	}

	public OTFQueryRemote answerQuery(AbstractQuery query) throws RemoteException {
		synchronized (updateFinished) {
			OTFServerQuad2 quad = quads.values().iterator().next().quad;
			query.installQuery(otfVisQueueSimFeature, getEvents(), quad);
			activeQueries.add(query);
			OTFQueryRemote stub = (OTFQueryRemote) UnicastRemoteObject.exportObject(query, 0);
			return stub;
		}
	}

	@Override
	public void removeQueries() throws RemoteException {
		for (AbstractQuery query : activeQueries) {
			query.uninstall();
			UnicastRemoteObject.unexportObject(query, true);
		}
		activeQueries.clear();
	}

	public Collection<Double> getTimeSteps() throws RemoteException {
		// There are no timesteps implemented here right now, so we return null instead
		return null;
	}

	public boolean requestControllerStatus(int status) throws RemoteException {
		stepToIteration = status & 0xffffff;
		requestStatus = status & OTFVisController.ALL_FLAGS;
		if(requestStatus == OTFVisController.CANCEL) {
			requestStatus = OTFVisController.CANCEL;
		}
		return true;
	}

	public int getControllerStatus() {
		if ((controllerStatus == OTFVisController.RUNNING) && (status == PAUSE)) 
			return  OTFVisController.RUNNING + OTFVisController.PAUSED;
		return controllerStatus;
	}

	public int getRequestStatus() {
		return requestStatus;
	}

	public void setControllerStatus(int controllerStatus) {
		this.controllerStatus = controllerStatus;
		switch(OTFVisController.getStatus(controllerStatus)) {
		case OTFVisController.STARTUP:
			// controller is starting up
			localTime = -1;
			break;
		case OTFVisController.RUNNING:
			// sim is running
			controllerIteration = OTFVisController.getIteration(controllerStatus);
			break;
		case OTFVisController.REPLANNING:
			// controller is replanning
			localTime = -1;
			break;
		}
	}
	
	public void addAdditionalElement(OTFDataWriter<?> element) {
		this.additionalElements.add(element);
	}

	public void setEvents(EventsManager events) {
		this.events = events;
	}

	public EventsManager getEvents() {
		return events;
	}

	public void setSimulation(OTFVisQueueSimFeature otfVisQueueSimFeature) {
		this.otfVisQueueSimFeature = otfVisQueueSimFeature;
	}
	
}