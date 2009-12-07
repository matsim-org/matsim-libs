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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
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
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.executables.OTFVisController;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFQuery;

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
public class OnTheFlyServer extends UnicastRemoteObject implements OTFLiveServerRemote{

	private static final long serialVersionUID = -4012748585344947013L;

	private static final Logger log = Logger.getLogger(OnTheFlyServer.class);
	
	public static final int UNCONNECTED = 0;
	public static final int PAUSE = 1;
	public static final int PLAY = 2;
	public static final int STEP = 3;
	private static Registry registry = null;

	private int status = UNCONNECTED;

	protected final Object paused = new Object();
	protected final Object stepDone = new Object();
	protected final Object updateFinished = new Object();
	protected int localTime = 0;

	private final Map<String, QuadStorage> quads = new HashMap<String, QuadStorage>();
	protected final Set<String> updateThis = new HashSet<String>();
	protected final HashMap<OTFQuery,OTFQuery> queryThis = new HashMap<OTFQuery,OTFQuery>();
	private final List<OTFDataWriter> additionalElements= new LinkedList<OTFDataWriter>();

	protected int controllerStatus = OTFVisController.NOCONTROL;
	protected int controllerIteration = 0;
	protected int stepToIteration = 0;
	protected int requestStatus = 0;

//	private final OTFNetHandler handler = null;
	private transient Population pop = null;
	public transient ByteArrayOutputStream out = null;
	public transient QueueNetwork network = null;
	public transient EventsManager events;

	protected OnTheFlyServer(RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory) throws RemoteException {
		super(0, new SslRMIClientSocketFactory(),	new SslRMIServerSocketFactory());
		OTFDataWriter.setServer(this);
	}

	protected OnTheFlyServer() throws RemoteException {
		super(0);
	}
	
	private void init(QueueNetwork network, Population population, EventsManager events){
		this.network = network;
		this.out = new ByteArrayOutputStream(20000000);
		this.pop = population;
		this.events = events;
	}
	
	
	public static OnTheFlyServer createInstance(String ReadableName, QueueNetwork network, Population population, EventsManager events, boolean useSSL) {
		OnTheFlyServer result = null;
		if (useSSL) {
			System.setProperty("javax.net.ssl.keyStore", "input/keystore");
			System.setProperty("javax.net.ssl.keyStorePassword", "vspVSP");
			System.setProperty("javax.net.ssl.trustStore", "input/truststore");
			System.setProperty("javax.net.ssl.trustStorePassword", "vspVSP");
			try {
				registry = LocateRegistry.createRegistry(4019,
						new SslRMIClientSocketFactory(),
						new SslRMIServerSocketFactory());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			try {
				registry = LocateRegistry.getRegistry(4019);
				// THIS Line is important, as this checks, if registry is REALLY connected "late binding"
				/*String[] liste =*/ registry.list();
			} catch (RemoteException e) {
				try {
					registry = LocateRegistry.createRegistry(4019);
				} catch (RemoteException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		// Register with RMI to be seen from client
		try {
			// Create SSL-based registry
			if (useSSL) {
				result = new OnTheFlyServer(new SslRMIClientSocketFactory(),	new SslRMIServerSocketFactory());
			} else {
				result  = new OnTheFlyServer();
			}
      result.init(network, population, events);
			// Bind this object instance to the name ReadableName
			registry.bind(ReadableName, result);

			log.info("OTFServer bound in RMI registry");
		} catch (Exception e) {
			log.error("OTFServer err: " + e.getMessage());
			e.printStackTrace();
		}
		return 	result;
	}

	public void reset() {
		status = PAUSE;
		localTime = 0;
		controllerStatus = OTFVisController.RUNNING | controllerIteration;
		stepToIteration = 0;
		requestStatus = 0;
//		stepToTime = 0;
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

	public void updateOut(double time) {
		for(String id : updateThis) {
			buf.position(0);
			QuadStorage act = quads.get(id);
			act.quad.writeDynData(act.rect, buf);
			act.buffer = new byte[buf.position()];
			buf.position(0);
			buf.get(act.buffer);
		}
		updateThis.clear();
		OTFServerQuad quad = quads.values().iterator().next().quad;
		for(OTFQuery query : queryThis.keySet()) {
			queryThis.put(query, query.query(network, pop, events, quad));
		}
	}


	public int updateStatus(double time) {
		localTime = (int)time;

		if ((updateThis.size() != 0) || (queryThis.size() != 0) ) {
			synchronized (updateFinished) {
				updateOut(time);
				updateFinished.notifyAll();
			}
		}

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


	public void doStep(int stepcounter) {
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

	double stepToTime = 0;
	
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

	public void setQuad(String id, OTFServerQuad quad) {
		quads.put(id, new QuadStorage(id, quad, null, null));
	}
	
	public OTFServerQuad getQuad(String id, OTFConnectionManager connect) throws RemoteException {

		if (quads.containsKey(id)) return quads.get(id).quad;

		OTFServerQuad quad = new OTFServerQuad(network);
		quads.put(id, new QuadStorage(id, quad, null, null));

		OTFDataWriter.setServer(this);

		quad.fillQuadTree(connect);
		
		for(OTFDataWriter writer : additionalElements) quad.addAdditionalElement(writer);
		
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


	private transient final ByteBuffer buf = ByteBuffer.allocate(20000000);

	public byte[] getQuadDynStateBuffer(String id, QuadTree.Rect bounds) throws RemoteException {
		byte[] result;
//		Gbl.startMeasurement();
		QuadStorage updateQuad = quads.get(id);
		updateQuad.rect = bounds;

		if (status == PAUSE) {
			synchronized (buf) {
				buf.position(0);
				updateQuad.quad.writeDynData(bounds, buf);
				int pos = buf.position();
				result = new byte[pos];
				buf.position(0);
				buf.get(result);
//				Gbl.printElapsedTime();
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
//		Gbl.printElapsedTime();

		return updateQuad.buffer;
	}

	public OTFQuery answerQuery(OTFQuery query) throws RemoteException {
		OTFQuery result = null;
		synchronized (updateFinished) {
			OTFServerQuad quad = quads.values().iterator().next().quad;
			result = query.query(network, pop, events, quad);
		}

		return result;
	}

	public Collection<Double> getTimeSteps() throws RemoteException {
		// There are no timesteps implemented here right now, so we return null instead
		return null;
	}

	@SuppressWarnings("unchecked")
	public boolean replace(String id, double x, double y, int index, Class clazz)
			throws RemoteException {
		OTFServerQuad quad = quads.get(id).quad;
		if(quad != null) {
			quad.replace(x, y, index, clazz);
			return true;
		}
		// TODO Auto-generated method stub
		return false;
	}


	public boolean requestControllerStatus(int status) throws RemoteException {
		stepToIteration = status & 0xffffff;
		requestStatus = status & OTFVisController.ALL_FLAGS;
		if(requestStatus == OTFVisController.CANCEL) {
			//reset();
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
	
	public void addAdditionalElement(OTFDataWriter element) {
		this.additionalElements.add(element);
	}
	
	public void replaceQueueNetwork(QueueNetwork newNet) {
		this.network = newNet;
		
		for(QuadStorage quadS : quads.values()){
			quadS.quad.replaceSrc(newNet);
		}
	}

	private static class QuadStorage {
		public String id;
		public OTFServerQuad quad;
		public QuadTree.Rect rect;
		public byte [] buffer;
		public QuadStorage(String id, OTFServerQuad quad, Rect rect, byte[] buffer) {
			this.id = id;
			this.quad = quad;
			this.rect = rect;
			this.buffer = buffer;
		}
	}
	
	
}