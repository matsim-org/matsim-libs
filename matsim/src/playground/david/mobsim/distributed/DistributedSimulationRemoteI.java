/* *********************************************************************** *
 * project: org.matsim.*
 * DistributedSimulationRemoteI.java
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

package playground.david.mobsim.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.events.BasicEvent;
import org.matsim.mobsim.Vehicle;

public interface DistributedSimulationRemoteI extends Remote {
	public void signalSimStepDone(int partID, int living) throws RemoteException;
	public void incActivePartSims(int partID) throws RemoteException;
	public void decActivePartSims(int partID) throws RemoteException;
	public Map getPartIDIP() throws RemoteException;
	public void add(Vehicle veh) throws RemoteException;
	public void setLocalInitialTimeStep(int time) throws RemoteException;
	public int getGlobalInitialTimeStep() throws RemoteException;
	public ArrayList getPartitionTable() throws RemoteException;
	public void add(BasicEvent event) throws RemoteException;
	public void addEventList(LinkedList<BasicEvent> eventlist, int lastTime, int partID) throws RemoteException ;

}
