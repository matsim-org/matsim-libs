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

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class CASimDensityEstimator {

	// Laemmel Floetteroed constants
	/* package */static int LOOK_AHEAD = 7;
	/* package */static final int MX_TRAVERSE = 20;

	public static int H = 13;
	private static final int CUTTOFF_DIST = 2 * H;
	private static final boolean USE_SPH = false;

	private final CANetworkDynamic net;

	private final int nrThreads = 8;
	private final CyclicBarrier barrier = new CyclicBarrier(nrThreads + 1);
	private final Worker[] workers = new Worker[nrThreads];

	public CASimDensityEstimator(CANetworkDynamic net) {
		this.net = net;
		init();
	}

	private void init() {
		for (int i = 0; i < nrThreads; i++) {
			workers[i] = new Worker(barrier);
			Thread t = new Thread(workers[i]);
			t.setDaemon(true);
			t.setName(CASimDensityEstimator.class.toString() + i);
			t.start();
		}

	}

	private double estRho(CAMoveableEntity a) {
		if (a.getCurrentCANetworkEntity() instanceof CANodeDynamic
				|| a.getNextLinkId() == null) {
			return a.getRho();
		}
		CALink current = (CALink) a.getCurrentCANetworkEntity();
		CAMoveableEntity[] currentParts = current.getParticles();
		int pos = a.getPos();
		int dir = a.getDir();
		int[] spacings;// = new int []{0,0};
		if (currentParts[pos] != a) {
			// agent on node
			// log.warn("not yet implemented!!");
			return a.getRho();
		} else {

			double rho = bSplinesKernel(0);
			spacings = new int[] { 0, 0 };
			rho += traverseLink(currentParts, dir, pos + dir, spacings, rho);
			// check next node

			if (USE_SPH && spacings[1] < CUTTOFF_DIST || !USE_SPH
					&& spacings[0] < LOOK_AHEAD && spacings[1] < MX_TRAVERSE) {
				// if (spacings[1] < MX_TRAVERSE){
				CANode n;
				if (dir == 1) {
					n = current.getDownstreamCANode();
				} else {
					n = current.getUpstreamCANode();
				}
				spacings[1]++;
				if (n.peekForAgent() != null) {
					spacings[0]++;
				}
				// check next link(s)? TODO it would make sense to check all
				// outgoing links not only the next one ...
				if (USE_SPH && spacings[1] < CUTTOFF_DIST || !USE_SPH
						&& spacings[0] < LOOK_AHEAD
						&& spacings[1] < MX_TRAVERSE) {
					// if (spacings[1] < MX_TRAVERSE){
					CALink next = this.net.getCALink(a.getNextLinkId());
					CANode nn = next.getUpstreamCANode();
					int nextDir;
					int nextPos;
					CAMoveableEntity[] nextParts = next.getParticles();
					if (n == nn) {
						nextDir = 1;
						nextPos = 0;
					} else {
						nextDir = -1;
						nextPos = nextParts.length - 1;
					}
					rho += traverseLink(nextParts, nextDir, nextPos, spacings,
							rho);
				}
			}
			double coeff = (double) spacings[0] / (double) spacings[1];
			double cmp = CANetworkDynamic.RHO_HAT * coeff;
			// rho *= RHO_HAT;
			if (USE_SPH) {
				return rho;
			} else {
				return cmp;
			}
		}
	}

	private double traverseLink(CAMoveableEntity[] parts, int dir, int idx,
			int[] spacings, double rho) {
		if (idx < 0 || idx >= parts.length) {
			return rho;
		}
		int toMx = dir == -1 ? 0 : parts.length - 1;
		for (; idx != toMx; idx += dir) {
			spacings[1]++;
			if (parts[idx] != null) {
				spacings[0]++;
				rho += bSplinesKernel(spacings[1]);
				if (!USE_SPH && spacings[0] >= LOOK_AHEAD) {
					return rho;
				}
			}
			if (USE_SPH && spacings[1] >= CUTTOFF_DIST || !USE_SPH
					&& spacings[1] >= MX_TRAVERSE) {
				return rho;
			}
		}
		return rho;
	}

	private double bSplinesKernel(double r) {
		// r = H-r;
		final double v = r / H;
		final double hCube = Math.pow(H, 3);
		if (v < 0) {
			return 0;
		}
		if (v <= 1) {
			final double term1 = 3. / (4. * Math.PI * Math.pow(H, 3.));
			// final double term1 = 1./6.;
			final double term2 = 10. / 3. - 7 * Math.pow(v, 2) + 4
					* Math.pow(v, 3.);
			// final double term2 = 3*Math.pow(v, 3) - 6*Math.pow(v, 2) + 4;
			return term1 * term2 * hCube;
		} else if (v <= 2) {
			final double term1 = 3. / (4. * Math.PI * Math.pow(H, 3.));
			// final double term1 = 1./6.;
			final double term2 = Math.pow(2 - v, 2) * ((5. - 4. * v) / 3.);
			// final double term2 = - Math.pow(v, 3) + 6.*Math.pow(v, 2) - 12*v
			// +8;
			return term1 * term2 * hCube;
		}

		return 0.;
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

		public Worker(CyclicBarrier barrier) {
			this.barrier = barrier;
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
					double rho = CASimDensityEstimator.this.estRho(e);
					e.setRho((e.getRho() + rho) / 2);

				}
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

	}

}
