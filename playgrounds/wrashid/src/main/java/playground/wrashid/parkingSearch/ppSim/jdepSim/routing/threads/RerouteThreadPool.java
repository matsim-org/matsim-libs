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

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.DebugLib;

import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrix;

public class RerouteThreadPool {

	private RerouteThread[] rerouteThreads;
	int counter=0;
	private CyclicBarrier cyclicBarrier;
	public LinkedList<RerouteTask> rerouteTasks=new LinkedList<RerouteTask>();

	public RerouteThreadPool(int numberOfThreads, TTMatrix ttMatrix, Network network){
		rerouteThreads = new RerouteThread[numberOfThreads];

		cyclicBarrier = new CyclicBarrier(rerouteThreads.length + 1);
		for (int i = 0; i < rerouteThreads.length; i++) {
			rerouteThreads[i] = new RerouteThread(ttMatrix, network, cyclicBarrier);
		}
	}
	
	public void addTask(RerouteTask rt){
		rerouteTasks.add(rt);
		rerouteThreads[counter % rerouteThreads.length].addTask(rt);
		counter++;
	}
	
	public void start(){
		for (int i = 0; i < rerouteThreads.length; i++) {
			rerouteThreads[i].start();
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

		for (int i = 0; i < rerouteThreads.length; i++) {
			for (RerouteTask task : rerouteThreads[i].getTasks()) {
				synchronized (task) {
					DebugLib.emptyFunctionForSettingBreakPoint();
				}
			}
		}
	}

}

