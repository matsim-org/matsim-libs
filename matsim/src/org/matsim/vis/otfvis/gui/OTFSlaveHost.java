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

public class OTFSlaveHost extends OTFHostControlBar {

	public OTFSlaveHost(String address) throws RemoteException,
			InterruptedException, NotBoundException {
		super(address, false);
	}
	protected void readConstData(OTFClientQuad clientQ) throws RemoteException {
		clientQ.getConstData();
	}
}

