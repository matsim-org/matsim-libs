/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyServer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.david.vis;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.utils.collections.QuadTree;

import playground.david.vis.data.OTFNetWriterFactory;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.interfaces.OTFNetHandler;
import playground.david.vis.interfaces.OTFServerRemote;

public class OnTheFlyServer extends UnicastRemoteObject implements OTFServerRemote{

	public static final int UNCONNECTED = 0;
	public static final int PAUSE = 1;
	public static final int PLAY = 2;
	public static final int STEP = 3;
	private static Registry registry = null;

	private final String UserReadableName;
	private int status = UNCONNECTED;

	private final Object paused = new Object();
	private final Object stepDone = new Object();
	private final Object updateFinished = new Object();
	public boolean updateState = false;
	private int localTime = 0;

	private OTFVisNet net = null;
	private final Map<String, OTFServerQuad> quads = new HashMap<String, OTFServerQuad>();
	
	private OTFNetHandler handler = null;
	private Plans pop = null;
	public ByteArrayOutputStream out = null;
	private QueueNetworkLayer network = null;


	protected OnTheFlyServer(String ReadableName, QueueNetworkLayer network, Plans population) throws RemoteException {
		super(4020,new SslRMIClientSocketFactory(),
			      new SslRMIServerSocketFactory());
		//setDaemon(true);
		UserReadableName = ReadableName;
		net = new OTFVisNet(network);
		this.network = network;
		out = new ByteArrayOutputStream(20000000);
		this.pop = population;
	}

	public static OnTheFlyServer createInstance(String ReadableName, QueueNetworkLayer network, Plans population) {
		OnTheFlyServer result = null;
		System.setProperty("javax.net.ssl.keyStore", "input/keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "vspVSP");
		System.setProperty("javax.net.ssl.trustStore", "input/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "vspVSP");

		// Register with RMI to be seen from client
		try {
		    // Create SSL-based registry
		    registry = LocateRegistry.createRegistry(4019,
			new SslRMIClientSocketFactory(),
			new SslRMIServerSocketFactory());

		    result = new OnTheFlyServer(ReadableName, network, population);

		    // Bind this object instance to the name "HelloServer"
		    registry.bind("DSOTFServer_" + ReadableName, result);

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized void setStatus(int status)  throws RemoteException{
		this.status = status;
	}
	private double lastTime = -1.0;
	public void updateOut(double time) {
		if (lastTime != time) {
	try {
			out.reset();
			net.writeMyself(handler, new DataOutputStream(out));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		lastTime = time;
	};

	public int getStatus(double time){
		localTime = (int)time;

		if (updateState) {
			synchronized (out) {
				updateOut(time);
			}
			synchronized (updateFinished) {
				updateState = false;
				updateFinished.notifyAll();
			}
		}

		if (status == STEP) {
			synchronized (stepDone) {
				stepDone.notifyAll();
				status = PAUSE;
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

	public void step()  throws RemoteException{
		// leave Status on pause but let one step run (if one is waiting)
			synchronized(paused) {
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

	public void play()  throws RemoteException{
		synchronized(paused) {
			status = PLAY;
			paused.notifyAll();
		}
	}

	public void pause()  throws RemoteException{
		status = PAUSE;
	}

	public  byte[] getStateBuffer() throws RemoteException {
		updateState = true;
		if (status == PAUSE) step();

		if (updateState) {
			try {
				synchronized (updateFinished) {
					updateFinished.wait();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		byte [] result;
		synchronized (out) {
			result = out.toByteArray();
		}
		return result;
	}

	public OTFVisNet getNet(OTFNetHandler handler) throws RemoteException {
		this.handler = handler;
		net.handler = handler;

		return net;
	}


	public int getLocalTime() throws RemoteException {
		return localTime;
	}

	public boolean isLive() {
		return true;
	}

	public Plan getAgentPlan(String id) throws RemoteException {
		if (id.length()==0)return null;
		Plan plan = pop.getPerson(id).getSelectedPlan();
		return plan;
	}

	public OTFServerQuad getQuad(String id, OTFNetWriterFactory writers) throws RemoteException {

		if (quads.containsKey(id)) return quads.get(id);
		
		OTFServerQuad quad = new OTFServerQuad(network);
		quads.put(id, quad);
		
		quad.fillQuadTree(writers);
		return quad;
	}

	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		out.reset();
		quads.get(id).writeConstData(new DataOutputStream(out));
		byte [] result;
		synchronized (out) {
			result = out.toByteArray();
		}
		return result;
	}	
	public byte[] getQuadDynStateBuffer(String id, QuadTree.Rect bounds) throws RemoteException {
		out.reset();
		quads.get(id).writeDynData(bounds, new DataOutputStream(out));
		byte [] result;
		synchronized (out) {
			result = out.toByteArray();
		}
		return result;
	}
}