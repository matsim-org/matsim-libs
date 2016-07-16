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

import java.util.LinkedList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.EditRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.TTMatrixBasedTravelTime;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.TollAreaTravelDisutility;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrix;

public class RerouteThread extends Thread {
	private EditRoute editRoute;
	private TTMatrix ttMatrix;
	private Network network;
	private LinkedList<RerouteTask> tasks = new LinkedList<RerouteTask>();
	private CyclicBarrier cyclicBarrier;

	public void addTask(RerouteTask rt) {
		getTasks().add(rt);
	}

	public RerouteThread(TTMatrix ttMatrix, Network network, CyclicBarrier cyclicBarrier) {
		this.cyclicBarrier = cyclicBarrier;
		
		if (ZHScenarioGlobal
				.loadDoubleParam("radiusTolledArea")>0){
			
			TravelDisutility travelCost=new TollAreaTravelDisutility();
			TravelTime travelTime=new TTMatrixBasedTravelTime(ttMatrix);
			Dijkstra routingAlgo = new Dijkstra(network, travelCost,travelTime);
			
			this.editRoute = new EditRoute(ttMatrix, network,routingAlgo);
		} else {
			this.editRoute = new EditRoute(ttMatrix, network);
		}
	}

	public void run() {
		for (RerouteTask task:getTasks()){
			task.perform(editRoute);
		}
		
		try {
			cyclicBarrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public LinkedList<RerouteTask> getTasks() {
		return tasks;
	}

	public void setTasks(LinkedList<RerouteTask> tasks) {
		this.tasks = tasks;
	}
}
