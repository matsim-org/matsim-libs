/* *********************************************************************** *
 * project: org.matsim.*
 * QueueRemoteLink2.java
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

import java.util.List;

import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.Vehicle;
import org.matsim.network.NetworkLayer;

public class QueueRemoteLink2 extends QueueRemoteLink {

	public QueueRemoteLink2(QueueLink link, NetworkLayer network) {
		super(link, network);
		// TODO Auto-generated constructor stub
	}

	/* might be a good start for not having to update every time
	 * returns
	 * **actual number of veh on link from own simulation run (depends on 
	 *   buffer clearance rate)
	 * gets list of veh on the link with departure times
	 * 
	 * this sync needs to be done when eithe a freeTravelTime period is over,
	 * or hasSpace on the not-REMOTE link is false! (the non_REMOTE link
	 * can remove the first #(vehQueue.size() - remote.syncRemoteLinks()) vehs
	 */

	public int syncRemoteLinks( List<Vehicle> vehs ){
		// avoid counting vehicle twice!!
		return vehOnLinkCount(); //or something like this
	}}
