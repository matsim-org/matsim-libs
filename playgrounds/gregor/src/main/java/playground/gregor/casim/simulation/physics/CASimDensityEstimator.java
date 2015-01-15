/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.casim.simulation.physics;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class CASimDensityEstimator {

	// Laemmel Floetteroed constants
	public static int LOOK_AHEAD = 6;
	/* package */static final int MX_TRAVERSE = 20;

	public static boolean USE_SPH = false;

	private final AbstractCANetwork net;

	private final int nrThreads = 8;
	private final CyclicBarrier barrier = new CyclicBarrier(nrThreads + 1);
	private final Worker[] workers = new Worker[nrThreads];

	private final Set<CALink> safeLinks = new HashSet<>();
	private final CASimDensityEstimatorFactory fac;

	public CASimDensityEstimator(AbstractCANetwork net,
			CASimDensityEstimatorFactory fac) {
		this.net = net;
		this.fac = fac;
		init();
	}

	private void init() {
		for (CALink l : net.getLinks().values()) {
			if (l.getLink().getId().toString().contains("el")) {
				this.safeLinks.add(l); // .getLink().getId()
			}
		}

		for (int i = 0; i < nrThreads; i++) {
			workers[i] = new Worker(barrier,
					this.fac.createCASimDensityEstimator(net));
			Thread t = new Thread(workers[i]);
			t.setDaemon(true);
			t.setName(CASimDensityEstimator.class.toString() + i);
			t.start();
		}

	}

	public void handle(CAMoveableEntity a) {
		this.workers[a.hashCode() % nrThreads].add(a);
	}

	public void await() {
		LastElement le = new LastElement();
		for (Worker w : this.workers) {
			w.add(le);
		}
		try {
			this.barrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}

	}

	private final class Worker implements Runnable {

		private final LinkedBlockingQueue<CAMoveableEntity> queue = new LinkedBlockingQueue<>();
		private CyclicBarrier barrier;
		private CADensityEstimatorKernel k;

		public Worker(CyclicBarrier barrier, CADensityEstimatorKernel k) {
			this.barrier = barrier;
			this.k = k;
		}

		@Override
		public void run() {
			try {
				while (true) {
					CAMoveableEntity e = queue.take();
					if (e instanceof LastElement) {
						this.barrier.await();
						continue;
					} else if (e instanceof ShutdownElement) {
						break;
					}
					double rho = this.k.estRho(e);
					// e.setRho((e.getRho() + rho) / 2);
					e.setRho(rho);

				}
				this.k.report();
			} catch (InterruptedException | BrokenBarrierException e) {
				throw new RuntimeException(e);
			}

		}

		public void add(CAMoveableEntity e) {
			queue.add(e);
		}

	}

	public void shutdown() {
		for (Worker t : this.workers) {
			t.add(new ShutdownElement());
		}

	}

	// TODO the code below is a quick hack for thread management, needs to be
	// fixed!! [GL Nov. '14]

	private static final class ShutdownElement extends CAMoveableEntity {

		@Override
		Id<Link> getNextLinkId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		void moveOverNode(CALink nextLink, double time) {
			// TODO Auto-generated method stub

		}

		@Override
		public Id getId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public CANetworkEntity getCurrentCANetworkEntity() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void moveToNode(CANode n) {
			// TODO Auto-generated method stub

		}

		@Override
		public Link getCurrentLink() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public CANetworkEntity getLastCANetworkEntity() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	private static final class LastElement extends CAMoveableEntity {

		@Override
		Id<Link> getNextLinkId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		void moveOverNode(CALink nextLink, double time) {
			// TODO Auto-generated method stub

		}

		@Override
		public Id getId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public CANetworkEntity getCurrentCANetworkEntity() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void moveToNode(CANode n) {
			// TODO Auto-generated method stub

		}

		@Override
		public Link getCurrentLink() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public CANetworkEntity getLastCANetworkEntity() {
			// TODO Auto-generated method stub
			return null;
		}

	}

}
