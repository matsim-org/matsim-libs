/* *********************************************************************** *
 * project: org.matsim.*
 * OTFServerRemote.java
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

package playground.david.vis.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.matsim.plans.Plan;

import playground.david.vis.OTFVisNet;
import playground.david.vis.data.OTFNetWriterFactory;
import playground.david.vis.data.OTFServerQuad;

public interface OTFServerRemote extends Remote {
	public void setStatus(int status) throws RemoteException;;
	public void step() throws RemoteException;;
	public void play() throws RemoteException;;
	public void pause() throws RemoteException;
	public byte[] getStateBuffer() throws RemoteException;
	public OTFVisNet getNet(OTFNetHandler handler) throws RemoteException;
	public OTFServerQuad getQuad(OTFNetWriterFactory writers) throws RemoteException;
	public int getLocalTime() throws RemoteException;
	public Plan getAgentPlan(String id) throws RemoteException;
	public byte[] getQuadConstStateBuffer() throws RemoteException;
	public byte[] getQuadDynStateBuffer() throws RemoteException;
}

