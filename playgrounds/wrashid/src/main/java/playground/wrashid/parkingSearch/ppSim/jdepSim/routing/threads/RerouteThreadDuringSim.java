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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.obj.SortableMapObject;
import org.matsim.contrib.transEnergySim.visualization.charging.inductiveAtRoads.LinkEvent;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.DummyTravelDisutility;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.EditRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.TTMatrixBasedTravelTime;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.TollAreaTravelDisutility;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrix;

public class RerouteThreadDuringSim extends Thread {

	private EditRoute editRoute;
	private PriorityQueue<SortableMapObject<RerouteTaskDuringSim>> taskPriorityQueue=new PriorityQueue<SortableMapObject<RerouteTaskDuringSim>>();
	private LinkedList<SortableMapObject<RerouteTaskDuringSim>> tasksList=new LinkedList<SortableMapObject<RerouteTaskDuringSim>>();

	private synchronized void addTask_(RerouteTaskDuringSim rt) {
		tasksList.add(new SortableMapObject<RerouteTaskDuringSim>(rt, rt.getTime()));
		this.notify();
	}

	public RerouteThreadDuringSim(TTMatrix ttMatrix, Network network) {
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
		RerouteTaskDuringSim currentTask=null;
		while (true){
			synchronized (this) {
				taskPriorityQueue.addAll(tasksList);
				tasksList.clear();
				if (taskPriorityQueue.size()==0){
					try {
						this.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					currentTask=taskPriorityQueue.poll().getKey();
				}
			}
			
			if (currentTask!=null){
				currentTask.perform(editRoute);
			}
		}
	}
	
	
	public static void addTask(RerouteTaskDuringSim rt) {
		ZHScenarioGlobal.rerouteThreadsDuringSim[RerouteTaskDuringSim.getTaskCounter() % ZHScenarioGlobal.numberOfRoutingThreadsDuringSim].addTask_(rt);
	}

}

