/* *********************************************************************** *
 * project: org.matsim.*
 * OTFHostControl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;



/**
 * @author dgrether
 *
 */
public class OTFHostConnectionManager {

	private String address;

	private OTFServerRemote host = null;

	protected OTFLiveServerRemote liveHost = null;

	protected int controllerStatus = 0;

	private final Map <String,OTFClientQuad> quads = new HashMap<String,OTFClientQuad>();

	private final Map <String,OTFDrawer> drawer = new HashMap<String,OTFDrawer>();

	public OTFHostConnectionManager(String address, OTFServerRemote server) {
		setAddressAndServer(address, server);
	}

	public OTFHostConnectionManager(String url) {
		try {
			this.openAddress(url);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

	public OTFServerRemote getOTFServer(){
		return this.host;
	}

	private void openAddress(final String address) throws RemoteException, InterruptedException, NotBoundException {
		OTFServerRemote createdHost = new OTFHostConnectionBuilder().createRemoteServerConnection(address);
		setAddressAndServer(address, createdHost);
	}

	private void setAddressAndServer(final String address, OTFServerRemote server) {
		this.address = address;
		this.host = server;
		if (host != null) {
			try {
				if (host.isLive()){
					liveHost = (OTFLiveServerRemote)host;
					controllerStatus = liveHost.getControllerStatus();
				}
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public boolean isLiveHost() {
		return liveHost != null;
	}

	public double getTime() {
		try {
			return host.getLocalTime();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public String getAddress() {
		return address;
	}

	public Map<String, OTFClientQuad> getQuads() {
		return quads;
	}

	public Map<String, OTFDrawer> getDrawer() {
		return drawer;
	}

}
