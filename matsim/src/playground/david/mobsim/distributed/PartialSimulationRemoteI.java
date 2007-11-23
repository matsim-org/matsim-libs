/* *********************************************************************** *
 * project: org.matsim.*
 * PartialSimulationRemoteI.java
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
import java.util.List;

public interface PartialSimulationRemoteI extends Remote {

	public void initPartition(int ID, String partfilename) throws RemoteException;
	public void createVehicle(String driverID, List actLegs) throws RemoteException;
	public boolean isActive() throws RemoteException;
	public void exit() throws RemoteException;
}
