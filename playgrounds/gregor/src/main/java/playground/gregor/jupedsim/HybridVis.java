/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.jupedsim;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.events.EventsReaderXMLv1ExtendedSim2DVersion;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.XYVxVyEventsHandler;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class HybridVis {

	public static void main(String[] args) {
		String jupedsimFile = "/Users/laemmel/arbeit/papers/2015/trgindia2015/hhwsim/input/hybrid_trajectories.txt";
		EventsManagerImpl em = new EventsManagerImpl();

		// VIS only
		Config c = ConfigUtils
				.loadConfig("/Users/laemmel/arbeit/papers/2015/trgindia2015/hhwsim/input/config.xml");
		c.global().setCoordinateSystem("EPSG:3395");
		Scenario sc = ScenarioUtils.loadScenario(c);
		Sim2DConfig conf2d = Sim2DConfigUtils
				.loadConfig("/Users/laemmel/devel/hhw_hybrid/input/s2d_config_v0.3.xml");
		Sim2DScenario sc2d = Sim2DScenarioUtils.loadSim2DScenario(conf2d);
		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);

		EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(sc);
		InfoBox iBox = new InfoBox(dbg, sc);
		dbg.addAdditionalDrawer(iBox);
		QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
		dbg.addAdditionalDrawer(qDbg);
		// em.addHandler(qDbg);

		em.addHandler(dbg);
		em.addHandler(qDbg);

		ReentrantLock lock = new ReentrantLock();
		PriorityQueue<Event> q = new PriorityQueue<>(1000,new Comparator<Event>() {

			@Override
			public int compare(Event o1, Event o2) {
				return o1.getTime() < o2.getTime() ? -1 : 1;
			}
		});

		Worker1 w = new Worker1(q, lock, jupedsimFile);
		Thread t1 = new Thread(w);
		t1.start();

		Worker2 w2 = new Worker2(q,lock,"/Users/laemmel/arbeit/papers/2015/trgindia2015/"
				+ "hhwsim/output/ITERS/it.0/0.events.xml.gz");
		Thread t2 = new Thread(w2);
		t2.start();

		while (true) {
			if (!lock.isHeldByCurrentThread()) {
				lock.lock();
			}
			if (q.size() > 100) {
				Event e = q.poll();
				em.processEvent(e);
			}
			if (q.size() < 1000) {
				lock.unlock();
			}
		}

	}

	private static final class Worker2 implements Runnable {

		private String file;
		private SynchronizedMATSimEventsReader er;
		private ReentrantLock lock;

		public Worker2(PriorityQueue<Event> q, ReentrantLock lock, String file) {
			this.er = new SynchronizedMATSimEventsReader(q, lock);
			this.file = file;
			this.lock = lock;
		}

		@Override
		public void run() {
			this.er.parse(this.file);
			if (this.lock.isHeldByCurrentThread()) {
				this.lock.unlock();
			}

		}


	}

	private static final class Worker1 implements Runnable {

		private TrajectoryParser tr;

		private String file;

		private ReentrantLock lock;

		public Worker1(PriorityQueue<Event> q, ReentrantLock lock, String file) {
			this.tr = new TrajectoryParser(q,lock);
			tr.setValidating(false);
			this.file = file;
			this.lock = lock;

		}

		@Override
		public void run() {
			this.tr.parse(file);
			if (this.lock.isHeldByCurrentThread()) {
				this.lock.unlock();
			}
		}

	}


}
