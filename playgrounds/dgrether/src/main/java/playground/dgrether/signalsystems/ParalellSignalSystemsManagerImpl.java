/* *********************************************************************** *
 * project: org.matsim.*
 * ParalellSignalSystemsManagerImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.matsim.signalsystems.model.AmberLogic;
import org.matsim.signalsystems.model.SignalSystem;
import org.matsim.signalsystems.model.SignalSystemsManager;
import org.matsim.signalsystems.model.SignalSystemsManagerImpl;


/**
 * @author dgrether
 *
 */
public class ParalellSignalSystemsManagerImpl extends SignalSystemsManagerImpl implements SignalSystemsManager {

	//TODO
	private int numberOfThreads = 2;
	
	private List<Worker> workers = new Vector<Worker>();
	
	public ParalellSignalSystemsManagerImpl(){
		for (int i = 0; i < numberOfThreads; i++) {
			Worker w = new Worker();
			w.start();
			this.workers.add(w);
		}
		
	}
	

	@Override
	public void requestControlUpdate(double time_sec) {
		int systemsPerThread = this.getSignalSystems().size() / this.workers.size();
		int currentThreadLoad = 0;
		List<SignalSystem> systemsList = new ArrayList<SignalSystem>();
		systemsList.addAll(this.getSignalSystems().values());
		Iterator<Worker> workerIterator = this.workers.iterator();
		Worker currentWorker = workerIterator.next();
		currentWorker.setTime(time_sec);
		for (SignalSystem ss : systemsList){
			if (currentThreadLoad % systemsPerThread == 0){
				currentWorker = workerIterator.next();
				currentWorker.setTime(time_sec);
			}
			currentThreadLoad++;
			currentWorker.addSignalSystem(ss);
		}
		for (Worker w : this.workers){
			try {
				w.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public synchronized AmberLogic getAmberLogic() {
		return super.getAmberLogic();
	}

	private static final class Worker extends Thread{

		private List<SignalSystem> signalSystems;
		
		private double time;
		
		@Override
		public void run() {
			Iterator<SignalSystem> it = signalSystems.iterator();
			while (it.hasNext()){
				SignalSystem ss = it.next();
				ss.updateState(this.time);
			}
		}

		public void addSignalSystem(SignalSystem ss) {
			if (signalSystems == null){
				signalSystems = new LinkedList<SignalSystem>();
			}
			signalSystems.add(ss);
		}

		public void setTime(double time) {
			this.time = time;
		}
		
	};
	
}
