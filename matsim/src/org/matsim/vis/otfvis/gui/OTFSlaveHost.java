/* *********************************************************************** *
 * project: org.matsim.*
 * OTFSlaveHost.java
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

import org.matsim.vis.otfvis.data.OTFClientQuad;

/**
 * An OTFSlaveHost is like an OTFHostControlBar, therefore responsible for the connection to a particular server.
 * But it lacks a GUI and is added to another instance of OTFHostControlbar  via the addSlave() method. It will be in time sync wioth the parent Host.
 * This is used in OTFDoubleMVI to display two movies in synch.
 * 
 * @author dstrippgen
 *
 */
public class OTFSlaveHost extends OTFHostControlBar {

	public OTFSlaveHost(String address) throws RemoteException,
			InterruptedException, NotBoundException {
		super(address, false);
	}
	protected void readConstData(OTFClientQuad clientQ) throws RemoteException {
		clientQ.getConstData();
	}
}

