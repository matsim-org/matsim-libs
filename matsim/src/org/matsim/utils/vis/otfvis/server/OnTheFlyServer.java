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

package org.matsim.utils.vis.otfvis.server;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.matsim.events.Events;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.population.Plans;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.vis.otfvis.data.OTFDataWriter;
import org.matsim.utils.vis.otfvis.data.OTFNetWriterFactory;
import org.matsim.utils.vis.otfvis.data.OTFServerQuad;
import org.matsim.utils.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.utils.vis.otfvis.interfaces.OTFNetHandler;
import org.matsim.utils.vis.otfvis.interfaces.OTFQuery;


public class OnTheFlyServer extends UnicastRemoteObject implements OTFLiveServerRemote{

	private static final long serialVersionUID = -4012748585344947013L;

	static class QuadStorage {
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

	public static final int UNCONNECTED = 0;
	public static final int PAUSE = 1;
	public static final int PLAY = 2;
	public static final int STEP = 3;
	private static Registry registry = null;

	private final String userReadableName;
	private int status = UNCONNECTED;

	private final Object paused = new Object();
	private final Object stepDone = new Object();
	private final Object updateFinished = new Object();
	private int localTime = 0;

	private final Map<String, QuadStorage> quads = new HashMap<String, QuadStorage>();
	private final Set<String> updateThis = new HashSet<String>();
	private final Set<OTFQuery> queryThis = new HashSet<OTFQuery>();

	private final OTFNetHandler handler = null;
	private transient Plans pop = null;
	public transient ByteArrayOutputStream out = null;
	public transient QueueNetworkLayer network = null;
	public transient Events events;

	protected OnTheFlyServer(String ReadableName, QueueNetworkLayer network, Plans population, Events events) throws RemoteException {
		super(4019, new SslRMIClientSocketFactory(),	new SslRMIServerSocketFactory());
		this.userReadableName = ReadableName;
		this.network = network;
		this.out = new ByteArrayOutputStream(20000000);
		this.pop = population;
		this.events = events;
		OTFDataWriter.setServer(this);
	}

	protected OnTheFlyServer(String ReadableName, QueueNetworkLayer network, Plans population, Events events, boolean noSSL) throws RemoteException {
		super(4019);
		this.userReadableName = ReadableName;
		this.network = network;
		this.out = new ByteArrayOutputStream(20000000);
		this.pop = population;
		this.events = events;
	}

	public static boolean useSSL = true;
	
	public static OnTheFlyServer createInstance(String ReadableName, QueueNetworkLayer network, Plans population, Events events, boolean useSSL) {
		OnTheFlyServer result = null;
		
		OnTheFlyServer.useSSL = useSSL;
		
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
				registry = LocateRegistry.createRegistry(4019);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		// Register with RMI to be seen from client
		try {
			// Create SSL-based registry
			if (useSSL) {
				result = new OnTheFlyServer(ReadableName, network, population,events);
			} else {
				result  = new OnTheFlyServer(ReadableName, network, population, events, true);
			}

			// Bind this object instance to the name "HelloServer"
			registry.bind("DSOTFServer_" + ReadableName, result);

			// THIS Line is important, as this checks, if registry is REALLY connected "late binding"
			String[] liste = registry.list();
			System.out.println("OTFServer bound in registry");
		} catch (Exception e) {
			System.out.println("OTFServer err: " + e.getMessage());
			e.printStackTrace();
		}
		return 	result;
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

	private double lastTime = -1.0;
	public void updateOut(double time) {
		
		for(String id : updateThis) {
			buf.position(0);
			QuadStorage act = quads.get(id);
			act.quad.writeDynData(act.rect, buf);
			act.buffer = buf.array();
		}
		updateThis.clear();

		OTFServerQuad quad = quads.values().iterator().next().quad;
		
		for(OTFQuery query : queryThis) {
			query.query(network, pop, events, quad);
		}
		queryThis.clear();
		
		lastTime = time;
	}

	public int updateStatus(double time) {
		localTime = (int)time;

		if (updateThis.size() != 0 || queryThis.size() != 0 ) {
			synchronized (updateFinished) {
				updateOut(time);
				updateFinished.notifyAll();
			}
		}

		if (status == STEP) {
			stepToTime--;
			if( stepToTime <= 0) {
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
	
	public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
		// if requested time lies in the past, sorry we cannot do that right now
		if (time < localTime) {
			time = localTime;
			stepToTime = 0;
			// if forward search is OK, then the actual timestep is the BEST fit
			if (searchDirection == TimePreference.EARLIER) return false;
			else return true; 
		}
		if (time == localTime) return true;

		doStep(time - localTime);
		return true;
	}

	public void pause()  throws RemoteException{
		status = PAUSE;
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

	public OTFServerQuad getQuad(String id, OTFNetWriterFactory writers) throws RemoteException {

		if (quads.containsKey(id)) return quads.get(id).quad;

		OTFServerQuad quad = new OTFServerQuad(network);
		quads.put(id, new QuadStorage(id, quad, null, null));

		OTFDataWriter.setServer(this);

		quad.fillQuadTree(writers);
		return quad;
	}

	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		buf.position(0);
		quads.get(id).quad.writeConstData(buf);
		byte [] result;
		synchronized (buf) {
			result = buf.array();
		}
		return result;
	}


	private transient final ByteBuffer buf = ByteBuffer.allocate(20000000);

	public byte[] getQuadDynStateBuffer(String id, QuadTree.Rect bounds) throws RemoteException {
		QuadStorage updateQuad = quads.get(id);
		updateQuad.rect = bounds;

		if (status == PAUSE) {
			synchronized (buf) {
				buf.position(0);
				updateQuad.quad.writeDynData(bounds, buf);
				return buf.array();
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

	public OTFQuery answerQuery(org.matsim.utils.vis.otfvis.interfaces.OTFQuery query) throws RemoteException {
		if (status == PAUSE) {
			OTFServerQuad quad = quads.values().iterator().next().quad;
			query.query(network, pop, events, quad);
		} else {
			// otherwise == PLAY, we need to sort this into the array of demanding queries and then they will be answered next when updateStatus() is called
			try {
				synchronized (updateFinished) {
					queryThis.add(query);
					updateFinished.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return query;
	}

	public Collection<Double> getTimeSteps() throws RemoteException {
		// There are no timesteps implemented here right now, so we return null instead
		return null;
	}
}