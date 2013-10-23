/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.EditRoute;

public class RerouteTaskDuringSim {

	boolean done=false;
	private double time;
	private Id startLinkId;
	private Leg leg;
	
	private static int taskCounter=0;
	
	
	public double getTime() {
		return time;
	}
	public Id getStartLinkId() {
		return startLinkId;
	}
	public Leg getLeg() {
		return leg;
	}
	public RerouteTaskDuringSim(double time, Id startLinkId, Leg leg) {
		super();
		this.time = time;
		this.startLinkId = startLinkId;
		this.leg = leg;
		taskCounter++;
	}
	

	public synchronized void waitUntilDone(){
		if (!done){
			try {
				this.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void perform(EditRoute editRoute) {
		LinkNetworkRouteImpl addInitialPartToRoute = editRoute.addInitialPartToRoute(time, startLinkId, leg);
		
		leg.setRoute(addInitialPartToRoute);
		
		done=true;
		this.notify();
	}
	public static int getTaskCounter() {
		return taskCounter;
	}
	public static void setTaskCounter(int taskCounter) {
		RerouteTaskDuringSim.taskCounter = taskCounter;
	}
}

